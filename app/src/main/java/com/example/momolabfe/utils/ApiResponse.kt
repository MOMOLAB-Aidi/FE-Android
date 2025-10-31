package com.example.momolabfe.utils

data class ApiResponse<T>(
    val isSuccess: Boolean,
    val code: Int,
    val message: String,
    val result: T? = null
)