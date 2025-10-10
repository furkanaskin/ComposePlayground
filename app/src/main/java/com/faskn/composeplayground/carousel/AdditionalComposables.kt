package com.faskn.composeplayground.carousel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.AndroidGreen
import com.faskn.composeplayground.ui.theme.CarouselGradientEnd
import com.faskn.composeplayground.ui.theme.Green300

// All paddings hardcoded for showcase purposes
@Composable
fun BoxScope.HeaderText(isPortrait: Boolean) {
    Text(
        text = buildAnnotatedString {
            append("From\n")
            withStyle(style = SpanStyle(AndroidGreen)) {
                append("Android ")
            }
            append(if (isPortrait) "Devs" else "\nDevs")
        },
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 48.sp,
        color = Color.White,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .align(if (isPortrait) Alignment.TopStart else Alignment.CenterStart)
            .safeContentPadding()
            .padding(
                start = if (!isPortrait) 24.dp else 0.dp,
                end = 24.dp,
                top = if (isPortrait) 72.dp else 0.dp
            ),
        minLines = 2
    )
}

@Composable
fun BoxScope.ActionButton(isPortrait: Boolean) {
    Button(
        onClick = { /* no-op */ },
        shape = RoundedCornerShape(32.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp),
        border = BorderStroke(
            width = 2.dp,
            color = AndroidGreen
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = CarouselGradientEnd,
            contentColor = Color.White
        ),
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = Modifier
            .safeContentPadding()
            .align(if (isPortrait) Alignment.BottomCenter else Alignment.CenterEnd)
            .padding(
                end = if (!isPortrait) 24.dp else 0.dp
            )
            .dropShadow(
                RoundedCornerShape(32.dp), Shadow(
                    radius = 12.dp,
                    color = Green300,
                )
            )
    ) {
        Text(
            text = "Get Started",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp),
            color = Color.White
        )
    }
}