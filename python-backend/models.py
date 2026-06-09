"""数据模型 — 与 Go 后端完全兼容的 JSON 结构"""

from typing import Optional
from pydantic import BaseModel


class CPUInfo(BaseModel):
    percent: float = 0
    cores: int = 0


class MemoryInfo(BaseModel):
    total_gb: float = 0
    used_gb: float = 0
    free_gb: float = 0
    used_percent: float = 0


class DiskInfo(BaseModel):
    filesystem: str = ""
    mount_point: str = ""
    total_gb: float = 0
    used_gb: float = 0
    free_gb: float = 0
    used_percent: float = 0


class NetInfo(BaseModel):
    interface: str = ""
    rx_bytes: int = 0
    tx_bytes: int = 0


class LoadInfo(BaseModel):
    load_1: float = 0
    load_5: float = 0
    load_15: float = 0


class HardwareStatus(BaseModel):
    cpu: CPUInfo = CPUInfo()
    memory: MemoryInfo = MemoryInfo()
    disks: list[DiskInfo] = []
    network: list[NetInfo] = []
    load: LoadInfo = LoadInfo()
    uptime: str = ""
    hostname: str = ""


class DockerInfo(BaseModel):
    containers_total: int = 0
    containers_running: int = 0
    containers_paused: int = 0
    containers_stopped: int = 0
    version: str = ""
    server_version: str = ""


class ContainerInfo(BaseModel):
    id: str = ""
    name: str = ""
    image: str = ""
    status: str = ""
    state: str = ""
    created_at: str = ""
    ports: str = ""
    cpu_percent: float = 0
    mem_percent: float = 0
    mem_usage: str = ""
    mem_limit: str = ""
    network_input: int = 0
    network_output: int = 0


class ImageInfo(BaseModel):
    repo_tag: str = ""
    size: str = ""
    created_at: str = ""
    id: str = ""


class DockerStatus(BaseModel):
    info: DockerInfo = DockerInfo()
    containers: list[ContainerInfo] = []
    images: list[ImageInfo] = []


class ServerStatus(BaseModel):
    name: str = ""
    host: str = ""
    online: bool = True
    type: str = "local"
    hardware: Optional[HardwareStatus] = None
    docker: Optional[DockerStatus] = None
    collected_at: int = 0
    error: Optional[str] = None


class AllServersStatus(BaseModel):
    servers: list[ServerStatus] = []
    updated: int = 0
