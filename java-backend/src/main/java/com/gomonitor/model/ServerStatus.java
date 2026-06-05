package com.gomonitor.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** 单台服务器完整状态快照 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServerStatus {
    private String name;
    private String host;
    private boolean online;
    private HardwareStatus hardware;
    private DockerStatus docker;
    @JsonProperty("collected_at")
    private long collectedAt;
    private String error;

    // Getters & Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public HardwareStatus getHardware() { return hardware; }
    public void setHardware(HardwareStatus hardware) { this.hardware = hardware; }
    public DockerStatus getDocker() { return docker; }
    public void setDocker(DockerStatus docker) { this.docker = docker; }
    public long getCollectedAt() { return collectedAt; }
    public void setCollectedAt(long collectedAt) { this.collectedAt = collectedAt; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    /** 硬件状态 */
    public static class HardwareStatus {
        private CPUInfo cpu;
        private MemoryInfo memory;
        private List<DiskInfo> disks;
        private List<NetInfo> network;
        private LoadInfo load;
        private String uptime;

        public CPUInfo getCpu() { return cpu; }
        public void setCpu(CPUInfo cpu) { this.cpu = cpu; }
        public MemoryInfo getMemory() { return memory; }
        public void setMemory(MemoryInfo memory) { this.memory = memory; }
        public List<DiskInfo> getDisks() { return disks; }
        public void setDisks(List<DiskInfo> disks) { this.disks = disks; }
        public List<NetInfo> getNetwork() { return network; }
        public void setNetwork(List<NetInfo> network) { this.network = network; }
        public LoadInfo getLoad() { return load; }
        public void setLoad(LoadInfo load) { this.load = load; }
        public String getUptime() { return uptime; }
        public void setUptime(String uptime) { this.uptime = uptime; }
    }

    public static class CPUInfo {
        private double percent;
        private int cores;
        public double getPercent() { return percent; }
        public void setPercent(double percent) { this.percent = percent; }
        public int getCores() { return cores; }
        public void setCores(int cores) { this.cores = cores; }
    }

    public static class MemoryInfo {
        @JsonProperty("total_gb") private double totalGB;
        @JsonProperty("used_gb") private double usedGB;
        @JsonProperty("free_gb") private double freeGB;
        @JsonProperty("used_percent") private double usedPercent;
        public double getTotalGB() { return totalGB; }
        public void setTotalGB(double totalGB) { this.totalGB = totalGB; }
        public double getUsedGB() { return usedGB; }
        public void setUsedGB(double usedGB) { this.usedGB = usedGB; }
        public double getFreeGB() { return freeGB; }
        public void setFreeGB(double freeGB) { this.freeGB = freeGB; }
        public double getUsedPercent() { return usedPercent; }
        public void setUsedPercent(double usedPercent) { this.usedPercent = usedPercent; }
    }

    public static class DiskInfo {
        private String filesystem;
        @JsonProperty("mount_point") private String mountPoint;
        @JsonProperty("total_gb") private double totalGB;
        @JsonProperty("used_gb") private double usedGB;
        @JsonProperty("free_gb") private double freeGB;
        @JsonProperty("used_percent") private double usedPercent;

        public String getFilesystem() { return filesystem; }
        public void setFilesystem(String filesystem) { this.filesystem = filesystem; }
        public String getMountPoint() { return mountPoint; }
        public void setMountPoint(String mountPoint) { this.mountPoint = mountPoint; }
        public double getTotalGB() { return totalGB; }
        public void setTotalGB(double totalGB) { this.totalGB = totalGB; }
        public double getUsedGB() { return usedGB; }
        public void setUsedGB(double usedGB) { this.usedGB = usedGB; }
        public double getFreeGB() { return freeGB; }
        public void setFreeGB(double freeGB) { this.freeGB = freeGB; }
        public double getUsedPercent() { return usedPercent; }
        public void setUsedPercent(double usedPercent) { this.usedPercent = usedPercent; }
    }

    public static class NetInfo {
        @JsonProperty("interface") private String iface;
        @JsonProperty("rx_bytes") private long rxBytes;
        @JsonProperty("tx_bytes") private long txBytes;

        public String getInterface() { return iface; }
        public void setInterface(String iface) { this.iface = iface; }
        public long getRxBytes() { return rxBytes; }
        public void setRxBytes(long rxBytes) { this.rxBytes = rxBytes; }
        public long getTxBytes() { return txBytes; }
        public void setTxBytes(long txBytes) { this.txBytes = txBytes; }
    }

    public static class LoadInfo {
        @JsonProperty("load_1") private double load1;
        @JsonProperty("load_5") private double load5;
        @JsonProperty("load_15") private double load15;

        public double getLoad1() { return load1; }
        public void setLoad1(double load1) { this.load1 = load1; }
        public double getLoad5() { return load5; }
        public void setLoad5(double load5) { this.load5 = load5; }
        public double getLoad15() { return load15; }
        public void setLoad15(double load15) { this.load15 = load15; }
    }

    /** Docker 状态 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DockerStatus {
        private DockerInfo info;
        private List<ContainerInfo> containers;
        private List<ImageInfo> images;

        public DockerInfo getInfo() { return info; }
        public void setInfo(DockerInfo info) { this.info = info; }
        public List<ContainerInfo> getContainers() { return containers; }
        public void setContainers(List<ContainerInfo> containers) { this.containers = containers; }
        public List<ImageInfo> getImages() { return images; }
        public void setImages(List<ImageInfo> images) { this.images = images; }
    }

    public static class DockerInfo {
        @JsonProperty("containers_total") private int containersTotal;
        @JsonProperty("containers_running") private int containersRunning;
        @JsonProperty("containers_paused") private int containersPaused;
        @JsonProperty("containers_stopped") private int containersStopped;
        private String version;
        @JsonProperty("server_version") private String serverVersion;

        public int getContainersTotal() { return containersTotal; }
        public void setContainersTotal(int containersTotal) { this.containersTotal = containersTotal; }
        public int getContainersRunning() { return containersRunning; }
        public void setContainersRunning(int containersRunning) { this.containersRunning = containersRunning; }
        public int getContainersPaused() { return containersPaused; }
        public void setContainersPaused(int containersPaused) { this.containersPaused = containersPaused; }
        public int getContainersStopped() { return containersStopped; }
        public void setContainersStopped(int containersStopped) { this.containersStopped = containersStopped; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getServerVersion() { return serverVersion; }
        public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    }

    public static class ContainerInfo {
        private String id;
        private String name;
        private String image;
        private String status;
        private String state;
        @JsonProperty("created_at") private String createdAt;
        private String ports;
        @JsonProperty("cpu_percent") private double cpuPercent;
        @JsonProperty("mem_percent") private double memPercent;
        @JsonProperty("mem_usage") private String memUsage;
        @JsonProperty("mem_limit") private String memLimit;
        @JsonProperty("network_input") private long networkInput;
        @JsonProperty("network_output") private long networkOutput;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getPorts() { return ports; }
        public void setPorts(String ports) { this.ports = ports; }
        public double getCpuPercent() { return cpuPercent; }
        public void setCpuPercent(double cpuPercent) { this.cpuPercent = cpuPercent; }
        public double getMemPercent() { return memPercent; }
        public void setMemPercent(double memPercent) { this.memPercent = memPercent; }
        public String getMemUsage() { return memUsage; }
        public void setMemUsage(String memUsage) { this.memUsage = memUsage; }
        public String getMemLimit() { return memLimit; }
        public void setMemLimit(String memLimit) { this.memLimit = memLimit; }
        public long getNetworkInput() { return networkInput; }
        public void setNetworkInput(long networkInput) { this.networkInput = networkInput; }
        public long getNetworkOutput() { return networkOutput; }
        public void setNetworkOutput(long networkOutput) { this.networkOutput = networkOutput; }
    }

    public static class ImageInfo {
        @JsonProperty("repo_tag") private String repoTag;
        private String size;
        @JsonProperty("created_at") private String createdAt;
        private String id;

        public String getRepoTag() { return repoTag; }
        public void setRepoTag(String repoTag) { this.repoTag = repoTag; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    /** 全部服务器状态汇总 */
    public static class AllServersStatus {
        private List<ServerStatus> servers;
        private long updated;

        public List<ServerStatus> getServers() { return servers; }
        public void setServers(List<ServerStatus> servers) { this.servers = servers; }
        public long getUpdated() { return updated; }
        public void setUpdated(long updated) { this.updated = updated; }
    }
}
