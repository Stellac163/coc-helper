#!/usr/bin/env python3
"""COC 跑团助手 — 本地静态服务器（带正确的 .wasm MIME 类型）。"""
import http.server
import os
import socket
import socketserver
import sys

ROOT = os.path.dirname(os.path.abspath(__file__))
DIST = os.path.join(ROOT, "composeApp", "build", "dist", "wasmJs", "productionExecutable")
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 8080

MIME = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".mjs": "text/javascript; charset=utf-8",
    ".wasm": "application/wasm",
    ".map": "application/json",
    ".json": "application/json",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".svg": "image/svg+xml",
    ".css": "text/css",
    ".txt": "text/plain; charset=utf-8",
    ".woff": "font/woff",
    ".woff2": "font/woff2",
    ".otf": "font/otf",
    ".ttf": "font/ttf",
}


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIST, **kwargs)

    def guess_type(self, path):
        ext = os.path.splitext(path)[1].lower()
        return MIME.get(ext, "application/octet-stream")

    def log_message(self, fmt, *args):
        sys.stderr.write("[%s] %s\n" % (self.log_date_time_string(), fmt % args))


def lan_ip() -> str:
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except OSError:
        return "127.0.0.1"


class Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    ip = lan_ip()
    print("=" * 60)
    print("COC 跑团助手 — 本地服务器已启动")
    print(f"  本机访问:   http://localhost:{PORT}")
    print(f"  手机访问:   http://{ip}:{PORT}   (需与电脑同一 WiFi)")
    print(f"  服务目录:   {DIST}")
    print("  按 Ctrl+C 停止")
    print("=" * 60)
    with Server(("0.0.0.0", PORT), Handler) as httpd:
        httpd.serve_forever()
