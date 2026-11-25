<?php
declare(strict_types=1);

error_reporting(E_ALL);
ini_set('display_errors', '0');
date_default_timezone_set('Asia/Jakarta');

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

$CONFIG = require __DIR__ . '/.config.php';

set_exception_handler(static function (Throwable $error): void {
    error_log('[wisatajogja-api] ' . $error->getMessage() . ' @ ' . $error->getFile() . ':' . $error->getLine());
    respond_error('Server sedang sibuk. Coba lagi sebentar.', 500);
});

function respond_ok(mixed $data = null, string $message = 'OK'): never
{
    echo json_encode(['success' => true, 'message' => $message, 'data' => $data], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function respond_error(string $message, int $status = 400): never
{
    http_response_code($status);
    echo json_encode(['success' => false, 'message' => $message, 'data' => null], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function db(): PDO
{
    static $pdo = null;
    global $CONFIG;
    if ($pdo instanceof PDO) {
        return $pdo;
    }
    $db = $CONFIG['db'];
    $dsn = sprintf('mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4', $db['host'], $db['port'], $db['name']);
    $pdo = new PDO($dsn, $db['user'], $db['pass'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        PDO::ATTR_EMULATE_PREPARES => false,
    ]);
    return $pdo;
}

function request_input(): array
{
    $input = $_POST;
    $raw = file_get_contents('php://input');
    if ($raw !== false && trim($raw) !== '') {
        $json = json_decode($raw, true);
        if (is_array($json)) {
            $input = array_merge($input, $json);
        }
    }
    return $input;
}

function input_string(array $input, string $key, string $default = ''): string
{
    $value = $input[$key] ?? $default;
    return trim(is_scalar($value) ? (string) $value : $default);
}

function input_int(array $input, string $key, int $default = 0): int
{
    return (int) ($input[$key] ?? $default);
}

function bearer_token(): ?string
{
    $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/^Bearer\s+(.+)$/i', $header, $match)) {
        return trim($match[1]);
    }
    return null;
}

function optional_user(): ?array
{
    static $cached = false;
    static $user = null;
    if ($cached) {
        return $user;
    }
    $cached = true;
    $token = bearer_token();
    if ($token === null || $token === '') {
        return null;
    }
    $stmt = db()->prepare('SELECT * FROM users WHERE api_token = ? AND status = "aktif" LIMIT 1');
    $stmt->execute([$token]);
    $row = $stmt->fetch();
    $user = $row ?: null;
    return $user;
}

function require_user(): array
{
    $user = optional_user();
    if ($user === null) {
        respond_error('Sesi berakhir. Silakan masuk kembali.', 401);
    }
    return $user;
}

function require_admin(): array
{
    $user = require_user();
    if (($user['role'] ?? '') !== 'admin') {
        respond_error('Akses hanya untuk admin.', 403);
    }
    return $user;
}

function asset_url(?string $path): ?string
{
    global $CONFIG;
    if ($path === null || trim($path) === '') {
        return null;
    }
    if (preg_match('#^https?://#i', $path)) {
        return $path;
    }
    return rtrim($CONFIG['base_url'], '/') . '/' . ltrim($path, '/');
}

function user_payload(array $row, bool $withStats = true): array
{
    $payload = [
        'id' => (int) $row['id'],
        'nama' => $row['nama'],
        'username' => $row['username'],
        'email' => $row['email'],
        'telepon' => $row['telepon'] ?? '',
        'domisili' => $row['domisili'] ?? '',
        'bio' => $row['bio'] ?? '',
        'role' => $row['role'],
        'status' => $row['status'],
        'foto_profil' => asset_url($row['foto_profil'] ?? null),
        'created_at' => $row['created_at'] ?? null,
        'last_login_at' => $row['last_login_at'] ?? null,
    ];
    if ($withStats) {
        $stats = db()->prepare('SELECT
            (SELECT COUNT(*) FROM reservasi WHERE user_id = ? AND status = "selesai") AS dikunjungi,
            (SELECT COUNT(*) FROM favorit WHERE user_id = ?) AS tersimpan,
            (SELECT COUNT(*) FROM ulasan WHERE user_id = ?) AS ulasan');
        $stats->execute([$row['id'], $row['id'], $row['id']]);
        $payload['statistik'] = array_map('intval', $stats->fetch() ?: ['dikunjungi' => 0, 'tersimpan' => 0, 'ulasan' => 0]);
    }
    return $payload;
}

function destination_payload(array $row): array
{
    return [
        'id' => (int) $row['id'],
        'nama' => $row['nama'],
        'slug' => $row['slug'],
        'kategori' => $row['kategori'],
        'area' => $row['area'],
        'deskripsi_singkat' => $row['deskripsi_singkat'],
        'deskripsi' => $row['deskripsi'],
        'gambar' => asset_url($row['gambar']),
        'harga_tiket' => (int) $row['harga_tiket'],
        'jam_buka' => $row['jam_buka'],
        'rating' => round((float) $row['rating'], 1),
        'jumlah_ulasan' => (int) $row['jumlah_ulasan'],
        'jumlah_dilihat' => (int) $row['jumlah_dilihat'],
        'latitude' => (float) $row['latitude'],
        'longitude' => (float) $row['longitude'],
        'status' => $row['status'],
        'unggulan' => (bool) $row['unggulan'],
        'favorit' => isset($row['favorit']) ? (bool) $row['favorit'] : false,
        'peta' => is_file(__DIR__ . '/../assets/img/peta/' . $row['slug'] . '.webp')
            ? asset_url('assets/img/peta/' . $row['slug'] . '.webp')
            : null,
    ];
}

function slugify(string $text): string
{
    $text = strtolower(trim($text));
    $text = preg_replace('/[^a-z0-9]+/', '-', $text) ?? '';
    return trim($text, '-') ?: 'destinasi-' . time();
}

function generate_token(): string
{
    return bin2hex(random_bytes(24));
}

$requestedAction = $_GET['action'] ?? 'ping';
