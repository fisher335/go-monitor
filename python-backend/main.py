"""FastAPI 服务器监控后端 — 本机硬件 + Docker + WebSocket 推送"""

import asyncio
import json
import time
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, WebSocket, WebSocketDisconnect
from fastapi.responses import FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles

import sys
if sys.stdout.encoding and sys.stdout.encoding.upper() in ("GBK", "GB2312", "GB18030"):
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8", errors="replace")

from collector import collect_server_status
from models import AllServersStatus, ServerStatus

# 连接管理
connected_clients: set[WebSocket] = set()
latest_status: ServerStatus | None = None


async def collect_loop():
    global latest_status
    while True:
        try:
            latest_status = collect_server_status()
            payload = json.dumps({
                "servers": [latest_status.model_dump()],
                "updated": int(time.time() * 1000),
            }, ensure_ascii=False)
            for ws in set(connected_clients):
                try:
                    await ws.send_text(payload)
                except Exception:
                    connected_clients.discard(ws)
        except Exception as e:
            print(f"collect error: {e}")
        await asyncio.sleep(5)


@asynccontextmanager
async def lifespan(app: FastAPI):
    task = asyncio.create_task(collect_loop())
    print("[Go-Monitor Python] http://localhost:9502")
    yield
    task.cancel()


app = FastAPI(title="Go-Monitor", version="1.0.0", lifespan=lifespan)


@app.get("/api/health")
async def health():
    return {"code": 0, "message": "ok", "data": {"version": "1.0.0", "type": "local"}}


@app.get("/api/servers")
async def list_servers():
    all_status = AllServersStatus(
        servers=[latest_status] if latest_status else [],
        updated=int(time.time() * 1000),
    )
    return {"code": 0, "message": "success", "data": all_status.model_dump()}


@app.websocket("/api/ws")
async def websocket_endpoint(ws: WebSocket):
    await ws.accept()
    connected_clients.add(ws)
    print(f"WS connected ({len(connected_clients)})")
    try:
        if latest_status:
            data = {"servers": [latest_status.model_dump()], "updated": int(time.time() * 1000)}
            await ws.send_text(json.dumps(data, ensure_ascii=False))
        while True:
            try:
                await ws.receive_text()
            except WebSocketDisconnect:
                break
    finally:
        connected_clients.discard(ws)
        print(f"WS disconnected ({len(connected_clients)})")


# 静态文件
STATIC_DIR = Path(__file__).parent / "static"
if STATIC_DIR.exists():
    app.mount("/assets", StaticFiles(directory=str(STATIC_DIR / "assets")), name="assets")

    @app.get("/{full_path:path}")
    async def serve_spa(full_path: str):
        if full_path.startswith("api/"):
            return JSONResponse({"code": 404, "message": "not found"}, status_code=404)
        index = STATIC_DIR / "index.html"
        if index.exists():
            return FileResponse(str(index))
        return JSONResponse({"code": 404, "message": "frontend not built"}, status_code=404)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9502)
