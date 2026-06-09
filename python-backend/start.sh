#!/bin/bash
# Go-Monitor Python Backend - Linux startup script
# Usage: chmod +x start.sh && ./start.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Go-Monitor Python Backend ==="

# 1. venv
if [ ! -f ".venv/bin/python3" ] && [ ! -f ".venv/bin/python" ]; then
    echo "[1/3] Creating venv..."
    python3 -m venv .venv || python -m venv .venv
else
    echo "[1/3] venv exists"
fi

# 2. deps
PYTHON=".venv/bin/python3"
if [ ! -f "$PYTHON" ]; then
    PYTHON=".venv/bin/python"
fi

echo "[2/3] Installing dependencies..."
$PYTHON -m pip install -r requirements.txt -q

# 3. start
echo "[3/3] Starting server..."
echo ""
echo "Open browser: http://localhost:9502"
echo "Press Ctrl+C to stop"
echo ""
$PYTHON -m uvicorn main:app --host 0.0.0.0 --port 9502
