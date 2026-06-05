package com.gomonitor.handler;

import com.gomonitor.collector.CollectorManager;
import com.gomonitor.model.ServerStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** REST API 控制器 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CollectorManager collectorManager;

    public ApiController(CollectorManager collectorManager) {
        this.collectorManager = collectorManager;
    }

    /** 健康检查 */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "ok",
                "data", Map.of(
                        "version", "1.0.0",
                        "serverCount", collectorManager.getAllStatus().size()
                )
        ));
    }

    /** 所有服务器状态 */
    @GetMapping("/servers")
    public ResponseEntity<Map<String, Object>> listServers() {
        List<ServerStatus> servers = collectorManager.getAllStatus();
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of(
                        "servers", servers,
                        "updated", System.currentTimeMillis()
                )
        ));
    }

    /** 单台服务器状态 */
    @GetMapping("/server/{name}")
    public ResponseEntity<Map<String, Object>> getServer(@PathVariable String name) {
        ServerStatus status = collectorManager.getServerStatus(name);
        if (status == null) {
            return ResponseEntity.status(404).body(Map.of(
                    "code", 404,
                    "message", "服务器不存在"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", status
        ));
    }
}
