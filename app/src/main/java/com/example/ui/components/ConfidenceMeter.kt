package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Verdict
import com.example.ui.theme.VerdictFakeRed
import com.example.ui.theme.VerdictFakeRedDark
import com.example.ui.theme.VerdictMisleadingOrange
import com.example.ui.theme.VerdictRealGreen
import com.example.ui.theme.VerdictRealGreenDark
import com.example.ui.theme.isAppDark

@Composable
fun ConfidenceMeter(
    score: Int,
    verdict: Verdict? = null,
    modifier: Modifier = Modifier
) {
    val progressAnimated by animateFloatAsState(targetValue = score / 100f, label = "evidenceProgress")
    val isDark = MaterialTheme.colorScheme.isAppDark

    val evidenceLabel = when {
        verdict == Verdict.NOT_ENOUGH_EVIDENCE || score <= 50 -> stringResource(R.string.confidence_preliminary)
        verdict == Verdict.REAL -> stringResource(R.string.confidence_high_official)
        verdict == Verdict.FAKE || verdict == Verdict.POTENTIAL_SCAM -> stringResource(R.string.confidence_high_scam)
        verdict == Verdict.MISLEADING -> stringResource(R.string.confidence_moderate_context)
        score >= 80 -> stringResource(R.string.confidence_strong)
        else -> stringResource(R.string.confidence_moderate)
    }

    val indicatorColor = when {
        verdict == Verdict.NOT_ENOUGH_EVIDENCE || score <= 50 -> MaterialTheme.colorScheme.onSurfaceVariant
        verdict == Verdict.REAL -> if (isDark) VerdictRealGreenDark else VerdictRealGreen
        verdict == Verdict.FAKE || verdict == Verdict.POTENTIAL_SCAM -> if (isDark) VerdictFakeRedDark else VerdictFakeRed
        verdict == Verdict.MISLEADING -> VerdictMisleadingOrange
        score >= 80 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.confidence_grounding),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = evidenceLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progressAnimated },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
