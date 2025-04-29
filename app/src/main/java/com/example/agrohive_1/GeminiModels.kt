package com.example.agrohive_1

import java.io.Serializable

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val safetySettings: List<SafetySetting>? = null // Optional: Add safety settings
) : Serializable

data class Content(
    val parts: List<Part>,
    val role: String? = null
) : Serializable

data class Part(
    val text: String
) : Serializable

data class GenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null
) : Serializable

data class SafetySetting(
    val category: String,
    val threshold: String
) : Serializable

data class GeminiResponse(
    val candidates: List<Candidate>?,
    val error: GeminiError? = null
) : Serializable

data class Candidate(
    val content: Content?,
    val finishReason: String?
) : Serializable

data class GeminiError(
    val code: Int?,
    val message: String? ,
    val status: String?
) : Serializable