-- Skema database Wisata Jogja (MariaDB/MySQL)
-- Aplikasi Android tidak menyimpan kredensial database; akses hanya melalui REST API.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS ulasan;
DROP TABLE IF EXISTS reservasi;
DROP TABLE IF EXISTS favorit;
DROP TABLE IF EXISTS layanan;
DROP TABLE IF EXISTS galeri_destinasi;
DROP TABLE IF EXISTS destinasi;
DROP TABLE IF EXISTS statistik_harian;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nama VARCHAR(100) NOT NULL,
    username VARCHAR(40) NOT NULL,
    email VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('user', 'admin') NOT NULL DEFAULT 'user',
    status ENUM('aktif', 'nonaktif') NOT NULL DEFAULT 'aktif',
    telepon VARCHAR(25) DEFAULT NULL,
    domisili VARCHAR(80) DEFAULT NULL,
    bio VARCHAR(255) DEFAULT NULL,
    foto_profil VARCHAR(255) DEFAULT NULL,
    api_token CHAR(48) DEFAULT NULL,
    reset_code CHAR(6) DEFAULT NULL,
    reset_expires_at DATETIME DEFAULT NULL,
    last_login_at DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_token (api_token),
    KEY idx_users_status (status),
    KEY idx_users_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE destinasi (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    nama VARCHAR(120) NOT NULL,
    slug VARCHAR(140) NOT NULL,
    kategori ENUM('Pantai', 'Budaya', 'Alam') NOT NULL,
    area VARCHAR(80) NOT NULL,
    deskripsi_singkat VARCHAR(255) NOT NULL,
    deskripsi TEXT NOT NULL,
    gambar VARCHAR(255) NOT NULL,
    harga_tiket INT UNSIGNED NOT NULL DEFAULT 0,
    jam_buka VARCHAR(80) NOT NULL,
    rating DECIMAL(2,1) NOT NULL DEFAULT 0.0,
    jumlah_ulasan INT UNSIGNED NOT NULL DEFAULT 0,
    jumlah_dilihat INT UNSIGNED NOT NULL DEFAULT 0,
    latitude DECIMAL(10,7) NOT NULL,
    longitude DECIMAL(10,7) NOT NULL,
    status ENUM('terbit', 'draf') NOT NULL DEFAULT 'draf',
    unggulan TINYINT(1) NOT NULL DEFAULT 0,
    dibuat_oleh INT UNSIGNED DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_destinasi_slug (slug),
    KEY idx_destinasi_status (status),
    KEY idx_destinasi_kategori (kategori),
    KEY idx_destinasi_area (area),
    CONSTRAINT fk_destinasi_admin FOREIGN KEY (dibuat_oleh) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE galeri_destinasi (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    destinasi_id INT UNSIGNED NOT NULL,
    gambar VARCHAR(255) NOT NULL,
    urutan INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_galeri_destinasi (destinasi_id),
    CONSTRAINT fk_galeri_destinasi FOREIGN KEY (destinasi_id) REFERENCES destinasi (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE layanan (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    destinasi_id INT UNSIGNED NOT NULL,
    tipe ENUM('layanan', 'komunitas') NOT NULL DEFAULT 'layanan',
    nama VARCHAR(120) NOT NULL,
    deskripsi VARCHAR(255) NOT NULL,
    harga INT UNSIGNED NOT NULL DEFAULT 0,
    ikon VARCHAR(40) NOT NULL DEFAULT 'local_guide',
    urutan INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_layanan_destinasi (destinasi_id),
    CONSTRAINT fk_layanan_destinasi FOREIGN KEY (destinasi_id) REFERENCES destinasi (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE favorit (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    destinasi_id INT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_favorit (user_id, destinasi_id),
    KEY idx_favorit_destinasi (destinasi_id),
    CONSTRAINT fk_favorit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_favorit_destinasi FOREIGN KEY (destinasi_id) REFERENCES destinasi (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservasi (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    kode_booking VARCHAR(24) NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    destinasi_id INT UNSIGNED NOT NULL,
    tanggal_kunjungan DATE NOT NULL,
    jumlah_orang TINYINT UNSIGNED NOT NULL DEFAULT 1,
    total_harga INT UNSIGNED NOT NULL DEFAULT 0,
    catatan VARCHAR(500) DEFAULT NULL,
    status ENUM('menunggu', 'dikonfirmasi', 'selesai', 'dibatalkan') NOT NULL DEFAULT 'menunggu',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_reservasi_kode (kode_booking),
    KEY idx_reservasi_user (user_id),
    KEY idx_reservasi_destinasi (destinasi_id),
    CONSTRAINT fk_reservasi_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_reservasi_destinasi FOREIGN KEY (destinasi_id) REFERENCES destinasi (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ulasan (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    destinasi_id INT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL,
    komentar VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ulasan_user_destinasi (user_id, destinasi_id),
    KEY idx_ulasan_destinasi (destinasi_id),
    CONSTRAINT fk_ulasan_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ulasan_destinasi FOREIGN KEY (destinasi_id) REFERENCES destinasi (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE statistik_harian (
    tanggal DATE NOT NULL,
    kunjungan INT UNSIGNED NOT NULL DEFAULT 0,
    PRIMARY KEY (tanggal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
