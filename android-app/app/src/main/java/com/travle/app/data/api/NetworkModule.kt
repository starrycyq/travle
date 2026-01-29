package com.travle.app.data.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import com.travle.app.data.auth.AuthManager
import com.travle.app.BuildConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // 从BuildConfig读取BASE_URL，支持多种环境配置
    private val BASE_URL = BuildConfig.API_BASE_URL
    // 本地开发: "http://10.0.2.2:5000" (Android模拟器使用10.0.2.2访问localhost)
    // 真机测试: "http://你的电脑IP:5000"
    // 生产环境: "https://your-production-domain.com"
    // 开发环境: "https://dev-your-domain.com"

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(authManager: AuthManager): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val authHeader = authManager.getAuthHeader()
            
            val newRequest = if (authHeader != null) {
                originalRequest.newBuilder()
                    .header("Authorization", authHeader)
                    .build()
            } else {
                originalRequest
            }
            
            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor,
        authManager: AuthManager
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Token刷新拦截器
        val tokenRefreshInterceptor = Interceptor { chain ->
            val response = chain.proceed(chain.request())
            
            // 检查是否是401错误（Token过期）
            if (response.code == 401 && authManager.isLoggedIn()) {
                response.close()
                
                // 这里可以添加自动刷新Token的逻辑
                // 暂时返回原响应，让应用处理Token过期
                return@Interceptor response
            }
            
            response
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(tokenRefreshInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // 为HTTPS添加必要的配置
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL.toString())
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideTravelApiService(retrofit: Retrofit): TravelApiService {
        return retrofit.create(TravelApiService::class.java)
    }
}