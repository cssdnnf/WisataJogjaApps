#!/usr/bin/env python3
"""Mengisi database Wisata Jogja dengan data awal untuk prototipe UTS.

Jalankan di mesin yang dapat menjangkau MySQL server:
    WJ_DB_HOST=... WJ_DB_USER=... WJ_DB_PASS=... python seed_database.py
"""
from __future__ import annotations

import datetime as dt
import os
import random
import sys

import pymysql

SCHEMA_PATH = os.path.join(os.path.dirname(__file__), "schema.sql")
random.seed(26042026)

USER_HASH = "$2b$10$wciOY9OcKKSRhcImfaTxPe.WDybsWYWhU58ktTJJwIt.8HRawnoii"  # wisata123
ADMIN_HASH = "$2b$10$TbqAoG6rlxbhPNnDi4CtCusOtka/TUkBodZSK2VrAPbKKgQthcISK"  # admin123

DESTINATIONS = [
    {
        "nama": "Pantai Timang", "slug": "pantai-timang", "kategori": "Pantai", "area": "Gunungkidul",
        "harga": 30000, "jam": "07.00 - 18.00", "lat": -8.1756475, "lng": 110.6632846,
        "unggulan": 1, "status": "terbit", "dilihat": 6842,
        "singkat": "Pantai karang dengan gondola ikonik dan ombak selatan yang dramatis.",
        "deskripsi": "Pantai Timang adalah pantai karang di pesisir Gunungkidul yang terkenal dengan gondola kayu penyeberangan menuju Pulau Panjang. Ombak Samudra Hindia menghantam karang dengan dramatis, menciptakan pemandangan yang cocok untuk pencinta petualangan. Pengunjung dapat menaiki gondola tradisional atau menikmati jeep trail di sepanjang pantai. Datanglah pagi hari untuk cahaya terbaik dan laut yang lebih tenang.",
        "services": [("layanan", "Gondola Timang", "Penyeberangan gondola tradisional ke Pulau Panjang", 200000, "beach"),
                     ("komunitas", "Komunitas Jeep Timang", "Jeep trail bersama warga lokal pesisir", 350000, "jeep"),
                     ("layanan", "Pemandu Lokal", "Pemandu berpengalaman untuk rute karang aman", 100000, "local_guide")],
    },
    {
        "nama": "Pantai Parangtritis", "slug": "pantai-parangtritis", "kategori": "Pantai", "area": "Bantul",
        "harga": 10000, "jam": "06.00 - 19.00", "lat": -8.0272827, "lng": 110.3370078,
        "unggulan": 1, "status": "terbit", "dilihat": 9280,
        "singkat": "Hamparan pasir hitam legendaris dengan pemandangan matahari terbenam.",
        "deskripsi": "Parangtritis menghadirkan garis pantai panjang dengan pasir hitam dan bukit karang sebagai latar. Tempat ini lekat dengan legenda Ratu Kidul dan menjadi salah satu ikon wisata Bantul. Nikmati sunset dari tepi pantai, naik andong, atau mencoba ATV di jalur pasir. Fasilitas kuliner dan penginapan tersedia di sekitar kawasan.",
        "services": [("layanan", "Sewa ATV Pantai", "Jelajahi pasir pantai dengan ATV", 150000, "jeep"),
                     ("komunitas", "Pokdarwis Parangtritis", "Pemanduan dan informasi wisata warga lokal", 0, "groups"),
                     ("layanan", "Kuliner Seafood", "Rekomendasi warung seafood pesisir", 75000, "food")],
    },
    {
        "nama": "Gunung Merapi", "slug": "gunung-merapi", "kategori": "Alam", "area": "Sleman",
        "harga": 30000, "jam": "06.00 - 17.00", "lat": -7.5407, "lng": 110.4459,
        "unggulan": 1, "status": "terbit", "dilihat": 7810,
        "singkat": "Petualangan jeep lava tour di kaki gunung paling aktif di Indonesia.",
        "deskripsi": "Kawasan Merapi di Sleman menawarkan lanskap vulkanik yang memukau, dari Kaliurang hingga Museum Sisa Hartaku. Jeep lava tour membawa pengunjung menyusuri jalur bekas erupsi dan bunker kaliadem. Udara sejuk dan panorama puncak Merapi menjadikan tempat ini favorit untuk wisata alam. Persiapkan jaket dan sepatu yang nyaman.",
        "services": [("layanan", "Jeep Lava Tour", "Rute Kaliadem, bunker, dan sisa erupsi", 400000, "jeep"),
                     ("komunitas", "Komunitas Jeep Merapi", "Driver dan pemandu lokal bersertifikat", 0, "groups"),
                     ("layanan", "Homestay Kaliurang", "Menginap dengan pemandangan Merapi", 350000, "homestay")],
    },
    {
        "nama": "Candi Prambanan", "slug": "candi-prambanan", "kategori": "Budaya", "area": "Sleman",
        "harga": 50000, "jam": "06.30 - 17.00", "lat": -7.7522269, "lng": 110.4915331,
        "unggulan": 1, "status": "terbit", "dilihat": 8460,
        "singkat": "Mahakarya arsitektur Hindu abad ke-9 dengan siluet yang megah.",
        "deskripsi": "Candi Prambanan merupakan kompleks candi Hindu terbesar di Indonesia dan situs warisan dunia UNESCO. Tiga candi utama yang didedikasikan untuk Siwa, Wisnu, dan Brahma menjulang di tengah taman yang tertata. Relief Ramayana pada dinding candi menyimpan kisah epik yang memukau. Pagi dan sore hari memberi cahaya paling indah untuk berfoto.",
        "services": [("layanan", "Pemandu Sejarah", "Tur bercerita relief Ramayana bersama pemandu", 150000, "local_guide"),
                     ("komunitas", "Komunitas Seni Prambanan", "Pertunjukan seni dan kriya warga sekitar", 0, "groups"),
                     ("layanan", "Museum & Galeri", "Akses koleksi sejarah di kompleks candi", 0, "museum")],
    },
    {
        "nama": "Malioboro", "slug": "malioboro", "kategori": "Budaya", "area": "Kota Yogyakarta",
        "harga": 0, "jam": "24 jam", "lat": -7.7932485, "lng": 110.3657751,
        "unggulan": 1, "status": "terbit", "dilihat": 11230,
        "singkat": "Jantung kota Yogyakarta yang hidup dengan seni, kuliner, dan cerita.",
        "deskripsi": "Jalan Malioboro adalah denyut nadi Yogyakarta yang tidak pernah tidur. Deretan pedagang, pertunjukan musik jalanan, dan bangunan kolonial menciptakan suasana khas. Cicipi gudeg, berburu batik, atau duduk di trotoar sambil menikmati lampu kota. Area ini juga menjadi titik awal menjelajah Keraton dan Taman Sari.",
        "services": [("komunitas", "Komunitas Malioboro", "Seniman jalanan dan pemandu cerita kota", 0, "groups"),
                     ("layanan", "Heritage Walking Tour", "Jelajahi sejarah Malioboro bersama guide", 120000, "local_guide"),
                     ("layanan", "Kuliner Malam", "Rekomendasi gudeg dan angkringan lokal", 50000, "food")],
    },
    {
        "nama": "Goa Jomblang", "slug": "goa-jomblang", "kategori": "Alam", "area": "Gunungkidul",
        "harga": 450000, "jam": "08.00 - 14.00", "lat": -8.0286285, "lng": 110.6382419,
        "unggulan": 1, "status": "terbit", "dilihat": 6120,
        "singkat": "Cahaya surga di dasar goa vertikal yang menjadi ikon Gunungkidul.",
        "deskripsi": "Goa Jomblang menawarkan pengalaman turun ke kedalaman bumi melalui lubang vertikal dan hutan purba di dasarnya. Berkas cahaya matahari yang jatuh dari mulut goa menciptakan fenomena 'cahaya surga' yang magis. Perjalanan dibatasi per hari dan dipandu tim penyelamat profesional. Reservasi awal sangat disarankan.",
        "services": [("layanan", "Tur Goa & Cave Tubing", "Paket tur lengkap dengan perlengkapan keselamatan", 450000, "hiking"),
                     ("komunitas", "Tim Penyelamat Jomblang", "Pemandu lokal terlatih dan tim SAR", 0, "groups"),
                     ("layanan", "Sewa Sepatu & Headlamp", "Perlengkapan tambahan untuk jalur goa", 25000, "camera")],
    },
    {
        "nama": "HeHa Sky View", "slug": "heha-sky-view", "kategori": "Alam", "area": "Gunungkidul",
        "harga": 25000, "jam": "10.00 - 21.00", "lat": -7.8491296, "lng": 110.4782259,
        "unggulan": 1, "status": "terbit", "dilihat": 5390,
        "singkat": "Spot perbukitan dengan taman warna-warni dan panorama kota Jogja.",
        "deskripsi": "HeHa Sky View berada di perbukitan Patuk dengan pemandangan kota Yogyakarta dari ketinggian. Taman tematik, spot foto, dan area kuliner membuatnya cocok untuk keluarga. Menjelang senja, lampu kota mulai menyala dan menciptakan suasana romantis. Tersedia area parkir luas dan akses jalan yang sudah baik.",
        "services": [("layanan", "Oleh-oleh HeHa", "Aneka camilan dan suvenir khas Jogja", 50000, "food"),
                     ("komunitas", "Komunitas Seni HeHa", "Musik akustik dan pertunjukan akhir pekan", 0, "groups"),
                     ("layanan", "Sewa Bean Bag", "Bersantai di area pandang sunset", 25000, "beach")],
    },
    {
        "nama": "Taman Sari", "slug": "taman-sari", "kategori": "Budaya", "area": "Kota Yogyakarta",
        "harga": 15000, "jam": "09.00 - 15.00", "lat": -7.80985, "lng": 110.35910,
        "unggulan": 0, "status": "terbit", "dilihat": 4720,
        "singkat": "Pesona istana air Keraton Yogyakarta dengan lorong bawah tanah.",
        "deskripsi": "Taman Sari adalah kompleks istana air peninggalan Sultan Hamengkubuwono I. Kolam pemandian, lorong bawah tanah, dan arsitektur Jawa-Eropa menjadi daya tarik utamanya. Jelajahi sumur Gumuling yang ikonik dan abadikan momen di jendela lengkungnya. Pemandu lokal siap menceritakan sejarah kompleks ini.",
        "services": [("layanan", "Tur Sejarah Taman Sari", "Bersama pemandu lokal bersertifikat", 100000, "local_guide"),
                     ("komunitas", "Komunitas Kampung Taman", "Batik dan kriya warga sekitar istana", 0, "groups"),
                     ("layanan", "Sewa Kamera", "Dokumentasi foto di spot istana air", 50000, "camera")],
    },
    {
        "nama": "Pantai Drini", "slug": "pantai-drini", "kategori": "Pantai", "area": "Gunungkidul",
        "harga": 10000, "jam": "07.00 - 18.00", "lat": -8.1380974, "lng": 110.5783985,
        "unggulan": 0, "status": "draf", "dilihat": 1980,
        "singkat": "Pantai berpasir putih dengan pulau karang kecil dan laguna tenang.",
        "deskripsi": "Pantai Drini memiliki karakter unik berupa laguna tenang di sisi barat dan ombak besar di sisi timur. Pulau karang kecil dengan pohon hijau menjadi latar foto yang khas. Tempat ini cocok untuk bersantai bersama keluarga sambil menikmati kelapa muda. Fasilitas gazebo dan warung tersedia di sepanjang pantai.",
        "services": [("layanan", "Sewa Gazebo", "Bersantai menghadap laguna Drini", 50000, "beach"),
                     ("komunitas", "Pokdarwis Drini", "Pengelolaan pantai dan pemanduan lokal", 0, "groups"),
                     ("layanan", "Kuliner Kelapa Muda", "Minuman segar khas pesisir", 15000, "food")],
    },
    {
        "nama": "Kalibiru", "slug": "kalibiru", "kategori": "Alam", "area": "Kulon Progo",
        "harga": 30000, "jam": "07.00 - 17.00", "lat": -7.8034810, "lng": 110.1257488,
        "unggulan": 0, "status": "draf", "dilihat": 2640,
        "singkat": "Bukit pandang Waduk Sermo dengan spot foto di atas pohon.",
        "deskripsi": "Kalibiru menawarkan panorama perbukitan hijau dan Waduk Sermo dari ketinggian. Spot foto ikonik di atas pohon menarik pengunjung dari berbagai kota. Warga setempat mengelola kawasan ini melalui kelompok tani dan pokdarwis. Udara pagi yang sejuk membuatnya nyaman untuk berjalan santai.",
        "services": [("layanan", "Spot Foto Bukit", "Akses spot foto dan pemandu keamanan", 30000, "camera"),
                     ("komunitas", "Kelompok Tani Kalibiru", "Pengelola wisata berbasis masyarakat", 0, "groups"),
                     ("layanan", "Jeep Bukit", "Keliling perbukitan bersama warga", 250000, "jeep")],
    },
]

COMMENTS = [
    "Tempatnya luar biasa, pemandangannya tidak membuat kecewa.",
    "Datang pagi-pagi jauh lebih sepi dan cahayanya bagus untuk foto.",
    "Akses jalan sudah nyaman, fasilitas umum juga bersih.",
    "Pemandu lokalnya ramah dan banyak bercerita tentang sejarah tempat ini.",
    "Cocok untuk healing sebentar dari rutinitas kota.",
    "Tiketnya masih terjangkau untuk pengalaman yang didapat.",
    "Lokasi bersih, tetapi saat sore cukup ramai pengunjung.",
    "Sensasinya khas Jogja, budaya dan alamnya terasa sekali.",
    "Saya datang bersama keluarga dan semuanya menikmati.",
    "Pemandangan sunset di sini benar-benar indah.",
    "Akan kembali lagi saat musim kemarau untuk langit yang lebih cerah.",
    "Peralatan dan arahan keselamatan dari petugas sangat jelas.",
    "Spot fotonya banyak, tapi tetap harus jaga kebersihan.",
    "Makanan di sekitar lokasi enak dan harganya wajar.",
    "Pelayanan tiket cepat meskipun antrean cukup panjang.",
    "Pengalaman yang tidak bisa ditemukan di kota lain.",
]

FIRST_NAMES = ["Aisyah", "Bagas", "Citra", "Damar", "Eka", "Farhan", "Gita", "Hendra", "Intan", "Joko",
               "Kirana", "Laras", "Made", "Naufal", "Oki", "Putri", "Raka", "Sari", "Tegar", "Umi",
               "Vina", "Wahyu", "Yasmin", "Zaki", "Adi", "Bella", "Cahya", "Dewi", "Erwin", "Fajar",
               "Galih", "Hana", "Indra", "Jihan", "Krisna", "Laily", "Mira", "Nanda", "Oscar", "Prita"]
LAST_NAMES = ["Prasetyo", "Rahmawati", "Nugroho", "Saputra", "Wulandari", "Hidayat", "Kusuma", "Lestari",
              "Firmansyah", "Anggraini", "Setiawan", "Maharani", "Pradana", "Sari", "Wijaya", "Utami",
              "Santoso", "Ayu", "Ramadhan", "Puspita", "Hartono", "Safitri", "Maulana", "Dewanti",
              "Surya", "Prameswari", "Fauzi", "Handayani", "Arifin", "Sekar"]


def connect() -> pymysql.connections.Connection:
    return pymysql.connect(
        host=os.environ.get("WJ_DB_HOST", "127.0.0.1"),
        port=int(os.environ.get("WJ_DB_PORT", "3306")),
        user=os.environ.get("WJ_DB_USER", "apps"),
        password=os.environ["WJ_DB_PASS"],
        database=os.environ.get("WJ_DB_NAME", "wisatajogjaapps"),
        charset="utf8mb4",
        autocommit=False,
    )


def main() -> None:
    con = connect()
    try:
        with con.cursor() as cur:
            with open(SCHEMA_PATH, "r", encoding="utf-8") as schema_file:
                schema_sql = "\n".join(
                    line for line in schema_file.read().splitlines()
                    if not line.strip().startswith("--")
                )
            statements = [s.strip() for s in schema_sql.split(";") if s.strip()]
            for statement in statements:
                cur.execute(statement)

            today = dt.date.today()
            now = dt.datetime.now().replace(microsecond=0)
            bagas_created = now - dt.timedelta(days=320)
            nafisa_created = now - dt.timedelta(days=210)

            cur.execute(
                "INSERT INTO users (id, nama, username, email, password_hash, role, status, telepon, domisili, bio, foto_profil, created_at, last_login_at) "
                "VALUES (1, %s, %s, %s, %s, 'admin', 'aktif', %s, %s, %s, %s, %s, %s)",
                ("Ferdinand", "dinandxd", "admin@gmail.com", ADMIN_HASH, "085157060802",
                 "Yogyakarta", "Admin Wisata Jogja. Menjaga cerita dan pengalaman tetap rapi.",
                 "assets/img/profil/bagas.webp", bagas_created, now - dt.timedelta(hours=2)),
            )
            cur.execute(
                "INSERT INTO users (id, nama, username, email, password_hash, role, status, telepon, domisili, bio, foto_profil, created_at, last_login_at) "
                "VALUES (2, %s, %s, %s, %s, 'user', 'aktif', %s, %s, %s, %s, %s, %s)",
                ("Nafisa Rahma", "nafisa", "nafisa@wisatajogja.id", USER_HASH, "0813 9021 4477",
                 "Yogyakarta", "Pencari cerita di setiap sudut Jogja.", "assets/img/profil/nafisa.webp",
                 nafisa_created, now - dt.timedelta(hours=5)),
            )

            # 1.247 pengguna lain -> total 1.248 pengguna biasa + 1 admin.
            user_rows = []
            emails = {"admin@gmail.com", "nafisa@wisatajogja.id"}
            for i in range(1, 1248):
                nama = f"{FIRST_NAMES[i % len(FIRST_NAMES)]} {LAST_NAMES[(i * 7) % len(LAST_NAMES)]}"
                email = f"user{i:04d}@wisatajogja.id"
                while email in emails:
                    email = f"user{i:04d}{random.randint(1, 9)}@wisatajogja.id"
                emails.add(email)
                created = now - dt.timedelta(days=random.randint(0, 240), hours=random.randint(0, 23))
                status = "nonaktif" if random.random() < 0.045 else "aktif"
                last_login = None if status == "nonaktif" else now - dt.timedelta(days=random.randint(0, 40), hours=random.randint(0, 20))
                domisili = random.choice(["Yogyakarta", "Sleman", "Bantul", "Gunungkidul", "Kulon Progo",
                                          "Semarang", "Surabaya", "Jakarta", "Bandung", "Magelang"])
                user_rows.append((nama, f"user{i:04d}", email, USER_HASH, status, "Yogyakarta" if i % 5 == 0 else domisili,
                                  None, created, last_login))
            cur.executemany(
                "INSERT INTO users (nama, username, email, password_hash, role, status, domisili, foto_profil, created_at, last_login_at) "
                "VALUES (%s, %s, %s, %s, 'user', %s, %s, %s, %s, %s)",
                user_rows,
            )

            destination_ids: dict[str, int] = {}
            published_ids: list[int] = []
            for d in DESTINATIONS:
                cur.execute(
                    "INSERT INTO destinasi (nama, slug, kategori, area, deskripsi_singkat, deskripsi, gambar, harga_tiket, jam_buka, "
                    "rating, jumlah_ulasan, jumlah_dilihat, latitude, longitude, status, unggulan, dibuat_oleh, created_at, updated_at) "
                    "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 0, 0, %s, %s, %s, %s, %s, 1, %s, %s)",
                    (d["nama"], d["slug"], d["kategori"], d["area"], d["singkat"], d["deskripsi"],
                     ("assets/img/destinasi/pantai-timang-gondola.webp" if d["slug"] == "pantai-timang"
                      else f"assets/img/destinasi/{d['slug']}-1.webp"), d["harga"], d["jam"], d["dilihat"],
                     d["lat"], d["lng"], d["status"], d["unggulan"],
                     now - dt.timedelta(days=random.randint(20, 200)), now - dt.timedelta(days=random.randint(0, 10))),
                )
                destination_id = cur.lastrowid
                destination_ids[d["slug"]] = destination_id
                if d["status"] == "terbit":
                    published_ids.append(destination_id)
                for order in range(2, 4):
                    image_path = f"assets/img/destinasi/{d['slug']}-{order}.webp"
                    cur.execute("INSERT INTO galeri_destinasi (destinasi_id, gambar, urutan) VALUES (%s, %s, %s)",
                                (destination_id, image_path, order - 1))
                for order, (tipe, nama, deskripsi, harga, ikon) in enumerate(d["services"], start=1):
                    cur.execute(
                        "INSERT INTO layanan (destinasi_id, tipe, nama, deskripsi, harga, ikon, urutan) VALUES (%s, %s, %s, %s, %s, %s, %s)",
                        (destination_id, tipe, nama, deskripsi, harga, ikon, order),
                    )

            # 386 favorit termasuk 2 milik Nafisa.
            favorite_pairs: set[tuple[int, int]] = {(2, destination_ids["pantai-timang"]), (2, destination_ids["goa-jomblang"])}
            while len(favorite_pairs) < 386:
                user_id = random.randint(3, 1248)
                favorite_pairs.add((user_id, random.choice(published_ids)))
            favorite_rows = []
            for user_id, destination_id in favorite_pairs:
                favorite_rows.append((user_id, destination_id,
                                      now - dt.timedelta(days=random.randint(0, 120), hours=random.randint(0, 23))))
            cur.executemany("INSERT INTO favorit (user_id, destinasi_id, created_at) VALUES (%s, %s, %s)", favorite_rows)

            # 900 ulasan: 8 dari Nafisa + 892 dari pengguna lain.
            nafisa_reviews = [
                (destination_ids["pantai-timang"], 5, "Gondolanya seru dan pemandangan karangnya luar biasa."),
                (destination_ids["pantai-parangtritis"], 5, "Sunset di sini selalu jadi alasan untuk kembali."),
                (destination_ids["gunung-merapi"], 4, "Jeep tour-nya mendebarkan, pemandu sangat informatif."),
                (destination_ids["candi-prambanan"], 5, "Relief Ramayana-nya indah dan sarat cerita."),
                (destination_ids["malioboro"], 5, "Malam di Malioboro tidak pernah membosankan."),
                (destination_ids["goa-jomblang"], 5, "Cahaya surga di dasar goa benar-benar magis."),
                (destination_ids["heha-sky-view"], 4, "Tempatnya instagramable, datang saat senja."),
                (destination_ids["taman-sari"], 4, "Lorong istana airnya unik dan fotogenik."),
            ]
            review_rows = []
            used_pairs: set[tuple[int, int]] = set()
            for destination_id, rating, comment in nafisa_reviews:
                used_pairs.add((2, destination_id))
                review_rows.append((2, destination_id, rating, comment,
                                    now - dt.timedelta(days=random.randint(3, 90), hours=random.randint(0, 20))))
            target_reviews = 892
            while len(review_rows) < target_reviews + 8:
                user_id = random.randint(3, 1248)
                destination_id = random.choice(published_ids)
                pair = (user_id, destination_id)
                if pair in used_pairs:
                    continue
                used_pairs.add(pair)
                roll = random.random()
                rating = 5 if roll < 0.62 else 4 if roll < 0.92 else 3 if roll < 0.98 else 2
                review_rows.append((user_id, destination_id, rating, random.choice(COMMENTS),
                                    now - dt.timedelta(days=random.randint(0, 180), hours=random.randint(0, 23))))
            cur.executemany(
                "INSERT INTO ulasan (user_id, destinasi_id, rating, komentar, created_at) VALUES (%s, %s, %s, %s, %s)",
                review_rows,
            )

            # Statistik 60 hari; 30 hari terakhir berjumlah 24.861 kunjungan.
            weights = []
            for offset in range(59, -1, -1):
                day = today - dt.timedelta(days=offset)
                weight = 0.72 + (59 - offset) / 95.0
                if day.weekday() >= 5:
                    weight *= 1.24
                weights.append(weight + random.uniform(-0.05, 0.05))
            recent_weights = weights[30:]
            recent_total = sum(recent_weights)
            stats_rows = []
            remaining_recent = 24861
            for index, weight in enumerate(weights):
                day = today - dt.timedelta(days=59 - index)
                if index < 30:
                    value = int(round(24861 * 0.84 * weight / sum(weights[:30])))
                elif index == 59:
                    value = remaining_recent
                else:
                    value = int(round(24861 * weight / recent_total))
                    value = min(value, max(0, remaining_recent - (59 - index) * 350))
                if index >= 30:
                    remaining_recent -= value
                stats_rows.append((day, max(300, value)))
            stats_rows[-1] = (stats_rows[-1][0], stats_rows[-1][1] + remaining_recent)
            cur.executemany("INSERT INTO statistik_harian (tanggal, kunjungan) VALUES (%s, %s)", stats_rows)

            # Reservasi: 12 kunjungan selesai + 1 mendatang untuk Nafisa.
            reservation_rows = []
            for i in range(12):
                destination_id = published_ids[i % len(published_ids)]
                visit = today - dt.timedelta(days=14 + i * 19)
                code = f"WJ-{visit.strftime('%y%m%d')}-{1000 + i}"
                harga = 50000 if destination_id == destination_ids["candi-prambanan"] else 30000
                reservation_rows.append((code, 2, destination_id, visit, random.randint(1, 4),
                                         harga * 2, "Kunjungan akhir pekan bersama keluarga.", "selesai",
                                         now - dt.timedelta(days=15 + i * 19)))
            visit_upcoming = today + dt.timedelta(days=9)
            reservation_rows.append((f"WJ-{visit_upcoming.strftime('%y%m%d')}-2048", 2,
                                     destination_ids["goa-jomblang"], visit_upcoming, 2, 900000,
                                     "Membawa perlengkapan ganti dan sepatu anti slip.", "dikonfirmasi",
                                     now - dt.timedelta(days=2)))
            reservation_rows.append((f"WJ-{(today + dt.timedelta(days=4)).strftime('%y%m%d')}-3182", 15,
                                     destination_ids["pantai-parangtritis"], today + dt.timedelta(days=4), 3, 30000,
                                     None, "menunggu", now - dt.timedelta(days=1)))
            reservation_rows.append((f"WJ-{(today + dt.timedelta(days=6)).strftime('%y%m%d')}-4770", 27,
                                     destination_ids["candi-prambanan"], today + dt.timedelta(days=6), 2, 100000,
                                     "Datang pagi untuk sesi foto.", "menunggu", now - dt.timedelta(hours=9)))
            cur.executemany(
                "INSERT INTO reservasi (kode_booking, user_id, destinasi_id, tanggal_kunjungan, jumlah_orang, total_harga, catatan, status, created_at) "
                "VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)",
                reservation_rows,
            )

            # Sinkronkan rating dan jumlah ulasan di tabel destinasi.
            cur.execute(
                "UPDATE destinasi d SET d.jumlah_ulasan = (SELECT COUNT(*) FROM ulasan u WHERE u.destinasi_id = d.id), "
                "d.rating = COALESCE((SELECT ROUND(AVG(u.rating), 1) FROM ulasan u WHERE u.destinasi_id = d.id), 0)"
            )
            con.commit()
            cur.execute("SELECT COUNT(*) FROM users")
            users_count = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM destinasi")
            destinations_count = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM ulasan")
            reviews_count = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM favorit")
            favorite_count = cur.fetchone()[0]
            cur.execute("SELECT SUM(kunjungan) FROM statistik_harian WHERE tanggal >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)")
            monthly_visits = cur.fetchone()[0]
            print(f"Seed selesai: {users_count} pengguna, {destinations_count} destinasi, "
                  f"{reviews_count} ulasan, {favorite_count} favorit, {monthly_visits} kunjungan/30 hari.")
    except Exception:
        con.rollback()
        raise
    finally:
        con.close()


if __name__ == "__main__":
    try:
        main()
    except KeyError:
        print("Set WJ_DB_PASS terlebih dahulu.", file=sys.stderr)
        raise
