#!/usr/bin/env python3
"""本地 mock 苹果CMS 采集源 —— 用于验证「锤锤影视」的分类→列表→详情→播放全链路。

运行:  python mock-maccms.py            (默认 0.0.0.0:8000)
       python mock-maccms.py 8080       (换端口)

App 内填的源地址:
  安卓官方模拟器:  http://10.0.2.2:8000/api.php/provide/vod
  雷电等模拟器:    http://<电脑局域网IP>:8000/api.php/provide/vod  (脚本启动时会打印)
  真实手机(同一WiFi): 同上

数据: 2 个分类、2 部测试片（公开测试视频多码率/多格式），并故意含一条打不开的坏线路。
"""
import json
import socket
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

BAD = "https://invalid.example.com/broken.m3u8"  # 故意打不开，用于观察失败与（将来的）自动换源

MP4_720 = "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/mp4/xgplayer-demo-720p.mp4"
MP4_360 = "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/mp4/xgplayer-demo-360p.mp4"
HLS_DEMO = "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/hls/xgplayer-demo.m3u8"

CLASS = [
    {"type_id": "1", "type_name": "电影"},
    {"type_id": "2", "type_name": "剧集"},
]

VODS = [
    {
        "vod_id": "1",
        "vod_name": "演示电影（测试片）",
        "vod_pic": "",
        "vod_remarks": "测试",
        "type_id": "1",
        "type_name": "电影",
        "vod_year": "2024",
        "vod_play_from": "超清MP4$$$流畅360P$$$坏线路",
        "vod_play_url": (
            "正片$" + MP4_720
            + "$$$"
            + "正片$" + MP4_360
            + "$$$"
            + "打不开$" + BAD
        ),
    },
    {
        "vod_id": "2",
        "vod_name": "演示剧集（测试）",
        "vod_pic": "",
        "vod_remarks": "共2集",
        "type_id": "2",
        "type_name": "剧集",
        "vod_year": "2024",
        "vod_play_from": "HLS$$$坏线路",
        "vod_play_url": (
            "第01集$" + HLS_DEMO
            + "#第02集$" + MP4_360
            + "$$$"
            + "打不开$" + BAD
        ),
    },
]


def find_vods(tid=None, wd=None, pg=1):
    out = VODS
    if tid:
        out = [v for v in out if v.get("type_id") == str(tid)]
    if wd:
        out = [v for v in out if wd in v.get("vod_name", "")]
    return {"list": out, "pagecount": 1, "total": len(out)}


def find_detail(ids):
    for v in VODS:
        if v["vod_id"] == str(ids):
            return {"list": [v]}
    return {"list": []}


class Handler(BaseHTTPRequestHandler):
    def _json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        u = urlparse(self.path)
        q = {k: v[0] for k, v in parse_qs(u.query).items()}
        ac = q.get("ac", "list")
        print(f"[mock] {self.path} -> ac={ac}")
        if ac == "list":
            self._json({"class": CLASS})
        elif ac == "videolist":
            self._json(find_vods(tid=q.get("t"), wd=q.get("wd"), pg=int(q.get("pg", "1"))))
        elif ac == "detail":
            self._json(find_detail(q.get("ids", "")))
        else:
            self._json({"code": 0, "msg": f"unknown ac={ac}"}, 404)

    def log_message(self, *args):
        pass  # 请求日志已在 do_GET 打印


def lan_ips():
    ips = set()
    try:
        host = socket.gethostname()
        for info in socket.getaddrinfo(host, None, socket.AF_INET):
            ip = info[4][0]
            if not ip.startswith("127."):
                ips.add(ip)
    except Exception:
        pass
    return sorted(ips)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    print(f"mock 苹果CMS 采集源: http://0.0.0.0:{port}/api.php/provide/vod")
    print(f"  安卓官方模拟器填: http://10.0.2.2:{port}/api.php/provide/vod")
    for ip in lan_ips():
        print(f"  雷电/真机(同WiFi)可填: http://{ip}:{port}/api.php/provide/vod")
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
