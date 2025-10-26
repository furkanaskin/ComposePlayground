package com.faskn.composeplayground.shadows

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.carousel.CarouselControlButton
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Black950
import com.faskn.composeplayground.ui.theme.Cyan100
import com.faskn.composeplayground.ui.theme.Gray400
import com.faskn.composeplayground.ui.theme.Gray500
import com.faskn.composeplayground.ui.theme.TechBlack
import com.faskn.composeplayground.ui.theme.White900

// Data class for shadow samples
private data class ShadowSample(
    val modifier: Modifier,
    val title: String,
    val description: String,
    val borderColor: Color,
    val icon: ImageVector,
)

// Shadow configuration constants
data class ShadowConfig(
    val strokeColor: Color = Color(0xFFFF9718),
    val fillColor: Color = Color(0xFFFF5722),
    val secondaryFillColor: Color = Color(0xFF536DFE),
    val glassReflectColor: Color = Color(0xFFF3DFAC),
    val themeColor: Color = Black900
)

@Composable
fun ShadowsScreen() {
    val samples = remember { createShadowSamples() }
    var currentIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val sample = samples[currentIndex]

        ShadowComposable(
            modifier = sample.modifier,
            title = sample.title,
            description = sample.description,
            borderColor = sample.borderColor,
            icon = sample.icon
        )

        Spacer(modifier = Modifier.height(28.dp))

        NavigationControls(onPrevious = {
            currentIndex = if (currentIndex - 1 < 0) samples.lastIndex else currentIndex - 1
        }, onNext = {
            currentIndex = if (currentIndex + 1 > samples.lastIndex) 0 else currentIndex + 1
        })
    }
}

@Composable
private fun NavigationControls(
    onPrevious: () -> Unit, onNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CarouselControlButton(
            onClick = onPrevious,
            icon = Icons.AutoMirrored.Default.ArrowBack,
        )

        CarouselControlButton(
            onClick = onNext,
            icon = Icons.AutoMirrored.Default.ArrowForward,
        )
    }
}

private fun createShadowSamples(): List<ShadowSample> {
    return listOf(
        createFillShadowSample(),
        createGradientFillShadowSample(),
        createCyanVolumetricShadowSample(
            ShadowConfig(
                strokeColor = Color(0xFF00F5FF),
                fillColor = Color(0xFFFF00FF),
                secondaryFillColor = Color(0xFF9D00FF),
                glassReflectColor = Color(0xFFFFFFFF)
            )
        ),
        createVolumetricShadowSample(
            ShadowConfig(
                strokeColor = Color(0xFF00FF99),
                fillColor = Color(0xFF00FF7F),
                secondaryFillColor = Color(0xFF39FF14),
                glassReflectColor = Color(0xFFFFFFFF),
            )
        ),
        createInnerDropShadowSample(),
    )
}

private fun createGradientFillShadowSample(): ShadowSample {
    val config = ShadowConfig(
        strokeColor = Color(0xFFFFD500),
        fillColor = Color(0xFFFF6B35),
        secondaryFillColor = Color(0xFFFFD500)
    )

    val modifier = Modifier
        .width(200.dp)
        .wrapContentHeight()
        .dropShadow(
            RoundedCornerShape(42.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(12.dp, 10.dp),
                color = config.strokeColor,
            )
        )
        .dropShadow(
            RoundedCornerShape(42.dp), Shadow(
                radius = 2.dp,
                offset = DpOffset(8.dp, 6.dp),
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.25f to config.fillColor,
                        0.75f to config.secondaryFillColor,
                    ),
                    start = Offset.Infinite.copy(y = 0f),
                    end = Offset.Infinite.copy(x = 0f),
                ),
            )
        )
        .background(config.themeColor, RoundedCornerShape(40.dp))
        .graphicsLayer { clip = true }

    return ShadowSample(
        modifier = modifier,
        title = "Gradient Fill Shadow",
        description = "Linear gradient shadow transitioning from orange to gold for dynamic effects.",
        borderColor = config.strokeColor,
        icon = Icons.Default.Gradient
    )
}

private fun createFillShadowSample(): ShadowSample {
    val config = ShadowConfig(strokeColor = Color(0xFFFA0505))

    val modifier = Modifier
        .width(200.dp)
        .wrapContentHeight()
        .dropShadow(
            RoundedCornerShape(42.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(12.dp, 10.dp),
                color = config.strokeColor,
            )
        )
        .dropShadow(
            RoundedCornerShape(38.dp), Shadow(
                radius = 2.dp,
                offset = DpOffset(8.dp, 6.dp),
                color = Color(0xFFFAE0E0),
            )
        )
        .background(config.themeColor, RoundedCornerShape(40.dp))
        .graphicsLayer { clip = true }

    return ShadowSample(
        modifier = modifier,
        title = "Chaining Drop Shadows",
        description = "Chaining two drop shadows with solid color fills to create depth and dimension.",
        borderColor = config.strokeColor,
        icon = Icons.Default.Layers
    )
}

private fun createVolumetricShadowSample(
    config: ShadowConfig,
    title: String = "Volumetric Shadows",
    description: String = "Layering multiple gradient shadows to create realistic 3D depth and dimension.",
    icon: ImageVector = Icons.Default.AutoAwesome
): ShadowSample {

    val modifier = Modifier
        .width(200.dp)
        .wrapContentHeight()
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(14.dp, 10.dp),
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.05f to config.strokeColor,
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to Color.Transparent,
                        1f to config.strokeColor.copy(alpha = 0.025f)
                    ),
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(10.dp, 10.dp),
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to Color.Transparent,
                        1f to config.strokeColor.copy(alpha = 0.025f),
                    )
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(36.dp), Shadow(
                radius = 2.dp,
                offset = DpOffset(8.dp, 6.dp),
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.35f to config.fillColor,
                        0.50f to Color.Black,
                        0.75f to Color.Black,
                        0.90f to config.fillColor,
                    ),
                    start = Offset.Infinite.copy(y = 0f),
                    end = Offset.Infinite.copy(x = 0f),
                ),
            )
        )
        .background(config.themeColor, RoundedCornerShape(40.dp))
        .graphicsLayer { clip = true }

    return ShadowSample(
        modifier = modifier,
        title = title,
        description = description,
        borderColor = config.strokeColor,
        icon = icon
    )
}

private fun createCyanVolumetricShadowSample(
    config: ShadowConfig,
    title: String = "Glass Effect?",
    description: String = "Combining multiple gradient shadows with transparent color stops to simulate a glassy effect.",
    icon: ImageVector = Icons.Default.FilterVintage
): ShadowSample {

    val modifier = Modifier
        .width(200.dp)
        .wrapContentHeight()
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(14.dp, 10.dp),
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to Color.Transparent,
                        1f to config.strokeColor.copy(alpha = 0.025f)
                    ),
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(10.dp, 10.dp),
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to Color.Transparent,
                        1f to config.strokeColor.copy(alpha = 0.025f),
                    )
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(36.dp), Shadow(
                radius = 2.dp,
                offset = DpOffset(8.dp, 6.dp),
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.025f to Color.White,
                        0.35f to config.fillColor,
                        0.50f to Color.Black,
                        0.75f to Color.Black,
                        0.90f to config.fillColor,
                        0.95f to Color.White,
                    ),
                    start = Offset.Infinite.copy(y = 0f),
                    end = Offset.Infinite.copy(x = 0f),
                ),
            )
        )
        .background(config.themeColor, RoundedCornerShape(40.dp))
        .graphicsLayer { clip = true }

    return ShadowSample(
        modifier = modifier,
        title = title,
        description = description,
        borderColor = config.strokeColor,
        icon = icon
    )
}

private fun createInnerDropShadowSample(): ShadowSample {
    val config = ShadowConfig(
        strokeColor = Color(0xFF18C9FF),
        fillColor = Color(0xFFFF4081),
        glassReflectColor = Color(0xFF40FFE9),
        secondaryFillColor = Color(0xFF525B5B)
    )

    val modifier = Modifier
        .width(200.dp)
        .wrapContentHeight()
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(14.dp, 10.dp),
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to config.glassReflectColor.copy(alpha = 0.15f),
                        1f to config.strokeColor.copy(alpha = 0.025f)
                    ),
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(40.dp), Shadow(
                radius = 1.dp,
                offset = DpOffset(10.dp, 10.dp),
                brush = Brush.horizontalGradient(
                    colorStops = arrayOf(
                        0.40f to config.strokeColor,
                        0.70f to config.glassReflectColor.copy(alpha = 0.5f),
                        0.85f to config.glassReflectColor.copy(alpha = 0.15f),
                        1f to config.strokeColor.copy(alpha = 0.025f),
                    )
                ),
            )
        )
        .dropShadow(
            RoundedCornerShape(44.dp), Shadow(
                radius = 2.dp,
                offset = DpOffset(8.dp, 6.dp),
                brush = Brush.linearGradient(
                    colorStops = arrayOf(
                        0.05f to Color.White,
                        0.15f to config.secondaryFillColor,
                        0.35f to config.fillColor,
                        0.45f to config.secondaryFillColor,
                        0.50f to Color.Black,
                        0.75f to Color.Black,
                        0.85f to config.fillColor,
                        0.90f to config.secondaryFillColor,
                        1f to Color.White,
                    ),
                    start = Offset.Infinite.copy(y = 0f),
                    end = Offset.Infinite.copy(x = 0f),
                ),
            )
        )
        .background(config.themeColor, RoundedCornerShape(40.dp))
        .innerShadow(
            RoundedCornerShape(42.dp), Shadow(
                radius = 50.dp,
                offset = DpOffset(10.dp, -10.dp),
                brush = Brush.linearGradient(
                    colors = listOf(
                        White900,
                        config.themeColor,
                        Color.Red,
                        config.themeColor,
                    )
                ),
            )
        )
        .innerShadow(
            RoundedCornerShape(42.dp), Shadow(
                radius = 50.dp,
                offset = DpOffset(-10.dp, 10.dp),
                brush = Brush.linearGradient(
                    colors = listOf(
                        config.themeColor,
                        Cyan100,
                        config.themeColor,
                        White900,
                    )
                ),
            )
        )
        .graphicsLayer { clip = true }

    return ShadowSample(
        modifier = modifier,
        title = "Inner + Drop\nShadows",
        description = "Looks like background blur? It's actually inner shadows with high radius values.",
        borderColor = config.strokeColor,
        icon = Icons.Default.MultipleStop
    )
}

@Composable
fun ShadowComposable(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    borderColor: Color,
    icon: ImageVector = Icons.Outlined.LightMode,
) {
    Box(
        modifier = modifier
            .border(
                width = 4.dp, color = borderColor, shape = RoundedCornerShape(36.dp)
            )
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween
        ) {
            AnimatedContent(
                targetState = icon, transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 2 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 2 })
                }, label = "IconAnimation"
            ) { targetIcon ->
                IconBox(icon = targetIcon)
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedContent(
                targetState = title, transitionSpec = {
                    (fadeIn() + slideInHorizontally { it / 4 }) togetherWith (fadeOut() + slideOutVertically { -it / 4 })
                }, label = "TitleAnimation"
            ) { targetTitle ->
                CardTitle(title = targetTitle)
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = description, transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }, label = "DescriptionAnimation"
            ) { targetDescription ->
                CardDescription(description = targetDescription)
            }
        }
    }
}

@Composable
private fun IconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .innerShadow(
                RoundedCornerShape(12.dp), Shadow(
                    12.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(White900, Gray500),
                        start = Offset(90f, -90f),
                        end = Offset.Zero
                    ),
                )
            )
            .border(
                width = 1.dp, color = Black950, shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun CardTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.ExtraBold,
        lineHeight = 24.sp
    )
}

@Composable
private fun CardDescription(description: String) {
    Text(
        text = description, color = Gray400, fontSize = 14.sp
    )
}

@Preview
@Composable
fun ShadowsScreenPreview() {
    val sample = createCyanVolumetricShadowSample(
        ShadowConfig(
            strokeColor = Color(0xFF00F5FF),
            fillColor = Color(0xFFFF00FF),
            secondaryFillColor = Color(0xFF9D00FF),
            glassReflectColor = Color(0xFFFFFFFF)
        ),
        title = "Glass Effect?",
        description = "Combining multiple gradient shadows with transparent color stops to simulate a glassy effect.",
        icon = Icons.Default.AutoAwesome
    )

    Box(
        modifier = Modifier
            .background(Black900)
            .padding(100.dp)
    ) {
        ShadowComposable(
            sample.modifier,
            title = sample.title,
            description = sample.description,
            borderColor = sample.borderColor,
            icon = sample.icon
        )
    }
}
