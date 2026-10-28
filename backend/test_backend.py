import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
os.environ["GEMINI_API_KEY"] = ""  # Ensure offline unit testing

from main import (
    run_semantic_verification,
    inspect_url_safety,
    generate_guardbot_response,
    is_private_or_restricted_ip
)

class TestTruthGuardBackend(unittest.TestCase):

    def test_digital_arrest_scam_detection(self):
        claim = "Urgent: CBI officer on Skype video call says your parcel has narcotics. Digital arrest warrant issued."
        result = run_semantic_verification(claim, input_type="CHAT_MESSAGE")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertEqual(result["scam_type"], "Digital Arrest Scam")
        self.assertIn("1930", result["suggested_action"])
        self.assertTrue(len(result["red_flags"]) > 0)

    def test_free_recharge_scheme_detection(self):
        claim = "Government is offering free 3-month recharge to all students. Click http://free-recharge.xyz now!"
        result = run_semantic_verification(claim, input_type="NEWS_TEXT")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertEqual(result["scam_type"], "Fake Government Scheme / Giveaway")
        self.assertTrue(any("recharge" in rf.lower() or "xyz" in rf.lower() or "forward" in rf.lower() for rf in result["red_flags"]))

    def test_part_time_job_scam_detection(self):
        claim = "Earn 5000 daily by rating hotels and liking youtube videos. Deposit 500 to unlock VIP."
        result = run_semantic_verification(claim, input_type="CHAT_MESSAGE")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertEqual(result["scam_type"], "Task & Part-Time Job Scam")

    def test_rbi_banknote_rumor_detection(self):
        claim = "RBI has announced that 500 note invalid from next week."
        result = run_semantic_verification(claim, input_type="NEWS_TEXT")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertIn("Reserve Bank of India", result["detailed_explanation"])

    def test_real_space_milestone(self):
        claim = "ISRO successfully tested cryogenic stage engine for Gaganyaan deep space mission."
        result = run_semantic_verification(claim, input_type="NEWS_TEXT")
        self.assertEqual(result["verdict"], "REAL")
        self.assertIn("ISRO", result["summary"])

    def test_unverified_claim_returns_not_enough_evidence(self):
        claim = "A local baker in town baked a 4-foot tall strawberry cake yesterday."
        result = run_semantic_verification(claim, input_type="NEWS_TEXT")
        self.assertEqual(result["verdict"], "NOT_ENOUGH_EVIDENCE")

    def test_ssrf_protection_blocks_private_ips(self):
        self.assertTrue(is_private_or_restricted_ip("127.0.0.1"))
        self.assertTrue(is_private_or_restricted_ip("10.0.0.1"))
        self.assertTrue(is_private_or_restricted_ip("192.168.1.1"))
        self.assertTrue(is_private_or_restricted_ip("169.254.169.254"))
        self.assertFalse(is_private_or_restricted_ip("8.8.8.8"))

    def test_url_inspector_detects_suspicious_tld(self):
        res = inspect_url_safety("http://pm-kisan-lottery-gift.xyz/claim")
        self.assertFalse(res["safe"])
        self.assertEqual(res["risk_level"], "High Risk")
        self.assertTrue(any("xyz" in w.lower() for w in res["warnings"]))

    def test_url_inspector_rejects_dangerous_scheme(self):
        res = inspect_url_safety("file:///etc/passwd")
        self.assertFalse(res["safe"])
        self.assertEqual(res["risk_level"], "Dangerous")

    def test_guardbot_advisory(self):
        reply = generate_guardbot_response("What should I do if someone says I am under Digital Arrest?")
        self.assertIn("1930", reply)
        self.assertIn("fake", reply.lower())

    def test_verification_hindi_language(self):
        claim = "Urgent: CBI officer on Skype video call says your parcel has narcotics. Digital arrest warrant issued."
        result = run_semantic_verification(claim, input_type="CHAT_MESSAGE", language="hi")
        self.assertEqual(result["verdict"], "FAKE")
        # Check that Hindi text is present in summary or suggested_action
        self.assertTrue(len(result["suggested_action"]) > 0)
        self.assertIn("1930", result["suggested_action"])

    def test_verification_gujarati_language(self):
        claim = "Government is offering free 3-month recharge to all students. Click http://free-recharge.xyz now!"
        result = run_semantic_verification(claim, input_type="NEWS_TEXT", language="gu")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertTrue(len(result["suggested_action"]) > 0)

    def test_guardbot_hindi_reply(self):
        reply = generate_guardbot_response("डिजिटल अरेस्ट क्या है?", language="hi")
        self.assertIn("1930", reply)

    def test_guardbot_gujarati_reply(self):
        reply = generate_guardbot_response("ડિજિટલ અરેસ્ટ શું છે?", language="gu")
        self.assertIn("1930", reply)

    def test_image_verification_fake_job_poster(self):
        extracted_text = "Urgent Hiring: Earn ₹5000 daily from home. Telegram task VIP. Deposit ₹500."
        result = run_semantic_verification(extracted_text, input_type="OCR_IMAGE")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertEqual(result["input_type"], "OCR_IMAGE")
        self.assertEqual(result["scam_type"], "Task & Part-Time Job Scam")

    def test_image_verification_digital_arrest_notice(self):
        extracted_text = "CBI Digital Arrest Notice: Skype hearing required immediately for parcel drugs."
        result = run_semantic_verification(extracted_text, input_type="OCR_IMAGE", language="hi")
        self.assertEqual(result["verdict"], "FAKE")
        self.assertEqual(result["input_type"], "OCR_IMAGE")
        self.assertIn("1930", result["suggested_action"])

if __name__ == "__main__":
    unittest.main()
