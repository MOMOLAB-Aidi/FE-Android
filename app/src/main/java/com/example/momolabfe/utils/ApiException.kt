package com.example.momolabfe.utils

class ApiException(
    val code: String,
    override val message: String
) : Exception(message)