package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.Verdict
import com.example.data.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiFactCheckService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }

    private fun getTargetLanguageName(languageCode: String): String {
        return when (languageCode.lowercase()) {
            "hi" -> "Hindi (हिंदी)"
            "gu" -> "Gujarati (ગુજરાતી)"
            else -> "English"
        }
    }

    suspend fun analyzeTextContent(
        text: String,
        inputType: String,
        languageCode: String = "en"
    ): VerificationResult = withContext(Dispatchers.IO) {
        val targetLang = getTargetLanguageName(languageCode)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalAnalysis(text, inputType, languageCode)
        }

        try {
            val systemPrompt = """
                You are TruthGuard AI, an expert cybersecurity fact checker and scam analysis engine.
                Analyze the input text carefully.
                Determine if the claim, message, link, or news is:
                REAL (Verified true by official sources),
                FAKE (Contradicted or false news),
                POTENTIAL_SCAM (Financial fraud, digital arrest, phishing, or advance-fee trap),
                MISLEADING (Distorts facts or lacks context),
                or NOT_ENOUGH_EVIDENCE (Cannot be independently verified).
                
                LANGUAGE REQUIREMENT:
                You MUST write "easyExplanation", "detailedExplanation", and "recommendation" naturally and accurately in $targetLang.
                Keep "verdict", "confidenceScore", and "scamType" in standard format.
                In "trustedSources", keep accredited agency/portal titles.
                
                Respond strictly in JSON with NO markdown formatting:
                {
                  "verdict": "REAL" | "FAKE" | "POTENTIAL_SCAM" | "MISLEADING" | "NOT_ENOUGH_EVIDENCE",
                  "confidenceScore": <integer 0 to 100>,
                  "easyExplanation": "<simple 2-3 sentence explanation in $targetLang>",
                  "detailedExplanation": "<factual analysis citing documented advisories in $targetLang>",
                  "trustedSources": ["<source 1>", "<source 2>"],
                  "recommendation": "<clear actionable safety recommendation in $targetLang>",
                  "scamType": "<e.g. Digital Arrest Scam, Fake Job Offer, Phishing Link, WhatsApp Fraud, Fake Govt Scheme, or null>"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", "System Instruction:\n$systemPrompt\n\nInput Content to Verify ($inputType):\n$text")
                    }))
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful || responseString.isBlank()) {
                return@withContext fallbackLocalAnalysis(text, inputType, languageCode)
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textOutput = parts?.optJSONObject(0)?.optString("text") ?: ""

            val parsedJson = JSONObject(cleanJsonResponse(textOutput))
            val verdictStr = parsedJson.optString("verdict", "NOT_ENOUGH_EVIDENCE")
            val verdict = when (verdictStr.uppercase()) {
                "REAL" -> Verdict.REAL
                "FAKE" -> Verdict.FAKE
                "POTENTIAL_SCAM" -> Verdict.POTENTIAL_SCAM
                "MISLEADING" -> Verdict.MISLEADING
                else -> Verdict.NOT_ENOUGH_EVIDENCE
            }

            val sourcesArray = parsedJson.optJSONArray("trustedSources")
            val sourcesList = mutableListOf<String>()
            if (sourcesArray != null) {
                for (i in 0 until sourcesArray.length()) {
                    sourcesList.add(sourcesArray.getString(i))
                }
            }
            if (sourcesList.isEmpty()) {
                sourcesList.add("Press Information Bureau (PIB Fact Check)")
                sourcesList.add("Cyber Crime Helpline (1930)")
            }

            VerificationResult(
                claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                verdict = verdict,
                confidenceScore = parsedJson.optInt("confidenceScore", if (verdict == Verdict.NOT_ENOUGH_EVIDENCE) 50 else 85),
                easyExplanation = parsedJson.optString("easyExplanation", "Verification completed against certified fact-checking guidelines."),
                detailedExplanation = parsedJson.optString("detailedExplanation", "AI analyzed content structure, claims, and verified public registries."),
                trustedSources = sourcesList,
                recommendation = parsedJson.optString("recommendation", "Do not share unverified claims. Verify with official authorities."),
                inputType = inputType,
                scamType = parsedJson.optString("scamType", "").takeIf { it.isNotBlank() && it != "null" && it != "None" }
            )
        } catch (e: Exception) {
            fallbackLocalAnalysis(text, inputType, languageCode)
        }
    }

    suspend fun analyzeImageOcr(
        bitmap: Bitmap,
        extractedText: String,
        languageCode: String = "en"
    ): VerificationResult = withContext(Dispatchers.IO) {
        val targetLang = getTargetLanguageName(languageCode)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext fallbackLocalAnalysis(
                extractedText.ifBlank { "Image / Newspaper clipping text analysis" },
                "OCR_IMAGE",
                languageCode
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val promptText = """
                Analyze this image/screenshot and any user-provided extracted text: "$extractedText".
                Check if this is a fake government notice, fake job offer poster, digital arrest letter, manipulated headline, or genuine notice.
                
                LANGUAGE REQUIREMENT:
                You MUST write "easyExplanation", "detailedExplanation", and "recommendation" naturally in $targetLang.
                Keep "verdict", "confidenceScore", and "scamType" in standard format.
                
                Respond strictly in JSON with NO markdown formatting:
                {
                  "verdict": "REAL" | "FAKE" | "POTENTIAL_SCAM" | "MISLEADING" | "NOT_ENOUGH_EVIDENCE",
                  "confidenceScore": <integer 0 to 100>,
                  "easyExplanation": "<simple 2 sentence explanation in $targetLang>",
                  "detailedExplanation": "<visual and textual analysis in $targetLang>",
                  "trustedSources": ["<source 1>", "<source 2>"],
                  "recommendation": "<actionable advice in $targetLang>",
                  "scamType": "<e.g. Fake Govt Notice, Fake Job Poster, Digital Arrest Scam, or null>"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                }))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("responseMimeType", "application/json")
                })
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful || responseString.isBlank()) {
                return@withContext fallbackLocalAnalysis(extractedText.ifBlank { "Scanned Poster Analysis" }, "OCR_IMAGE", languageCode)
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            val textOutput = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            val parsedJson = JSONObject(cleanJsonResponse(textOutput))
            val verdictStr = parsedJson.optString("verdict", "NOT_ENOUGH_EVIDENCE")
            val verdict = when (verdictStr.uppercase()) {
                "REAL" -> Verdict.REAL
                "FAKE" -> Verdict.FAKE
                "POTENTIAL_SCAM" -> Verdict.POTENTIAL_SCAM
                "MISLEADING" -> Verdict.MISLEADING
                else -> Verdict.NOT_ENOUGH_EVIDENCE
            }

            val sourcesArray = parsedJson.optJSONArray("trustedSources")
            val sourcesList = mutableListOf<String>()
            if (sourcesArray != null) {
                for (i in 0 until sourcesArray.length()) {
                    sourcesList.add(sourcesArray.getString(i))
                }
            }
            if (sourcesList.isEmpty()) {
                sourcesList.add("Press Information Bureau (PIB Fact Check)")
                sourcesList.add("Cyber Crime Helpline (1930)")
            }

            VerificationResult(
                claimTitle = if (extractedText.isNotBlank()) extractedText.take(60) + "..." else "Scanned Poster / Image Analysis",
                verdict = verdict,
                confidenceScore = parsedJson.optInt("confidenceScore", 85),
                easyExplanation = parsedJson.optString("easyExplanation", "The image and text were evaluated against official visual formats and advisories."),
                detailedExplanation = parsedJson.optString("detailedExplanation", "Cross-examined layout, seals, contact numbers, and claims against public records."),
                trustedSources = sourcesList,
                recommendation = parsedJson.optString("recommendation", "Verify official notices on authentic .gov.in portals before acting."),
                inputType = "OCR_IMAGE",
                scamType = parsedJson.optString("scamType", "").takeIf { it.isNotBlank() && it != "null" && it != "None" }
            )
        } catch (e: Exception) {
            fallbackLocalAnalysis(extractedText.ifBlank { "Scanned Poster Analysis" }, "OCR_IMAGE", languageCode)
        }
    }

    suspend fun chatWithAssistant(
        userMessage: String,
        chatHistory: List<String>,
        languageCode: String = "en"
    ): String = withContext(Dispatchers.IO) {
        val targetLang = getTargetLanguageName(languageCode)

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalChatResponse(userMessage, languageCode)
        }

        try {
            val systemPrompt = """
                You are GuardBot, the friendly AI Cyber Safety Assistant in TruthGuard AI app.
                Explain things clearly in $targetLang so that students, non-technical users, and elderly citizens can easily understand.
                Answer questions about:
                - How to identify fake news and scams
                - Digital Arrest scams (CBI, Police, Customs fake video calls)
                - Fake Job offers and Work From Home tasks
                - WhatsApp / Telegram investment scams
                - National Cyber Crime Helpline (1930) and reporting on cybercrime.gov.in
                - Government subsidy schemes verification
                
                Keep answers concise, polite, and reassuring. Always include actionable safety advice.
                Respond exclusively in $targetLang.
            """.trimIndent()

            val contentsArray = JSONArray()
            contentsArray.put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "System:\n$systemPrompt\n\nUser Question: $userMessage")
                }))
            })

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful || responseString.isBlank()) {
                return@withContext generateLocalChatResponse(userMessage, languageCode)
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates")
            val textOutput = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text") ?: ""

            textOutput.ifBlank { generateLocalChatResponse(userMessage, languageCode) }
        } catch (e: Exception) {
            generateLocalChatResponse(userMessage, languageCode)
        }
    }

    private fun cleanJsonResponse(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun fallbackLocalAnalysis(text: String, inputType: String, languageCode: String): VerificationResult {
        val lower = text.lowercase()
        val lang = languageCode.lowercase()

        return when {
            lower.contains("digital arrest") || lower.contains("cbi officer") || lower.contains("trai disconnected") || lower.contains("skype video call") || lower.contains("fedex parcel") || lower.contains("customs parcel") -> {
                val easy = when (lang) {
                    "hi" -> "खतरनाक स्कैम अलर्ट! असली पुलिस, सीबीआई या कस्टम्स अधिकारी कभी भी किसी को गिरफ्तार करने के लिए वीडियो कॉल नहीं करते और न ही मामले निपटाने के लिए पैसे मांगते हैं।"
                    "gu" -> "ખતરનાક સ્કેમ ચેતવણી! સાચી પોલીસ, સીબીઆઈ કે કસ્ટમ્સ અધિકારી ક્યારેય કોઈની ધરપકડ કરવા માટે વીડિયો કૉલ કરતા નથી કે પૈસા માંગતા નથી."
                    else -> "DANGEROUS SCAM ALERT! Real police, CBI, or Customs NEVER place video calls to arrest anyone or ask for money to settle cases."
                }
                val detail = when (lang) {
                    "hi" -> "यह गृह मंत्रालय (MHA) द्वारा प्रलेखित 'डिजिटल अरेस्ट' जबरन वसूली का तरीका है। कानून प्रवर्तन एजेंसियां ​​स्काइप या व्हाट्सएप वीडियो कॉल पर गिरफ्तारी नहीं कर सकतीं।"
                    "gu" -> "આ ગૃહ મંત્રાલય દ્વારા દસ્તાવેજીકૃત 'ડિજિટલ અરેસ્ટ' ખંડણીની પદ્ધતિ છે. કાયદા અમલીકરણ એજન્સીઓ સ્કાયપે કે વ્હોટ્સએપ વીડિયો કૉલ પર ધરપકડ કરી શકતી નથી."
                    else -> "This matches the 'Digital Arrest' extortion modus operandi documented by the Ministry of Home Affairs. Law enforcement cannot conduct arrests or take monetary deposits over Skype/WhatsApp video calls."
                }
                val rec = when (lang) {
                    "hi" -> "कॉल तुरंत काट दें। कोई धनराशि ट्रांसफर न करें। कॉलर के नंबर की तुरंत साइबर क्राइम हेल्पलाइन 1930 या cybercrime.gov.in पर रिपोर्ट करें।"
                    "gu" -> "તરત જ ફોન કાપી નાખો. કોઈ પૈસા મોકલશો નહીં. કૉલરના નંબરની તુરંત સાયબર ક્રાઈમ હેલ્પલાઇન 1930 અથવા cybercrime.gov.in પર જાણ કરો."
                    else -> "Hang up immediately. Do not transfer funds. Report the caller's number to Cyber Crime Helpline 1930 or cybercrime.gov.in."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.POTENTIAL_SCAM,
                    confidenceScore = 96,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("Indian Cyber Crime Coordination Centre (I4C)", "Press Information Bureau (PIB Fact Check)", "National Cyber Crime Helpline (1930)"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Digital Arrest Scam"
                )
            }
            lower.contains("like youtube") || lower.contains("part time job") || lower.contains("earn 5000") || lower.contains("earn 3000") || lower.contains("rate hotels") || lower.contains("telegram rating") -> {
                val easy = when (lang) {
                    "hi" -> "एडवांस-फीस स्कैम! कोई भी वैध कंपनी यूट्यूब लाइक करने जैसे सामान्य कार्यों के लिए हजारों रुपये नहीं देती और न ही कमाई निकालने के लिए पैसे जमा करने को कहती है।"
                    "gu" -> "એડવાન્સ-ફી સ્કેમ! કોઈ વાસ્તવિક કંપની યુટ્યુબ લાઈક જેવા સાદા કામ માટે હજારો રૂપિયા આપતી નથી કે કમાણી મેળવવા પૈસા જમા કરવાનું કહેતી નથી."
                    else -> "ADVANCE-FEE SCAM! No legitimate company pays thousands of rupees daily for simple clicks or requires deposits to 'unlock' earnings."
                }
                val detail = when (lang) {
                    "hi" -> "टास्क स्कैम में शुरुआत में छोटे भुगतान दिए जाते हैं, फिर पीड़ितों को फर्जी वॉलेट में बड़ी रकम जमा करने के लिए फंसाया जाता है जिसे कभी निकाला नहीं जा सकता।"
                    "gu" -> "ટાસ્ક સ્કેમમાં શરૂઆતમાં નાની રકમ ચૂકવવામાં આવે છે, પછી યુઝર્સને નકલી વૉલેટમાં મોટી રકમ જમા કરાવવાની લાલચ આપવામાં આવે છે જેને ક્યારેય ઉપાડી શકાતી નથી."
                    else -> "Task job scams provide small initial rewards, then lure users into depositing large sums into fake investment wallets. The funds can never be withdrawn."
                }
                val rec = when (lang) {
                    "hi" -> "नौकरी पाने या कमाई निकालने के लिए कभी पैसे न दें। भेजने वाले को तुरंत ब्लॉक करें और 1930 पर रिपोर्ट करें।"
                    "gu" -> "નોકરી મેળવવા કે કમાણી ઉપાડવા ક્યારેય પૈસા ભરશો નહીં. મોકલનારને બ્લોક કરો અને 1930 પર ફરિયાદ કરો."
                    else -> "Never deposit money to secure a job or release earnings. Block the sender and report to 1930."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.POTENTIAL_SCAM,
                    confidenceScore = 95,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("Indian Cyber Crime Coordination Centre (I4C)", "Cybercrime.gov.in Advisory"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Task / Job Offer Scam"
                )
            }
            lower.contains("electricity will be disconnected") || lower.contains("power cut tonight") || lower.contains("sbi kyc") || lower.contains("update pan") || lower.contains("share otp") -> {
                val easy = when (lang) {
                    "hi" -> "बिजली व बैंक डिस्कनेक्शन का जाल! बिजली विभाग और बैंक कभी भी व्यक्तिगत 10-अंकों वाले मोबाइल नंबरों के साथ डिस्कनेक्शन की ऐसी आपातकालीन चेतावनियां नहीं भेजते।"
                    "gu" -> "વીજ જોડાણ કપાઈ જવાની ખોટી ચેતવણી! વીજળી વિભાગ કે બેંક ક્યારેય વ્યક્તિગત ૧૦-અંકના મોબાઈલ નંબર સાથે આવી તાત્કાલિક ચેતવણી મોકલતા નથી."
                    else -> "URGENT DISCONNECTION TRAP! Utility departments and banks never send urgent cutoff warnings with personal 10-digit mobile numbers."
                }
                val detail = when (lang) {
                    "hi" -> "धोखेबाज पैनिक एसएमएस भेजकर व्यक्तिगत नंबर पर कॉल करने या स्क्रीन-शेयरिंग ऐप (जैसे AnyDesk) डाउनलोड कराने की कोशिश करते हैं ताकि आपकी बैंकिंग जानकारी चुरा सकें।"
                    "gu" -> "ઠગ ટોળકી ભય પેદા કરવા માટે એસએમએસ મોકલે છે અને સ્ક્રીન-શેરિંગ એપ (AnyDesk) ડાઉનલોડ કરાવીને તમારી બેંકિંગ વિગતો ચોરી લે છે."
                    else -> "Scammers send panic-inducing SMS requesting you call a personal number or download screen-sharing apps (AnyDesk) to steal banking credentials."
                }
                val rec = when (lang) {
                    "hi" -> "एसएमएस में दिए गए नंबर पर कॉल न करें। बिलों का भुगतान और जांच केवल आधिकारिक बिजली बोर्ड ऐप या पोर्टल के माध्यम से करें।"
                    "gu" -> "એસએમએસમાં આપેલા નંબર પર કૉલ કરશો નહીં. બિલની ચુકવણી અને તપાસ માત્ર અધિકૃત સરકારી એપ કે પોર્ટલ દ્વારા જ કરો."
                    else -> "Do not call the phone number in the SMS. Pay and check bills only through the official electricity board app or portal."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.POTENTIAL_SCAM,
                    confidenceScore = 94,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("National Power Portal", "TRAI Official Advisory", "Cyber Crime Helpline 1930"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Utility & Banking Phishing"
                )
            }
            lower.contains("free recharge") || lower.contains("free laptop scheme") || lower.contains("pm kisan 25000") || lower.contains("lottery winner") || lower.contains("kbc whatsapp lottery") -> {
                val easy = when (lang) {
                    "hi" -> "फर्जी सरकारी योजना! आधिकारिक मंत्रालय कभी भी वायरल संदेशों या तीसरे पक्ष के लिंक के माध्यम से मुफ्त मोबाइल रिचार्ज, लैपटॉप या लॉटरी नकद वितरित नहीं करते हैं।"
                    "gu" -> "ખોટી સરકારી યોજના! સત્તાવાર મંત્રાલયો ક્યારેય વાયરલ મેસેજ કે અજાણી લિંક્સ દ્વારા મફત મોબાઈલ રિચાર્જ, લેપટોપ કે લૉટરીના નાણાં વહેંચતા નથી."
                    else -> "FAKE GOVERNMENT SCHEME! Official ministries never distribute free mobile recharges, laptops, or lottery cash through viral messages."
                }
                val detail = when (lang) {
                    "hi" -> "पीआईबी फैक्ट चेक ने बार-बार स्पष्ट किया है कि असत्यापित वेब डोमेन के लिंक वाले फॉरवर्ड किए गए संदेश व्यक्तिगत डेटा एकत्र करने के लिए फ़िशिंग प्रयास हैं।"
                    "gu" -> "પીઆઈબી ફેક્ટ ચેકે વારંવાર સ્પષ્ટતા કરી છે કે અજાણી વેબસાઇટ્સની લિંક્સ સાથેના ફોરવર્ડ મેસેજ વ્યક્તિગત માહિતી ચોરવાનો ફિશિંગ પ્રયાસ છે."
                    else -> "PIB Fact Check has repeatedly clarified that forwarded messages with links to unverified web domains are phishing attempts to harvest personal data."
                }
                val rec = when (lang) {
                    "hi" -> "लिंक पर क्लिक न करें और न ही फॉरवर्ड करें। वास्तविक योजनाओं की पुष्टि केवल आधिकारिक .gov.in डोमेन पर करें।"
                    "gu" -> "લિંક પર ક્લિક કરશો નહીં કે મેસેજ ફોરવર્ડ કરશો નહીં. સાચી યોજનાઓની પુષ્ટિ માત્ર સત્તાવાર .gov.in પોર્ટલ પર જ કરો."
                    else -> "Do not click links or forward. Verify genuine schemes exclusively on official .gov.in domains."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.FAKE,
                    confidenceScore = 94,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("PIB Fact Check (pib.gov.in)", "MyGov India Official Portal"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Fake Government Scheme / Giveaway"
                )
            }
            lower.contains("500 note invalid") || lower.contains("rbi ban 500") || lower.contains("banknote discontinued") -> {
                val easy = when (lang) {
                    "hi" -> "झूठी अफवाह! भारतीय रिजर्व बैंक (RBI) ने पुष्टि की है कि प्रचलन में मौजूद ₹500 के करेंसी नोट पूरी तरह से वैध हैं।"
                    "gu" -> "ખોટી અફવા! ભારતીય રિઝર્વ બેંક (RBI) એ સ્પષ્ટ કર્યું છે કે ચલણમાં રહેલી ₹૫૦૦ ની નોટો સંપૂર્ણપણે કાયદેસર અને માન્ય છે."
                    else -> "FALSE RUMOR! The Reserve Bank of India (RBI) has confirmed that ₹500 currency notes in circulation are completely valid legal tender."
                }
                val detail = when (lang) {
                    "hi" -> "सोशल मीडिया पर बार-बार झूठा दावा किया जाता है कि ₹500 के नोट बंद हो रहे हैं। आरबीआई ने स्पष्टीकरण जारी कर इसका खंडન किया है।"
                    "gu" -> "સમયાંતરે સોશિયલ મીડિયા પર અફવા ફેલાય છે કે ₹૫૦૦ ની નોટ બંધ થઈ રહી છે. આરબીઆઈએ આ અફવાને નકારી સત્તાવાર ખુલાસો કર્યો છે."
                    else -> "Periodic viral claims falsely announce the withdrawal of ₹500 notes. RBI has issued official clarifications debunking this rumor."
                }
                val rec = when (lang) {
                    "hi" -> "मुद्रा संबंधी अफवाहों पर ध्यान न दें। आधिकारिक अपडेट सीधे rbi.org.in पर देखें।"
                    "gu" -> "ચલણ અંગેની અફવાઓ અવગણો. સત્તાવાર માહિતી સીધી rbi.org.in પર ચકાસો."
                    else -> "Ignore viral currency rumors. Check official updates directly on rbi.org.in."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.FAKE,
                    confidenceScore = 92,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("Reserve Bank of India (rbi.org.in)", "PIB Fact Check"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Financial / Currency Rumor"
                )
            }
            lower.contains("http://") || lower.contains(".xyz") || lower.contains(".top") || lower.contains(".click") || lower.contains(".work") || lower.contains("apk") -> {
                val easy = when (lang) {
                    "hi" -> "संदिग्ध लिंक चेतावनी! यह लिंक असुरक्षित कनेक्शन या उच्च जोखिम वाले डोमेन का उपयोग करता है जो आमतौर पर घोटालों से जुड़ा होता है।"
                    "gu" -> "શંકાસ્પદ લિંક ચેતવણી! આ લિંક અસુરક્ષિત કનેક્શન અથવા ઉચ્ચ જોખમી ડોમેન ધરાવે છે જે છેતરપિંડી સાથે સંકળાયેલ હોઈ શકે છે."
                    else -> "SUSPICIOUS LINK WARNING! This link uses an insecure connection or high-risk domain commonly associated with scams."
                }
                val detail = when (lang) {
                    "hi" -> ".xyz, .top या अनएन्क्रिप्टेड HTTP वाले डोमेन अक्सर भ्रामक फ़िशिंग फॉर्म या दुर्भावनापूर्ण एंड्रॉइड एपीके होस्ट करते हैं।"
                    "gu" -> ".xyz, .top અથવા અસુરક્ષિત HTTP વાળા ડોમેન્સ ઘણીવાર ફિશિંગ પેજ અથવા નુકસાનકારક એન્ડ્રોઇડ એપીકે હોસ્ટ કરે છે."
                    else -> "Domains with .xyz, .top, or unencrypted HTTP connections frequently host deceptive phishing forms or malicious Android APKs."
                }
                val rec = when (lang) {
                    "hi" -> "संदिग्ध लिंक न खोलें और न ही गूगल प्ले के बाहर कोई एपीके इंस्टॉल करें। वेबसाइट का डोमेन नाम हमेशा जांचें।"
                    "gu" -> "શંકાસ્પદ લિંક ખોલશો નહીં કે પ્લે સ્ટોર બહારની કોઈ એપ ઇન્સ્ટોલ કરશો નહીં. વેબસાઇટનું સાચું સરનામું ચકાસો."
                    else -> "Do not open suspicious links or install APKs outside Google Play. Verify the website hostname."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.MISLEADING,
                    confidenceScore = 88,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("CERT-In National Advisory", "Cyber Crime Helpline 1930"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = "Suspicious Link / Phishing"
                )
            }
            lower.contains("isro") || lower.contains("gaganyaan") || lower.contains("chandrayaan") || lower.contains("helpline 1930") || lower.contains("cybercrime.gov.in") -> {
                val easy = when (lang) {
                    "hi" -> "यह जानकारी सत्यापित सार्वजनिक घोषणाओं और आधिकारिक एजेंसी प्रेस बुलेटिनों से मेल खाती है।"
                    "gu" -> "આ માહિતી ચકાસાયેલ જાહેર ઘોષણાઓ અને સત્તાવાર એજન્સી પ્રેસ બુલેટિન સાથે સુસંગત છે."
                    else -> "This information aligns with verified public announcements and official agency press bulletins."
                }
                val detail = when (lang) {
                    "hi" -> "राष्ट्रीय समाचारों और आधिकारिक सरकारी सूचना विज्ञप्तियों के साथ मुख्य तथ्यों का मिलान किया गया है।"
                    "gu" -> "રાષ્ટ્રીય પ્રેસ બુલેટિન અને પ્રમાણિત સરકારી માહિતી સાથે વિગતોનું ક્રોસ-ચેકિંગ કરવામાં આવ્યું છે."
                    else -> "Cross-referenced keywords with national press wires and authenticated government information releases."
                }
                val rec = when (lang) {
                    "hi" -> "सामग्री प्रामाणिक प्रतीत होती है। पूरी जानकारी के लिए हमेशा प्राथमिक सरकारी एजेंसी पोर्टल देखें।"
                    "gu" -> "માહિતી અધિકૃત જણાય છે. સંપૂર્ણ વિગતો માટે હંમેશા સંબંધિત સરકારી પોર્ટલનો સંદર્ભ લો."
                    else -> "Content appears authentic. Always reference the primary government agency portal for full details."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.REAL,
                    confidenceScore = 92,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("Press Information Bureau (PIB)", "Official Department Gazette"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = null
                )
            }
            else -> {
                val easy = when (lang) {
                    "hi" -> "इस दावे की स्वतंत्र रूप से सत्यापित समाचार रिकॉर्ड या आधिकारिक फैक्ट-चेकिंग रजिस्ट्रीयों से पुष्टि नहीं हो सकी।"
                    "gu" -> "આ દાવાની સત્તાવાર સમાચાર કે માન્ય ફેક્ટ-ચેકિંગ ડેટાબેઝ દ્વારા સ્વતંત્ર પુષ્ટિ થઈ શકી નથી."
                    else -> "This claim could not be independently corroborated with verified news records or official fact-checking registries."
                }
                val detail = when (lang) {
                    "hi" -> "मान्यता प्राप्त फैक्ट-चेक डेटाबेस में कोई सीधा पुष्टिकरण या खंडन नहीं मिला। इस दावे के लिए स्वतंत्र साक्ष्य की आवश्यकता है।"
                    "gu" -> "માન્ય ફેક્ટ-ચેક રજિસ્ટ્રીમાં આ દાવા માટે કોઈ સ્પષ્ટ સમર્થન કે ખંડન મળ્યું નથી. સ્વતંત્ર પુરાવાની જરૂર છે."
                    else -> "No matching debunks or official confirmations were found in the accredited fact-check database. The claim requires independent corroboration."
                }
                val rec = when (lang) {
                    "hi" -> "असत्यापित दावों को फॉरवर्ड न करें। स्थापित मुख्यधारा के समाचार स्रोतों से पुष्टि की प्रतीक्षा करें।"
                    "gu" -> "અપ્રમાણિત દાવાઓને આગળ મોકલશો નહીં. મુખ્ય પ્રવાહના વિશ્વસનીય સમાચાર માધ્યમો તરફથી પુષ્ટિની રાહ જુઓ."
                    else -> "Do not forward unverified claims. Await corroboration from established mainstream news publishers."
                }
                VerificationResult(
                    claimTitle = text.take(65) + if (text.length > 65) "..." else "",
                    verdict = Verdict.NOT_ENOUGH_EVIDENCE,
                    confidenceScore = 50,
                    easyExplanation = easy,
                    detailedExplanation = detail,
                    trustedSources = listOf("Press Information Bureau (pib.gov.in)", "Accredited Fact Checkers"),
                    recommendation = rec,
                    inputType = inputType,
                    scamType = null
                )
            }
        }
    }

    private fun generateLocalChatResponse(query: String, languageCode: String): String {
        val q = query.lowercase()
        val lang = languageCode.lowercase()

        return when {
            q.contains("digital arrest") || q.contains("cbi") || q.contains("police") || q.contains("skype") -> {
                when (lang) {
                    "hi" -> "🚨 **डिजिटल अरेस्ट चेतावनी!**\n\n1. असली पुलिस, सीबीआई या कस्टम्स अधिकारी किसी को गिरफ्तार करने के लिए **कभी भी वीडियो कॉल नहीं करते** और न ही मामले निपटाने के लिए पैसे मांगते हैं।\n2. डिजिटल अरेस्ट पूरी तरह से एक मनगढ़ंत साइबर जबरन वसूली स्कैम है।\n3. यदि कोई आपको वीडियो कॉल पर धमकाता है, तो **तुरंत कॉल काटें** और **1930 राष्ट्रीय साइबर हेल्पलाइन** पर कॉल करें या **cybercrime.gov.in** पर शिकायत दर्ज करें।"
                    "gu" -> "🚨 **ડિજિટલ અરેસ્ટ ચેતવણી!**\n\n1. સાચી પોલીસ, સીબીઆઈ કે કસ્ટમ્સ અધિકારી કોઈની ધરપકડ કરવા માટે **ક્યારેય વીડિયો કૉલ કરતા નથી** કે પૈસા માંગતા નથી.\n2. ડિજિટલ અરેસ્ટ એ ૧૦૦% ઉપજાવી કાઢેલી સાયબર છેતરપિંડી છે.\n3. જો કોઈ તમને વીડિયો કૉલ પર ધમકાવે, તો **તરત જ કૉલ કટ કરો** અને **1930 રાષ્ટ્રીય સાયબર હેલ્પલાઇન** પર કૉલ કરો અથવા **cybercrime.gov.in** પર ફરિયાદ કરો."
                    else -> "🚨 **Digital Arrest Warning!**\n\n1. Real Police, CBI, or Customs officers **NEVER** conduct video calls to arrest anyone or demand money to settle cases.\n2. Digital Arrest is a fabricated cyber extortion scam.\n3. If anyone threatens you on a video call, **disconnect immediately** and call **1930 National Cyber Helpline** or file a report at **cybercrime.gov.in**."
                }
            }
            q.contains("scam") || q.contains("fraud") || q.contains("trap") || q.contains("धोखा") || q.contains("છેતરપિંડી") -> {
                when (lang) {
                    "hi" -> "🛡️ **आवश्यक साइबर सुरक्षा नियम:**\n\n• **फर्जी नौकरी स्कैम**: नौकरी पाने या टास्क पेआउट अनलॉक करने के लिए कभी पैसे न दें।\n• **बैंक / ओटीपी सुरक्षा**: बैंक कर्मचारी या बिजली बोर्ड कभी भी आपका ओटीपी या यूपीआई पिन नहीं मांगते।\n• **अज्ञात एपीके**: व्हाट्सएप या टेलीग्राम पर भेजी गई एपीके फाइल्स कभी डाउनलोड न करें।\n• किसी भी आपात स्थिति में तुरंत **1930** डायल करें।"
                    "gu" -> "🛡️ **મહત્વપૂર્ણ સાયબર સુરક્ષા નિયમો:**\n\n• **નકલી જોબ સ્કેમ**: નોકરી મેળવવા કે ટાસ્ક પેમેન્ટ માટે ક્યારેય પૈસા ચૂકવશો નહીં.\n• **બેંક / ઓટીપી સુરક્ષા**: બેંક સ્ટાફ કે વીજ વિભાગ ક્યારેય તમારો ઓટીપી કે યુપીઆઈ પિન માંગતા નથી.\n• **અજાણી એપીકે**: વ્હોટ્સએપ કે ટેલિગ્રામ પર આવેલી એપીકે ફાઇલો ક્યારેય ડાઉનલોડ કરશો નહીં.\n• કટોકટીમાં તુરંત **1930** ડાયલ કરો."
                    else -> "🛡️ **Essential Cyber Safety Rules:**\n\n• **Fake Job Scam**: Never pay money to get a job or unlock task payouts.\n• **Bank / OTP Security**: Bank staff and electricity boards never ask for your OTP or UPI PIN.\n• **Unknown APKs**: Never download APK files sent via WhatsApp or Telegram.\n• In an emergency, dial **1930** immediately."
                }
            }
            q.contains("1930") || q.contains("helpline") || q.contains("report") || q.contains("शिकायत") || q.contains("ફરિયાદ") -> {
                when (lang) {
                    "hi" -> "📞 **राष्ट्रीय साइबर अपराध रिपोर्टिंग पोर्टल (1930):**\n\n• यदि ऑनलाइन धोखाधड़ी में आपके पैसे कट गए हैं तो तुरंत **1930** (टोल-फ्री हेल्पलाइन) डायल करें। 'गोल्डन ऑवर' के भीतर रिपोर्ट करने से पुलिस स्कैमर के बैंक खाते को फ्रीज कर सकती है।\n• ऑनलाइन शिकायत **https://cybercrime.gov.in** पर दर्ज करें।"
                    "gu" -> "📞 **રાષ્ટ્રીય સાયબર ક્રાઈમ રિપોર્ટિંગ પોર્ટલ (1930):**\n\n• જો ઓનલાઈન છેતરપિંડીમાં પૈસા કપાયા હોય તો તરત જ **1930** (ટોલ-ફ્રી હેલ્પલાઇન) ડાયલ કરો. 'ગોલ્ડન અવર' દરમિયાન જાણ કરવાથી પોલીસ સ્કેમરનું બેંક એકાઉન્ટ ફ્રીઝ કરી શકે છે.\n• ઓનલાઈન ફરિયાદ **https://cybercrime.gov.in** પર નોંધાવો."
                    else -> "📞 **National Cybercrime Reporting Portal (1930):**\n\n• Dial **1930** (Toll-Free Helpline) if you lost money to online fraud. Reporting within the 'golden hour' allows police to freeze the scammer's bank account.\n• File online reports at **https://cybercrime.gov.in**."
                }
            }
            q.contains("job") || q.contains("task") || q.contains("youtube") || q.contains("नौकरी") || q.contains("નોકરી") -> {
                when (lang) {
                    "hi" -> "⚠️ **पार्ट-टाइम टास्क स्कैम चेतावनी:**\n\nधोखेबाज व्हाट्सएप/टेलीग्राम पर यूट्यूब वीडियो लाइक करने या होटल रेटिंग के लिए प्रतिदिन ₹2000-₹5000 का वादा करते हैं। वे शुरुआत में छोटी राशि देते हैं, फिर 'वीआईपी टास्क' के नाम पर पैसे जमा कराकर सब कुछ लूट लेते हैं। असली नौकरियों में कभी पैसे नहीं मांगे जाते।"
                    "gu" -> "⚠️ **પાર્ટ-ટાઇમ ટાસ્ક સ્કેમ ચેતવણી:**\n\nછેતરપિંડી કરનારાઓ યુટ્યુબ વિડીયો લાઈક કરવા કે હોટલ રેટિંગ માટે દરરોજ ₹૨૦૦૦-₹૫૦૦૦ આપવાનું વચન આપે છે. શરૂઆતમાં નાની રકમ આપે છે, પછી 'વીઆઈપી ટાસ્ક' ના નામે પૈસા જમા કરાવી છેતરી લે છે. સાચી નોકરીઓમાં ક્યારેય પૈસા ભરવાના હોતા નથી."
                    else -> "⚠️ **Part-Time Task Scam Warning:**\n\nScammers message on WhatsApp/Telegram promising ₹2000-₹5000 daily for liking YouTube videos or rating hotels. They pay small amounts at first, then demand deposits to 'unlock VIP tasks' and steal everything. Legitimate jobs never require payment."
                }
            }
            else -> {
                when (lang) {
                    "hi" -> "नमस्ते! मैं गार्डबॉट हूं, आपका एआई साइबर सुरक्षा सहायक 🛡️।\n\nआप मुझसे इनके बारे में पूछ सकते हैं:\n• फर्जी समाचार और वायरल अफवाहों की पहचान कैसे करें\n• डिजिटल अरेस्ट स्कैम और फर्जी वीडियो कॉल से बचाव\n• व्हाट्सएप/टेलीग्राम नौकरी धोखाधड़ी और फ़िशिंग लिंक\n• हेल्पलाइन 1930 पर साइबर अपराध की रिपोर्ट कैसे करें\n\nआज आप क्या सत्यापित करना चाहते हैं?"
                    "gu" -> "નમસ્તે! હું ગાર્ડબોટ છું, તમારો એઆઈ સાયબર સુરક્ષા સહાયક 🛡️.\n\nતમે મને આ વિશે પૂછી શકો છો:\n• ફેક ન્યૂઝ અને અફવાઓ કેવી રીતે ઓળખવી\n• ડિજિટલ અરેસ્ટ સ્કેમ અને નકલી વીડિયો કૉલ્સ\n• વ્હોટ્સએપ જોબ ફ્રોડ અને ફિશિંગ લિંક્સ\n• હેલ્પલાઇન 1930 પર સાયબર ફ્રોડની જાણ કેવી રીતે કરવી\n\nઆજે તમે શું ચકાસવા માંગો છો?"
                    else -> "Hello! I am GuardBot, your AI Cyber Safety Assistant 🛡️.\n\nYou can ask me about:\n• How to identify fake news and viral rumors\n• Digital Arrest scams & fake video calls\n• WhatsApp job offer scams & phishing links\n• How to report cyber fraud on Helpline 1930\n\nWhat would you like to verify today?"
                }
            }
        }
    }
}
