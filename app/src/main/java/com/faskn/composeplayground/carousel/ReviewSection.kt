package com.faskn.composeplayground.carousel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.TechBlack

@Composable
fun ReviewSection(
    developer: ComposeDeveloper?,
    onNavigationEvent: (CarouselDirection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CarouselControlButton(
                icon = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                contentDescription = "Previous",
                onClick = { onNavigationEvent(CarouselDirection.PREV) }
            )
            AnimatedContent(
                modifier = Modifier.weight(1f),
                targetState = developer,
                label = "ReviewText",
                transitionSpec = { fadeIn() + slideInVertically() togetherWith slideOutVertically() + fadeOut() }
            ) { currentReviewer ->
                Text(
                    modifier = Modifier
                        .wrapContentSize()
                        .background(TechBlack.copy(alpha = 0.25f), CircleShape)
                        .padding(10.dp),
                    text = currentReviewer?.review.orEmpty(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = White,
                    textAlign = TextAlign.Center,
                )
            }
            CarouselControlButton(
                icon = Icons.AutoMirrored.Default.KeyboardArrowRight,
                contentDescription = "Next",
                onClick = { onNavigationEvent(CarouselDirection.NEXT) }
            )
        }

        Text(
            modifier = Modifier
                .padding(top = 4.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessVeryLow
                    )
                ),
            text = developer?.name.orEmpty(),
            fontWeight = FontWeight.Bold,
            color = White.copy(alpha = 0.25f),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
    }
}
