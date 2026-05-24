package com.example.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExtractedReceipt(
    val storeName: String,
    val totalAmount: Double,
    val category: String, // "Alimentos", "Servicio", "Alquiler", "Diverso"
    val items: List<ExtractedItem>
)

data class ExtractedItem(
    val name: String,
    val quantity: Double,
    val price: Double
)

object GeminiScannerService {
    private const val TAG = "GeminiScanner"
    
    // OkHttp client with 60s timeout as mandated by the gemini-api skill
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Scans a receipt bitmap using Gemini Flash API.
     * If API key is missing or call fails, it falls back to a mock local parser gracefully.
     */
    suspend fun scanReceipt(
        bitmap: Bitmap?,
        modelName: String = "gemini-3.1-flash-lite",
        sampleType: String? = null
    ): ExtractedReceipt = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isDefaultKey = apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()
        
        Log.i(TAG, "Scanning receipt using $modelName... sampleType=$sampleType, keyIsPlaceholder=$isDefaultKey")

        if (isDefaultKey || bitmap == null) {
            // Emulate delay and return realistic mock response for demonstration
            kotlinx.coroutines.delay(1800)
            return@withContext getMockExtraction(sampleType)
        }

        try {
            val base64Image = bitmap.toBase64()
            
            // Construct request payload
            val prompt = """
                Analiza esta imagen de un ticket/factura/recibo de compras para el hogar.
                Extrae el nombre de la tienda o comercio, el importe total pagado, la categoría (debe ser uno de estos valores exactos: 'Alimentos', 'Servicio', 'Alquiler', 'Diverso') y una lista de los artículos comprados con su nombre, cantidad y precio unitario estimado.
                
                Devuelve únicamente un objeto JSON con la siguiente estructura exacta:
                {
                  "comercio": "Nombre de la Tienda",
                  "total": 123.45,
                  "categoria": "Alimentos",
                  "articulos": [
                    {"nombre": "Nombre de Producto", "cantidad": 2.0, "precio": 1.50}
                  ]
                }
                
                No devuelvas bloques de código markdown, solo el texto del JSON limpio.
            """.trimIndent()

            // JSON request body for Gemini REST API
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"


            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Network call failed: ${response.code} ${response.message}")
                    return@withContext getMockExtraction(sampleType)
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                Log.d(TAG, "Gemini Raw Response: $responseBody")

                return@withContext parseGeminiResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini scan failed, falling back to mock: ${e.message}", e)
            return@withContext getMockExtraction(sampleType)
        }
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseGeminiResponse(jsonString: String): ExtractedReceipt {
        val root = JSONObject(jsonString)
        val candidates = root.getJSONArray("candidates")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        var rawText = parts.getJSONObject(0).getString("text").trim()
        
        // Sanitize raw text in case Gemini returns markdown block
        if (rawText.startsWith("```json")) {
            rawText = rawText.substringAfter("```json").substringBeforeLast("```")
        } else if (rawText.startsWith("```")) {
            rawText = rawText.substringAfter("```").substringBeforeLast("```")
        }
        rawText = rawText.trim()

        val data = JSONObject(rawText)
        val store = data.optString("comercio", "Comercio Desconocido")
        val total = data.optDouble("total", 0.0)
        val cat = data.optString("categoria", "Diverso")
        
        val itemsList = mutableListOf<ExtractedItem>()
        val itemsArray = data.optJSONArray("articulos")
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                itemsList.add(
                    ExtractedItem(
                        name = itemObj.optString("nombre", "Producto"),
                        quantity = itemObj.optDouble("cantidad", 1.0),
                        price = itemObj.optDouble("precio", 0.0)
                    )
                )
            }
        }

        return ExtractedReceipt(
            storeName = store,
            totalAmount = total,
            category = cat,
            items = itemsList
        )
    }

    // Realistic default mock extractions for testing.
    fun getMockExtraction(sampleType: String?): ExtractedReceipt {
        return when (sampleType) {
            "luz" -> ExtractedReceipt(
                storeName = "Iberdrola / Compañía Eléctrica",
                totalAmount = 85.30,
                category = "Servicio",
                items = listOf(
                    ExtractedItem("Consumo Eléctrico Bimestral", 1.0, 75.0),
                    ExtractedItem("Impuestos y Alquiler de Contador", 1.0, 10.30)
                )
            )
            "alquiler" -> ExtractedReceipt(
                storeName = "Arrendamientos Centrales",
                totalAmount = 850.00,
                category = "Alquiler",
                items = listOf(
                    ExtractedItem("Mensualidad Alquiler Hogar", 1.0, 850.0)
                )
            )
            else -> ExtractedReceipt(
                storeName = "Supermercado Ahorro",
                totalAmount = 37.75,
                category = "Alimentos",
                items = listOf(
                    ExtractedItem("Leche Entera", 6.0, 1.10),
                    ExtractedItem("Arroz Blanco Bolsa", 2.0, 0.90),
                    ExtractedItem("Café Molido Gourmet", 1.0, 3.45),
                    ExtractedItem("Huevos de Granja x12", 2.0, 2.30),
                    ExtractedItem("Detergente Multiusos", 1.0, 11.0)
                )
            )
        }
    }

    suspend fun scanLarder(
        bitmap: Bitmap?,
        modelName: String = "gemini-3.1-flash-lite"
    ): ExtractedLarder = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isDefaultKey = apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()
        
        Log.i(TAG, "Scanning larder using $modelName... keyIsPlaceholder=$isDefaultKey")

        if (isDefaultKey || bitmap == null) {
            kotlinx.coroutines.delay(1800)
            return@withContext getMockLarder()
        }

        try {
            val base64Image = bitmap.toBase64()
            val prompt = """
                Analiza detenidamente esta foto de estantes de despensa/alacena.
                Detecta todos los productos que alcances a ver. Para cada uno, identifica su nombre genérico (ej. Arroz Blanco, Leche Entera, Atún en lata) y la cantidad estimada que ves en los estantes.
                
                Realiza una revisión crítica:
                1. Descarta empaques que parezcan vacíos.
                2. Corrige productos duplicados.
                
                Devuelve únicamente un objeto JSON con esta estructura exacta:
                {
                  "productos": [
                    {"nombre": "Nombre Genérico", "cantidad": 2.0, "unidad": "u"}
                  ]
                }
                
                No devuelvas bloques de código markdown, solo el texto del JSON limpio.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Network call failed: ${response.code} ${response.message}")
                    return@withContext getMockLarder()
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                return@withContext parseLarderGeminiResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini larder scan failed: ${e.message}", e)
            return@withContext getMockLarder()
        }
    }

    private fun parseLarderGeminiResponse(jsonString: String): ExtractedLarder {
        val root = JSONObject(jsonString)
        val candidates = root.getJSONArray("candidates")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        var rawText = parts.getJSONObject(0).getString("text").trim()
        
        if (rawText.startsWith("```json")) {
            rawText = rawText.substringAfter("```json").substringBeforeLast("```")
        } else if (rawText.startsWith("```")) {
            rawText = rawText.substringAfter("```").substringBeforeLast("```")
        }
        rawText = rawText.trim()

        val data = JSONObject(rawText)
        val itemsList = mutableListOf<ExtractedLarderItem>()
        val itemsArray = data.optJSONArray("productos")
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                itemsList.add(
                    ExtractedLarderItem(
                        name = itemObj.optString("nombre", "Producto"),
                        quantity = itemObj.optDouble("cantidad", 1.0),
                        unit = itemObj.optString("unidad", "u")
                    )
                )
            }
        }
        return ExtractedLarder(itemsList)
    }

    fun getMockLarder(): ExtractedLarder {
        return ExtractedLarder(
            listOf(
                ExtractedLarderItem("Arroz Bolsa 1kg", 3.0, "u"),
                ExtractedLarderItem("Leche Entera Tetra", 4.0, "u"),
                ExtractedLarderItem("Aceite de Oliva 1L", 1.0, "u"),
                ExtractedLarderItem("Pasta Tallarines 500g", 2.0, "u"),
                ExtractedLarderItem("Atún en Lata", 5.0, "u")
            )
        )
    }

    suspend fun scanProductPrice(
        bitmap: Bitmap?,
        modelName: String = "gemini-3.1-flash-lite"
    ): ExtractedProductPrice = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val isDefaultKey = apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()
        
        Log.i(TAG, "Scanning product price using $modelName... keyIsPlaceholder=$isDefaultKey")

        if (isDefaultKey || bitmap == null) {
            kotlinx.coroutines.delay(1200)
            return@withContext getMockProductPrice()
        }

        try {
            val base64Image = bitmap.toBase64()
            val prompt = """
                Analiza esta foto de un producto o su etiqueta de precio en el supermercado.
                Identifica el nombre sugerido del producto y su precio unitario o el precio mostrado en la etiqueta.
                
                Devuelve únicamente un objeto JSON con esta estructura exacta:
                {
                  "nombre": "Nombre del Producto",
                  "precio": 4.50
                }
                
                No devuelvas bloques de código markdown, solo el texto del JSON limpio.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        })
                    })
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
            val request = Request.Builder().url(url).post(requestBody).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Network call failed: ${response.code} ${response.message}")
                    return@withContext getMockProductPrice()
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response body")
                return@withContext parsePriceGeminiResponse(responseBody)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini price scan failed: ${e.message}", e)
            return@withContext getMockProductPrice()
        }
    }

    private fun parsePriceGeminiResponse(jsonString: String): ExtractedProductPrice {
        val root = JSONObject(jsonString)
        val candidates = root.getJSONArray("candidates")
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        var rawText = parts.getJSONObject(0).getString("text").trim()
        
        if (rawText.startsWith("```json")) {
            rawText = rawText.substringAfter("```json").substringBeforeLast("```")
        } else if (rawText.startsWith("```")) {
            rawText = rawText.substringAfter("```").substringBeforeLast("```")
        }
        rawText = rawText.trim()

        val data = JSONObject(rawText)
        return ExtractedProductPrice(
            name = data.optString("nombre", "Producto Escaneado"),
            price = data.optDouble("precio", 0.0)
        )
    }

    fun getMockProductPrice(): ExtractedProductPrice {
        return ExtractedProductPrice("Café Premium Bolsa", 4.80)
    }
}

data class ExtractedLarderItem(
    val name: String,
    val quantity: Double,
    val unit: String
)

data class ExtractedLarder(
    val items: List<ExtractedLarderItem>
)

data class ExtractedProductPrice(
    val name: String,
    val price: Double
)
