<?php
declare(strict_types=1);

function handle_favorite_toggle(): never
{
    $user = require_user();
    $input = request_input();
    $destinasiId = input_int($input, 'destinasi_id');
    if ($destinasiId <= 0) {
        respond_error('Destinasi tidak valid.');
    }
    $check = db()->prepare('SELECT id FROM destinasi WHERE id = ? LIMIT 1');
    $check->execute([$destinasiId]);
    if (!$check->fetch()) {
        respond_error('Destinasi tidak ditemukan.', 404);
    }
    $exists = db()->prepare('SELECT id FROM favorit WHERE user_id = ? AND destinasi_id = ? LIMIT 1');
    $exists->execute([$user['id'], $destinasiId]);
    $favorite = $exists->fetch();
    if ($favorite) {
        db()->prepare('DELETE FROM favorit WHERE id = ?')->execute([$favorite['id']]);
        respond_ok(['favorit' => false], 'Dihapus dari lokasi tersimpan.');
    }
    db()->prepare('INSERT INTO favorit (user_id, destinasi_id, created_at) VALUES (?, ?, NOW())')->execute([$user['id'], $destinasiId]);
    respond_ok(['favorit' => true], 'Disimpan ke lokasi tersimpan.');
}

function handle_favorite_list(): never
{
    $user = require_user();
    $stmt = db()->prepare('SELECT d.*, 1 AS favorit, f.created_at AS disimpan_pada FROM favorit f JOIN destinasi d ON d.id = f.destinasi_id WHERE f.user_id = ? AND d.status = "terbit" ORDER BY f.created_at DESC');
    $stmt->execute([$user['id']]);
    respond_ok(array_map('destination_payload', $stmt->fetchAll()));
}

function handle_reservation_list(): never
{
    $user = require_user();
    $stmt = db()->prepare('SELECT r.*, d.nama, d.area, d.gambar FROM reservasi r JOIN destinasi d ON d.id = r.destinasi_id WHERE r.user_id = ? ORDER BY r.created_at DESC LIMIT 30');
    $stmt->execute([$user['id']]);
    $rows = $stmt->fetchAll();
    respond_ok(array_map(static fn (array $r): array => [
        'id' => (int) $r['id'],
        'kode_booking' => $r['kode_booking'],
        'destinasi' => $r['nama'],
        'area' => $r['area'],
        'gambar' => asset_url($r['gambar']),
        'tanggal_kunjungan' => $r['tanggal_kunjungan'],
        'jumlah_orang' => (int) $r['jumlah_orang'],
        'total_harga' => (int) $r['total_harga'],
        'catatan' => $r['catatan'],
        'status' => $r['status'],
        'created_at' => $r['created_at'],
    ], $rows));
}

function handle_reservation_create(): never
{
    $user = require_user();
    $input = request_input();
    $destinasiId = input_int($input, 'destinasi_id');
    $tanggal = input_string($input, 'tanggal_kunjungan');
    $jumlah = max(1, min(20, input_int($input, 'jumlah_orang', 1)));
    $catatan = input_string($input, 'catatan');
    if ($destinasiId <= 0) {
        respond_error('Pilih destinasi terlebih dahulu.');
    }
    $date = DateTime::createFromFormat('Y-m-d', $tanggal);
    if (!$date || $date->format('Y-m-d') !== $tanggal) {
        respond_error('Tanggal kunjungan tidak valid.');
    }
    $today = new DateTime('today');
    if ($date < $today) {
        respond_error('Tanggal kunjungan tidak boleh di masa lalu.');
    }
    $stmt = db()->prepare('SELECT * FROM destinasi WHERE id = ? AND status = "terbit" LIMIT 1');
    $stmt->execute([$destinasiId]);
    $destinasi = $stmt->fetch();
    if (!$destinasi) {
        respond_error('Destinasi tidak tersedia.', 404);
    }
    $total = (int) $destinasi['harga_tiket'] * $jumlah;
    $kode = 'WJ-' . date('ymd') . '-' . strtoupper(substr(bin2hex(random_bytes(3)), 0, 4));
    $insert = db()->prepare('INSERT INTO reservasi (kode_booking, user_id, destinasi_id, tanggal_kunjungan, jumlah_orang, total_harga, catatan, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, "menunggu", NOW())');
    $insert->execute([$kode, $user['id'], $destinasiId, $tanggal, $jumlah, $total, $catatan]);
    respond_ok([
        'id' => (int) db()->lastInsertId(),
        'kode_booking' => $kode,
        'total_harga' => $total,
        'status' => 'menunggu',
    ], 'Reservasi berhasil dibuat.');
}
