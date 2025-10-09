package com.faskn.composeplayground.carousel

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ReviewSection(
    reviewer: ComposeReviewer,
    longestEdge: Dp,
    onPageDirection: (CarouselDirection) -> Unit
) {
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val verticalPadding = if (isPortrait) {
        longestEdge / 4
    } else {
        0.dp
    }
    AnimatedContent(
        modifier = Modifier
            .fillMaxWidth()
            .safeContentPadding()
            .padding(vertical = verticalPadding),
        targetState = reviewer,
        label = "ReviewText",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { currentReviewer ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CarouselControlButton(
                    icon = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    onClick = { onPageDirection(CarouselDirection.PREV) }
                )
                Text(
                    text = currentReviewer.review,
                    fontWeight = FontWeight.SemiBold,
                    color = White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                CarouselControlButton(
                    icon = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = "Next",
                    onClick = { onPageDirection(CarouselDirection.NEXT) }
                )
            }
            Text(
                text = currentReviewer.name,
                fontWeight = FontWeight.Bold,
                color = White.copy(alpha = 0.25f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}
