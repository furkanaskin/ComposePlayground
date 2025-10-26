package com.faskn.composeplayground.sharedelement

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.faskn.composeplayground.ui.theme.Green300
import com.faskn.composeplayground.ui.theme.TechBlack

@Composable
fun SharedElementScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
            .safeContentPadding(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = buildAnnotatedString {
                append("This sample has been moved to the ")
                withStyle(
                    SpanStyle(
                        color = Green300
                    )
                ) {
                    append("compose-beta-bom-shared-transition")
                }
                append("\nbranch because it requires ")
                withStyle(
                    SpanStyle(
                        color = Green300
                    )
                ) {
                    append("compose.animation:1.10.0-beta01")
                }
                append("\nwhich may break the Layout Inspector in stable Compose versions.\n\n")
                append("You can review and test the implementation in that branch. ")
                append("This screen will be updated here once the feature is available in a stable release.")
            }
        )
    }
}