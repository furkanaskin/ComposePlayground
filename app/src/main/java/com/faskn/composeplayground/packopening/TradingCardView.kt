package com.faskn.composeplayground.packopening

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.telemetry.Exo2Family
import com.faskn.composeplayground.telemetry.MichromaFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val FOIL_PERIOD_MS = 12000

/**
 * The revealed collectible trading card. Supports:
 * 1) Double tap to flip 180° between Front and Back faces.
 * 2) 3D tilt and inspection by dragging finger across the card.
 * 3) Smooth fade-out on transition.
 */
@Composable
fun CollectibleCardView(
    state: BoosterPackState,
    modifier: Modifier = Modifier,
    packWidthDp: Float = PackConstants.PACK_WIDTH_DP,
    packHeightDp: Float = PackConstants.PACK_HEIGHT_DP,
) {
    val localDensity = LocalDensity.current
    val rect = PackGeometry.cardRect(packWidthDp, packHeightDp, state.stripFraction)
    val card = state.card

    var isFlipped by rememberSaveable(card) { mutableStateOf(false) }
    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }

    var cardWidthPx by remember { mutableFloatStateOf(1f) }
    var cardHeightPx by remember { mutableFloatStateOf(1f) }

    var touchX by remember { mutableFloatStateOf(0.5f) }
    var touchY by remember { mutableFloatStateOf(0.5f) }

    val flipRotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "flipRotation"
    )

    val animatedRotationX by animateFloatAsState(
        targetValue = rotationX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationX"
    )

    val animatedRotationY by animateFloatAsState(
        targetValue = rotationY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotationY"
    )

    val animatedTouchX by animateFloatAsState(
        targetValue = touchX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "touchX"
    )

    val animatedTouchY by animateFloatAsState(
        targetValue = touchY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "touchY"
    )

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                cardWidthPx = coordinates.size.width.toFloat()
                cardHeightPx = coordinates.size.height.toFloat()
            }
            .then(
                if (state.revealed) {
                    Modifier.pointerInput(card) {
                        var lastClickTime = 0L
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var isDrag = false

                            do {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break

                                if (change.pressed) {
                                    val positionChange = change.positionChange()
                                    if (positionChange.getDistance() > 3f) {
                                        isDrag = true
                                        change.consume()

                                        val currentTouchX = change.position.x
                                        val currentTouchY = change.position.y

                                        val normalizedX = (currentTouchX - cardWidthPx / 2f) / (cardWidthPx / 2f)
                                        val normalizedY = (currentTouchY - cardHeightPx / 2f) / (cardHeightPx / 2f)

                                        val maxRotation = 25f

                                        rotationX = -normalizedY * maxRotation
                                        rotationY = normalizedX * maxRotation

                                        touchX = (currentTouchX / cardWidthPx).coerceIn(0f, 1f)
                                        touchY = (currentTouchY / cardHeightPx).coerceIn(0f, 1f)
                                    }
                                }
                            } while (event.changes.any { it.pressed })

                            if (!isDrag) {
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime < 350) {
                                    isFlipped = !isFlipped
                                    lastClickTime = 0L
                                } else {
                                    lastClickTime = currentTime
                                }
                            }

                            rotationX = 0f
                            rotationY = 0f
                            touchX = 0.5f
                            touchY = 0.5f
                        }
                    }
                } else Modifier
            )
            .graphicsLayer {
                val restY = rect.topDp + rect.heightDp / 2f - packHeightDp / 2f
                translationY = (restY * (1f - state.cardSettle)).dp.toPx()

                val scale = (1f + 0.05f * state.cardSettle) * (0.85f + 0.15f * state.smokeFade)
                scaleX = scale
                scaleY = scale
                alpha = state.smokeFade

                this.rotationY = flipRotation + animatedRotationY
                this.rotationX = animatedRotationX
                cameraDistance = 12f * localDensity.density
            },
        contentAlignment = Alignment.Center
    ) {
        TradingCardFace(
            modifier = Modifier.run {
                if (flipRotation > 90f) graphicsLayer { this.rotationY = 180f }
                else this
            },
            isFront = flipRotation > 90f,
            card = card,
            widthDp = rect.widthDp,
            heightDp = rect.heightDp,
            touchX = animatedTouchX,
            touchY = animatedTouchY,
        )
    }
}

/**
 * Container choosing between front and back trading card faces.
 */
@Composable
fun TradingCardFace(
    modifier: Modifier = Modifier,
    isFront: Boolean,
    card: TradingCard,
    widthDp: Float,
    heightDp: Float,
    touchX: Float = 0.5f,
    touchY: Float = 0.5f,
) {
    Box(modifier = modifier) {
        if (isFront) {
            TradingCardFront(
                card = card,
                widthDp = widthDp,
                heightDp = heightDp,
                touchX = touchX,
                touchY = touchY,
            )
        } else {
            TradingCardBack(
                widthDp = widthDp,
                heightDp = heightDp,
                rarity = card.rarity,
            )
        }
    }
}

/**
 * Dark metallic physical collectible card front layout.
 */
@Composable
fun TradingCardFront(
    card: TradingCard,
    widthDp: Float,
    heightDp: Float,
    touchX: Float = 0.5f,
    touchY: Float = 0.5f,
) {
    val rarity = card.rarity
    val palette = remember(rarity) { FoilPalette.forRarity(rarity) }
    val infinite = rememberInfiniteTransition(label = "tradingCardFoil")
    val phase: State<Float> = if (rarity == CardRarity.COMMON) {
        remember { mutableFloatStateOf(0f) }
    } else {
        infinite.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(FOIL_PERIOD_MS, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ), label = "foilPhase"
        )
    }
    val shape = RoundedCornerShape(14.dp)

    Box(
        Modifier
            .size(widthDp.dp, heightDp.dp)
            .clip(shape)
            .background(Color(0xFF08090D))
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(Color(0xFF1B1D25), Color(0xFF08090C), Color(0xFF13141C)),
                    start = Offset.Zero, end = Offset(w, h)
                ),
                cornerRadius = CornerRadius(14.dp.toPx())
            )

            if (rarity == CardRarity.EPIC || rarity == CardRarity.LEGENDARY || rarity == CardRarity.MYTHIC) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.primary.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(w * 0.5f, h * 0.38f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.5f, h * 0.38f)
                )
            }

            val edge = Brush.linearGradient(
                listOf(palette.dark, palette.light, palette.primary, palette.dark),
                start = Offset(w * (0.1f + touchX * 0.3f), 0f),
                end = Offset(w * (0.8f - touchX * 0.2f), h)
            )
            drawRoundRect(
                edge,
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(2.dp.toPx())
            )

            val inset = 7.dp.toPx()
            drawRoundRect(
                color = palette.primary.copy(alpha = 0.35f),
                topLeft = Offset(inset, inset),
                size = Size(w - 2 * inset, h - 2 * inset),
                cornerRadius = CornerRadius(10.dp.toPx()),
                style = Stroke(0.8.dp.toPx())
            )
        }

        Column(
            Modifier
                .matchParentSize()
                .padding(11.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(card.artworkRes),
                    contentDescription = card.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                palette.primary.copy(alpha = 0.65f),
                                Color.Transparent,
                                palette.light.copy(alpha = 0.45f)
                            )
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                        style = Stroke(1.2.dp.toPx())
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = card.title.uppercase(Locale.current.platformLocale),
                    fontFamily = MichromaFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            listOf(palette.light, Color.White, palette.primary, palette.light)
                        )
                    )
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = card.cardId,
                    fontFamily = Exo2Family,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.light.copy(alpha = 0.85f),
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = card.loreDescription,
                fontFamily = Exo2Family,
                fontSize = 8.sp,
                lineHeight = 11.5.sp,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 2.dp)
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "${card.subtitle.uppercase()}  •  COLLECTIBLE",
                fontFamily = Exo2Family,
                fontSize = 7.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.light.copy(alpha = 0.60f),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        HolographicFoil(
            rarity = rarity,
            phase = phase,
            touchX = touchX,
            touchY = touchY,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun HolographicFoil(
    rarity: CardRarity,
    phase: State<Float>,
    touchX: Float,
    touchY: Float,
    modifier: Modifier = Modifier,
) {
    if (rarity == CardRarity.COMMON) return
    val palette = remember(rarity) { FoilPalette.forRarity(rarity) }
    val tier = when (rarity) {
        CardRarity.COMMON -> 0f
        CardRarity.RARE -> 1f
        CardRarity.EPIC -> 2f
        CardRarity.LEGENDARY -> 3f
        CardRarity.MYTHIC -> 4f
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember {
            try {
                RuntimeShader(SHIFT_FOIL_AGSL)
            } catch (_: Exception) {
                null
            }
        }
        if (shader != null) {
            Canvas(modifier) {
                shader.setFloatUniform("resolution", size.width, size.height)
                shader.setFloatUniform("pointer", touchX, touchY)
                shader.setFloatUniform("time", phase.value)
                shader.setFloatUniform("strength", palette.intensity)
                shader.setFloatUniform("tier", tier)
                shader.setFloatUniform("tint", palette.light.red, palette.light.green, palette.light.blue)
                drawRect(brush = ShaderBrush(shader), blendMode = BlendMode.Plus)
            }
        }
    } else {
        Canvas(modifier) {
            val p = phase.value
            val band = Brush.linearGradient(
                colors = listOf(Color.Transparent, palette.light.copy(alpha = palette.intensity * 0.22f), Color.Transparent),
                start = Offset(size.width * (p - 0.6f), 0f),
                end = Offset(size.width * (p + 0.2f), size.height)
            )
            drawRoundRect(band, cornerRadius = CornerRadius(14.dp.toPx()), style = Stroke(2.dp.toPx()))
        }
    }
}

/**
 * Trading card back design.
 */
@Composable
fun TradingCardBack(
    modifier: Modifier = Modifier,
    widthDp: Float = PackConstants.DEFAULT_CARD_WIDTH_DP,
    heightDp: Float = PackConstants.DEFAULT_CARD_HEIGHT_DP,
    rarity: CardRarity = CardRarity.COMMON,
) {
    val tint = rarity.color
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(widthDp.dp, heightDp.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF15161D), Color(0xFF08090C)))),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            val edge = Brush.linearGradient(
                listOf(tint, tint.copy(alpha = 0.5f), tint),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
            drawRoundRect(
                edge,
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(1.5.dp.toPx())
            )

            val inset = 9.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(1.dp.toPx())
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StarMark(Modifier.size(16.dp), tint)
            Spacer(Modifier.height(14.dp))
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "CITY//SHIFT",
                    modifier = Modifier.offset(x = (-2.5).dp, y = (-1).dp),
                    fontFamily = MichromaFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PackColors.HoloPink.copy(alpha = 0.42f),
                    letterSpacing = (-1.1).sp,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = "CITY//SHIFT",
                    modifier = Modifier.offset(x = (-1).dp, y = (-1).dp),
                    fontFamily = MichromaFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PackColors.HoloCyan.copy(alpha = 0.38f),
                    letterSpacing = (-1.1).sp,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = "CITY//SHIFT",
                    fontFamily = MichromaFamily,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PackColors.PackWhite,
                    letterSpacing = (-1.1).sp,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .width(28.dp)
                    .height(1.dp)
                    .background(tint)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "REALITY IS COLLECTIBLE",
                fontFamily = Exo2Family,
                fontSize = 6.5.sp,
                letterSpacing = 3.sp,
                color = Color.White.copy(alpha = 0.5f),
                maxLines = 1
            )
        }

        Text(
            text = "SERIES 001",
            fontFamily = Exo2Family,
            fontSize = 6.5.sp,
            letterSpacing = 3.sp,
            color = tint.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
private fun StarMark(modifier: Modifier, color: Color) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val star = Path()
        for (k in 0 until 8) {
            val a = (-PI / 2).toFloat() + k * (PI / 4).toFloat()
            val rad = if (k % 2 == 0) r else r * 0.24f
            val x = center.x + cos(a) * rad
            val y = center.y + sin(a) * rad
            if (k == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, color)
    }
}
