package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val quickSuggestions: List<String> = emptyList(),
    val verificationResult: VerificationResult? = null
)
