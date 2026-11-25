<?php
declare(strict_types=1);

function handle_auth_login(): never
{
    $input = request_input();
    $email = strtolower(input_string($input, 'email'));
    $password = input_string($input, 'password');
    if ($email === '' || $password === '') {
        respond_error('Email dan kata sandi wajib diisi.');
    }
    $stmt = db()->prepare('SELECT * FROM users WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $user = $stmt->fetch();
    if (!$user || !password_verify($password, $user['password_hash'])) {
        respond_error('Email atau kata sandi tidak sesuai.', 401);
    }
    if ($user['status'] !== 'aktif') {
        respond_error('Akun ini sedang dinonaktifkan. Hubungi admin.', 403);
    }
    $token = generate_token();
    db()->prepare('UPDATE users SET api_token = ?, last_login_at = NOW() WHERE id = ?')->execute([$token, $user['id']]);
    $user['api_token'] = $token;
    respond_ok(['token' => $token, 'user' => user_payload($user)], 'Selamat datang kembali.');
}

function handle_auth_register(): never
{
    $input = request_input();
    $nama = input_string($input, 'nama');
    $email = strtolower(input_string($input, 'email'));
    $password = input_string($input, 'password');
    if (mb_strlen($nama) < 3) {
        respond_error('Nama lengkap minimal 3 karakter.');
    }
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        respond_error('Format email tidak valid.');
    }
    if (mb_strlen($password) < 6) {
        respond_error('Kata sandi minimal 6 karakter.');
    }
    $exists = db()->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
    $exists->execute([$email]);
    if ($exists->fetch()) {
        respond_error('Email sudah terdaftar. Silakan masuk.', 409);
    }
    $usernameBase = preg_replace('/[^a-z0-9]+/', '', explode('@', $email)[0]) ?: 'wisatawan';
    $username = $usernameBase;
    $suffix = 1;
    $check = db()->prepare('SELECT id FROM users WHERE username = ? LIMIT 1');
    while (true) {
        $check->execute([$username]);
        if (!$check->fetch()) {
            break;
        }
        $username = $usernameBase . $suffix++;
    }
    $token = generate_token();
    $stmt = db()->prepare('INSERT INTO users (nama, username, email, password_hash, role, status, domisili, api_token, last_login_at, created_at) VALUES (?, ?, ?, ?, "user", "aktif", "Yogyakarta", ?, NOW(), NOW())');
    $stmt->execute([$nama, $username, $email, password_hash($password, PASSWORD_DEFAULT), $token]);
    $id = (int) db()->lastInsertId();
    $stmt = db()->prepare('SELECT * FROM users WHERE id = ?');
    $stmt->execute([$id]);
    respond_ok(['token' => $token, 'user' => user_payload($stmt->fetch())], 'Akun berhasil dibuat.');
}

function handle_auth_me(): never
{
    respond_ok(user_payload(require_user()));
}

function handle_auth_logout(): never
{
    $user = require_user();
    db()->prepare('UPDATE users SET api_token = NULL WHERE id = ?')->execute([$user['id']]);
    respond_ok(null, 'Berhasil keluar.');
}

function handle_auth_forgot(): never
{
    global $CONFIG;
    $input = request_input();
    $email = strtolower(input_string($input, 'email'));
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        respond_error('Format email tidak valid.');
    }
    $stmt = db()->prepare('SELECT id FROM users WHERE email = ? LIMIT 1');
    $stmt->execute([$email]);
    $user = $stmt->fetch();
    // Selalu balas pesan yang sama agar keberadaan akun tidak bocor.
    $payload = ['reset_code' => null];
    if ($user) {
        $code = str_pad((string) random_int(0, 999999), 6, '0', STR_PAD_LEFT);
        db()->prepare('UPDATE users SET reset_code = ?, reset_expires_at = DATE_ADD(NOW(), INTERVAL 15 MINUTE) WHERE id = ?')->execute([$code, $user['id']]);
        if (!empty($CONFIG['demo_mode'])) {
            $payload['reset_code'] = $code;
        }
    }
    respond_ok($payload, 'Jika email terdaftar, kode pemulihan telah dibuat.');
}

function handle_auth_reset(): never
{
    $input = request_input();
    $email = strtolower(input_string($input, 'email'));
    $code = input_string($input, 'code');
    $password = input_string($input, 'password');
    if (mb_strlen($password) < 6) {
        respond_error('Kata sandi baru minimal 6 karakter.');
    }
    $stmt = db()->prepare('SELECT * FROM users WHERE email = ? AND reset_code = ? AND reset_expires_at > NOW() LIMIT 1');
    $stmt->execute([$email, $code]);
    $user = $stmt->fetch();
    if (!$user) {
        respond_error('Kode pemulihan tidak valid atau sudah kedaluwarsa.', 400);
    }
    db()->prepare('UPDATE users SET password_hash = ?, reset_code = NULL, reset_expires_at = NULL, api_token = NULL WHERE id = ?')
        ->execute([password_hash($password, PASSWORD_DEFAULT), $user['id']]);
    respond_ok(null, 'Kata sandi berhasil diperbarui. Silakan masuk.');
}
