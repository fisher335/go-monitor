package com.gomonitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "monitor")
public class AppConfig {

    private int interval = 5;
    private List<ServerConfig> servers;

    public int getInterval() { return interval; }
    public void setInterval(int interval) { this.interval = interval; }
    public List<ServerConfig> getServers() { return servers; }
    public void setServers(List<ServerConfig> servers) { this.servers = servers; }

    public static class ServerConfig {
        private String name;
        private String host;
        private int port = 22;
        private String user;
        private String password;
        private String keyFile;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getUser() { return user; }
        public void setUser(String user) { this.user = user; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getKeyFile() { return keyFile; }
        public void setKeyFile(String keyFile) { this.keyFile = keyFile; }
    }
}
