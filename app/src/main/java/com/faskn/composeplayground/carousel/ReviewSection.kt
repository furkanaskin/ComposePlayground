package com.faskn.composeplayground.carousel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ReviewSection(
    reviewer: ComposeReviewer?,
    onPageDirection: (CarouselDirection) -> Unit
) {

    AnimatedContent(
        modifier = Modifier.fillMaxWidth(),
        targetState = reviewer,
        label = "ReviewText",
        transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { currentReviewer ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CarouselControlButton(
                    icon = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    onClick = { onPageDirection(CarouselDirection.PREV) }
                )
                Text(
                    text = currentReviewer?.review.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                    color = White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                CarouselControlButton(
                    icon = Icons.AutoMirrored.Default.KeyboardArrowRight,
                    contentDescription = "Next",
                    onClick = { onPageDirection(CarouselDirection.NEXT) }
                )
            }

            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = currentReviewer?.name.orEmpty(),
                fontWeight = FontWeight.Bold,
                color = White.copy(alpha = 0.25f),
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}
