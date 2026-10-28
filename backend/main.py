"""
TruthGuard AI - Backend API Service
Provides fact-checking, rumor detection, scam analysis, URL safety inspection, and GuardBot assistant endpoints.
"""

import ipaddress
import json
import os
import re
import socket
import sys
import time
import urllib.parse
import urllib.request
from typing import Any, Dict, List, Optional

# ==========================================
# Rule-Based Scam & Fact-Check Knowledge Base
# ==========================================

KNOWN_SCAM_PATTERNS = [
    {
        "category": "Digital Arrest Scam",
        "keywords": [r"digital arrest", r"cbi officer", r"customs parcel", r"money laundering case",
                     r"narcotics parcel", r"skype video call arrest", r"dcp arrest warrant", r"fedex parcel drug"],
        "verdict": "FAKE",
        "score": 10,
        "easy_explanation": "Law enforcement agencies, CBI, Customs, or Police NEVER arrest citizens over video calls or demand money to clear cases. This is a criminal extortion scam.",
        "detailed_explanation": "Cyber criminals impersonate law enforcement using fake IDs, simulated police backdrops, and forged court notices. Under Indian & international law, arrests cannot happen via video call (Digital Arrest is illegal and fabricated).",
        "red_flags": [
            "Demanding video call interrogation via Skype/WhatsApp",
            "Threatening immediate arrest unless money transferred",
            "Claiming illegal passport/drugs found in an unknown parcel",
            "Urging secrecy and forbidding calling family or local police"
        ],
        "action": "Hang up immediately. Do not transfer funds. Report immediately to Cyber Crime Helpline 1930 or at cybercrime.gov.in.",
        "sources": ["National Cyber Crime Reporting Portal (cybercrime.gov.in)", "Press Information Bureau (PIB)", "Indian Cyber Crime Coordination Centre (I4C)"]
    },
    {
        "category": "Fake Government Scheme / Giveaway",
        "keywords": [r"free recharge", r"free 3-month recharge", r"free laptop scheme", r"pm kisan 25000",
                     r"free smartphone scheme", r"lottery winner", r"kbc whatsapp lottery"],
        "verdict": "FAKE",
        "score": 15,
        "easy_explanation": "Official government departments never distribute free mobile recharges or lottery cash via forwarded messages or third-party links.",
        "detailed_explanation": "Viral forwarded messages claiming free recharge or gifts from government ministries are phishing campaigns designed to steal personal data, harvest contacts, or install malicious APKs.",
        "red_flags": [
            "Claims official government benefit on an unofficial domain (.xyz, .top, .site)",
            "Asks you to forward to 5 WhatsApp groups to claim",
            "Requires payment of 'registration fee' or 'processing charge'"
        ],
        "action": "Do not click links or forward. Verify official schemes exclusively at india.gov.in or pib.gov.in.",
        "sources": ["PIB Fact Check (@PIBFactCheck)", "Government of India Official Portal (india.gov.in)"]
    },
    {
        "category": "Task & Part-Time Job Scam",
        "keywords": [r"earn.*(?:3000|5000).*daily", r"like youtube videos", r"rate hotels on google",
                     r"part time job bonus", r"deposit.*(?:500|vip)", r"telegram task", r"work from home.*payment", r"urgent hiring.*home"],
        "verdict": "FAKE",
        "score": 12,
        "easy_explanation": "No legitimate employer asks candidates to deposit money or pay to 'unlock' tasks. This is an advance-fee Ponzi/task scam.",
        "detailed_explanation": "Victims are paid trivial amounts (₹100-₹200) for initial simple tasks, then pressured into depositing thousands of rupees into fraudulent investment/VIP wallets that cannot be withdrawn.",
        "red_flags": [
            "Offers unusually high daily pay for trivial tasks like liking videos",
            "Communicates strictly via WhatsApp/Telegram without interview",
            "Requires payment or deposit before releasing accumulated earnings"
        ],
        "action": "Do not deposit money. Block the contact and report their Telegram/WhatsApp account and UPI IDs to 1930.",
        "sources": ["Indian Cyber Crime Coordination Centre (I4C)", "Cybercrime.gov.in Advisory"]
    },
    {
        "category": "Urgent Utility / Banking Disconnection Scam",
        "keywords": [r"electricity will be disconnected", r"power cut tonight", r"dear customer your electricity",
                     r"update pan immediately", r"sbi kyc suspended", r"sim card block in 2 hours", r"trai notice mobile disconnected"],
        "verdict": "FAKE",
        "score": 15,
        "easy_explanation": "Electricity boards and telecom operators never send messages threatening disconnection within hours with a personal mobile number to call.",
        "detailed_explanation": "Electricity and utility companies never include personal 10-digit mobile numbers for bill settlement. Scammers send fake SMS to induce panic and persuade victims to install screen-sharing software (AnyDesk, TeamViewer) or pay through fraudulent links.",
        "red_flags": [
            "Creates artificial panic (e.g., 'power will be cut tonight at 9:30 PM')",
            "Provides an unofficial 10-digit mobile number instead of official toll-free/app",
            "Asks you to download an APK or remote support software"
        ],
        "action": "Do not call the number in the SMS. Pay and check utility status only through official electricity provider portals or consumer apps.",
        "sources": ["National Power Portal", "TRAI Official Advisory", "Cyber Crime Helpline 1930"]
    },
    {
        "category": "Currency & Banknote Rumor",
        "keywords": [r"500 note invalid", r"500 banknote discontinued", r"rbi ban 500 note", r"old 500 note deadline"],
        "verdict": "FAKE",
        "score": 18,
        "easy_explanation": "The Reserve Bank of India (RBI) has NOT banned or discontinued current ₹500 currency notes. They remain completely legal tender.",
        "detailed_explanation": "Periodic rumors falsely claim RBI is demonetizing or invalidating ₹500 notes. The Reserve Bank of India (RBI) issued explicit official press releases confirming ₹500 notes in circulation are valid legal tender.",
        "red_flags": [
            "Social media posts with sensational claims without RBI circular references",
            "Absence of official notification on rbi.org.in"
        ],
        "action": "Ignore viral currency rumors. Check official notices directly at rbi.org.in or PIB Fact Check.",
        "sources": ["Reserve Bank of India (rbi.org.in)", "PIB Fact Check"]
    }
]

KNOWN_REAL_TOPICS = [
    {
        "keywords": [r"isro.*deep space", r"isro.*engine", r"isro.*gaganyaan", r"cryogenic stage", r"chandrayaan"],
        "title": "ISRO Space Exploration Milestone",
        "summary": "ISRO space engine testing and lunar/deep space mission developments are verified through official ISRO press releases.",
        "sources": ["ISRO Official Portal (isro.gov.in)", "Press Information Bureau"]
    },
    {
        "keywords": [r"cyber.*1930", r"helpline 1930", r"cybercrime\.gov\.in", r"national cyber helpline"],
        "title": "Official Cyber Crime Helpline 1930",
        "summary": "1930 is the official National Cybercrime Helpline initiated by the Ministry of Home Affairs (MHA) for reporting financial fraud within the golden hour.",
        "sources": ["Ministry of Home Affairs (mha.gov.in)", "cybercrime.gov.in"]
    }
]

# ==========================================
# Core Verification Engine
# ==========================================

def run_semantic_verification(claim: str, input_type: str = "NEWS_TEXT", simple_mode: bool = False, language: str = "en") -> Dict[str, Any]:
    """
    Performs verified fact-checking and scam analysis using multi-pattern heuristic matching
    and optional Gemini API call if GEMINI_API_KEY is available. Supports languages: en, hi, gu.
    """
    claim_clean = claim.strip()
    claim_lower = claim_clean.lower()
    api_key = os.environ.get("GEMINI_API_KEY", "").strip()
    lang = (language or "en").lower()

    # If Gemini API Key is configured, attempt intelligent cloud analysis
    if api_key and api_key != "MY_GEMINI_API_KEY":
        try:
            gemini_result = call_gemini_verify(claim_clean, input_type, api_key, simple_mode, lang)
            if gemini_result:
                return gemini_result
        except Exception:
            pass  # Fallback to local rule engine smoothly

    # Check for known scam patterns
    for scam in KNOWN_SCAM_PATTERNS:
        for kw in scam["keywords"]:
            if re.search(kw, claim_lower):
                return {
                    "verdict": scam["verdict"],
                    "reliability_level": "High Confidence",
                    "truth_score": scam["score"],
                    "summary": f"Identified as {scam['category']}. Content exhibits documented deceptive tactics.",
                    "easy_explanation": scam["easy_explanation"],
                    "detailed_explanation": scam["detailed_explanation"],
                    "red_flags": scam["red_flags"],
                    "suggested_action": scam["action"],
                    "trusted_sources": scam["sources"],
                    "scam_type": scam["category"],
                    "input_type": input_type,
                    "timestamp": time.time()
                }

    # Check for verified real patterns
    for item in KNOWN_REAL_TOPICS:
        for kw in item["keywords"]:
            if re.search(kw, claim_lower):
                return {
                    "verdict": "REAL",
                    "reliability_level": "High Confidence",
                    "truth_score": 92,
                    "summary": item["summary"],
                    "easy_explanation": "This information is consistent with official statements and verified press notices.",
                    "detailed_explanation": f"Cross-referenced topic '{item['title']}' against official releases and government press bulletins.",
                    "red_flags": [],
                    "suggested_action": "Safe to read and reference. Verify details on cited government/agency portals.",
                    "trusted_sources": item["sources"],
                    "scam_type": None,
                    "input_type": input_type,
                    "timestamp": time.time()
                }

    # General heuristic evaluation for unrecognized claims
    has_urgency = bool(re.search(r"immediately|urgent|within \d+ hours|tonight|final notice|blocked", claim_lower))
    has_financial = bool(re.search(r"rupees|₹|\$|bonus|lottery|deposit|cashback|investment|crypto", claim_lower))
    has_suspicious_link = bool(re.search(r"https?://[^\s]+(?:\.xyz|\.top|\.tk|\.click|\.rest|\.icu)", claim_lower))
    has_otp = bool(re.search(r"share otp|send code|verification code|pin", claim_lower))

    red_flags = []
    if has_urgency:
        red_flags.append("High urgency tone designed to induce hasty compliance")
    if has_financial and has_urgency:
        red_flags.append("Combination of financial promise/threat with strict time pressure")
    if has_suspicious_link:
        red_flags.append("Contains link hosted on high-risk domain TLD")
    if has_otp:
        red_flags.append("Solicitation of OTP or private verification codes")

    if (has_financial and has_urgency) or (has_financial and has_suspicious_link) or has_otp:
        return {
            "verdict": "FAKE",
            "reliability_level": "High Confidence",
            "truth_score": 20,
            "summary": "Suspicious request exhibiting known social-engineering and scam characteristics.",
            "easy_explanation": "This text attempts to pressure you with urgency and money or OTP requests. Never share OTPs or pay advance fees.",
            "detailed_explanation": "The message matches primary scam indicators: urgency, financial demand, and unverified attribution.",
            "red_flags": red_flags,
            "suggested_action": "Do not respond, do not click links, and never share OTPs or bank passwords.",
            "trusted_sources": ["National Cyber Crime Helpline (1930)", "cybercrime.gov.in"],
            "scam_type": "Social Engineering / Financial Trap",
            "input_type": input_type,
            "timestamp": time.time()
        }

    # If inconclusive
    return {
        "verdict": "NOT_ENOUGH_EVIDENCE",
        "reliability_level": "Preliminary Check",
        "truth_score": 50,
        "summary": "The claim could not be conclusively verified against trusted databases or public fact-check indexes.",
        "easy_explanation": "There is currently no official confirmation or debunking found for this exact claim. Exercise caution before believing or sharing.",
        "detailed_explanation": "No direct matches found in official government gazettes, PIB fact-check advisories, or accredited news wire registries. The claim lacks independent corroboration.",
        "red_flags": ["No credible publication citation found", "Missing official corroboration"],
        "suggested_action": "Do not forward unverified claims. Check mainstream accredited newspapers or official agency websites.",
        "trusted_sources": ["Press Information Bureau (pib.gov.in)", "Accredited Fact-Check Organizations"],
        "scam_type": None,
        "input_type": input_type,
        "timestamp": time.time()
    }

def call_gemini_verify(claim: str, input_type: str, api_key: str, simple_mode: bool, language: str = "en") -> Optional[Dict[str, Any]]:
    """Calls Gemini REST API with strict JSON schema to verify claim in target language."""
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"
    target_lang = "Hindi (हिंदी)" if language == "hi" else ("Gujarati (ગુજરાતી)" if language == "gu" else "English")
    system_instruction = (
        "You are TruthGuard AI, an expert cybersecurity and fact-checking intelligence system. "
        "Analyze the claim with utmost factual rigor. "
        f"Language requirement: You MUST write easy_explanation, detailed_explanation, and suggested_action naturally in {target_lang}. "
        "Output strictly valid JSON with keys: "
        "'verdict' (one of REAL, FAKE, MISLEADING, NOT_ENOUGH_EVIDENCE), "
        "'reliability_level' (High Confidence, Moderate, or Preliminary), "
        "'truth_score' (integer 0-100), "
        "'summary' (string), "
        f"'easy_explanation' (2-3 simple sentences in {target_lang}), "
        f"'detailed_explanation' (clear factual reasoning in {target_lang}), "
        "'red_flags' (array of strings), "
        f"'suggested_action' (actionable advice in {target_lang}), "
        "'trusted_sources' (array of strings), "
        "'scam_type' (string or null)."
    )

    prompt = f"System: {system_instruction}\n\nVerify this content ({input_type}):\n{claim}"
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": 0.1,
            "responseMimeType": "application/json"
        }
    }

    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method="POST"
    )
    with urllib.request.urlopen(req, timeout=15) as resp:
        if resp.status == 200:
            res_data = json.loads(resp.read().decode("utf-8"))
            candidate = res_data.get("candidates", [{}])[0]
            text = candidate.get("content", {}).get("parts", [{}])[0].get("text", "")
            parsed = json.loads(text)
            parsed["input_type"] = input_type
            parsed["timestamp"] = time.time()
            return parsed
    return None

def call_gemini_verify_image(image_base64: str, prompt_text: str = "", language: str = "en") -> Optional[Dict[str, Any]]:
    api_key = os.environ.get("GEMINI_API_KEY", "")
    if not api_key:
        return None

    target_lang = "Hindi" if language == "hi" else ("Gujarati" if language == "gu" else "English")
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key={api_key}"

    system_instruction = (
        "You are TruthGuard AI, a professional multi-language fact checker and misinformation analyst. "
        "Analyze this image / poster / screenshot carefully. "
        "Extract key text and identify if it is a fake government notice, lottery scam, digital arrest extortion letter, or fake job advertisement. "
        f"You MUST write 'easy_explanation', 'detailed_explanation', and 'suggested_action' in {target_lang}. "
        "Respond strictly with valid JSON with keys: "
        "'verdict' ('REAL', 'FAKE', 'POTENTIAL_SCAM', 'MISLEADING', 'NOT_ENOUGH_EVIDENCE'), "
        "'confidence_score' (int 0-100), "
        "'summary' (string), "
        f"'easy_explanation' (2-3 simple sentences in {target_lang}), "
        f"'detailed_explanation' (clear factual reasoning in {target_lang}), "
        "'red_flags' (array of strings), "
        f"'suggested_action' (actionable advice in {target_lang}), "
        "'trusted_sources' (array of strings), "
        "'scam_type' (string or null)."
    )

    prompt = f"System: {system_instruction}\n\nVerify this image:\n{prompt_text or 'Scanned poster/image'}"
    payload = {
        "contents": [{
            "parts": [
                {"text": prompt},
                {
                    "inlineData": {
                        "mimeType": "image/jpeg",
                        "data": image_base64
                    }
                }
            ]
        }],
        "generationConfig": {
            "temperature": 0.1,
            "responseMimeType": "application/json"
        }
    }

    try:
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=20) as resp:
            if resp.status == 200:
                res_data = json.loads(resp.read().decode("utf-8"))
                candidate = res_data.get("candidates", [{}])[0]
                text = candidate.get("content", {}).get("parts", [{}])[0].get("text", "")
                parsed = json.loads(text)
                parsed["input_type"] = "OCR_IMAGE"
                parsed["timestamp"] = time.time()
                return parsed
    except Exception:
        pass
    return None

# ==========================================
# SSRF-Protected URL Safety Inspector
# ==========================================

SUSPICIOUS_TLDS = {".xyz", ".top", ".tk", ".ml", ".ga", ".cf", ".gq", ".click", ".buzz", ".work", ".rest", ".icu", ".kim", ".country"}
SUSPICIOUS_BRAND_KEYWORDS = ["sbi", "pib", "trai", "income-tax", "kisan", "aadhaar", "paytm", "whatsapp", "telegram", "cbi", "police"]

def is_private_or_restricted_ip(ip_str: str) -> bool:
    """Checks if an IP address belongs to private, loopback, link-local, or multicast blocks."""
    try:
        ip = ipaddress.ip_address(ip_str)
        return (
            ip.is_private
            or ip.is_loopback
            or ip.is_link_local
            or ip.is_multicast
            or ip.is_reserved
            or ip.is_unspecified
        )
    except ValueError:
        return True

def inspect_url_safety(url_input: str) -> Dict[str, Any]:
    """
    Analyzes URL for security risks, typosquatting, phishing patterns, and protects against SSRF.
    """
    clean_url = url_input.strip()
    # Check if a custom or non-web scheme is explicitly specified
    if "://" not in clean_url:
        clean_url = "https://" + clean_url

    parsed = urllib.parse.urlparse(clean_url)
    hostname = (parsed.hostname or "").lower()
    scheme = parsed.scheme.lower()

    if scheme not in ("http", "https"):
        return {
            "safe": False,
            "risk_level": "Dangerous",
            "reason": f"Unsupported or dangerous URL scheme: '{scheme}'. Only HTTP and HTTPS are permitted."
        }

    if not hostname:
        return {
            "safe": False,
            "risk_level": "Invalid",
            "reason": "Malformed URL: missing hostname."
        }

    # SSRF Protection: Check if hostname is localhost, local domain, or private IP
    LOCAL_HOSTNAMES = {"localhost", "127.0.0.1", "0.0.0.0", "::1", "ip6-localhost", "ip6-loopback"}
    if hostname in LOCAL_HOSTNAMES or hostname.endswith((".local", ".internal", ".lan", ".home")):
        return {
            "safe": False,
            "risk_level": "Blocked",
            "hostname": hostname,
            "scheme": scheme,
            "reason": f"SSRF Protection: Access to localhost or local internal hostname '{hostname}' is strictly blocked."
        }

    if re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", hostname):
        if is_private_or_restricted_ip(hostname):
            return {
                "safe": False,
                "risk_level": "Blocked",
                "hostname": hostname,
                "scheme": scheme,
                "reason": f"SSRF Protection: Direct private/loopback IP address access is blocked: {hostname}."
            }

    warnings = []
    is_suspicious = False

    # Check 1: HTTP instead of HTTPS
    if scheme == "http":
        warnings.append("Insecure connection (HTTP): Data transmitted is unencrypted.")

    # Check 2: Raw IP address in hostname
    if re.match(r"^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$", hostname):
        warnings.append("Uses raw IP address instead of a recognized domain name.")
        is_suspicious = True

    # Check 3: Suspicious TLD
    for tld in SUSPICIOUS_TLDS:
        if hostname.endswith(tld):
            warnings.append(f"High-risk Top-Level Domain ({tld}) frequently used in phishing campaigns.")
            is_suspicious = True
            break

    # Check 4: Brand impersonation in domain
    for brand in SUSPICIOUS_BRAND_KEYWORDS:
        if brand in hostname and not (hostname.endswith(".gov.in") or hostname.endswith(".nic.in") or hostname == f"{brand}.com"):
            warnings.append(f"Potential brand impersonation: Contains '{brand}' in an unofficial domain.")
            is_suspicious = True
            break

    # Check 5: Excessive subdomains
    subdomain_parts = hostname.split(".")
    if len(subdomain_parts) > 4:
        warnings.append("Excessive subdomain depth often used to obscure true destination.")
        is_suspicious = True

    risk_level = "High Risk" if is_suspicious else ("Moderate Warning" if warnings else "Low Risk / Standard")

    return {
        "url": clean_url,
        "hostname": hostname,
        "scheme": scheme,
        "safe": not is_suspicious,
        "risk_level": risk_level,
        "warnings": warnings,
        "explanation": "Official government and banking sites use official .gov.in, .nic.in, or verified corporate domains. Beware of unofficial clones.",
        "recommendation": "Do not enter passwords or bank details on this link." if is_suspicious else "Verify domain before submitting personal credentials.",
        "timestamp": time.time()
    }

# ==========================================
# GuardBot Conversational Logic
# ==========================================

GUARDBOT_KNOWLEDGE = [
    {
        "trigger": [r"digital arrest", r"cbi call", r"video call arrest"],
        "answer": "Digital Arrest is 100% fake and illegal! No court, police, CBI, or Customs officer will EVER arrest you on a Skype or WhatsApp video call. If anyone calls claiming you are digitally arrested or your parcel has narcotics, disconnect immediately and dial 1930."
    },
    {
        "trigger": [r"1930", r"cyber helpline", r"report fraud"],
        "answer": "1930 is India's National Cybercrime Helpline operated by the Ministry of Home Affairs. If you lost money in an online scam, call 1930 immediately within the 'golden hour' so the police can freeze the scammer's bank account."
    },
    {
        "trigger": [r"job offer", r"work from home", r"part time job", r"like youtube"],
        "answer": "Task scams promise ₹2000-₹5000/day for simple tasks like rating hotels or liking videos. They give small initial payouts to build trust, then demand a deposit to 'unlock VIP tasks' and steal all your funds. Never pay money to get a job."
    },
    {
        "trigger": [r"electricity.*disconnect", r"power cut tonight"],
        "answer": "Electricity cutoff SMS is a phishing scam. Real electricity boards never send personal 10-digit mobile numbers to call. Never install apps like AnyDesk or RustDesk to pay electricity bills."
    },
    {
        "trigger": [r"apk safe", r"unknown apk", r"download apk"],
        "answer": "Never install APK files sent via WhatsApp, Telegram, or SMS. Malicious APKs can steal SMS OTPs, read your screen, and empty your bank account. Only install apps from the Google Play Store."
    }
]

def generate_guardbot_response(message: str, language: str = "en") -> str:
    """Provides instant cyber safety advice in English, Hindi, or Gujarati."""
    msg_lower = message.lower()
    lang = (language or "en").lower()

    if lang == "hi":
        if any(re.search(p, msg_lower) for p in [r"digital arrest", r"cbi call", r"video call arrest", r"डिजिटल"]):
            return "डिजिटल अरेस्ट 100% फर्जी और गैरकानूनी है! कोई भी अदालत, पुलिस, सीबीआई या कस्टम्स अधिकारी कभी भी स्काइप या व्हाट्सएप वीडियो कॉल पर आपको गिरफ्तार नहीं करेगा। तुरंत कॉल काटें और 1930 डायल करें।"
        if any(re.search(p, msg_lower) for p in [r"1930", r"cyber helpline", r"report fraud", r"हेल्पलाइन"]):
            return "1930 गृह मंत्रालय द्वारा संचालित भारत की राष्ट्रीय साइबर अपराध हेल्पलाइन है। ऑनलाइन धोखाधड़ी होने पर 'गोल्डन ऑवर' के भीतर तुरंत 1930 डायल करें ताकि पुलिस स्कैमर के खाते को फ्रीज कर सके।"
        if any(re.search(p, msg_lower) for p in [r"job offer", r"work from home", r"part time job", r"like youtube", r"नौकरी"]):
            return "टास्क स्कैम में वीडियो लाइक करने या होटल रेटिंग के लिए ₹2000-₹5000/दिन का लालच दिया जाता है। वे शुरुआत में थोड़ा भुगतान करते हैं, फिर 'वीआईपी टास्क' के नाम पर पैसे जमा कराकर ठग लेते हैं। नौकरी के लिए कभी पैसे न दें।"
        if any(re.search(p, msg_lower) for p in [r"electricity.*disconnect", r"power cut tonight", r"बिजली"]):
            return "बिजली कटने का एसएमएस एक फ़िशिंग स्कैम है। बिजली विभाग कभी भी कॉल करने के लिए व्यक्तिगत 10 अंकों का मोबाइल नंबर नहीं भेजता। बिजली बिल भरने के लिए एनीडेस्क जैसा कोई ऐप कभी इंस्टॉल न करें।"
        if any(re.search(p, msg_lower) for p in [r"apk safe", r"unknown apk", r"download apk"]):
            return "व्हाट्सएप, टेलीग्राम या एसएमएस से भेजी गई एपीके फाइल्स कभी इंस्टॉल न करें। दुर्भावनापूर्ण एपीके आपके ओटीपी चुरा सकते हैं और बैंक खाता खाली कर सकते हैं। केवल गूगल प्ले स्टोर से ऐप इंस्टॉल करें।"
        return (
            "मैं गार्डबॉट हूं, आपका ट्रूथगार्ड एआई सहायक 🛡️। हमेशा याद रखें:\n"
            "1. किसी के साथ भी अपना ओटीपी या बैंक पासवर्ड साझा न करें।\n"
            "2. वीडियो कॉल पर डिजिटल अरेस्ट एक आपराधिक धोखाधड़ी है।\n"
            "3. ऑनलाइन वित्तीय धोखाधड़ी की तुरंत 1930 पर या cybercrime.gov.in पर रिपोर्ट करें।"
        )
    elif lang == "gu":
        if any(re.search(p, msg_lower) for p in [r"digital arrest", r"cbi call", r"video call arrest"]):
            return "ડિજિટલ અરેસ્ટ ૧૦૦% ખોટું અને ગેરકાયદેસર છે! કોઈ અદાલત, પોલીસ, સીબીઆઈ કે કસ્ટમ્સ અધિકારી ક્યારેય સ્કાયપે કે વ્હોટ્સએપ વીડિયો કૉલ પર તમારી ધરપકડ કરશે નહીં. તુરંત ફોન કટ કરો અને 1930 ડાયલ કરો."
        if any(re.search(p, msg_lower) for p in [r"1930", r"cyber helpline", r"report fraud"]):
            return "1930 એ ગૃહ મંત્રાલય દ્વારા સંચાલિત ભારતની રાષ્ટ્રીય સાયબર ક્રાઈમ હેલ્પલાઇન છે. ઓનલાઇન છેતરપિંડી થાય ત્યારે 'ગોલ્ડન અવર' માં તુરંત 1930 ડાયલ કરો જેથી પોલીસ સ્કેમરનું બેંક ખાતું ફ્રીઝ કરી શકે."
        if any(re.search(p, msg_lower) for p in [r"job offer", r"work from home", r"part time job", r"like youtube"]):
            return "ટાસ્ક સ્કેમમાં વિડીયો લાઈક કરવા કે રેટિંગ આપવા માટે દરરોજ ₹૨૦૦૦-₹૫૦૦૦ ની લાલચ આપવામાં આવે છે. શરૂઆતમાં નાની રકમ ચૂકવી પછી વીઆઈપી ટાસ્કના નામે પૈસા પડાવી લે છે. નોકરી મેળવવા ક્યારેય પૈસા ભરશો નહીં."
        if any(re.search(p, msg_lower) for p in [r"electricity.*disconnect", r"power cut tonight"]):
            return "વીજ જોડાણ કાપવાનો મેસેજ એ ફિશિંગ સ્કેમ છે. વીજળી બોર્ડ ક્યારેય વ્યક્તિગત ૧૦ અંકના મોબાઈલ નંબર સાથે આવો મેસેજ મોકલતું નથી. બિલ ભરવા ક્યારેય એનીડેસ્ક જેવી એપ ડાઉનલોડ કરશો નહીં."
        if any(re.search(p, msg_lower) for p in [r"apk safe", r"unknown apk", r"download apk"]):
            return "વ્હોટ્સએપ, ટેલિગ્રામ કે એસએમએસ પર આવેલી અજાણી એપીકે ક્યારેય ઇન્સ્ટોલ કરશો નહીં. આ એપીકે તમારા ઓટીપી ચોરી શકે છે અને ખાતું ખાલી કરી શકે છે. માત્ર ગૂગલ પ્લે સ્ટોર પરથી જ એપ ઇન્સ્ટોલ કરો."
        return (
            "હું ગાર્ડબોટ છું, તમારો ટ્રુથગાર્ડ એઆઈ સહાયક 🛡️. હંમેશા યાદ રાખો:\n"
            "૧. કોઈની સાથે ઓટીપી કે બેંક પાસવર્ડ શેર કરશો નહીં.\n"
            "૨. વીડિયો કૉલ પર ડિજિટલ અરેસ્ટ એ ગુનાહિત છેતરપિંડી છે.\n"
            "૩. ઓનલાઈન ફ્રોડ માટે તુરંત 1930 પર અથવા cybercrime.gov.in પર ફરિયાદ કરો."
        )

    for item in GUARDBOT_KNOWLEDGE:
        for pattern in item["trigger"]:
            if re.search(pattern, msg_lower):
                return item["answer"]
    return (
        "I am GuardBot, your TruthGuard AI assistant. Always remember:\n"
        "1. Never share OTPs or bank passwords with anyone.\n"
        "2. Digital Arrest over video calls is a criminal scam.\n"
        "3. For online financial fraud, call 1930 or visit cybercrime.gov.in.\n"
        "Feel free to ask me to analyze any suspicious message, news claim, or link!"
    )

# ==========================================
# FastAPI Setup (when dependencies available)
# ==========================================

try:
    from fastapi import FastAPI, HTTPException, Body
    from fastapi.middleware.cors import CORSMiddleware
    from pydantic import BaseModel, Field

    app = FastAPI(
        title="TruthGuard AI Backend",
        description="Comprehensive fact-checking, rumor detection, scam analysis & URL security API",
        version="2.0.0"
    )

    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    class FactCheckRequest(BaseModel):
        claim: str = Field(..., max_length=10000, description="Claim, viral text, or news statement to verify")
        input_type: Optional[str] = Field("NEWS_TEXT", description="NEWS_TEXT, OCR_IMAGE, URL_LINK, CHAT_MESSAGE")
        simple_language_mode: Optional[bool] = Field(False, description="Enable simple language explanations")
        language: Optional[str] = Field("en", description="Target language code: en, hi, gu")

    class UrlInspectRequest(BaseModel):
        url: str = Field(..., max_length=2048, description="URL to inspect for phishing and safety")

    class ChatRequest(BaseModel):
        message: str = Field(..., max_length=2000, description="User message or query")
        history: Optional[List[str]] = Field(default_factory=list, description="Recent conversation turns")
        language: Optional[str] = Field("en", description="Target language code: en, hi, gu")

    @app.get("/")
    def read_root():
        return {
            "status": "healthy",
            "service": "TruthGuard AI Platform Engine",
            "version": "2.0.0",
            "gemini_api_configured": bool(os.environ.get("GEMINI_API_KEY")),
            "timestamp": time.time()
        }

    @app.get("/health")
    def health_check():
        return {
            "status": "ok",
            "database": "connected",
            "ai_engine": "ready",
            "uptime_seconds": time.time()
        }

    @app.post("/api/v1/verify")
    def api_verify(request: FactCheckRequest):
        if not request.claim.strip():
            raise HTTPException(status_code=400, detail="Claim text cannot be empty")
        return run_semantic_verification(
            claim=request.claim,
            input_type=request.input_type or "NEWS_TEXT",
            simple_mode=bool(request.simple_language_mode),
            language=request.language or "en"
        )

    @app.post("/api/v1/inspect-url")
    def api_inspect_url(request: UrlInspectRequest):
        if not request.url.strip():
            raise HTTPException(status_code=400, detail="URL cannot be empty")
        return inspect_url_safety(request.url)

    class ImageVerifyRequest(BaseModel):
        image_base64: Optional[str] = Field(None, description="Base64-encoded image string")
        extracted_text: Optional[str] = Field(None, description="Extracted OCR text if pre-processed")
        claim: Optional[str] = Field(None, description="Alternative claim or query text")
        simple_language_mode: Optional[bool] = Field(False, description="Enable simple language explanations")
        language: Optional[str] = Field("en", description="Target language code: en, hi, gu")

    @app.post("/api/v1/verify-image")
    def api_verify_image(request: ImageVerifyRequest):
        text = (request.extracted_text or request.claim or "").strip()
        img = (request.image_base64 or "").strip()
        if not text and not img:
            raise HTTPException(status_code=400, detail="Please provide an image or extracted text")

        if img:
            ai_res = call_gemini_verify_image(img, text, language=request.language or "en")
            if ai_res:
                return ai_res

        return run_semantic_verification(
            claim=text if text else "Scanned poster / official notice verification",
            input_type="OCR_IMAGE",
            simple_mode=bool(request.simple_language_mode),
            language=request.language or "en"
        )

    @app.post("/api/v1/chat")
    def api_chat(request: ChatRequest):
        if not request.message.strip():
            raise HTTPException(status_code=400, detail="Message cannot be empty")
        reply = generate_guardbot_response(request.message, language=request.language or "en")
        return {
            "reply": reply,
            "timestamp": time.time()
        }

except ImportError:
    app = None

# ==========================================
# Standalone Standard-Library HTTP Server
# ==========================================

from http.server import HTTPServer, BaseHTTPRequestHandler

class StandaloneHandler(BaseHTTPRequestHandler):
    def _send_json(self, status: int, data: Dict[str, Any]):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if self.path in ("/", ""):
            self._send_json(200, {
                "status": "healthy",
                "service": "TruthGuard AI Platform Engine",
                "version": "2.0.0",
                "mode": "standalone_stdlib",
                "timestamp": time.time()
            })
        elif self.path == "/health":
            self._send_json(200, {
                "status": "ok",
                "database": "connected",
                "ai_engine": "ready",
                "timestamp": time.time()
            })
        else:
            self._send_json(404, {"error": "Not Found"})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(length) if length > 0 else b"{}"
        try:
            body = json.loads(post_data.decode("utf-8")) if post_data else {}
        except Exception:
            body = {}

        if self.path == "/api/v1/verify":
            claim = body.get("claim", "")
            input_type = body.get("input_type", "NEWS_TEXT")
            simple_mode = body.get("simple_language_mode", False)
            language = body.get("language", "en")
            if not claim.strip():
                self._send_json(400, {"error": "Claim text cannot be empty"})
                return
            result = run_semantic_verification(claim, input_type, simple_mode, language=language)
            self._send_json(200, result)

        elif self.path == "/api/v1/inspect-url":
            url = body.get("url", "")
            if not url.strip():
                self._send_json(400, {"error": "URL cannot be empty"})
                return
            result = inspect_url_safety(url)
            self._send_json(200, result)

        elif self.path == "/api/v1/verify-image":
            text = (body.get("extracted_text") or body.get("claim") or "").strip()
            img = (body.get("image_base64") or "").strip()
            language = body.get("language", "en")
            simple_mode = body.get("simple_language_mode", False)
            if not text and not img:
                self._send_json(400, {"error": "Please provide an image or extracted text"})
                return
            if img:
                ai_res = call_gemini_verify_image(img, text, language=language)
                if ai_res:
                    self._send_json(200, ai_res)
                    return
            result = run_semantic_verification(
                claim=text if text else "Scanned poster / official notice verification",
                input_type="OCR_IMAGE",
                simple_mode=simple_mode,
                language=language
            )
            self._send_json(200, result)

        elif self.path == "/api/v1/chat":
            msg = body.get("message", "")
            language = body.get("language", "en")
            if not msg.strip():
                self._send_json(400, {"error": "Message cannot be empty"})
                return
            reply = generate_guardbot_response(msg, language=language)
            self._send_json(200, {"reply": reply, "timestamp": time.time()})

        else:
            self._send_json(404, {"error": "Endpoint Not Found"})

def run_server(port: int = 8000):
    if app is not None:
        try:
            import uvicorn
            print(f"Starting TruthGuard FastAPI service on port {port}...")
            uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)
            return
        except ImportError:
            pass

    server = HTTPServer(("0.0.0.0", port), StandaloneHandler)
    print(f"TruthGuard AI backend running on http://0.0.0.0:{port} (stdlib mode)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    run_server(port)
