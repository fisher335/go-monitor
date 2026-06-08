package collector

import (
	"fmt"
	"math"
	"runtime"

	"go-monitor/models"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/load"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
)

type LocalCollector struct{}

func NewLocalCollector() *LocalCollector { return &LocalCollector{} }

func (c *LocalCollector) Collect() *models.HardwareStatus {
	s := &models.HardwareStatus{}
	c.collectCPU(s)
	c.collectMemory(s)
	c.collectDisks(s)
	c.collectNetwork(s)
	c.collectLoad(s)
	c.collectUptime(s)
	c.collectHostname(s)
	return s
}

func (c *LocalCollector) collectCPU(s *models.HardwareStatus) {
	p, err := cpu.Percent(0, false)
	if err == nil && len(p) > 0 {
		s.CPU.Percent = math.Round(p[0]*10) / 10
	}
	cores, err := cpu.Counts(true)
	if err == nil && cores > 0 {
		s.CPU.Cores = cores
	}
}

func (c *LocalCollector) collectMemory(s *models.HardwareStatus) {
	v, err := mem.VirtualMemory()
	if err != nil {
		return
	}
	s.Memory = models.MemoryInfo{
		TotalGB:     roundGB(float64(v.Total)),
		UsedGB:      roundGB(float64(v.Used)),
		FreeGB:      roundGB(float64(v.Free)),
		UsedPercent: math.Round(v.UsedPercent*10) / 10,
	}
}

func (c *LocalCollector) collectDisks(s *models.HardwareStatus) {
	parts, err := disk.Partitions(false)
	if err != nil {
		return
	}
	for _, p := range parts {
		if p.Fstype == "tmpfs" || p.Fstype == "devtmpfs" || p.Fstype == "squashfs" || p.Fstype == "overlay" {
			continue
		}
		if runtime.GOOS == "windows" && !(len(p.Mountpoint) == 2 && p.Mountpoint[1] == ':') {
			continue
		}
		u, err := disk.Usage(p.Mountpoint)
		if err != nil {
			continue
		}
		s.Disks = append(s.Disks, models.DiskInfo{
			Filesystem: p.Fstype, MountPoint: p.Mountpoint,
			TotalGB: roundGB(float64(u.Total)), UsedGB: roundGB(float64(u.Used)),
			FreeGB: roundGB(float64(u.Free)), UsedPercent: math.Round(u.UsedPercent*10) / 10,
		})
	}
}

func (c *LocalCollector) collectNetwork(s *models.HardwareStatus) {
	ns, err := net.IOCounters(false)
	if err != nil {
		return
	}
	for _, n := range ns {
		s.Network = append(s.Network, models.NetInfo{Interface: n.Name, RxBytes: int64(n.BytesRecv), TxBytes: int64(n.BytesSent)})
	}
}

func (c *LocalCollector) collectLoad(s *models.HardwareStatus) {
	a, err := load.Avg()
	if err != nil {
		return
	}
	s.Load = models.LoadInfo{Load1: math.Round(a.Load1*100) / 100, Load5: math.Round(a.Load5*100) / 100, Load15: math.Round(a.Load15*100) / 100}
}

func (c *LocalCollector) collectUptime(s *models.HardwareStatus) {
	u, err := host.Uptime()
	if err != nil {
		return
	}
	s.Uptime = formatUptime(u)
}

func (c *LocalCollector) collectHostname(s *models.HardwareStatus) {
	h, err := host.Info()
	if err == nil {
		s.Hostname = h.Hostname
	}
}

func roundGB(b float64) float64        { return math.Round(b/1024/1024/1024*10) / 10 }
func formatUptime(s uint64) string {
	d, h, m := int(s)/86400, (int(s)%86400)/3600, (int(s)%3600)/60
	if d > 0 { return fmt.Sprintf("%d天%d小时%d分钟", d, h, m) }
	if h > 0 { return fmt.Sprintf("%d小时%d分钟", h, m) }
	return fmt.Sprintf("%d分钟", m)
}
