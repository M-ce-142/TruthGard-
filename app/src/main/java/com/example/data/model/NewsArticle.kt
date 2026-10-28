package com.example.data.model

data class NewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val category: String, // Technology, Business, Sports, Politics, Health, Education, Science, Entertainment
    val source: String,
    val publishedTime: String,
    val url: String,
    val imageUrl: String? = null,
    val verificationStatus: Verdict = Verdict.REAL,
    val verifiedCount: Int = 1420
)
