package com.example.momolabfe.utils

import com.example.momolabfe.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoAuthClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideGson(): Gson {
        val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        // 서버가 "07:30"이면 HH:mm, "07:30:00"이면 HH:mm:ss. 섞여오면 커스텀 파서로 유연 처리.
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

        return GsonBuilder()
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonDeserializer { json, _, _ ->
                    LocalDate.parse(json.asString, dateFmt)
                })
            .registerTypeAdapter(LocalDate::class.java,
                JsonSerializer<LocalDate> { src, _, _ ->
                    JsonPrimitive(src.format(dateFmt))
                })
            .registerTypeAdapter(
                LocalTime::class.java,
                JsonDeserializer { json, _, _ ->
                    // 필요 시 try-catch로 "HH:mm:ss"도 허용
                    try { LocalTime.parse(json.asString, timeFmt) }
                    catch (e: Exception) { LocalTime.parse(json.asString) } // ISO/기타 포맷 대응
                })
            .registerTypeAdapter(LocalTime::class.java,
                JsonSerializer<LocalTime> { src, _, _ ->
                    JsonPrimitive(src.format(timeFmt))
                })
            .create()
    }

    @Provides @Singleton
    fun provideLogging(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BASIC
            else
                HttpLoggingInterceptor.Level.NONE
        }

    @Provides @Singleton @NoAuthClient
    fun provideNoAuthOkHttp(logging: HttpLoggingInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @NoAuthRetrofit
    fun provideNoAuthRetrofit(
        gson: Gson,
        @NoAuthClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}