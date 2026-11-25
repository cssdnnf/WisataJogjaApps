<?php
declare(strict_types=1);

function handle_admin_summary(): never
{
    require_admin();
    $pdo = db();
    $metrics = [];
    $metrics['kunjungan_30'] = (int) $pdo->query('SELECT COALESCE(SUM(kunjungan),0) FROM statistik_harian WHERE tanggal >= DATE_SUB(CURDATE(), INTERVAL 29 DAY)')->fetchColumn();
    $previous = (int) $pdo->query('SELECT COALESCE(SUM(kunjungan),0) FROM statistik_harian WHERE tanggal >= DATE_SUB(CURDATE(), INTERVAL 59 DAY) AND tanggal < DATE_SUB(CURDATE(), INTERVAL 29 DAY)')->fetchColumn();
    $metrics['kunjungan_persen'] = $previous > 0 ? round((($metrics['kunjungan_30'] - $previous) / $previous) * 100, 1) : 0;
    $metrics['total_pengguna'] = (int) $pdo->query('SELECT COUNT(*) FROM users WHERE role = "user"')->fetchColumn();
    $metrics['pengguna_baru'] = (int) $pdo->query('SELECT COUNT(*) FROM users WHERE role = "user" AND created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)')->fetchColumn();
    $metrics['total_destinasi'] = (int) $pdo->query('SELECT COUNT(*) FROM destinasi')->fetchColumn();
    $metrics['destinasi_terbit'] = (int) $pdo->query('SELECT COUNT(*) FROM destinasi WHERE status = "terbit"')->fetchColumn();
    $metrics['destinasi_draf'] = (int) $pdo->query('SELECT COUNT(*) FROM destinasi WHERE status = "draf"')->fetchColumn();
    $metrics['total_tersimpan'] = (int) $pdo->query('SELECT COUNT(*) FROM favorit')->fetchColumn();
    $metrics['total_ulasan'] = (int) $pdo->query('SELECT COUNT(*) FROM ulasan')->fetchColumn();
    $metrics['rating_rata'] = round((float) $pdo->query('SELECT COALESCE(AVG(rating),0) FROM destinasi WHERE status = "terbit"')->fetchColumn(), 1);
    $metrics['total_reservasi'] = (int) $pdo->query('SELECT COUNT(*) FROM reservasi')->fetchColumn();

    $chart = $pdo->query('SELECT DATE_FORMAT(tanggal, "%Y-%m-%d") AS tanggal, kunjungan FROM statistik_harian WHERE tanggal >= DATE_SUB(CURDATE(), INTERVAL 13 DAY) ORDER BY tanggal')->fetchAll();
    $chartPayload = array_map(static fn (array $r): array => [
        'tanggal' => $r['tanggal'],
        'label' => date('d/n', strtotime((string) $r['tanggal'])),
        'kunjungan' => (int) $r['kunjungan'],
    ], $chart);

    $latestUsers = $pdo->query('SELECT id, nama, email, role, status, foto_profil, created_at FROM users ORDER BY created_at DESC LIMIT 5')->fetchAll();
    $usersPayload = array_map(static fn (array $u): array => [
        'id' => (int) $u['id'],
        'nama' => $u['nama'],
        'email' => $u['email'],
        'role' => $u['role'],
        'status' => $u['status'],
        'foto_profil' => asset_url($u['foto_profil'] ?? null),
        'created_at' => $u['created_at'],
    ], $latestUsers);

    $popular = $pdo->query('SELECT d.id, d.nama, d.area, d.gambar, d.rating, COUNT(f.id) AS tersimpan FROM destinasi d LEFT JOIN favorit f ON f.destinasi_id = d.id WHERE d.status = "terbit" GROUP BY d.id ORDER BY tersimpan DESC, d.rating DESC LIMIT 5')->fetchAll();
    $popularPayload = array_map(static fn (array $d): array => [
        'id' => (int) $d['id'],
        'nama' => $d['nama'],
        'area' => $d['area'],
        'gambar' => asset_url($d['gambar']),
        'rating' => round((float) $d['rating'], 1),
        'tersimpan' => (int) $d['tersimpan'],
    ], $popular);

    $reservations = $pdo->query('SELECT r.kode_booking, r.tanggal_kunjungan, r.jumlah_orang, r.status, r.created_at, u.nama AS pengguna, d.nama AS destinasi FROM reservasi r JOIN users u ON u.id = r.user_id JOIN destinasi d ON d.id = r.destinasi_id ORDER BY r.created_at DESC LIMIT 5')->fetchAll();

    respond_ok([
        'metrik' => $metrics,
        'grafik' => $chartPayload,
        'pengguna_terbaru' => $usersPayload,
        'destinasi_populer' => $popularPayload,
        'reservasi_terbaru' => array_map(static fn (array $r): array => [
            'kode_booking' => $r['kode_booking'],
            'pengguna' => $r['pengguna'],
            'destinasi' => $r['destinasi'],
            'tanggal_kunjungan' => $r['tanggal_kunjungan'],
            'jumlah_orang' => (int) $r['jumlah_orang'],
            'status' => $r['status'],
        ], $reservations),
    ]);
}

function handle_admin_users(): never
{
    require_admin();
    $q = trim((string) ($_GET['q'] ?? ''));
    $status = trim((string) ($_GET['status'] ?? ''));
    $page = max(1, (int) ($_GET['page'] ?? 1));
    $limit = 20;
    $where = ['1 = 1'];
    $params = [];
    if ($q !== '') {
        $where[] = '(nama LIKE ? OR email LIKE ? OR username LIKE ?)';
        $like = '%' . $q . '%';
        array_push($params, $like, $like, $like);
    }
    if (in_array($status, ['aktif', 'nonaktif'], true)) {
        $where[] = 'status = ?';
        $params[] = $status;
    }
    $sqlWhere = implode(' AND ', $where);
    $count = db()->prepare('SELECT COUNT(*) FROM users WHERE ' . $sqlWhere);
    $count->execute($params);
    $total = (int) $count->fetchColumn();
    $offset = ($page - 1) * $limit;
    $stmt = db()->prepare('SELECT u.*,
        (SELECT COUNT(*) FROM favorit f WHERE f.user_id = u.id) AS jumlah_tersimpan,
        (SELECT COUNT(*) FROM reservasi r WHERE r.user_id = u.id) AS jumlah_reservasi
        FROM users u WHERE ' . $sqlWhere . ' ORDER BY u.created_at DESC LIMIT ' . $limit . ' OFFSET ' . $offset);
    $stmt->execute($params);
    $rows = array_map(static fn (array $u): array => [
        'id' => (int) $u['id'],
        'nama' => $u['nama'],
        'username' => $u['username'],
        'email' => $u['email'],
        'role' => $u['role'],
        'status' => $u['status'],
        'foto_profil' => asset_url($u['foto_profil'] ?? null),
        'created_at' => $u['created_at'],
        'jumlah_tersimpan' => (int) $u['jumlah_tersimpan'],
        'jumlah_reservasi' => (int) $u['jumlah_reservasi'],
    ], $stmt->fetchAll());
    respond_ok(['items' => $rows, 'total' => $total, 'page' => $page, 'limit' => $limit]);
}

function handle_admin_user_status(): never
{
    $admin = require_admin();
    $input = request_input();
    $id = input_int($input, 'id');
    $status = input_string($input, 'status');
    if (!in_array($status, ['aktif', 'nonaktif'], true)) {
        respond_error('Status pengguna tidak valid.');
    }
    if ($id === (int) $admin['id']) {
        respond_error('Tidak dapat mengubah status akun sendiri.');
    }
    $stmt = db()->prepare('SELECT role FROM users WHERE id = ?');
    $stmt->execute([$id]);
    $target = $stmt->fetch();
    if (!$target) {
        respond_error('Pengguna tidak ditemukan.', 404);
    }
    if ($target['role'] === 'admin') {
        respond_error('Akun admin lain tidak dapat dinonaktifkan dari sini.', 403);
    }
    db()->prepare('UPDATE users SET status = ?, api_token = IF(? = "nonaktif", NULL, api_token) WHERE id = ?')->execute([$status, $status, $id]);
    respond_ok(['status' => $status], $status === 'aktif' ? 'Pengguna diaktifkan.' : 'Pengguna dinonaktifkan.');
}

function handle_admin_destinations(): never
{
    require_admin();
    $q = trim((string) ($_GET['q'] ?? ''));
    $status = trim((string) ($_GET['status'] ?? ''));
    $where = ['1 = 1'];
    $params = [];
    if ($q !== '') {
        $where[] = '(d.nama LIKE ? OR d.area LIKE ?)';
        $like = '%' . $q . '%';
        array_push($params, $like, $like);
    }
    if (in_array($status, ['terbit', 'draf'], true)) {
        $where[] = 'd.status = ?';
        $params[] = $status;
    }
    $stmt = db()->prepare('SELECT d.*, (SELECT COUNT(*) FROM favorit f WHERE f.destinasi_id = d.id) AS tersimpan FROM destinasi d WHERE ' . implode(' AND ', $where) . ' ORDER BY d.updated_at DESC, d.id DESC');
    $stmt->execute($params);
    respond_ok(array_map(static function (array $d): array {
        $payload = destination_payload($d);
        $payload['tersimpan'] = (int) $d['tersimpan'];
        $payload['updated_at'] = $d['updated_at'];
        return $payload;
    }, $stmt->fetchAll()));
}

function handle_admin_destination_save(): never
{
    $admin = require_admin();
    $input = request_input();
    $id = input_int($input, 'id');
    $nama = input_string($input, 'nama');
    $kategori = input_string($input, 'kategori');
    $area = input_string($input, 'area');
    $deskripsiSingkat = input_string($input, 'deskripsi_singkat');
    $deskripsi = input_string($input, 'deskripsi');
    $gambar = input_string($input, 'gambar');
    $harga = max(0, input_int($input, 'harga_tiket'));
    $jamBuka = input_string($input, 'jam_buka');
    $latitude = (float) ($input['latitude'] ?? 0);
    $longitude = (float) ($input['longitude'] ?? 0);
    $status = input_string($input, 'status', 'draf');
    $unggulan = !empty($input['unggulan']) ? 1 : 0;
    if (mb_strlen($nama) < 3) {
        respond_error('Nama destinasi minimal 3 karakter.');
    }
    if (!in_array($kategori, ['Pantai', 'Budaya', 'Alam'], true)) {
        respond_error('Kategori wajib dipilih.');
    }
    if ($area === '') {
        respond_error('Area wajib diisi.');
    }
    if ($deskripsiSingkat === '') {
        respond_error('Deskripsi singkat wajib diisi.');
    }
    if (!in_array($status, ['terbit', 'draf'], true)) {
        $status = 'draf';
    }
    if ($latitude === 0.0 || $longitude === 0.0) {
        respond_error('Koordinat lokasi wajib diisi (latitude dan longitude).');
    }
    $slug = slugify($nama);
    $check = db()->prepare('SELECT id FROM destinasi WHERE slug = ? AND id <> ? LIMIT 1');
    $check->execute([$slug, $id]);
    if ($check->fetch()) {
        $slug .= '-' . random_int(10, 99);
    }
    if ($id > 0) {
        $exists = db()->prepare('SELECT id FROM destinasi WHERE id = ?');
        $exists->execute([$id]);
        if (!$exists->fetch()) {
            respond_error('Destinasi tidak ditemukan.', 404);
        }
        $stmt = db()->prepare('UPDATE destinasi SET nama=?, slug=?, kategori=?, area=?, deskripsi_singkat=?, deskripsi=?, gambar=?, harga_tiket=?, jam_buka=?, latitude=?, longitude=?, status=?, unggulan=?, updated_at=NOW() WHERE id=?');
        $stmt->execute([$nama, $slug, $kategori, $area, $deskripsiSingkat, $deskripsi, $gambar, $harga, $jamBuka, $latitude, $longitude, $status, $unggulan, $id]);
    } else {
        $stmt = db()->prepare('INSERT INTO destinasi (nama, slug, kategori, area, deskripsi_singkat, deskripsi, gambar, harga_tiket, jam_buka, latitude, longitude, status, unggulan, rating, jumlah_ulasan, jumlah_dilihat, dibuat_oleh, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,0,0,0,?,NOW(),NOW())');
        $stmt->execute([$nama, $slug, $kategori, $area, $deskripsiSingkat, $deskripsi, $gambar, $harga, $jamBuka, $latitude, $longitude, $status, $unggulan, $admin['id']]);
        $id = (int) db()->lastInsertId();
    }
    $fresh = db()->prepare('SELECT * FROM destinasi WHERE id = ?');
    $fresh->execute([$id]);
    respond_ok(destination_payload($fresh->fetch()), 'Destinasi berhasil disimpan.');
}

function handle_admin_destination_delete(): never
{
    require_admin();
    $input = request_input();
    $id = input_int($input, 'id');
    $stmt = db()->prepare('SELECT id FROM destinasi WHERE id = ?');
    $stmt->execute([$id]);
    if (!$stmt->fetch()) {
        respond_error('Destinasi tidak ditemukan.', 404);
    }
    db()->prepare('DELETE FROM destinasi WHERE id = ?')->execute([$id]);
    respond_ok(null, 'Destinasi berhasil dihapus.');
}
