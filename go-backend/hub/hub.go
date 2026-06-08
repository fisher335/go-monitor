package hub

import (
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/gorilla/websocket"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize: 1024, WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool { return true },
}

type Client struct {
	conn *websocket.Conn
	send chan []byte
}

type Hub struct {
	mu sync.RWMutex
	clients    map[*Client]bool
	broadcast  chan []byte
	register   chan *Client
	unregister chan *Client
}

func NewHub() *Hub {
	return &Hub{
		clients: make(map[*Client]bool), broadcast: make(chan []byte, 256),
		register: make(chan *Client), unregister: make(chan *Client),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case c := <-h.register:
			h.mu.Lock(); h.clients[c] = true; n := len(h.clients); h.mu.Unlock()
			log.Printf("📡 WS连接(%d)", n)
		case c := <-h.unregister:
			h.mu.Lock()
			if _, ok := h.clients[c]; ok { delete(h.clients, c); close(c.send) }
			n := len(h.clients); h.mu.Unlock()
			log.Printf("📡 WS断开(%d)", n)
		case m := <-h.broadcast:
			h.mu.RLock()
			for c := range h.clients {
				select { case c.send <- m: default: close(c.send); delete(h.clients, c) }
			}
			h.mu.RUnlock()
		}
	}
}

func (h *Hub) Broadcast(data []byte)           { h.broadcast <- data }
func (h *Hub) ClientCount() int                { h.mu.RLock(); defer h.mu.RUnlock(); return len(h.clients) }

func (h *Hub) HandleWebSocket(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil { log.Printf("WS升级失败: %v", err); return }
	c := &Client{conn: conn, send: make(chan []byte, 256)}
	h.register <- c
	go h.writePump(c); go h.readPump(c)
}

func (h *Hub) writePump(c *Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() { ticker.Stop(); c.conn.Close() }()
	for {
		select {
		case msg, ok := <-c.send:
			if !ok { c.conn.WriteMessage(websocket.CloseMessage, nil); return }
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.TextMessage, msg); err != nil { return }
		case <-ticker.C:
			c.conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := c.conn.WriteMessage(websocket.PingMessage, nil); err != nil { return }
		}
	}
}

func (h *Hub) readPump(c *Client) {
	defer func() { h.unregister <- c; c.conn.Close() }()
	c.conn.SetReadLimit(4096)
	c.conn.SetPongHandler(func(string) error { c.conn.SetReadDeadline(time.Now().Add(60 * time.Second)); return nil })
	for {
		if _, _, err := c.conn.ReadMessage(); err != nil { break }
	}
}

func BroadcastJSON(hub *Hub, v interface{}) {
	b, err := json.Marshal(v)
	if err != nil { log.Printf("序列化失败: %v", err); return }
	hub.Broadcast(b)
}
