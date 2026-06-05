package com.gomonitor.collector;

import com.gomonitor.config.AppConfig;
import com.gomonitor.config.AppConfig.ServerConfig;
import com.gomonitor.hub.MonitorHub;
import com.gomonitor.model.ServerStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/** 采集管理器 — 调度所有服务器采集任务 */
@Service
public class CollectorManager {

    private static final Logger log = LoggerFactory.getLogger(CollectorManager.class);

    private final AppConfig appConfig;
    private final MonitorHub hub;
    private final Map<String, ServerStatus> cache = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    public CollectorManager(AppConfig appConfig, MonitorHub hub) {
        this.appConfig = appConfig;
        this.hub = hub;
    }

    @PostConstruct
    public void start() {
        List<ServerConfig> servers = appConfig.getServers();
        if (servers == null || servers.isEmpty()) {
            log.warn("⚠ 没有配置目标服务器");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(servers.size());
        int interval = appConfig.getInterval() > 0 ? appConfig.getInterval() : 5;

        for (ServerConfig sc : servers) {
            cache.put(sc.getName(), createOfflineStatus(sc));

            scheduler.scheduleAtFixedRate(
                    () -> collectOnce(sc),
                    0, interval, TimeUnit.SECONDS);
        }

        log.info("✅ 采集管理器已启动，监控 {} 台服务器，间隔 {} 秒", servers.size(), interval);
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    /** 获取所有服务器最新状态 */
    public List<ServerStatus> getAllStatus() {
        return List.copyOf(cache.values());
    }

    /** 获取单台服务器状态 */
    public ServerStatus getServerStatus(String name) {
        return cache.get(name);
    }

    /** 采集一次 */
    private void collectOnce(ServerConfig sc) {
        ServerStatus status = new ServerStatus();
        status.setName(sc.getName());
        status.setHost(sc.getHost());
        status.setCollectedAt(System.currentTimeMillis());

        try (SSHClient client = new SSHClient(sc.getHost(), sc.getPort(), sc.getUser(), sc.getPassword(), null)) {
            client.connect();
            status.setOnline(true);

            // 采集硬件
            HardwareCollector hw = new HardwareCollector(client);
            status.setHardware(hw.collect());

            // 采集 Docker
            DockerCollector dk = new DockerCollector(client);
            status.setDocker(dk.collect());

        } catch (Exception e) {
            status.setOnline(false);
            status.setError(e.getMessage());
            log.warn("⚠ {} 采集失败: {}", sc.getName(), e.getMessage());
        }

        cache.put(sc.getName(), status);

        // 通过 WebSocket 推送
        hub.broadcast(getAllStatus());
    }

    private ServerStatus createOfflineStatus(ServerConfig sc) {
        ServerStatus status = new ServerStatus();
        status.setName(sc.getName());
        status.setHost(sc.getHost());
        status.setOnline(false);
        status.setCollectedAt(System.currentTimeMillis());
        return status;
    }
}
