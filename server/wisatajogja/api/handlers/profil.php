<?php
declare(strict_types=1);

function handle_profile_update(): never
{
    $user = require_user();
    $input = request_input();
    $nama = input_string($input, 'nama');
    $username = strtolower(input_string($input, 'username'));
    $telepon = input_string($input, 'telepon');
    $domisili = input_string($input, 'domisili');
    $bio = input_string($input, 'bio');
    if (mb_strlen($nama) < 3) {
        respond_error('Nama lengkap minimal 3 karakter.');
    }
    if (!preg_match('/^[a-z0-9._]{3,30}$/', $username)) {
        respond_error('Username 3-30 karakter, hanya huruf, angka, titik, dan garis bawah.');
    }
    $check = db()->prepare('SELECT id FROM users WHERE username = ? AND id <> ? LIMIT 1');
    $check->execute([$username, $user['id']]);
    if ($check->fetch()) {
        respond_error('Username sudah digunakan.', 409);
    }
    db()->prepare('UPDATE users SET nama = ?, username = ?, telepon = ?, domisili = ?, bio = ? WHERE id = ?')
        ->execute([$nama, $username, $telepon, $domisili, $bio, $user['id']]);
    $fresh = db()->prepare('SELECT * FROM users WHERE id = ?');
    $fresh->execute([$user['id']]);
    respond_ok(user_payload($fresh->fetch()), 'Profil berhasil diperbarui.');
}

function validate_image_upload(string $field): array
{
    if (!isset($_FILES[$field]) || !is_array($_FILES[$field])) {
        respond_error('Pilih gambar terlebih dahulu.');
    }
    $file = $_FILES[$field];
    if (($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
        respond_error('Gambar gagal diunggah (kode ' . ($file['error'] ?? '?') . ').');
    }
    if (($file['size'] ?? 0) > 8 * 1024 * 1024) {
        respond_error('Ukuran gambar maksimal 8 MB.');
    }
    $finfo = new finfo(FILEINFO_MIME_TYPE);
    $mime = $finfo->file($file['tmp_name']);
    $extensions = ['image/jpeg' => 'jpg', 'image/png' => 'png', 'image/webp' => 'webp'];
    if (!isset($extensions[$mime])) {
        respond_error('Format gambar harus JPG, PNG, atau WEBP.');
    }
    return ['tmp' => $file['tmp_name'], 'ext' => $extensions[$mime]];
}

function store_upload(string $tmp, string $ext, string $context): array
{
    global $CONFIG;
    $allowed = ['profil', 'destinasi'];
    if (!in_array($context, $allowed, true)) {
        respond_error('Konteks unggah tidak valid.');
    }
    $relativeDir = 'assets/uploads/' . $context . '/' . date('Y-m');
    $absoluteDir = $CONFIG['upload_dir'] . '/' . $context . '/' . date('Y-m');
    if (!is_dir($absoluteDir) && !mkdir($absoluteDir, 0755, true) && !is_dir($absoluteDir)) {
        respond_error('Folder unggah tidak dapat dibuat.', 500);
    }
    $filename = $context . '-' . date('Ymd-His') . '-' . bin2hex(random_bytes(4)) . '.' . $ext;
    $target = $absoluteDir . '/' . $filename;
    if (!move_uploaded_file($tmp, $target)) {
        respond_error('Gambar gagal disimpan di server.', 500);
    }
    @chmod($target, 0644);
    $relative = $relativeDir . '/' . $filename;
    return ['path' => $relative, 'url' => asset_url($relative)];
}

function handle_profile_photo(): never
{
    $user = require_user();
    $image = validate_image_upload('foto');
    $stored = store_upload($image['tmp'], $image['ext'], 'profil');
    db()->prepare('UPDATE users SET foto_profil = ? WHERE id = ?')->execute([$stored['path'], $user['id']]);
    respond_ok(['foto_profil' => $stored['url']], 'Foto profil berhasil diperbarui.');
}

function handle_profile_password(): never
{
    $user = require_user();
    $input = request_input();
    $lama = input_string($input, 'password_lama');
    $baru = input_string($input, 'password_baru');
    if (!password_verify($lama, $user['password_hash'])) {
        respond_error('Kata sandi lama tidak sesuai.', 401);
    }
    if (mb_strlen($baru) < 6) {
        respond_error('Kata sandi baru minimal 6 karakter.');
    }
    db()->prepare('UPDATE users SET password_hash = ? WHERE id = ?')->execute([password_hash($baru, PASSWORD_DEFAULT), $user['id']]);
    respond_ok(null, 'Kata sandi berhasil diubah.');
}

function handle_upload_image(): never
{
    $user = require_user();
    $context = strtolower(trim((string) ($_POST['konteks'] ?? 'profil')));
    if ($context === 'destinasi' && $user['role'] !== 'admin') {
        respond_error('Hanya admin yang dapat mengunggah gambar destinasi.', 403);
    }
    $image = validate_image_upload('gambar');
    respond_ok(store_upload($image['tmp'], $image['ext'], $context), 'Gambar berhasil diunggah.');
}
