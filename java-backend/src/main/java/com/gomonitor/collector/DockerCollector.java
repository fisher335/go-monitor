package com.gomonitor.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gomonitor.model.ServerStatus.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;

/** Docker 信息采集器 */
public class DockerCollector {

    private static final Logger log = LoggerFactory.getLogger(DockerCollector.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final SSHClient client;

    public DockerCollector(SSHClient client) {
        this.client = client;
    }

    /** 采集一次 Docker 状态，如果 Docker 不可用返回 null */
    public DockerStatus collect() {
        try {
            // 检查 Docker 是否可用
            String check = client.exec("docker info --format '{{json .}}' 2>/dev/null");
            if (check == null || check.trim().isEmpty()) {
                return null;
            }

            DockerStatus status = new DockerStatus();

            // 解析 docker info
            parseDockerInfo(check, status);

            // 容器列表
            String ps = client.exec("docker ps -a --format '{{json .}}' 2>/dev/null");
            if (ps != null && !ps.trim().isEmpty()) {
                parseContainerList(ps, status);
            }

            // 容器实时状态
            String stats = client.exec("docker stats --no-stream --format '{{json .}}' 2>/dev/null");
            if (stats != null && !stats.trim().isEmpty()) {
                parseContainerStats(stats, status);
            }

            // 镜像列表
            String images = client.exec("docker images --format '{{json .}}' 2>/dev/null");
            if (images != null && !images.trim().isEmpty()) {
                parseImageList(images, status);
            }

            return status;

        } catch (Exception e) {
            log.warn("采集 Docker 信息失败: {}", e.getMessage());
            return null;
        }
    }

    private void parseDockerInfo(String output, DockerStatus status) throws Exception {
        JsonNode root = mapper.readTree(output.trim());
        DockerInfo info = new DockerInfo();
        info.setContainersTotal(root.path("Containers").asInt());
        info.setContainersRunning(root.path("ContainersRunning").asInt());
        info.setContainersPaused(root.path("ContainersPaused").asInt());
        info.setContainersStopped(root.path("ContainersStopped").asInt());
        info.setServerVersion(root.path("ServerVersion").asText());
        info.setVersion(root.path("ServerVersion").asText());
        status.setInfo(info);
    }

    private void parseContainerList(String output, DockerStatus status) throws Exception {
        var containers = new ArrayList<ContainerInfo>();
        for (String line : output.trim().split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            JsonNode node = mapper.readTree(line);

            ContainerInfo c = new ContainerInfo();
            String id = node.path("ID").asText();
            c.setId(id.length() > 12 ? id.substring(0, 12) : id);
            c.setName(node.path("Names").asText());
            c.setImage(node.path("Image").asText());
            c.setState(node.path("State").asText());
            c.setStatus(node.path("Status").asText());
            c.setPorts(node.path("Ports").asText());
            c.setCreatedAt(node.path("CreatedAt").asText());
            containers.add(c);
        }
        status.setContainers(containers);
    }

    private void parseContainerStats(String output, DockerStatus status) throws Exception {
        var statsMap = new java.util.HashMap<String, ContainerInfo>();
        for (String line : output.trim().split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            JsonNode node = mapper.readTree(line);

            String id = node.path("ID").asText();
            ContainerInfo c = new ContainerInfo();

            c.setCpuPercent(parsePercent(node.path("CPUPerc").asText()));
            String memUsage = node.path("MemUsage").asText();
            String[] memParts = memUsage.split("/");
            if (memParts.length == 2) {
                c.setMemUsage(memParts[0].trim());
                c.setMemLimit(memParts[1].trim());
            }
            c.setMemPercent(parsePercent(node.path("MemPerc").asText()));

            String netIO = node.path("NetIO").asText();
            String[] netParts = netIO.split("/");
            if (netParts.length == 2) {
                c.setNetworkInput(parseBytes(netParts[0].trim()));
                c.setNetworkOutput(parseBytes(netParts[1].trim()));
            }

            statsMap.put(id.length() > 12 ? id.substring(0, 12) : id, c);
        }

        // 合并到容器列表
        if (status.getContainers() != null) {
            for (ContainerInfo c : status.getContainers()) {
                ContainerInfo stat = statsMap.get(c.getId());
                if (stat != null) {
                    c.setCpuPercent(stat.getCpuPercent());
                    c.setMemPercent(stat.getMemPercent());
                    c.setMemUsage(stat.getMemUsage());
                    c.setMemLimit(stat.getMemLimit());
                    c.setNetworkInput(stat.getNetworkInput());
                    c.setNetworkOutput(stat.getNetworkOutput());
                }
            }
        }
    }

    private void parseImageList(String output, DockerStatus status) throws Exception {
        var images = new ArrayList<ImageInfo>();
        for (String line : output.trim().split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            JsonNode node = mapper.readTree(line);

            ImageInfo img = new ImageInfo();
            img.setRepoTag(node.path("Repository").asText() + ":" + node.path("Tag").asText());
            String id = node.path("ID").asText();
            img.setId(id.length() > 19 ? id.substring(7, 19) : id);
            img.setSize(node.path("Size").asText());
            img.setCreatedAt(node.path("CreatedAt").asText());
            images.add(img);
        }
        status.setImages(images);
    }

    private double parsePercent(String s) {
        if (s == null || s.isEmpty()) return 0;
        try {
            return Double.parseDouble(s.replace("%", "").replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    private long parseBytes(String s) {
        if (s == null || s.trim().isEmpty() || s.equals("0B")) return 0;
        s = s.trim();
        StringBuilder num = new StringBuilder();
        String unit = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '.' || c == ',') {
                num.append(c == ',' ? '.' : c);
            } else {
                unit = s.substring(i).trim();
                break;
            }
        }
        if (num.isEmpty()) return 0;
        double val = Double.parseDouble(num.toString());
        switch (unit.toUpperCase()) {
            case "KB": case "K": return (long) (val * 1024);
            case "MB": case "M": return (long) (val * 1024 * 1024);
            case "GB": case "G": return (long) (val * 1024 * 1024 * 1024);
            case "TB": case "T": return (long) (val * 1024L * 1024 * 1024 * 1024);
            default: return (long) val;
        }
    }
}
