package models

import "time"

type ServerStatus struct {
	Name        string          `json:"name"`
	Host        string          `json:"host"`
	Online      bool            `json:"online"`
	Type        string          `json:"type"`
	Hardware    *HardwareStatus `json:"hardware"`
	Docker      *DockerStatus   `json:"docker,omitempty"`
	CollectedAt int64           `json:"collected_at"`
	Error       string          `json:"error,omitempty"`
}

type HardwareStatus struct {
	CPU      CPUInfo    `json:"cpu"`
	Memory   MemoryInfo `json:"memory"`
	Disks    []DiskInfo `json:"disks"`
	Network  []NetInfo  `json:"network"`
	Load     LoadInfo   `json:"load"`
	Uptime   string     `json:"uptime"`
	Hostname string     `json:"hostname"`
}

type CPUInfo struct {
	Percent float64 `json:"percent"`
	Cores   int     `json:"cores"`
}

type MemoryInfo struct {
	TotalGB     float64 `json:"total_gb"`
	UsedGB      float64 `json:"used_gb"`
	FreeGB      float64 `json:"free_gb"`
	UsedPercent float64 `json:"used_percent"`
}

type DiskInfo struct {
	Filesystem  string  `json:"filesystem"`
	MountPoint  string  `json:"mount_point"`
	TotalGB     float64 `json:"total_gb"`
	UsedGB      float64 `json:"used_gb"`
	FreeGB      float64 `json:"free_gb"`
	UsedPercent float64 `json:"used_percent"`
}

type NetInfo struct {
	Interface string `json:"interface"`
	RxBytes   int64  `json:"rx_bytes"`
	TxBytes   int64  `json:"tx_bytes"`
}

type LoadInfo struct {
	Load1  float64 `json:"load_1"`
	Load5  float64 `json:"load_5"`
	Load15 float64 `json:"load_15"`
}

type DockerStatus struct {
	Info       DockerInfo      `json:"info"`
	Containers []ContainerInfo `json:"containers"`
	Images     []ImageInfo     `json:"images"`
}

type DockerInfo struct {
	ContainersTotal    int    `json:"containers_total"`
	ContainersRunning  int    `json:"containers_running"`
	ContainersPaused   int    `json:"containers_paused"`
	ContainersStopped  int    `json:"containers_stopped"`
	Version            string `json:"version"`
	ServerVersion      string `json:"server_version"`
}

type ContainerInfo struct {
	ID            string  `json:"id"`
	Name          string  `json:"name"`
	Image         string  `json:"image"`
	Status        string  `json:"status"`
	State         string  `json:"state"`
	CreatedAt     string  `json:"created_at"`
	Ports         string  `json:"ports"`
	CPUPercent    float64 `json:"cpu_percent"`
	MemPercent    float64 `json:"mem_percent"`
	MemUsage      string  `json:"mem_usage"`
	MemLimit      string  `json:"mem_limit"`
	NetworkInput  int64   `json:"network_input"`
	NetworkOutput int64   `json:"network_output"`
}

type ImageInfo struct {
	RepoTag   string `json:"repo_tag"`
	Size      string `json:"size"`
	CreatedAt string `json:"created_at"`
	ID        string `json:"id"`
}

type AllServersStatus struct {
	Servers []ServerStatus `json:"servers"`
	Updated int64          `json:"updated"`
}

func NowUnix() int64 { return time.Now().UnixMilli() }
