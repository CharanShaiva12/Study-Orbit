package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.example.BuildConfig

// --- Raw network models matching Google Gemini API REST endpoints ---

data class EpiphanyResponse(
    val coreSpark: String,
    val analogy: String,
    val cheatcardItems: List<String>
)

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun getStudyEpiphany(topic: String): EpiphanyResponse? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default placeholder!")
            return@withContext getMockFallbackEpiphany(topic, "Please configure your GEMINI_API_KEY in the Secrets panel.")
        }

        val prompt = """
            You are "StudyOrbit Oracle", an expert real-time AI study coach designed to craft lightning summaries, creative mental analogies, and structured cheatcards for students.
            
            Produce a highly focused study aid for the student topic: "$topic".
            
            You must output your response STRICTLY in application/json format with the following keys:
            1. "coreSpark": A single, high-impact, easy-to-remember 1-sentence core definition of the concept.
            2. "analogy": A brilliant, creative physical analogy (e.g. comparing electric current to water flow, or a stack to a pile of cafeteria plates) that makes the concept instantly intuitive.
            3. "cheatcardItems": A JSON array of 4 to 5 concise points, bullet rules, formulas, key steps, or critical rules of thumb for this topic.
            
            Do not include any markdown backticks or formatting outside of the raw JSON object. Ensure the JSON is perfectly valid.
        """.trimIndent()

        // Construct JSON request body manually or via map for simplicity and absolute safety
        val requestJson = """
            {
              "contents": [{
                "parts": [{
                  "text": ${escapeJsonString(prompt)}
                }]
              }],
              "generationConfig": {
                "responseMimeType": "application/json",
                "temperature": 0.5
              }
            }
        """.trimIndent()

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toRequestBody(mediaType)

        val requestUrl = "$BASE_URL?key=$apiKey"
        val request = Request.Builder()
            .url(requestUrl)
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "Network error response: Code ${response.code}, message: $errBody")
                    return@withContext getMockFallbackEpiphany(topic, "Could not fetch epiphany from Gemini (Code ${response.code}).")
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    return@withContext getMockFallbackEpiphany(topic, "Empty response received.")
                }

                // Extract text from generative response candidates: candidates[0].content.parts[0].text
                val rawText = parseGeminiResponseText(responseBody)
                if (rawText.isNullOrEmpty()) {
                    return@withContext getMockFallbackEpiphany(topic, "Could not extract text parts.")
                }

                Log.d(TAG, "Raw text: $rawText")
                val adapter = moshi.adapter(EpiphanyResponse::class.java)
                return@withContext adapter.fromJson(rawText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network calling exception", e)
            return@withContext getMockFallbackEpiphany(topic, "Error contacting AI service: ${e.localizedMessage}")
        }
    }

    private fun escapeJsonString(string: String): String {
        val escaped = StringBuilder()
        escaped.append("\"")
        for (c in string) {
            when (c) {
                '\\' -> escaped.append("\\\\")
                '"' -> escaped.append("\\\"")
                '\n' -> escaped.append("\\n")
                '\r' -> escaped.append("\\r")
                '\t' -> escaped.append("\\t")
                else -> {
                    if (c.code < 0x20) {
                        escaped.append(String.format("\\u%04x", c.code))
                    } else {
                        escaped.append(c)
                    }
                }
            }
        }
        escaped.append("\"")
        return escaped.toString()
    }

    /**
     * Parses the standard Gemini REST JSON payload for candidate text response.
     */
    private fun parseGeminiResponseText(json: String): String? {
        try {
            // Quick robust manual index parsing to avoid extra nested classes mapping definitions
            val searchCandidates = "\"candidates\""
            val idxCandidates = json.indexOf(searchCandidates)
            if (idxCandidates == -1) return null

            val searchText = "\"text\""
            val idxTextBefore = json.indexOf(searchText, idxCandidates)
            if (idxTextBefore == -1) return null

            val startQuote = json.indexOf("\"", idxTextBefore + searchText.length)
            if (startQuote == -1) return null

            var endQuote = -1
            var escaped = false
            for (i in (startQuote + 1) until json.length) {
                val c = json[i]
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    endQuote = i
                    break
                }
            }

            if (endQuote != -1) {
                val rawValue = json.substring(startQuote + 1, endQuote)
                // Unescape typical json letters
                return rawValue
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\t", "\t")
                    .trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manual candidate indexing error", e)
        }
        return null
    }

    /**
     * Fallback mock generator when no API key is specified or on offline/failures.
     */
    fun getMockFallbackEpiphany(topic: String, info: String): EpiphanyResponse {
        return EpiphanyResponse(
            coreSpark = "Offline Spark for '$topic': A primary concept representing study materials organized dynamically.",
            analogy = "It is like a personal orbital station—where all secondary concepts gravitate around the central focus topic.",
            cheatcardItems = listOf(
                "Keep study milestones small and actionable.",
                "Verify key terms using the Study Orbit dashboard.",
                "Engage in synchronized Pomodoro focus periods.",
                "Note: $info"
            )
        )
    }
}
