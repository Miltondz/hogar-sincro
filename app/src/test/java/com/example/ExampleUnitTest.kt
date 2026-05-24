package com.example

import com.example.api.RetrofitClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testRetrofitClientConfiguration() {
        println("--- START TEST RETROFIT CLIENT ---")
        val apiService = RetrofitClient.apiService
        assertNotNull("ApiService should be successfully initialized by Retrofit", apiService)
        println("Retrofit ApiService initialized successfully.")
        println("--- END TEST RETROFIT CLIENT ---")
    }
}
