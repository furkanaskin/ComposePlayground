package com.faskn.composeplayground.pathmorph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.Black950
import com.faskn.composeplayground.ui.theme.OrangeAccent
import com.faskn.composeplayground.ui.theme.White800

@Composable
fun AchievementBanner(onShowClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF2A1400), Black950)))
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(OrangeAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = OrangeAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    "Milestone reached",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = White800
                )
                Text(
                    "New achievement unlocked",
                    fontSize = 11.sp,
                    color = OrangeAccent
                )
            }
        }
        TextButton(onClick = onShowClick) {
            Text(
                "View",
                color = OrangeAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Preview
@Composable
fun AchievementBannerPreview() {
    AchievementBanner(onShowClick = {})
}
