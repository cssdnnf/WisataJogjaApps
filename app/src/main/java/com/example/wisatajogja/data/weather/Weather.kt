package com.example.wisatajogja.data.weather

data class Weather(
    val temperature: Int,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val uvIndex: Double,
    val source: String,
    val updatedAt: String,
)
