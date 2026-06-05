# Go-Monitor 🚀

基于 Go + Vue3 的服务器监控系统，通过 **SSH 远程连接**采集硬件信息和 Docker 状态，WebSocket 实时推送到前端展示。

## 架构

```
┌──────────────┐   SSH    ┌──────────┐   WebSocket   ┌──────────┐
│  目标服务器    │◄────────│ Go 后端   │──────────────►│ Vue3 前端 │
│  (Linux)      │────────►│ (Gin)    │  实时推送      │ (仪表盘)  │
└──────────────┘          └──────────┘               └──────────┘
```

## 特性

- ⚡ **无代理** — 只需 SSH 账号，目标服务器无需安装任何软件
- 📊 **硬件监控** — CPU、内存、磁盘、网络流量、系统负载
- 🐳 **Docker 监控** — 容器列表、CPU/内存使用率、镜像列表
- 🔄 **实时推送** — WebSocket 推送，前端秒级更新
- 🎨 **美观仪表盘** — Vue3 + Tailwind CSS，深色主题

## 快速开始

### 1. 配置

编辑 `config.yaml`：

```yaml
listen: ":9500"           # 监听端口
interval: 5               # 采集间隔（秒）

servers:
  - name: "server-01"     # 显示名称
    host: "192.168.1.100" # SSH 地址
    port: 22              # SSH 端口
    user: "root"          # SSH 用户名
    password: "yourpass"  # 密码认证
    # key_file: "~/.ssh/id_rsa"  # 或者用密钥认证
```

### 2. 启动后端

```bash
cd go-monitor
bin\go-monitor.exe
```

### 3. 访问

打开浏览器访问：`http://localhost:9500`

### 4. 开发模式（前后端分离）

```bash
# 终端 1：启动后端
cd backend && go run main.go

# 终端 2：启动前端开发服务器
cd frontend && npm run dev
```

## 项目结构

```
go-monitor/
├── config.yaml              # 服务器配置
├── bin/                     # 编译产物
│   └── go-monitor.exe
├── backend/                 # Go 后端
│   ├── main.go              # 入口
│   ├── config/              # 配置加载
│   ├── models/              # 数据模型
│   ├── collector/           # 采集器
│   │   ├── ssh.go           # SSH 连接管理
│   │   ├── hardware.go      # 硬件采集
│   │   ├── docker.go        # Docker 采集
│   │   └── collector.go     # 采集调度
│   ├── hub/                 # WebSocket Hub
│   └── handler/             # HTTP 处理器
├── frontend/                # Vue3 前端
│   ├── src/
│   │   ├── App.vue          # 主页面
│   │   ├── stores/          # Pinia 状态
│   │   ├── views/           # 页面
│   │   └── components/      # 组件
│   └── dist/                # 构建产物
└── README.md
```

## API

| 路径 | 方法 | 说明 |
|------|------|------|
| `/api/health` | GET | 健康检查 |
| `/api/servers` | GET | 所有服务器状态 |
| `/api/server/:name` | GET | 单台服务器状态 |
| `/api/ws` | WebSocket | 实时数据推送 |
| `/api/ws/status` | GET | WebSocket 连接状态 |

## 技术栈

- **后端**: Go 1.22+, Gin, gorilla/websocket, crypto/ssh
- **前端**: Vue 3, Pinia, Tailwind CSS, Vite
- **通信**: REST API + WebSocket
- **部署**: 单二进制文件 + 静态文件
