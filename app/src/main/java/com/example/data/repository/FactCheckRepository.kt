package com.example.data.repository

import android.graphics.Bitmap
import com.example.data.local.VerificationDao
import com.example.data.model.NewsArticle
import com.example.data.model.VerificationResult
import com.example.data.remote.GeminiFactCheckService
import com.example.data.remote.NewsApiService
import kotlinx.coroutines.flow.Flow

class FactCheckRepository(
    private val verificationDao: VerificationDao,
    private val geminiService: GeminiFactCheckService,
    private val newsApiService: NewsApiService
) {
    val allVerifications: Flow<List<VerificationResult>> = verificationDao.getAllVerifications()
    val totalChecksCount: Flow<Int> = verificationDao.getTotalChecksCount()
    val fakeCount: Flow<Int> = verificationDao.getFakeCount()
    val realCount: Flow<Int> = verificationDao.getRealCount()
    val misleadingCount: Flow<Int> = verificationDao.getMisleadingCount()

    suspend fun verifyTextContent(text: String, inputType: String = "NEWS_TEXT", languageCode: String = "en"): VerificationResult {
        val result = geminiService.analyzeTextContent(text, inputType, languageCode)
        verificationDao.insertVerification(result)
        return result
    }

    suspend fun verifyImageOcr(bitmap: Bitmap, extractedText: String, languageCode: String = "en", imageUri: String? = null): VerificationResult {
        val result = geminiService.analyzeImageOcr(bitmap, extractedText, languageCode)
        val toSave = if (imageUri != null) result.copy(imageUri = imageUri) else result
        verificationDao.insertVerification(toSave)
        return toSave
    }

    suspend fun chatWithAssistant(message: String, history: List<String>, languageCode: String = "en"): String {
        return geminiService.chatWithAssistant(message, history, languageCode)
    }

    fun searchVerifications(query: String): Flow<List<VerificationResult>> {
        return verificationDao.searchVerifications(query)
    }

    suspend fun deleteVerification(id: Long) {
        verificationDao.deleteVerification(id)
    }

    suspend fun clearHistory() {
        verificationDao.clearAll()
    }

    fun getLiveNewsFeed(): List<NewsArticle> {
        return newsApiService.getSampleNewsList()
    }
}
