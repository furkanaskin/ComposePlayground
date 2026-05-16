package com.faskn.composeplayground.pathmorph

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray700
import com.faskn.composeplayground.ui.theme.Gray800
import com.faskn.composeplayground.ui.theme.Gray850
import com.faskn.composeplayground.ui.theme.OrangeAccent
import com.faskn.composeplayground.ui.theme.White800

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailStatsScreen(onSpeedChange: (Float) -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black900)
    ) {
        // HEADER - NOT TOUCHED
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A0D00), Black900)))
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "PERFORMANCE",
                        fontSize = 10.sp,
                        letterSpacing = 3.sp,
                        color = OrangeAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Deep Dive",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = White800
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Mastering Jetpack Compose",
                        fontSize = 13.sp,
                        color = Gray700,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // CONTENT START
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                "794,128",
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = White800,
                letterSpacing = (-1.5).sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF30D158))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "UPDATED JUST NOW",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF30D158),
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KeyMetricCard("Avg. View", "4:22", Modifier.weight(1f))
                    KeyMetricCard("Retention", "68%", Modifier.weight(1f))
                    KeyMetricCard("CTR", "12.4%", Modifier.weight(1f))
                }
            }

            stickyHeader {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Black900)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { onSpeedChange(0.25f) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Gray850),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("0.25x Slow", style = MaterialTheme.typography.labelLarge)
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onSpeedChange(1f) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Gray850),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Text(
                            "Normal 1.0x",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Text(
                    "ENGAGEMENT OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Gray700,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            item {
                DetailStatRow(
                    icon = Icons.Default.Favorite,
                    label = "Likes",
                    value = "127,327",
                    sub = "+503.2%",
                    color = OrangeAccent,
                    barFraction = 0.78f
                )
            }
            item {
                DetailStatRow(
                    icon = Icons.Filled.ChatBubble,
                    label = "Comments",
                    value = "33,914",
                    sub = "+124.8%",
                    color = Color(0xFF0A84FF),
                    barFraction = 0.34f
                )
            }
            item {
                DetailStatRow(
                    icon = Icons.Default.Share,
                    label = "Shares",
                    value = "9,240",
                    sub = "+91.1%",
                    color = Color(0xFF30D158),
                    barFraction = 0.52f
                )
            }

            item {
                Text(
                    "TRAFFIC SOURCES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Gray700,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                TrafficCard()
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun KeyMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Gray800,
        border = BorderStroke(0.5.dp, Gray850)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 11.sp, color = Gray700, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = White800)
        }
    }
}

@Composable
private fun DetailStatRow(
    icon: ImageVector,
    label: String,
    value: String,
    sub: String,
    color: Color,
    barFraction: Float
) {
    val animatedFraction by animateFloatAsState(
        targetValue = barFraction,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "bar"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Gray800,
        border = BorderStroke(0.5.dp, Gray850)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Gray700,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = White800,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Text(
                    sub,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (sub.contains("+")) Color(0xFF30D158) else Gray700,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(color, color.copy(alpha = 0.6f))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun TrafficCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Gray800,
        border = BorderStroke(0.5.dp, Gray850)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrafficSourceRow("YouTube Search", 0.45f, Color(0xFF0A84FF))
            TrafficSourceRow("External / Direct", 0.28f, Color(0xFF30D158))
            TrafficSourceRow("Suggested Videos", 0.18f, Color(0xFFFF9F0A))
            TrafficSourceRow("Browse Features", 0.09f, Color(0xFFBF5AF2))
        }
    }
}

@Composable
private fun TrafficSourceRow(label: String, fraction: Float, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = White800, fontWeight = FontWeight.Medium)
            Text("${(fraction * 100).toInt()}%", fontSize = 12.sp, color = Gray700)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Preview
@Composable
fun VideoDetailStatsScreenPreview() {
    VideoDetailStatsScreen(onSpeedChange = {})
}
