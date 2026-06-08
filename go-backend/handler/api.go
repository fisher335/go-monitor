package handler

import (
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"go-monitor/collector"
	"go-monitor/models"
)

type Handler struct {
	status *models.ServerStatus
}

func NewHandler() *Handler { return &Handler{} }

func (h *Handler) SetStatus(s *models.ServerStatus) { h.status = s }

func (h *Handler) ListServers(c *gin.Context) {
	all := &models.AllServersStatus{Servers: []models.ServerStatus{}, Updated: models.NowUnix()}
	if h.status != nil { all.Servers = append(all.Servers, *h.status) }
	c.JSON(http.StatusOK, gin.H{"code": 0, "message": "success", "data": all})
}

func (h *Handler) Health(c *gin.Context) {
	c.JSON(http.StatusOK, gin.H{"code": 0, "message": "ok", "data": gin.H{"version": "1.0.0", "type": "local"}})
}

func CollectLoop(local *collector.LocalCollector, docker *collector.DockerCollector) *models.ServerStatus {
	s := &models.ServerStatus{Name: "本机", Host: "localhost", Online: true, Type: "local"}
	if hw := local.Collect(); hw != nil {
		s.Hardware = hw
		if hw.Hostname != "" { s.Name = hw.Hostname }
	}
	if docker != nil && docker.IsAvailable() {
		s.Docker = docker.Collect()
	}
	s.CollectedAt = time.Now().UnixMilli()
	return s
}
