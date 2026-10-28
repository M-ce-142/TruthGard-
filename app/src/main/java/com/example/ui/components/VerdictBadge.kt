package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Verdict
import com.example.ui.theme.VerdictFakeRed
import com.example.ui.theme.VerdictFakeRedBgDark
import com.example.ui.theme.VerdictFakeRedBgLight
import com.example.ui.theme.VerdictFakeRedDark
import com.example.ui.theme.VerdictMisleadingOrange
import com.example.ui.theme.VerdictMisleadingOrangeBgDark
import com.example.ui.theme.VerdictMisleadingOrangeBgLight
import com.example.ui.theme.VerdictRealGreen
import com.example.ui.theme.VerdictRealGreenBgDark
import com.example.ui.theme.VerdictRealGreenBgLight
import com.example.ui.theme.VerdictRealGreenDark
import com.example.ui.theme.VerdictScamOrange
import com.example.ui.theme.VerdictScamOrangeBgDark
import com.example.ui.theme.VerdictScamOrangeBgLight
import com.example.ui.theme.VerdictUnverifiedGray
import com.example.ui.theme.VerdictUnverifiedGrayBgDark
import com.example.ui.theme.VerdictUnverifiedGrayBgLight
import com.example.ui.theme.isAppDark

@Composable
fun VerdictBadge(
    verdict: Verdict,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.isAppDark

    val label = when (verdict) {
        Verdict.REAL -> stringResource(R.string.verdict_real)
        Verdict.FAKE -> stringResource(R.string.verdict_fake)
        Verdict.POTENTIAL_SCAM -> stringResource(R.string.verdict_scam)
        Verdict.MISLEADING -> stringResource(R.string.verdict_misleading)
        Verdict.NOT_ENOUGH_EVIDENCE -> stringResource(R.string.verdict_unverified)
    }

    val (bgColor, textColor, icon) = when (verdict) {
        Verdict.REAL -> Triple(
            if (isDark) VerdictRealGreenBgDark else VerdictRealGreenBgLight,
            if (isDark) VerdictRealGreenDark else VerdictRealGreen,
            Icons.Default.CheckCircle
        )
        Verdict.FAKE -> Triple(
            if (isDark) VerdictFakeRedBgDark else VerdictFakeRedBgLight,
            if (isDark) VerdictFakeRedDark else VerdictFakeRed,
            Icons.Default.Cancel
        )
        Verdict.POTENTIAL_SCAM -> Triple(
            if (isDark) VerdictScamOrangeBgDark else VerdictScamOrangeBgLight,
            VerdictScamOrange,
            Icons.Default.Dangerous
        )
        Verdict.MISLEADING -> Triple(
            if (isDark) VerdictMisleadingOrangeBgDark else VerdictMisleadingOrangeBgLight,
            VerdictMisleadingOrange,
            Icons.Default.Warning
        )
        Verdict.NOT_ENOUGH_EVIDENCE -> Triple(
            if (isDark) VerdictUnverifiedGrayBgDark else VerdictUnverifiedGrayBgLight,
            if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
            Icons.AutoMirrored.Filled.Help
        )
    }

    val paddingV = if (isLarge) 8.dp else 4.dp
    val paddingH = if (isLarge) 16.dp else 10.dp
    val fontSize = if (isLarge) 14.sp else 11.sp
    val iconSize = if (isLarge) 18.dp else 14.dp

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .border(1.dp, textColor.copy(alpha = if (isDark) 0.5f else 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = paddingH, vertical = paddingV),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
