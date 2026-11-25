package com.example.wisatajogja.data.remote

class ApiException(
    override val message: String,
    val statusCode: Int = 0,
) : Exception(message)
