package com.kutira.kushala.data.repository

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.kutira.kushala.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps the Gemini API (mirrors the GEMINI_API_KEY from .env).
 * Used for AI-powered features like product description generation
 * and business insights.
 */
class GeminiRepository {

    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Generate a product description given basic product info.
     * Returns a streaming Flow of text chunks.
     */
    fun generateProductDescription(
        productName: String,
        category: String,
        keywords: String
    ): Flow<String> = flow {
        val prompt = """
            You are a copywriter for a B2B wholesale marketplace connecting Indian cottage 
            industries with bulk buyers.
            
            Write a concise, professional product listing description (max 150 words) for:
            - Product: $productName
            - Category: $category
            - Key features/keywords: $keywords
            
            Focus on quality, craftsmanship, and bulk-order suitability.
        """.trimIndent()

        val response = model.generateContentStream(content { text(prompt) })
        response.collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }

    /**
     * Generate a business profile summary.
     */
    suspend fun generateBusinessSummary(
        businessName: String,
        category: String,
        location: String,
        capacity: Double,
        capacityUnit: String
    ): String {
        val prompt = """
            Write a short, professional business profile summary (max 80 words) for a 
            cottage industry on a B2B marketplace:
            - Business: $businessName
            - Category: $category  
            - Location: $location
            - Production capacity: $capacity $capacityUnit
            
            Highlight their craftsmanship and reliability for bulk orders.
        """.trimIndent()
        return model.generateContent(prompt).text ?: ""
    }

    /**
     * Answer buyer questions about a product.
     */
    suspend fun askAboutProduct(
        productName: String,
        productDescription: String,
        question: String
    ): String {
        val prompt = """
            You are a helpful assistant for a B2B wholesale marketplace.
            
            Product: $productName
            Description: $productDescription
            
            Buyer's question: $question
            
            Answer concisely and professionally.
        """.trimIndent()
        return model.generateContent(prompt).text ?: "Unable to answer at this time."
    }
}
