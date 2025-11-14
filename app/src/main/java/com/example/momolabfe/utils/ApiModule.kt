package com.example.momolabfe.utils

import com.example.momolabfe.data.remote.login.service.LoginService
import com.example.momolabfe.data.remote.record.service.RecordService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

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
    fun provideAuthApi(@NoAuthRetrofit retrofit: Retrofit): LoginService {
        return retrofit.create(LoginService::class.java)
    }
}