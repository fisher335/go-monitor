package com.gomonitor.collector;

import com.jcraft.jsch.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** SSH 客户端封装 */
public class SSHClient implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SSHClient.class);

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    private final String keyFile;

    private Session session;

    public SSHClient(String host, int port, String user, String password, String keyFile) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.keyFile = keyFile;
    }

    /** 建立连接 */
    public void connect() throws Exception {
        JSch jsch = new JSch();

        if (keyFile != null && !keyFile.isEmpty()) {
            jsch.addIdentity(keyFile);
        }

        session = jsch.getSession(user, host, port);
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications",
                keyFile != null && !keyFile.isEmpty() ? "publickey" : "password");

        if (password != null && !password.isEmpty()) {
            session.setPassword(password);
        }

        session.setTimeout(10000);
        session.connect(10000);
    }

    /** 执行远程命令，返回 stdout */
    public String exec(String command) throws Exception {
        if (session == null || !session.isConnected()) {
            throw new RuntimeException("SSH 未连接");
        }

        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        try {
            channel.setCommand(command);
            channel.setErrStream(System.err);

            InputStream in = channel.getInputStream();
            try {
                channel.connect(5000);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                return out.toString(StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } finally {
            channel.disconnect();
        }
    }

    /** 批量执行多个命令（带分隔符），返回按命令分组的输出 */
    public java.util.Map<String, String> execMulti(String[] commands) throws Exception {
        StringBuilder script = new StringBuilder();
        for (int i = 0; i < commands.length; i++) {
            script.append("echo '---CMD").append(i).append("_START---'\n");
            script.append(commands[i]).append("\n");
            script.append("echo '---CMD").append(i).append("_END---'\n");
        }

        String output = exec(script.toString());
        java.util.Map<String, String> results = new java.util.LinkedHashMap<>();

        for (int i = 0; i < commands.length; i++) {
            String start = "---CMD" + i + "_START---";
            String end = "---CMD" + i + "_END---";
            String result = extractBetween(output, start, end);
            if (result != null) {
                results.put(commands[i], result);
            }
        }
        return results;
    }

    private String extractBetween(String s, String start, String end) {
        int i = s.indexOf(start);
        if (i < 0) return null;
        i += start.length();
        // 跳过换行
        while (i < s.length() && (s.charAt(i) == '\n' || s.charAt(i) == '\r')) i++;
        int j = s.indexOf(end, i);
        if (j < 0) return s.substring(i);
        return s.substring(i, j);
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    @Override
    public void close() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }
}
