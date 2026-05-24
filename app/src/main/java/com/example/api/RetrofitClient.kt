package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Configured from environment or BuildConfig. Falls back to 10.0.2.2:3000 (emulator host loopback)
    private val BASE_URL: String
        get() {
            // Safe retrieval of BuildConfig field to prevent reference crash
            return try {
                val url = BuildConfig.BACKEND_URL
                if (url.isNullOrBlank() || url == "MY_BACKEND_URL" || url.contains("placeholder")) {
                    "http://10.0.2.2:3000/"
                } else {
                    if (url.endsWith("/")) url else "$url/"
                }
            } catch (e: Throwable) {
                "http://10.0.2.2:3000/"
            }
        }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)
    }
}
