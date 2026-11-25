#!/usr/bin/env python3
"""Membuat peta statis WebView dari ubin OpenStreetMap untuk setiap destinasi.

Jalankan dari root proyek:
    python server/database/generate_static_maps.py

Output: server/wisatajogja/assets/img/peta/<slug>.webp
"""
from __future__ import annotations

import io
import math
import os
import time

import pymysql
import requests
from PIL import Image, ImageDraw, ImageFont

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "wisatajogja", "assets", "img", "peta")
ZOOM = 14
SIZE = (720, 360)
USER_AGENT = "WisataJogjaUTS/1.0 (educational project; contact halo@wisatajogja.id)"


def main() -> None:
    password = os.environ.get("WJ_DB_PASS")
    if not password:
        raise SystemExit("Set WJ_DB_PASS terlebih dahulu.")
    connection = pymysql.connect(
        host=os.environ.get("WJ_DB_HOST", "127.0.0.1"),
        user=os.environ.get("WJ_DB_USER", "apps"),
        password=password,
        database=os.environ.get("WJ_DB_NAME", "wisatajogjaapps"),
        charset="utf8mb4",
    )
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    session = requests.Session()
    session.headers.update({"User-Agent": USER_AGENT})
    cache = {}

    def tile(x: int, y: int) -> Image.Image:
        if (x, y) in cache:
            return cache[(x, y)]
        response = session.get(f"https://tile.openstreetmap.org/{ZOOM}/{x}/{y}.png", timeout=40)
        response.raise_for_status()
        image = Image.open(io.BytesIO(response.content)).convert("RGB")
        cache[(x, y)] = image
        time.sleep(0.04)
        return image

    with connection.cursor() as cursor:
        cursor.execute("SELECT slug, nama, latitude, longitude FROM destinasi ORDER BY id")
        rows = cursor.fetchall()
    for slug, name, lat, lon in rows:
        lat, lon = float(lat), float(lon)
        count = 2 ** ZOOM
        pixel_x = ((lon + 180.0) / 360.0 * count) * 256
        pixel_y = (1.0 - math.log(math.tan(math.radians(lat)) + 1 / math.cos(math.radians(lat))) / math.pi) / 2.0 * count * 256
        offset_x, offset_y = int(pixel_x - SIZE[0] / 2), int(pixel_y - SIZE[1] / 2)
        tile_x, tile_y = offset_x // 256, offset_y // 256
        end_x, end_y = (offset_x + SIZE[0] - 1) // 256, (offset_y + SIZE[1] - 1) // 256
        mosaic = Image.new("RGB", ((end_x - tile_x + 1) * 256, (end_y - tile_y + 1) * 256), (220, 230, 235))
        for x in range(tile_x, end_x + 1):
            for y in range(tile_y, end_y + 1):
                mosaic.paste(tile(x % count, y), ((x - tile_x) * 256, (y - tile_y) * 256))
        crop = mosaic.crop((
            offset_x - tile_x * 256,
            offset_y - tile_y * 256,
            offset_x - tile_x * 256 + SIZE[0],
            offset_y - tile_y * 256 + SIZE[1],
        ))
        draw = ImageDraw.Draw(crop)
        cx, cy = SIZE[0] // 2, SIZE[1] // 2
        draw.ellipse((cx - 15, cy - 42, cx + 15, cy - 12), fill="#B95C36", outline="#FBF7EE", width=3)
        draw.ellipse((cx - 5, cy - 32, cx + 5, cy - 22), fill="#F6E5BD")
        draw.polygon([(cx - 7, cy - 16), (cx + 7, cy - 16), (cx, cy + 2)], fill="#B95C36")
        font = ImageFont.load_default(size=11)
        draw.rectangle((SIZE[0] - 142, SIZE[1] - 18, SIZE[0], SIZE[1]), fill=(251, 247, 238))
        draw.text((SIZE[0] - 137, SIZE[1] - 16), "© OpenStreetMap", fill="#5F5B52", font=font)
        path = os.path.join(OUTPUT_DIR, f"{slug}.webp")
        crop.save(path, "WEBP", quality=85, method=6)
        print(f"{name}: {path}")
    connection.close()


if __name__ == "__main__":
    main()
