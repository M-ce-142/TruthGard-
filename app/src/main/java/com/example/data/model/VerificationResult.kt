package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Verdict(val label: String, val description: String) {
    REAL("Verified Real", "Verified by official records and trusted news sources."),
    FAKE("False / Contradicted", "False, manipulated, or conclusively debunked statement."),
    MISLEADING("Misleading", "Partially accurate but distorts facts or omits crucial context."),
    POTENTIAL_SCAM("Potential Scam", "Deceptive financial trap, phishing link, or impersonation fraud."),
    NOT_ENOUGH_EVIDENCE("Not Enough Evidence", "Cannot be conclusively corroborated with credible sources.")
}

@Entity(tableName = "verifications")
data class VerificationResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val claimTitle: String,
    val verdict: Verdict,
    val confidenceScore: Int = 85, // Evidence grounding level 0 to 100
    val easyExplanation: String,
    val detailedExplanation: String,
    val trustedSources: List<String>,
    val recommendation: String,
    val inputType: String, // NEWS_TEXT, OCR_IMAGE, URL_LINK, CHAT_MESSAGE
    val scamType: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null
)

val VerificationResult.reliabilityLabel: String
    get() = when {
        verdict == Verdict.NOT_ENOUGH_EVIDENCE -> "Preliminary Evidence"
        confidenceScore >= 80 -> "High Evidence Grounding"
        confidenceScore >= 60 -> "Moderate Grounding"
        else -> "Limited Evidence Available"
    }
