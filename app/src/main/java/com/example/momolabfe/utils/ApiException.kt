package com.example.momolabfe.utils

class ApiException(
    val code: Int,
    override val message: String
) : Exception(message)