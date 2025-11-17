package com.example.momolabfe.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.momolabfe.data.remote.auth.repository.PreferenceRepository
import com.example.momolabfe.data.remote.auth.repository.SharedPreferencesRepository
import com.example.momolabfe.data.remote.auth.service.AuthService
import com.example.momolabfe.data.remote.record.service.RecordService
import com.example.momolabfe.data.remote.user.service.UserService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthApiForLogout

@Module
@InstallIn(SingletonComponent::class)
class ApiModule {

    // 인증이 필요한 API들 - @AuthRetrofit 사용할 것
    @Provides
    @Singleton
    fun provideRecordApi(@AuthRetrofit retrofit: Retrofit): RecordService {
        return retrofit.create(RecordService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserApi(@AuthRetrofit retrofit: Retrofit): UserService {
        return retrofit.create(UserService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApi(@NoAuthRetrofit retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    @NoAuthRetrofit
    fun provideNoAuthServiceForInterceptor(@NoAuthRetrofit retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    // 로그아웃 용도
    @Provides
    @Singleton
    @AuthApiForLogout
    fun provideAuthApiForLogout(@AuthRetrofit retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    fun providePreferenceRepository(repo: SharedPreferencesRepository): PreferenceRepository = repo
}