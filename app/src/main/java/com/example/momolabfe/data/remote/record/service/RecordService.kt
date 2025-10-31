package com.example.momolabfe.data.remote.record.service

import com.example.momolabfe.data.remote.record.model.RecordRequest
import com.example.momolabfe.data.remote.record.model.RecordResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RecordService {

    @POST("/api/v1/records")
    suspend fun recordByWriting(@Body request: RecordRequest): Response<RecordResponse>

    @Multipart
    @POST("/api/v1/ocr")
    suspend fun recordByOcr(@Part file: MultipartBody.Part): Response<RecordResponse>
}