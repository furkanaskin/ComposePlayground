package com.faskn.composeplayground.carousel

import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.faskn.composeplayground.ui.theme.Black700

@Composable
fun CarouselControlButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(containerColor = Black700.copy(alpha = 0.5f))
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}