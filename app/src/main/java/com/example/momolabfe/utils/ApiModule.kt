package com.example.momolabfe.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.momolabfe.remote.auth.repository.PreferenceRepository
import com.example.momolabfe.remote.auth.repository.SharedPreferencesRepository
import com.example.momolabfe.remote.auth.service.AuthService
import com.example.momolabfe.remote.consult.service.ConsultService
import com.example.momolabfe.remote.fcm.service.FcmService
import com.example.momolabfe.remote.stats.service.StatsService
import com.example.momolabfe.remote.user.service.UserService
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

    @Provides
    @Singleton
    fun provideFcmApi(@AuthRetrofit retrofit: Retrofit): FcmService {
        return retrofit.create(FcmService::class.java)
    }

    @Provides
    @Singleton
    fun provideConsultApi(@PythonRetrofit retrofit: Retrofit): ConsultService {
        return retrofit.create(ConsultService::class.java)
    }

    @Provides
    @Singleton
    fun provideStatsApi(@PythonRetrofit retrofit: Retrofit): StatsService {
        return retrofit.create(StatsService::class.java)
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