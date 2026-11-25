# Wisata Jogja

Prototipe aplikasi Android **Digital Tourism / OTA** untuk Daerah Istimewa
Yogyakarta. Aplikasi menampilkan katalog destinasi, cuaca langsung, peta,
komunitas lokal, reservasi, profil dengan foto kamera/galeri, serta panel admin.

## Akun demo

| Peran | Email               | Kata sandi      |
|---|---------------------|-----------------|
| Wisatawan | `cassiel@gmail.com` | `Qazwsx321!` |
| Admin | `admin@gmail.com`   | `admin123`      |

Masukkan email dan kata sandi demo pada halaman masuk; tombol akun demo
sudah dihapus dari UI. Guest mode tersedia lewat **Jelajahi sebagai tamu**.

## Fitur utama

- Autentikasi: masuk, daftar akun, lupa sandi (kode demo), atur ulang sandi.
- Beranda: cuaca langsung dari BMKG (UV dari Open-Meteo), destinasi unggulan,
  filter area, komunitas lokal, rekomendasi.
- Rekomendasi: pencarian, filter kategori, grid RecyclerView, favorit.
- Lokasi tersimpan: sinkron dengan tabel `favorit` di database.
- Detail destinasi: galeri, layanan/komunitas, peta WebView, ulasan, tombol tulis/ubah ulasan, dan reservasi.
- Reservasi: pilih tanggal, jumlah orang, catatan, kode booking, total harga.
- Profil: ganti foto dari kamera, galeri, atau file manager dengan editor crop/zoom/geser;
  edit profil; ubah sandi; halaman Reservasi Saya; notifikasi; bantuan; privasi.
- Reservasi Saya: daftar reservasi berupa kartu dengan status, tanggal, jumlah orang,
  total, kode booking, dan catatan.
- Admin: ringkasan metrik dan grafik, CRUD destinasi + upload gambar,
  pencarian/filter, aktivasi/nonaktivasi pengguna, pengaturan akun admin.

## Kriteria UTS yang dipenuhi

| Kriteria | Implementasi |
|---|---|
| Minimal 5 Activity | 14 Activity (`Login`, `Register`, `Forgot`, `Reset`, `Main`, `Detail`, `TulisUlasan`, `Reservasi`, `ReservasiSaya`, `EditProfil`, `UbahSandi`, `AdminMain`, `FormWisata`, `CropImage`) |
| RecyclerView / CardView | Katalog, rekomendasi, tersimpan, galeri, layanan, ulasan, destinasi admin, pengguna admin |
| WebView | Peta lokasi pada halaman detail dimuat sebagai HTML fragment di WebView |
| Tab / Bottom Navigation | Navigasi kustom terpisah di `ui/navigation` untuk pengguna dan admin |
| Fragment | Beranda, Rekomendasi, Tersimpan, Profil, Ringkasan, Kelola Wisata, Pengguna, Akun |
| Konten scrollable | NestedScrollView vertikal dan RecyclerView horizontal |
| Destinasi, area, services, komunitas, reservasi | Tabel dan UI terkait tersedia |
| Form input | Login, daftar, lupa sandi, reservasi, edit profil, form destinasi admin |
| Database | MySQL/MariaDB `wisatajogjaapps` dengan 8 tabel aplikasi |
| Cuaca | BMKG (`adm4=34.71.01.1001`) + UV Open-Meteo, fallback Open-Meteo |
| Aset gambar dari server | `https://45.127.35.252/wisatajogja/assets/...` |

## Struktur proyek

```
app/src/main/java/com/example/wisatajogja/
├── data/
│   ├── model/            # User, Destination, AdminSummary, ...
│   ├── remote/           # ApiClient (REST API, multipart upload)
│   ├── repository/       # Auth, Destination, Profile, Admin, Reservation
│   ├── session/          # SessionManager (SharedPreferences, token)
│   └── weather/          # BMKG + Open-Meteo
├── ui/
│   ├── auth/             # Masuk, daftar, lupa sandi, atur sandi
│   ├── main/             # Shell + Beranda, Rekomendasi, Tersimpan, Profil
│   ├── detail/           # Detail destinasi, galeri, WebView peta
│   ├── reservasi/        # Form reservasi
│   ├── profile/          # Edit profil + ubah sandi
│   ├── admin/            # Ringkasan, kelola wisata, pengguna, akun admin
│   ├── navigation/       # MainNavigator, AdminNavigator, BottomNavController
│   ├── common/           # SansTextView, SerifTextView, PhotoPickerDelegate
│   └── base/             # BaseActivity (edge-to-edge, locale)
└── util/                 # Formatter, image loader, photo compressor, view utils
```

Backend dan aset server:

```
server/
├── database/
│   ├── schema.sql
│   ├── seed_database.py
│   └── generate_static_maps.py
└── wisatajogja/
    ├── api/              # REST API PHP 8.5
    │   ├── index.php
    │   ├── bootstrap.php
    │   └── handlers/
    └── assets/           # gambar destinasi, profil, peta
```

## Menjalankan aplikasi

Prasyarat: Android Studio, JDK 21+, Android SDK 37, perangkat/emulator API 34+.

1. Buka proyek di Android Studio.
2. Pastikan `minSdk 34` dan koneksi internet aktif.
3. Jalankan `app` atau build APK:

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

APK hasil build: `app/build/outputs/apk/debug/app-debug.apk`.
APK siap pasang untuk UTS: `dist/WisataJogja-UTS-debug.apk`.

Base URL API ada di `data/remote/ApiClient.kt`. URL hero login ada di
`ui/auth/LoginActivity.kt`. Semua akses gambar memakai HTTPS ke server.

## Backend

- API: `https://45.127.35.252/wisatajogja/api/index.php?action=...`
- Aset publik: `https://45.127.35.252/wisatajogja/assets/...`
- Web root server: `/home/orangbaek/public_html/wisatajogja`
- Database: `wisatajogjaapps` (MariaDB 10.11), diakses **hanya** oleh backend PHP.
- Port 80/443 dibuka untuk akses GET publik; aturan firewall disimpan dengan
  `iptables-persistent`. HTTP dialihkan ke HTTPS.
- Kredensial SSH tidak pernah disimpan di aplikasi Android. Kredensial database
  hanya ada pada `.config.php` di server (lihat `.config.example.php`).

### Endpoint penting

| Action | Metode | Keterangan |
|---|---|---|
| `login`, `register`, `me`, `logout` | POST/GET | Autentikasi token Bearer |
| `forgot-password`, `reset-password` | POST | Kode pemulihan (demo mode menampilkan kode) |
| `destinasi`, `destinasi-detail`, `area` | GET | Katalog publik |
| `favorit`, `favorit-toggle` | GET/POST | Lokasi tersimpan |
| `ulasan-buat` | POST | Simpan/ubah ulasan pengunjung dan hitung ulang rating destinasi |
| `reservasi`, `reservasi-buat` | GET/POST | Reservasi pengguna |
| `profil-update`, `profil-password`, `profil-foto` | POST | Profil dan foto |
| `upload-gambar` | POST | Upload multipart gambar destinasi/profil |
| `admin-ringkasan`, `admin-pengguna`, `admin-destinasi` | GET | Data panel admin |
| `admin-pengguna-status` | POST | Aktivasi/nonaktivasi pengguna |
| `admin-destinasi-simpan`, `admin-destinasi-hapus` | POST | CRUD destinasi |

### Menyiapkan ulang database

`server/database/seed_database.py` bersifat **destruktif**: skema akan dibuat
ulang dan data awal dimasukkan.

```bash
WJ_DB_HOST=... WJ_DB_USER=... WJ_DB_PASS=... \
WJ_DB_NAME=wisatajogjaapps python server/database/seed_database.py
```

Peta statis legacy (tidak lagi dipakai aplikasi; detail memakai embed OpenStreetMap interaktif) dapat dibuat ulang dengan:

```bash
python server/database/generate_static_maps.py
```

## Sumber data dan aset

- Cuaca: BMKG `https://api.bmkg.go.id/publik/prakiraan-cuaca` dan
  Open-Meteo (UV/fallback).
- Peta: embed interaktif OpenStreetMap di WebView (tile © OpenStreetMap contributors); atribusi tampil di peta.
- Gambar destinasi: Wikimedia Commons. Foto profil demo: Unsplash.
  Detail kredit tersedia di `server/wisatajogja/assets/IMAGE_CREDITS.md`.
- Font: Playfair Display dan Inter (Google Fonts, lisensi OFL).
- Logo aplikasi: `logo-tugu.png` (aset logo yang diberikan untuk proyek ini).

## Pengujian yang sudah dilakukan

- Build debug berhasil; `testDebugUnitTest` berhasil.
- Login wisatawan/admin, registrasi, lupa sandi, atur ulang sandi.
- Cuaca BMKG tampil (contoh 26°, Hujan Ringan) dan metadata sumber.
- Filter kategori, pencarian, favorit/toggle, halaman tersimpan.
- Upload foto profil ke server melalui kamera/galeri/file manager.
- Detail destinasi, galeri, layanan, ulasan, peta WebView.
- Reservasi berhasil dibuat dan tersimpan di database.
- Admin ringkasan, tambah/edit/hapus destinasi, upload gambar, moderasi pengguna.
- Akses publik HTTPS untuk API dan gambar; konfigurasi `.config.php` tidak
  mengembalikan isi file.
- Catatan: pada emulator headless kamera dapat menampilkan bidang hitam;
  intent kamera dan izin runtime sudah terverifikasi.

## Batasan prototipe

- Pengiriman email belum dikonfigurasi; kode reset ditampilkan di aplikasi
  (mode demo) agar alur UTS tetap dapat diuji.
- Satu akun memiliki satu token aktif; login di perangkat lain menggantikan sesi
  sebelumnya.
- Peta statis disediakan untuk destinasi seed. Destinasi baru menggunakan
  fallback OpenStreetMap di WebView.
- Proyek ini adalah prototipe pembelajaran, bukan layanan produksi.

## Dokumentasi UTS

Lihat `docs/Wisata-Jogja-Dokumentasi-UTS.docx` dan folder
`docs/screenshots/` untuk tangkapan layar semua alur utama.
