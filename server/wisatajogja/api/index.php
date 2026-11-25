<?php
declare(strict_types=1);

require_once __DIR__ . '/bootstrap.php';
require_once __DIR__ . '/handlers/auth.php';
require_once __DIR__ . '/handlers/destinasi.php';
require_once __DIR__ . '/handlers/reservasi.php';
require_once __DIR__ . '/handlers/profil.php';
require_once __DIR__ . '/handlers/admin.php';

$action = $_GET['action'] ?? 'ping';
$method = $_SERVER['REQUEST_METHOD'] ?? 'GET';

$routes = [
    'ping' => 'handle_ping',
    'login' => 'handle_auth_login',
    'register' => 'handle_auth_register',
    'me' => 'handle_auth_me',
    'logout' => 'handle_auth_logout',
    'forgot-password' => 'handle_auth_forgot',
    'reset-password' => 'handle_auth_reset',
    'destinasi' => 'handle_destination_list',
    'destinasi-detail' => 'handle_destination_detail',
    'area' => 'handle_destination_areas',
    'favorit' => 'handle_favorite_list',
    'favorit-toggle' => 'handle_favorite_toggle',
    'reservasi' => 'handle_reservation_list',
    'reservasi-buat' => 'handle_reservation_create',
    'profil-update' => 'handle_profile_update',
    'profil-foto' => 'handle_profile_photo',
    'profil-password' => 'handle_profile_password',
    'upload-gambar' => 'handle_upload_image',
    'admin-ringkasan' => 'handle_admin_summary',
    'admin-pengguna' => 'handle_admin_users',
    'admin-pengguna-status' => 'handle_admin_user_status',
    'admin-destinasi' => 'handle_admin_destinations',
    'admin-destinasi-simpan' => 'handle_admin_destination_save',
    'admin-destinasi-hapus' => 'handle_admin_destination_delete',
    'ulasan-buat' => 'handle_review_create',
];

function handle_ping(): never
{
    respond_ok([
        'service' => 'Wisata Jogja API',
        'version' => '1.0.0',
        'time' => date(DATE_ATOM),
    ]);
}

if (!isset($routes[$action])) {
    respond_error('Endpoint tidak ditemukan.', 404);
}

$handler = $routes[$action];
$postOnly = [
    'login', 'register', 'logout', 'forgot-password', 'reset-password',
    'favorit-toggle', 'reservasi-buat', 'profil-update', 'profil-foto',
    'profil-password', 'upload-gambar', 'admin-pengguna-status',
    'admin-destinasi-simpan', 'admin-destinasi-hapus', 'ulasan-buat',
];
if (in_array($action, $postOnly, true) && $method !== 'POST') {
    respond_error('Gunakan metode POST untuk endpoint ini.', 405);
}

$handler();
