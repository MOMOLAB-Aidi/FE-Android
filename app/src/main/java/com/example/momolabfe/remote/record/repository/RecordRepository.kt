package com.example.momolabfe.remote.record.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.momolabfe.remote.record.model.RecordCreateRequest
import com.example.momolabfe.remote.record.model.RecordExchangeCreateRequest
import com.example.momolabfe.remote.record.model.RecordExchangeUpdateRequest
import com.example.momolabfe.remote.record.model.RecordGetResponse
import com.example.momolabfe.remote.record.model.RecordOcrResponse
import com.example.momolabfe.remote.record.model.RecordUpdateRequest
import com.example.momolabfe.remote.record.model.GetCalendarResponse
import com.example.momolabfe.remote.record.service.RecordService
import com.example.momolabfe.utils.ApiException
import com.example.momolabfe.utils.handleApiResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody

@Singleton
class RecordRepository @Inject constructor(
    private val recordService: RecordService,
    @ApplicationContext private val appContext: Context
) {
    private val ALLOWED_TYPES = setOf("image/jpeg", "image/png")

    // 캘린더 조회
    suspend fun getCalendar(year: Int, month: Int): Result<List<GetCalendarResponse>> = runCatching {
        val response = recordService.getCalendar(year, month)
        Log.d("Calendar", "response = ${response.body()}")
        handleApiResponse(response)
    }

    // 수기 작성 - 공통 정보 생성
    suspend fun recordCommonByWriting(request: RecordCreateRequest): Result<Long> = runCatching {
        val response = recordService.recordCommonByWriting(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        val body = response.body() ?: throw ApiException(500, "빈 응답 본문")
        body.id
    }

    // 공통 정보 수정
    suspend fun updateCommonRecord(request: RecordUpdateRequest): Result<Unit> = runCatching {
        val response = recordService.updateCommonRecord(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        Unit
    }

    // 수기 작성 - 회차별 정보 생성
    suspend fun recordExchangeByWriting(recId: Long, request: RecordExchangeCreateRequest): Result<Unit> = runCatching {
        val response = recordService.recordExchangeByWriting(recId, request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        Unit
    }

    // 회차별 정보 수정
    suspend fun updateExchangeRecord(request: RecordExchangeUpdateRequest): Result<Unit> = runCatching {
        val response = recordService.updateExchangeRecord(request)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        Unit
    }

    // 전체 기록 조회
    suspend fun getRecordList(year: Int, month: Int): Result<List<RecordGetResponse>> = runCatching {
        val response = recordService.getRecordList(year, month)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 특정 기록 조회
    suspend fun getRecord(recId: Long): Result<RecordGetResponse> = runCatching {
        val response = recordService.getRecord(recId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 최근 3개 기록 조회
    suspend fun getRecentRecords(): Result<List<RecordGetResponse>> = runCatching {
        val response = recordService.getRecentRecords()
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // 특정 기록 삭제
    suspend fun deleteRecord(recId: Long): Result<Unit> = runCatching {
        val response = recordService.deleteRecord(recId)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        Unit
    }

    // OCR 텍스트 인식
    suspend fun recordByOcr(imageUri: Uri): Result<RecordOcrResponse> = runCatching {
        val part = makeFilePartFromUri(appContext, imageUri, partName = "file")
        val response = recordService.recordByOcr(part)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }

    // OCR 이미지 다운로드
    suspend fun downloadOcrImage(gcsPath: String): Result<ResponseBody> = runCatching {
        val response = recordService.downloadOcrImage(gcsPath)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), "HTTP ${response.code()}")
        }
        response.body() ?: throw ApiException(response.code(), "빈 본문")
    }


    // 헬퍼 메소드
    private fun makeFilePartFromUri(
        context: Context,
        uri: Uri,
        partName: String
    ): MultipartBody.Part {
        val cr = context.contentResolver

        val fileName = cr.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else "upload.jpg"
        } ?: "upload.jpg"

        val mime = (cr.getType(uri) ?: "image/jpeg").lowercase()

        return if (mime in ALLOWED_TYPES) {
            // 허용 타입: 그대로 스트리밍 업로드
            val requestBody = object : RequestBody() {
                override fun contentType() = mime.toMediaTypeOrNull()
                override fun writeTo(sink: BufferedSink) {
                    cr.openInputStream(uri)?.use { input ->
                        val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            sink.write(buf, 0, read)
                        }
                    } ?: error("이미지 파일을 열 수 없습니다: $uri")
                }
            }
            MultipartBody.Part.createFormData(partName, fileName, requestBody)
        } else {
            // 비허용 타입: JPEG로 변환하여 전송
            val bmp = cr.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            } ?: error("이미지 파일을 열 수 없습니다: $uri")

            val bos = java.io.ByteArrayOutputStream()
            bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, bos)
            val jpegBytes = bos.toByteArray()
            val jpegBody = jpegBytes.toRequestBody("image/jpeg".toMediaType())
            val safeName = ensureJpegName(fileName)
            MultipartBody.Part.createFormData(partName, safeName, jpegBody)
        }
    }

    private fun ensureJpegName(name: String): String {
        val lower = name.lowercase()
        return if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) name
        else name.substringBeforeLast('.', name) + ".jpg"
    }
}