package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.AppThemeMode
import com.example.data.local.LanguagePreferences
import com.example.data.local.ThemePreferences
import com.example.data.model.ChatMessage
import com.example.data.model.NewsArticle
import com.example.data.model.Verdict
import com.example.data.model.VerificationResult
import com.example.data.remote.GeminiFactCheckService
import com.example.data.remote.NewsApiService
import com.example.data.repository.FactCheckRepository
import com.example.util.LocaleHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FactCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FactCheckRepository
    private val themePreferences = ThemePreferences(application)
    private val languagePreferences = LanguagePreferences(application)

    private val _themeMode = MutableStateFlow(themePreferences.themeMode)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _appLanguage = MutableStateFlow(languagePreferences.languageCode)
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    val searchQuery = MutableStateFlow("")
    val historyFilter = MutableStateFlow("ALL") // ALL, REAL, FAKE, SCAMS, MISLEADING

    val allVerifications: StateFlow<List<VerificationResult>>
    val totalChecksCount: StateFlow<Int>
    val fakeCount: StateFlow<Int>
    val realCount: StateFlow<Int>
    val misleadingCount: StateFlow<Int>

    private val _currentVerification = MutableStateFlow<VerificationResult?>(null)
    val currentVerification: StateFlow<VerificationResult?> = _currentVerification.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _lastFailedInput = MutableStateFlow<String?>(null)
    val lastFailedInput: StateFlow<String?> = _lastFailedInput.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(createInitialGreeting(languagePreferences.languageCode))
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _liveNewsList = MutableStateFlow<List<NewsArticle>>(emptyList())
    val liveNewsList: StateFlow<List<NewsArticle>> = _liveNewsList.asStateFlow()

    private val _selectedNewsCategory = MutableStateFlow("All")
    val selectedNewsCategory: StateFlow<String> = _selectedNewsCategory.asStateFlow()

    private val _simpleLanguageMode = MutableStateFlow(true)
    val simpleLanguageMode: StateFlow<Boolean> = _simpleLanguageMode.asStateFlow()

    init {
        LocaleHelper.applyLocale(application, languagePreferences.languageCode)

        val db = AppDatabase.getDatabase(application)
        val geminiService = GeminiFactCheckService(application)
        val newsApiService = NewsApiService()
        repository = FactCheckRepository(db.verificationDao(), geminiService, newsApiService)

        val rawHistoryFlow = searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allVerifications
            } else {
                repository.searchVerifications(query)
            }
        }

        allVerifications = combine(rawHistoryFlow, historyFilter) { list, filter ->
            when (filter) {
                "REAL" -> list.filter { it.verdict == Verdict.REAL }
                "FAKE" -> list.filter { it.verdict == Verdict.FAKE }
                "SCAMS" -> list.filter { it.verdict == Verdict.POTENTIAL_SCAM || it.scamType != null }
                "MISLEADING" -> list.filter { it.verdict == Verdict.MISLEADING }
                else -> list
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        totalChecksCount = repository.totalChecksCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        fakeCount = repository.fakeCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        realCount = repository.realCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        misleadingCount = repository.misleadingCount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        _liveNewsList.value = repository.getLiveNewsFeed()
    }

    fun verifyText(text: String, inputType: String = "NEWS_TEXT") {
        if (text.isBlank() || _isAnalyzing.value) return
        _errorMessage.value = null
        _lastFailedInput.value = text

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = repository.verifyTextContent(text.trim(), inputType, _appLanguage.value)
                _currentVerification.value = result
                _lastFailedInput.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Analysis error: ${e.localizedMessage ?: "Network or processing timeout. You can retry."}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun verifyImage(bitmap: Bitmap, extractedText: String, imageUri: String? = null) {
        if (_isAnalyzing.value) return
        _errorMessage.value = null

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val result = repository.verifyImageOcr(bitmap, extractedText.trim(), _appLanguage.value, imageUri)
                _currentVerification.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Image analysis error: ${e.localizedMessage ?: "Could not process image."}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun sendChatMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank() || _isAnalyzing.value) return

        val userMsg = ChatMessage(text = trimmed, isUser = true)
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(userMsg)
        _chatMessages.value = currentList

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                val history = currentList.takeLast(6).map { "${if (it.isUser) "User" else "Assistant"}: ${it.text}" }
                val response = repository.chatWithAssistant(trimmed, history, _appLanguage.value)
                val assistantMsg = ChatMessage(text = response, isUser = false)
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(assistantMsg)
                _chatMessages.value = updatedList
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    text = when (_appLanguage.value) {
                        "hi" -> "सुरक्षा ज्ञान इंजन से जुड़ने में समस्या आई। कृपया अपना इंटरनेट कनेक्शन जांचें या नीचे टैप कर पुनः प्रयास करें।"
                        "gu" -> "સુરક્ષા એન્જિન સાથે જોડાવામાં સમસ્યા આવી. કૃપા કરીને ઇન્ટરનેટ તપાસો અથવા નીચે ટેપ કરીને ફરી પ્રયાસ કરો."
                        else -> "I encountered an issue connecting to the safety knowledge engine. Please check your connection or tap below to retry."
                    },
                    isUser = false,
                    quickSuggestions = getQuickSuggestions(_appLanguage.value)
                )
                val updatedList = _chatMessages.value.toMutableList()
                updatedList.add(errorMsg)
                _chatMessages.value = updatedList
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearCurrentResult() {
        _currentVerification.value = null
        _errorMessage.value = null
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteVerification(id: Long) {
        viewModelScope.launch {
            repository.deleteVerification(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun toggleSimpleLanguageMode() {
        _simpleLanguageMode.value = !_simpleLanguageMode.value
    }

    fun setThemeMode(mode: AppThemeMode) {
        themePreferences.themeMode = mode
        _themeMode.value = mode
    }

    fun setAppLanguage(lang: String, context: Context? = null) {
        languagePreferences.languageCode = lang
        _appLanguage.value = lang

        val targetContext = context ?: getApplication()
        LocaleHelper.applyLocale(targetContext, lang)

        // If chat has only the default initial greeting, update it to the new language
        if (_chatMessages.value.size <= 1 && !_chatMessages.value.any { it.isUser }) {
            _chatMessages.value = listOf(createInitialGreeting(lang))
        }
    }

    fun setHistoryFilter(filter: String) {
        historyFilter.value = filter
    }

    fun setNewsCategory(category: String) {
        _selectedNewsCategory.value = category
    }

    private fun createInitialGreeting(langCode: String): ChatMessage {
        val text = when (langCode.lowercase()) {
            "hi" -> "नमस्ते! मैं गार्डबॉट 🛡️ हूं। मैं फर्जी खबरों की पहचान करने, डिजिटल अरेस्ट जबरन वसूली का पता लगाने, संदिग्ध नौकरियों को पहचानने और हेल्पलाइन 1930 पर धोखाधड़ी की रिपोर्ट करने में आपकी मदद कर सकता हूं। आज आप क्या सत्यापित करना चाहते हैं?"
            "gu" -> "નમસ્તે! હું ગાર્ડબોટ 🛡️ છું. હું ફેક ન્યૂઝ ઓળખવામાં, ડિજિટલ અરેસ્ટ સ્કેમ પકડવામાં, શંકાસ્પદ નોકરીઓ પારખવામાં અને હેલ્પલાઇન 1930 પર ફરિયાદ નોંધાવવામાં તમારી મદદ કરી શકું છું. આજે તમે શું ચકાસવા માંગો છો?"
            else -> "Hello! I am GuardBot 🛡️. I can assist you with identifying fake news, detecting Digital Arrest extortion, recognizing suspicious WhatsApp/Telegram jobs, and understanding how to report fraud on Cyber Helpline 1930. What would you like to verify today?"
        }
        return ChatMessage(
            text = text,
            isUser = false,
            quickSuggestions = getQuickSuggestions(langCode)
        )
    }

    private fun getQuickSuggestions(langCode: String): List<String> {
        return when (langCode.lowercase()) {
            "hi" -> listOf(
                "डिजिटल अरेस्ट स्कैम क्या है?",
                "1930 पर धोखाधड़ी की रिपोर्ट कैसे करें?",
                "फर्जी नौकरी के ऑफर की पहचान कैसे करें?",
                "क्या मेरा फॉरवर्ड संदेश सुरक्षित है?"
            )
            "gu" -> listOf(
                "ડિજિટલ અરેસ્ટ સ્કેમ શું છે?",
                "1930 પર ફ્રોડની ફરિયાદ કેવી રીતે કરવી?",
                "નકલી નોકરીની ઑફર કેવી રીતે ઓળખવી?",
                "શું મારો ફોરવર્ડ મેસેજ સુરક્ષિત છે?"
            )
            else -> listOf(
                "What is a Digital Arrest scam?",
                "How to report fraud on 1930?",
                "Identify fake job offers",
                "Is my forwarded message safe?"
            )
        }
    }
}
