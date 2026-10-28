package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.Verdict
import com.example.ui.components.VerdictBadge
import com.example.ui.theme.TruthGuardTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun verdictBadgeScreenshot() {
    composeTestRule.setContent {
      TruthGuardTheme {
        VerdictBadge(verdict = Verdict.REAL)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/verdict_badge.png")
  }
}
