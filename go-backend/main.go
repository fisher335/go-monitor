package main

import (
	"embed"
	"io/fs"
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"go-monitor/collector"
	"go-monitor/handler"
	"go-monitor/hub"
)

//go:embed static/*
var staticFS embed.FS

func main() {
	localCol := collector.NewLocalCollector()

	dockerCol := collector.NewDockerCollector()
	if !dockerCol.IsAvailable() {
		log.Println("⚠ Docker 不可用")
		dockerCol = nil
	}

	wsHub := hub.NewHub()
	go wsHub.Run()

	h := handler.NewHandler()

	gin.SetMode(gin.ReleaseMode)
	r := gin.Default()

	// CORS
	r.Use(func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Content-Type")
		if c.Request.Method == "OPTIONS" { c.AbortWithStatus(204); return }
		c.Next()
	})

	// API 路由
	r.GET("/api/health", h.Health)
	r.GET("/api/servers", h.ListServers)
	r.GET("/api/ws", func(c *gin.Context) { wsHub.HandleWebSocket(c.Writer, c.Request) })

	// 前端静态文件（嵌入的 SPA）
	staticSub, err := fs.Sub(staticFS, "static")
	if err != nil {
		log.Fatalf("❌ 读取嵌入的静态文件失败: %v", err)
	}
	staticHandler := http.FileServer(http.FS(staticSub))
	r.GET("/assets/*filepath", func(c *gin.Context) {
		staticHandler.ServeHTTP(c.Writer, c.Request)
	})
	// SPA 路由：所有未匹配的路径返回 index.html
	r.NoRoute(func(c *gin.Context) {
		if c.Request.URL.Path == "/api/ws" {
			wsHub.HandleWebSocket(c.Writer, c.Request)
			return
		}
		if len(c.Request.URL.Path) >= 4 && c.Request.URL.Path[:4] == "/api" {
			c.Status(404)
			return
		}
		c.Request.URL.Path = "/"
		staticHandler.ServeHTTP(c.Writer, c.Request)
	})

	// 采集循环
	go func() {
		for {
			status := handler.CollectLoop(localCol, dockerCol)
			h.SetStatus(status)

			all := map[string]interface{}{"servers": []interface{}{*status}, "updated": status.CollectedAt}
			hub.BroadcastJSON(wsHub, all)

			time.Sleep(5 * time.Second)
		}
	}()

	addr := ":9501"
	log.Printf("🚀 Go-Monitor 启动: http://localhost%s", addr)
	log.Printf("   API:  http://localhost%s/api/servers", addr)
	log.Printf("   前端: http://localhost%s", addr)
	log.Printf("   WS:   ws://localhost%s/api/ws", addr)

	if err := r.Run(addr); err != nil {
		log.Fatalf("❌ 启动失败: %v", err)
	}
}
