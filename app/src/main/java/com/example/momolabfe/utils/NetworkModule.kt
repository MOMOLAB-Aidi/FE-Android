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
import okhttp3.Interceptor
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

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PythonRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideGson(): Gson {
        // 서버가 "07:30"이면 HH:mm, "07:30:00"이면 HH:mm:ss. 섞여오면 커스텀 파서로 유연 처리.
        val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

        return GsonBuilder()
            .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())

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

    // --- Spring(인증 불필요) Retrofit ---
    @Provides @Singleton @NoAuthRetrofit
    fun provideNoAuthRetrofit(
        gson: Gson,
        @NoAuthClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_SPRING)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // 토큰 인터셉터를 주입받아 인증이 필요한 일반 OkHttpClient를 제공
    @Provides @Singleton @AuthClient
    fun provideAuthOkHttp(
        logging: HttpLoggingInterceptor,
        tokenInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor) // 토큰 추가
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    // --- Spring(인증 필요) Retrofit: LogoutManager 등에서 사용 ---
    @Provides @Singleton @AuthRetrofit
    fun provideAuthRetrofit(
        gson: Gson,
        @AuthClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_SPRING)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // --- Spring(인증 필요) Retrofit: 일반 서비스에서 사용 ---
    @Provides @Singleton
    fun provideDefaultRetrofit(
        gson: Gson,
        @AuthClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_SPRING)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    // --- Python(OCR용, 인증 필요) Retrofit 추가 ---
    @Provides @Singleton @PythonRetrofit
    fun providePythonRetrofit(
        gson: Gson,
        @AuthClient client: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_PYTHON)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
}