package com.faskn.composeplayground.pathmorph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.Black500
import com.faskn.composeplayground.ui.theme.Black800
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray500
import com.faskn.composeplayground.ui.theme.Gray700
import com.faskn.composeplayground.ui.theme.Gray800
import com.faskn.composeplayground.ui.theme.Gray850
import com.faskn.composeplayground.ui.theme.OrangeAccent
import com.faskn.composeplayground.ui.theme.White800
import kotlinx.coroutines.delay

val videoData = listOf(
    Triple("Building a Design System in 2025", "184K", "12.4K"),
    Triple("Advanced Compose Animations Deep Dive", "97K", "8.1K"),
    Triple("Why Your App Feels Slow — Fixed", "231K", "19.7K"),
    Triple("Mastering Jetpack Compose", "794K", "127.3K"),
    Triple("AGSL Shaders: Practical Guide", "58K", "4.9K"),
)

@Composable
fun VideoStatsHomeScreen(onNavigateToDetail: () -> Unit) {
    var showBanner by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(900)
        showBanner = true
    }

    Box(modifier = Modifier.fillMaxSize().background(Black900)) {
        VideoStatsHomeContent()

        AnimatedVisibility(
            visible = showBanner,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
        ) {
            AchievementBanner(onShowClick = onNavigateToDetail)
        }
    }
}

@Composable
private fun VideoStatsHomeContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black900)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0D00), Black900)))
                .padding(horizontal = 16.dp, vertical = 28.dp)
        ) {
            Text(
                text = "ANALYTICS",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = OrangeAccent,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Channel Stats",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = White800
            )
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard("1.3M", "Total views", Modifier.weight(1f))
                SummaryCard("104K", "Subscribers", Modifier.weight(1f))
                SummaryCard("11.2%", "Avg CTR", Modifier.weight(1f))
            }
        }

        HorizontalDivider(color = Gray850, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    "LATEST CONTENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Gray700,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
                )
            }
            itemsIndexed(videoData) { index, (title, views, likes) ->
                VideoStatRow(index, title, views, likes)
            }
        }
    }
}

@Composable
private fun SummaryCard(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Gray800.copy(alpha = 0.6f),
        border = BorderStroke(0.5.dp, Gray850)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = White800)
            Text(label, fontSize = 10.sp, color = Gray700, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun VideoStatRow(index: Int, title: String, views: String, likes: String) {
    val isTop = index == 3
    val backgroundColor = if (index % 2 == 0) Black900 else Black800

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { /* Handle click */ }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 50.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Gray500.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Black500,
                modifier = Modifier.size(20.dp)
            )
            if (isTop) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .background(OrangeAccent, CircleShape)
                        .border(1.5.dp, backgroundColor, CircleShape)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = White800,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniStat(Icons.Default.PlayArrow, views)
                MiniStat(Icons.Default.Favorite, likes)
            }
        }

        Icon(
            Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = if (isTop) OrangeAccent else Black500,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun MiniStat(icon: ImageVector, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Gray700, modifier = Modifier.size(12.dp))
        Text(value, fontSize = 11.sp, color = Gray700, fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true)
@Composable
fun VideoStatsHomeScreenPreview() {
    VideoStatsHomeScreen(onNavigateToDetail = {})
}
