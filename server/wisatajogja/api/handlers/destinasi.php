<?php
declare(strict_types=1);

function destination_query_filters(array &$where, array &$params): void
{
    $kategori = trim((string) ($_GET['kategori'] ?? ''));
    $area = trim((string) ($_GET['area'] ?? ''));
    $q = trim((string) ($_GET['q'] ?? ''));
    if ($kategori !== '' && $kategori !== 'Semua') {
        $where[] = 'kategori = ?';
        $params[] = $kategori;
    }
    if ($area !== '' && $area !== 'Semua') {
        $where[] = 'area = ?';
        $params[] = $area;
    }
    if ($q !== '') {
        $where[] = '(nama LIKE ? OR area LIKE ? OR deskripsi_singkat LIKE ?)';
        $like = '%' . $q . '%';
        array_push($params, $like, $like, $like);
    }
}

function handle_destination_list(): never
{
    $where = ['status = "terbit"'];
    $params = [];
    destination_query_filters($where, $params);
    if (($_GET['unggulan'] ?? '') === '1') {
        $where[] = 'unggulan = 1';
    }
    $limit = max(1, min(60, (int) ($_GET['limit'] ?? 30)));
    $sql = 'SELECT * FROM destinasi WHERE ' . implode(' AND ', $where) . ' ORDER BY unggulan DESC, rating DESC, jumlah_ulasan DESC LIMIT ' . $limit;
    $stmt = db()->prepare($sql);
    $stmt->execute($params);
    $rows = $stmt->fetchAll();
    $user = optional_user();
    $favoriteIds = [];
    if ($user) {
        $fav = db()->prepare('SELECT destinasi_id FROM favorit WHERE user_id = ?');
        $fav->execute([$user['id']]);
        $favoriteIds = array_map('intval', array_column($fav->fetchAll(), 'destinasi_id'));
    }
    foreach ($rows as &$row) {
        $row['favorit'] = in_array((int) $row['id'], $favoriteIds, true);
    }
    unset($row);
    respond_ok([
        'items' => array_map('destination_payload', $rows),
        'total' => count($rows),
    ]);
}

function handle_destination_detail(): never
{
    $id = (int) ($_GET['id'] ?? 0);
    $slug = trim((string) ($_GET['slug'] ?? ''));
    $stmt = db()->prepare($id > 0 ? 'SELECT * FROM destinasi WHERE id = ? LIMIT 1' : 'SELECT * FROM destinasi WHERE slug = ? LIMIT 1');
    $stmt->execute([$id > 0 ? $id : $slug]);
    $row = $stmt->fetch();
    if (!$row) {
        respond_error('Destinasi tidak ditemukan.', 404);
    }
    $user = optional_user();
    $isAdmin = $user && $user['role'] === 'admin';
    if ($row['status'] !== 'terbit' && !$isAdmin) {
        respond_error('Destinasi belum tersedia untuk publik.', 404);
    }
    db()->prepare('UPDATE destinasi SET jumlah_dilihat = jumlah_dilihat + 1, updated_at = updated_at WHERE id = ?')->execute([$row['id']]);
    $row['jumlah_dilihat'] = (int) $row['jumlah_dilihat'] + 1;
    if ($user) {
        $fav = db()->prepare('SELECT 1 FROM favorit WHERE user_id = ? AND destinasi_id = ? LIMIT 1');
        $fav->execute([$user['id'], $row['id']]);
        $row['favorit'] = (bool) $fav->fetchColumn();
    }
    $payload = destination_payload($row);

    $galeri = db()->prepare('SELECT id, gambar FROM galeri_destinasi WHERE destinasi_id = ? ORDER BY urutan, id');
    $galeri->execute([$row['id']]);
    $payload['galeri'] = array_map(static fn (array $g): array => [
        'id' => (int) $g['id'],
        'gambar' => asset_url($g['gambar']),
    ], $galeri->fetchAll());

    $layanan = db()->prepare('SELECT id, tipe, nama, deskripsi, harga, ikon FROM layanan WHERE destinasi_id = ? ORDER BY urutan, id');
    $layanan->execute([$row['id']]);
    $payload['layanan'] = array_map(static fn (array $s): array => [
        'id' => (int) $s['id'],
        'tipe' => $s['tipe'],
        'nama' => $s['nama'],
        'deskripsi' => $s['deskripsi'],
        'harga' => (int) $s['harga'],
        'ikon' => $s['ikon'],
    ], $layanan->fetchAll());

    $ulasan = db()->prepare('SELECT u.rating, u.komentar, u.created_at, us.nama, us.foto_profil FROM ulasan u JOIN users us ON us.id = u.user_id WHERE u.destinasi_id = ? ORDER BY u.created_at DESC LIMIT 5');
    $ulasan->execute([$row['id']]);
    $payload['ulasan'] = array_map(static fn (array $r): array => [
        'nama' => $r['nama'],
        'foto_profil' => asset_url($r['foto_profil'] ?? null),
        'rating' => (int) $r['rating'],
        'komentar' => $r['komentar'],
        'tanggal' => date('d M Y', strtotime((string) $r['created_at'])),
    ], $ulasan->fetchAll());

    $payload['ulasan_saya'] = null;
    if ($user) {
        $mine = db()->prepare('SELECT rating, komentar, created_at FROM ulasan WHERE user_id = ? AND destinasi_id = ? LIMIT 1');
        $mine->execute([$user['id'], $row['id']]);
        $review = $mine->fetch();
        if ($review) {
            $payload['ulasan_saya'] = [
                'rating' => (int) $review['rating'],
                'komentar' => $review['komentar'],
                'tanggal' => date('d M Y', strtotime((string) $review['created_at'])),
            ];
        }
    }

    $mirip = db()->prepare('SELECT * FROM destinasi WHERE status = "terbit" AND kategori = ? AND id <> ? ORDER BY rating DESC LIMIT 4');
    $mirip->execute([$row['kategori'], $row['id']]);
    $payload['mirip'] = array_map('destination_payload', $mirip->fetchAll());

    respond_ok($payload);
}

function handle_destination_areas(): never
{
    $rows = db()->query('SELECT area, COUNT(*) AS jumlah FROM destinasi WHERE status = "terbit" GROUP BY area ORDER BY jumlah DESC')->fetchAll();
    respond_ok(array_map(static fn (array $r): array => ['area' => $r['area'], 'jumlah' => (int) $r['jumlah']], $rows));
}


function handle_review_create(): never
{
    $user = require_user();
    $input = request_input();
    $destinasiId = input_int($input, 'destinasi_id');
    $rating = input_int($input, 'rating');
    $komentar = input_string($input, 'komentar');

    if ($destinasiId <= 0) {
        respond_error('Destinasi tidak valid.');
    }
    if ($rating < 1 || $rating > 5) {
        respond_error('Pilih rating antara 1 sampai 5.');
    }
    if (mb_strlen($komentar) < 5) {
        respond_error('Tulis ulasan minimal 5 karakter.');
    }
    if (mb_strlen($komentar) > 500) {
        respond_error('Ulasan maksimal 500 karakter.');
    }

    $check = db()->prepare('SELECT id FROM destinasi WHERE id = ? AND status = "terbit" LIMIT 1');
    $check->execute([$destinasiId]);
    if (!$check->fetch()) {
        respond_error('Destinasi tidak ditemukan atau belum terbit.', 404);
    }

    $save = db()->prepare(
        'INSERT INTO ulasan (user_id, destinasi_id, rating, komentar, created_at)
         VALUES (?, ?, ?, ?, NOW())
         ON DUPLICATE KEY UPDATE rating = VALUES(rating), komentar = VALUES(komentar), created_at = NOW()'
    );
    $save->execute([$user['id'], $destinasiId, $rating, $komentar]);

    $refresh = db()->prepare(
        'UPDATE destinasi d
         SET d.jumlah_ulasan = (SELECT COUNT(*) FROM ulasan u WHERE u.destinasi_id = d.id),
             d.rating = COALESCE((SELECT ROUND(AVG(u.rating), 1) FROM ulasan u WHERE u.destinasi_id = d.id), 0),
             d.updated_at = d.updated_at
         WHERE d.id = ?'
    );
    $refresh->execute([$destinasiId]);

    $fresh = db()->prepare('SELECT rating, jumlah_ulasan FROM destinasi WHERE id = ?');
    $fresh->execute([$destinasiId]);
    $aggregate = $fresh->fetch();

    respond_ok([
        'destinasi_id' => $destinasiId,
        'rating_pengguna' => $rating,
        'komentar' => $komentar,
        'rating' => round((float) $aggregate['rating'], 1),
        'jumlah_ulasan' => (int) $aggregate['jumlah_ulasan'],
    ], 'Ulasan berhasil disimpan.');
}
