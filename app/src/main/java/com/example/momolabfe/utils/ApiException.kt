package com.example.momolabfe.utils

class ApiException(
    val code: Any,
    override val message: String
) : Exception(message)