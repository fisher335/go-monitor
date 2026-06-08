package collector

import (
	"encoding/json"
	"fmt"
	"math"
	"os/exec"
	"strings"

	"go-monitor/models"
)

type DockerCollector struct{}

func NewDockerCollector() *DockerCollector { return &DockerCollector{} }

func (d *DockerCollector) IsAvailable() bool {
	out, err := exec.Command("docker", "info", "--format", "{{.ServerVersion}}").Output()
	return err == nil && len(out) > 0
}

func (d *DockerCollector) Collect() *models.DockerStatus {
	s := &models.DockerStatus{}
	d.collectInfo(s)
	d.collectContainers(s)
	d.collectStats(s)
	d.collectImages(s)
	return s
}

func (d *DockerCollector) collectInfo(s *models.DockerStatus) {
	out, err := d.exec("info", "--format", "{{json .}}")
	if err != nil { return }
	var raw struct {
		Containers, ContainersRunning, ContainersPaused, ContainersStopped int
		ServerVersion string `json:"ServerVersion"`
	}
	if json.Unmarshal([]byte(out), &raw) != nil { return }
	s.Info = models.DockerInfo{
		ContainersTotal: raw.Containers, ContainersRunning: raw.ContainersRunning,
		ContainersPaused: raw.ContainersPaused, ContainersStopped: raw.ContainersStopped,
		ServerVersion: raw.ServerVersion,
	}
}

func (d *DockerCollector) collectContainers(s *models.DockerStatus) {
	out, err := d.exec("ps", "-a", "--format", "{{json .}}")
	if err != nil { return }
	for _, line := range lines(out) {
		var raw struct {
			ID, Names, Image, Status, State, Ports, CreatedAt string
		}
		if json.Unmarshal([]byte(line), &raw) != nil { continue }
		id := raw.ID; if len(id) > 12 { id = id[:12] }
		s.Containers = append(s.Containers, models.ContainerInfo{
			ID: id, Name: raw.Names, Image: raw.Image, Status: raw.Status,
			State: raw.State, Ports: raw.Ports, CreatedAt: raw.CreatedAt,
		})
	}
}

func (d *DockerCollector) collectStats(s *models.DockerStatus) {
	if len(s.Containers) == 0 { return }
	out, err := d.exec("stats", "--no-stream", "--format", "{{json .}}")
	if err != nil { return }
	m := make(map[string]models.ContainerInfo)
	for _, line := range lines(out) {
		var raw struct {
			ID, Name, CPUPerc, MemUsage, MemPerc, NetIO string
		}
		if json.Unmarshal([]byte(line), &raw) != nil { continue }
		id := raw.ID; if len(id) > 12 { id = id[:12] }
		mu, ml := split2(raw.MemUsage, "/")
		ni, no := split2(raw.NetIO, "/")
		m[id] = models.ContainerInfo{
			CPUPercent: parsePct(raw.CPUPerc), MemPercent: parsePct(raw.MemPerc),
			MemUsage: mu, MemLimit: ml, NetworkInput: parseBytes(ni), NetworkOutput: parseBytes(no),
		}
	}
	for i, c := range s.Containers {
		if stat, ok := m[c.ID]; ok {
			s.Containers[i].CPUPercent, s.Containers[i].MemPercent = stat.CPUPercent, stat.MemPercent
			s.Containers[i].MemUsage, s.Containers[i].MemLimit = stat.MemUsage, stat.MemLimit
			s.Containers[i].NetworkInput, s.Containers[i].NetworkOutput = stat.NetworkInput, stat.NetworkOutput
		}
	}
}

func (d *DockerCollector) collectImages(s *models.DockerStatus) {
	out, err := d.exec("images", "--format", "{{json .}}")
	if err != nil { return }
	for _, line := range lines(out) {
		var raw struct {
			Repository, Tag, ID, CreatedAt, Size string
		}
		if json.Unmarshal([]byte(line), &raw) != nil { continue }
		rt := raw.Repository + ":" + raw.Tag
		if raw.Tag == "" { rt = raw.Repository }
		id := raw.ID; if len(id) > 19 { id = id[7:19] }
		s.Images = append(s.Images, models.ImageInfo{RepoTag: rt, ID: id, Size: raw.Size, CreatedAt: raw.CreatedAt})
	}
}

func (d *DockerCollector) exec(args ...string) (string, error) {
	out, err := exec.Command("docker", args...).Output()
	if err != nil { return "", fmt.Errorf("docker err: %w", err) }
	return string(out), nil
}

func lines(s string) []string {
	ls := strings.Split(strings.TrimSpace(s), "\n")
	var r []string
	for _, l := range ls {
		if strings.TrimSpace(l) != "" { r = append(r, strings.TrimSpace(l)) }
	}
	return r
}

func split2(s, sep string) (string, string) {
	p := strings.SplitN(s, sep, 2)
	if len(p) == 2 { return strings.TrimSpace(p[0]), strings.TrimSpace(p[1]) }
	return s, ""
}

func parsePct(s string) float64 {
	s = strings.TrimSpace(strings.ReplaceAll(s, "%", ""))
	if s == "" { return 0 }
	var v float64
	fmt.Sscanf(s, "%f", &v)
	return math.Round(v*10) / 10
}

func parseBytes(s string) int64 {
	s = strings.TrimSpace(s)
	if s == "" || s == "0B" { return 0 }
	var v float64; var u string
	for i, c := range s {
		if (c >= '0' && c <= '9') || c == '.' || c == ',' { continue }
		fmt.Sscanf(s[:i], "%f", &v); u = strings.TrimSpace(s[i:]); break
	}
	switch strings.ToUpper(u) {
	case "KB", "K": return int64(v * 1024)
	case "MB", "M": return int64(v * 1024 * 1024)
	case "GB", "G": return int64(v * 1024 * 1024 * 1024)
	case "TB", "T": return int64(v * 1024 * 1024 * 1024 * 1024)
	default: return int64(v)
	}
}
