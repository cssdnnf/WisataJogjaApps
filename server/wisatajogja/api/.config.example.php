<?php
// Salin file ini menjadi .config.php lalu isi kredensial database.
// File .config.php adalah konfigurasi server dan tidak boleh dikirim ke aplikasi Android.
return [
    'db' => [
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'wisatajogjaapps',
        'user' => 'apps',
        'pass' => 'GANTI_DENGAN_PASSWORD_DATABASE',
    ],
    'base_url' => 'https://45.127.35.252/wisatajogja',
    'upload_dir' => dirname(__DIR__) . '/assets/uploads',
    'demo_mode' => true,
];
