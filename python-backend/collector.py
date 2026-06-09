"""系统状态采集器 — CPU / 内存 / 磁盘 / 网络 / Docker"""

import json
import subprocess
import time
from datetime import datetime

import psutil

from models import (
    CPUInfo, MemoryInfo, DiskInfo, NetInfo, LoadInfo,
    HardwareStatus, DockerStatus, DockerInfo, ContainerInfo, ImageInfo, ServerStatus,
)


class SystemCollector:
    """本机硬件信息采集器"""

    def collect(self) -> HardwareStatus:
        hw = HardwareStatus()
        hw.cpu = self._cpu()
        hw.memory = self._memory()
        hw.disks = self._disks()
        hw.network = self._network()
        hw.load = self._load()
        hw.uptime = self._uptime()
        hw.hostname = self._hostname()
        return hw

    def _cpu(self) -> CPUInfo:
        return CPUInfo(
            percent=round(psutil.cpu_percent(interval=0.5), 1),
            cores=psutil.cpu_count(logical=True) or 0,
        )

    def _memory(self) -> MemoryInfo:
        m = psutil.virtual_memory()
        return MemoryInfo(
            total_gb=round(m.total / (1024**3), 1),
            used_gb=round(m.used / (1024**3), 1),
            free_gb=round(m.free / (1024**3), 1),
            used_percent=round(m.percent, 1),
        )

    def _disks(self) -> list[DiskInfo]:
        disks = []
        for p in psutil.disk_partitions():
            if p.fstype in ("tmpfs", "devtmpfs", "squashfs", "overlay", "proc", "sysfs", "cgroup", "efivarfs"):
                continue
            try:
                u = psutil.disk_usage(p.mountpoint)
                disks.append(DiskInfo(
                    filesystem=p.fstype,
                    mount_point=p.mountpoint,
                    total_gb=round(u.total / (1024**3), 1),
                    used_gb=round(u.used / (1024**3), 1),
                    free_gb=round(u.free / (1024**3), 1),
                    used_percent=round(u.percent, 1),
                ))
            except PermissionError:
                continue
        return disks

    def _network(self) -> list[NetInfo]:
        nets = []
        for name, stats in psutil.net_io_counters(pernic=True).items():
            nets.append(NetInfo(
                interface=name,
                rx_bytes=stats.bytes_recv,
                tx_bytes=stats.bytes_sent,
            ))
        return nets

    def _load(self) -> LoadInfo:
        try:
            l = psutil.getloadavg()
            return LoadInfo(load_1=round(l[0], 2), load_5=round(l[1], 2), load_15=round(l[2], 2))
        except OSError:
            return LoadInfo()

    def _uptime(self) -> str:
        boot = psutil.boot_time()
        secs = time.time() - boot
        d, r = divmod(int(secs), 86400)
        h, m = divmod(r, 3600)
        m //= 60
        if d > 0:
            return f"{d}天{h}小时{m}分钟"
        if h > 0:
            return f"{h}小时{m}分钟"
        return f"{m}分钟"

    def _hostname(self) -> str:
        import socket
        return socket.gethostname()


class DockerCollector:
    """Docker 状态采集器（通过 CLI）"""

    def is_available(self) -> bool:
        try:
            r = subprocess.run(["docker", "info", "--format", "{{.ServerVersion}}"],
                               capture_output=True, text=True, timeout=5)
            return r.returncode == 0 and len(r.stdout.strip()) > 0
        except Exception:
            return False

    def collect(self) -> DockerStatus:
        ds = DockerStatus()
        ds.info = self._info()
        ds.containers = self._containers()
        self._merge_stats(ds.containers)
        ds.images = self._images()
        return ds

    def _run(self, *args: str) -> str:
        r = subprocess.run(["docker", *args], capture_output=True, encoding="utf-8",
                         errors="replace", timeout=10)
        return r.stdout.strip()

    def _info(self) -> DockerInfo:
        out = self._run("info", "--format", "{{json .}}")
        if not out:
            return DockerInfo()
        try:
            d = json.loads(out)
            return DockerInfo(
                containers_total=d.get("Containers", 0),
                containers_running=d.get("ContainersRunning", 0),
                containers_paused=d.get("ContainersPaused", 0),
                containers_stopped=d.get("ContainersStopped", 0),
                server_version=d.get("ServerVersion", ""),
            )
        except Exception:
            return DockerInfo()

    def _containers(self) -> list[ContainerInfo]:
        out = self._run("ps", "-a", "--format", "{{json .}}")
        if not out:
            return []
        containers = []
        for line in out.split("\n"):
            line = line.strip()
            if not line:
                continue
            try:
                d = json.loads(line)
                cid = d.get("ID", "")
                if len(cid) > 12:
                    cid = cid[:12]
                containers.append(ContainerInfo(
                    id=cid,
                    name=d.get("Names", ""),
                    image=d.get("Image", ""),
                    status=d.get("Status", ""),
                    state=d.get("State", ""),
                    ports=d.get("Ports", ""),
                    created_at=d.get("CreatedAt", ""),
                ))
            except json.JSONDecodeError:
                continue
        return containers

    def _merge_stats(self, containers: list[ContainerInfo]):
        if not containers:
            return
        out = self._run("stats", "--no-stream", "--format", "{{json .}}")
        if not out:
            return
        stats_map = {}
        for line in out.split("\n"):
            line = line.strip()
            if not line:
                continue
            try:
                d = json.loads(line)
                cid = d.get("ID", "")
                if len(cid) > 12:
                    cid = cid[:12]
                mu = self._split2(d.get("MemUsage", ""), "/")
                ni = self._split2(d.get("NetIO", ""), "/")
                stats_map[cid] = ContainerInfo(
                    cpu_percent=self._parse_pct(d.get("CPUPerc", "")),
                    mem_percent=self._parse_pct(d.get("MemPerc", "")),
                    mem_usage=mu[0],
                    mem_limit=mu[1],
                    network_input=self._parse_bytes(ni[0]),
                    network_output=self._parse_bytes(ni[1]),
                )
            except json.JSONDecodeError:
                continue
        for i, c in enumerate(containers):
            if c.id in stats_map:
                s = stats_map[c.id]
                containers[i].cpu_percent = s.cpu_percent
                containers[i].mem_percent = s.mem_percent
                containers[i].mem_usage = s.mem_usage
                containers[i].mem_limit = s.mem_limit
                containers[i].network_input = s.network_input
                containers[i].network_output = s.network_output

    def _images(self) -> list[ImageInfo]:
        out = self._run("images", "--format", "{{json .}}")
        if not out:
            return []
        images = []
        for line in out.split("\n"):
            line = line.strip()
            if not line:
                continue
            try:
                d = json.loads(line)
                repo = d.get("Repository", "")
                tag = d.get("Tag", "")
                repo_tag = f"{repo}:{tag}" if tag else repo
                iid = d.get("ID", "")
                if len(iid) > 19:
                    iid = iid[7:19]
                images.append(ImageInfo(
                    repo_tag=repo_tag, id=iid,
                    size=d.get("Size", ""),
                    created_at=d.get("CreatedAt", ""),
                ))
            except json.JSONDecodeError:
                continue
        return images

    @staticmethod
    def _split2(s: str, sep: str) -> tuple[str, str]:
        parts = s.split(sep, 1)
        if len(parts) == 2:
            return parts[0].strip(), parts[1].strip()
        return s, ""

    @staticmethod
    def _parse_pct(s: str) -> float:
        s = s.strip().replace("%", "").replace(",", ".")
        if not s:
            return 0.0
        try:
            return round(float(s), 1)
        except ValueError:
            return 0.0

    @staticmethod
    def _parse_bytes(s: str) -> int:
        s = s.strip()
        if not s or s == "0B":
            return 0
        units = {"B": 1, "K": 1024, "KB": 1024, "M": 1024**2, "MB": 1024**2,
                 "G": 1024**3, "GB": 1024**3, "T": 1024**4, "TB": 1024**4}
        for i, c in enumerate(s):
            if c not in "0123456789.,":
                try:
                    val = float(s[:i].replace(",", "."))
                    unit = s[i:].strip().upper()
                    return int(val * units.get(unit, 1))
                except (ValueError, IndexError):
                    return 0
        return 0


def collect_server_status() -> ServerStatus:
    """采集一次完整状态"""
    sys_col = SystemCollector()
    docker_col = DockerCollector()
    hw = sys_col.collect()
    ss = ServerStatus(
        name=hw.hostname or "本机",
        host="localhost",
        online=True,
        type="local",
        hardware=hw,
        collected_at=int(time.time() * 1000),
    )
    if docker_col.is_available():
        try:
            ss.docker = docker_col.collect()
        except Exception:
            pass
    return ss
