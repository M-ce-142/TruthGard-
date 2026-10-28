package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("TruthGuard AI", appName)
  }

  @Test
  fun `verify Scan Image navigation screen route and resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("ocr_verify", com.example.ui.navigation.Screen.OcrVerify.route)
    val scanLabel = context.getString(com.example.ui.navigation.Screen.OcrVerify.titleRes)
    assertEquals("Scan Image", scanLabel)
  }

  @Test
  fun `verify OCR Image verification result model and inputType`() {
    val result = com.example.data.model.VerificationResult(
        claimTitle = "Urgent Notice: Telecom SIM KYC deactivation order",
        verdict = com.example.data.model.Verdict.FAKE,
        confidenceScore = 90,
        easyExplanation = "Telecom operators never demand immediate KYC via third party APK links.",
        detailedExplanation = "Fraudulent advisory impersonating TRAI/DoT authorities.",
        trustedSources = listOf("Press Information Bureau (PIB)", "DoT Advisory"),
        recommendation = "Do not click links or share OTPs. Report on 1930.",
        inputType = "OCR_IMAGE",
        scamType = "Urgent Utility / SIM Scam"
    )
    assertEquals("OCR_IMAGE", result.inputType)
    assertEquals(com.example.data.model.Verdict.FAKE, result.verdict)
    assertEquals(90, result.confidenceScore)
  }
}
