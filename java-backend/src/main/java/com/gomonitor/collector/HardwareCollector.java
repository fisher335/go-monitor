package com.gomonitor.collector;

import com.gomonitor.model.ServerStatus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/** 硬件信息采集器 */
public class HardwareCollector {

    private static final Logger log = LoggerFactory.getLogger(HardwareCollector.class);

    private final SSHClient client;

    public HardwareCollector(SSHClient client) {
        this.client = client;
    }

    /** 采集一次硬件状态 */
    public HardwareStatus collect() {
        HardwareStatus status = new HardwareStatus();

        try {
            String[] commands = {
                "cat /proc/stat | grep '^cpu '",
                "sleep 0.8 && cat /proc/stat | grep '^cpu '",
                "awk '/MemTotal/{t=$2} /MemFree/{f=$2} /MemAvailable/{a=$2} END{printf \"MEM:%d:%d:%d:%d\",t,t-f,f,a}' /proc/meminfo",
                "df -B1 --exclude-type=tmpfs --exclude-type=devtmpfs 2>/dev/null | awk 'NR>1{print \"DISK:\"$1\"|\"$6\"|\"$2\"|\"$3\"|\"$4}'",
                "cat /proc/net/dev | awk 'NR>2{gsub(/:/,\"\",$1); print \"NET:\"$1\"|\"$2\"|\"$10}'",
                "cat /proc/loadavg",
                "awk '{print $1}' /proc/uptime",
                "nproc"
            };

            Map<String, String> results = client.execMulti(commands);

            safeParse("CPU", () -> parseCPU(results, status));
            safeParse("内存", () -> parseMemory(results, status));
            safeParse("磁盘", () -> parseDisk(results, status));
            safeParse("网络", () -> parseNetwork(results, status));
            safeParse("负载", () -> parseLoad(results, status));
            safeParse("运行时间", () -> parseUptime(results, status));
            safeParse("CPU核心数", () -> parseCPUCores(results, status));

        } catch (Exception e) {
            log.warn("采集硬件信息失败: {}", e.getMessage());
        }

        return status;
    }

    private void safeParse(String name, Runnable parser) {
        try {
            parser.run();
        } catch (Exception e) {
            log.warn("解析 {} 数据失败: {} — {}", name, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void parseCPU(Map<String, String> results, HardwareStatus status) {
        String first = null, second = null;
        for (Map.Entry<String, String> e : results.entrySet()) {
            if (e.getKey().contains("/proc/stat")) {
                if (first == null) first = e.getValue();
                else second = e.getValue();
            }
        }
        if (first == null || second == null) {
            CPUInfo cpu = new CPUInfo();
            cpu.setCores(1);
            status.setCpu(cpu);
            return;
        }

        long[] a = parseProcStat(first);
        long[] b = parseProcStat(second);
        if (a != null && b != null && b[1] > a[1]) {
            double deltaTotal = b[1] - a[1];
            double deltaIdle = b[0] - a[0];
            double percent = (1 - deltaIdle / deltaTotal) * 100;
            if (percent < 0) percent = 0;
            if (percent > 100) percent = 100;
            CPUInfo cpu = new CPUInfo();
            cpu.setPercent(Math.round(percent * 10) / 10.0);
            status.setCpu(cpu);
        }
    }

    /** 返回 [idle, total] */
    private long[] parseProcStat(String line) {
        String[] fields = line.trim().split("\\s+");
        if (fields.length < 5) return null;
        long idle = 0, total = 0;
        for (int i = 1; i < fields.length; i++) {
            long v = Long.parseLong(fields[i]);
            total += v;
            if (i == 4) idle = v;
        }
        return new long[]{idle, total};
    }

    private void parseMemory(Map<String, String> results, HardwareStatus status) {
        for (String out : results.values()) {
            if (out != null && out.startsWith("MEM:")) {
                String[] parts = out.split(":");
                if (parts.length >= 4) {
                    MemoryInfo mem = new MemoryInfo();
                    long total = Long.parseLong(parts[1]);
                    long used = Long.parseLong(parts[2]);
                    long free = Long.parseLong(parts[3]);
                    mem.setTotalGB(roundGB(total));
                    mem.setUsedGB(roundGB(used));
                    mem.setFreeGB(roundGB(free));
                    if (total > 0) {
                        mem.setUsedPercent(Math.round((double) used / total * 1000) / 10.0);
                    }
                    status.setMemory(mem);
                    return;
                }
            }
        }
    }

    private void parseDisk(Map<String, String> results, HardwareStatus status) {
        var disks = new ArrayList<DiskInfo>();
        for (String out : results.values()) {
            if (out == null) continue;
            for (String line : out.split("\n")) {
                line = line.trim();
                if (!line.startsWith("DISK:")) continue;
                String content = line.substring(5);
                String[] parts = content.split("\\|");
                if (parts.length >= 5) {
                    DiskInfo disk = new DiskInfo();
                    disk.setFilesystem(parts[0]);
                    disk.setMountPoint(parts[1]);
                    long total = Long.parseLong(parts[2]);
                    long used = Long.parseLong(parts[3]);
                    disk.setTotalGB(roundGB(total));
                    disk.setUsedGB(roundGB(used));
                    disk.setFreeGB(roundGB(total - used));
                    if (total > 0) {
                        disk.setUsedPercent(Math.round((double) used / total * 1000) / 10.0);
                    }
                    disks.add(disk);
                }
            }
        }
        status.setDisks(disks);
    }

    private void parseNetwork(Map<String, String> results, HardwareStatus status) {
        var nets = new ArrayList<NetInfo>();
        for (String out : results.values()) {
            if (out == null) continue;
            for (String line : out.split("\n")) {
                line = line.trim();
                if (!line.startsWith("NET:")) continue;
                String content = line.substring(4);
                String[] parts = content.split("\\|");
                if (parts.length >= 3 && !parts[0].trim().equals("lo")) {
                    NetInfo net = new NetInfo();
                    net.setInterface(parts[0].trim());
                    net.setRxBytes(Long.parseLong(parts[1].trim()));
                    net.setTxBytes(Long.parseLong(parts[2].trim()));
                    nets.add(net);
                }
            }
        }
        status.setNetwork(nets);
    }

    private void parseLoad(Map<String, String> results, HardwareStatus status) {
        String out = results.get("cat /proc/loadavg");
        if (out == null) return;
        String[] fields = out.trim().split("\\s+");
        if (fields.length >= 3) {
            try {
                LoadInfo load = new LoadInfo();
                load.setLoad1(Double.parseDouble(fields[0]));
                load.setLoad5(Double.parseDouble(fields[1]));
                load.setLoad15(Double.parseDouble(fields[2]));
                status.setLoad(load);
            } catch (Exception e) {
                log.warn("解析负载数据失败: {}", e.getMessage());
            }
        }
    }

    private void parseUptime(Map<String, String> results, HardwareStatus status) {
        String out = results.get("awk '{print $1}' /proc/uptime");
        if (out == null) return;
        try {
            double secs = Double.parseDouble(out.trim());
            int d = (int) secs / 86400;
            int h = ((int) secs % 86400) / 3600;
            int m = ((int) secs % 3600) / 60;
            String uptime;
            if (d > 0) uptime = d + "天" + h + "小时" + m + "分钟";
            else if (h > 0) uptime = h + "小时" + m + "分钟";
            else uptime = m + "分钟";
            status.setUptime(uptime);
        } catch (Exception e) {
            log.warn("解析运行时间失败: {}", e.getMessage());
        }
    }

    private void parseCPUCores(Map<String, String> results, HardwareStatus status) {
        CPUInfo cpu = status.getCpu();
        if (cpu == null) {
            cpu = new CPUInfo();
            status.setCpu(cpu);
        }
        // 只从 nproc 命令的输出中解析
        for (Map.Entry<String, String> e : results.entrySet()) {
            if (e.getKey().equals("nproc") && e.getValue() != null) {
                try {
                    int cores = Integer.parseInt(e.getValue().trim());
                    if (cores > 0) cpu.setCores(cores);
                } catch (Exception ignored) {
                    // nproc 解析失败，留默认值
                }
                return;
            }
        }
    }

    private double roundGB(double bytes) {
        return Math.round(bytes / 1024 / 1024 / 1024 * 10) / 10.0;
    }
}
