package com.example.momolabfe.data.remote.record.service

import com.example.momolabfe.data.remote.record.model.RecordResponse
import com.example.momolabfe.utils.ApiResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RecordService {

    @Multipart
    @POST("/api/v1/ocr")
    suspend fun createRecordByOcr(@Part("file") filePart: MultipartBody.Part): Response<ApiResponse<RecordResponse>>
}