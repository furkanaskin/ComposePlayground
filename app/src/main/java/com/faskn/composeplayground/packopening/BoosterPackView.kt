package com.faskn.composeplayground.packopening

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val JAG_STEP_DP = 13f       // zigzag tear step size
private const val JAG_AMOUNT_DP = 3.5f   // zigzag tear amplitude
private const val GRAB_ZONE_X = 0.25f     // top-left grab area threshold
private const val GRAB_ZONE_EXTRA_DP = 36f

// ---- Peel strip curling ----
private const val PEEL_SLICE_DP = 5f      // slice width for smooth curl
private const val PEEL_BASE_DEG = 5f      // initial hinge rotation angle
private const val PEEL_TOTAL_CURL_DEG = 11f // total curl angle along strip

// ---- Swipe down to reveal ----
private const val DRAG_DISTANCE = 0.55f   // swipe down travel ratio
private const val EXIT_EXTRA = 0.3f       // additional exit travel distance

@Composable
fun BoosterPackView(
    state: BoosterPackState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val flapLayer = rememberGraphicsLayer()
    val attachedPath = remember { Path() }
    val flapPath = remember { Path() }

    val timeSec by produceState(0f) {
        val startTime = System.nanoTime()
        while (true) {
            withFrameNanos { frameTime ->
                value = (frameTime - startTime) / 1_000_000_000f
            }
        }
    }

    val shader = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                RuntimeShader(PUFFY_AGSL)
            } catch (_: Exception) {
                null
            }
        } else null
    }
    val glowShader = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            try {
                RuntimeShader(ZIP_GLOW_AGSL)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { state.packCoords = it; state.updateStripFraction() }
            .then(
                if (!state.revealed) {
                    Modifier.pointerInput(state) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)

                            if (!state.torn) {
                                val grabsStrip = down.position.x <= size.width * GRAB_ZONE_X &&
                                        down.position.y <= size.height * state.stripFraction + GRAB_ZONE_EXTRA_DP.dp.toPx()

                                state.touchDown(down.position)
                                if (grabsStrip) state.dragTear(down.position.x / size.width)

                                drag(down.id) { change ->
                                    change.consume()
                                    state.touchMove(change.position)
                                    if (grabsStrip) state.dragTear(change.position.x / size.width)
                                }
                                state.release()
                            } else if (!state.revealed) {
                                // Swipe down: pack follows finger
                                drag(down.id) { change ->
                                    val dy = change.position.y - change.previousPosition.y
                                    change.consume()
                                    state.updatePackDrag(state.packDrag + dy / (size.height * DRAG_DISTANCE))
                                }
                                state.releasePackDrag()
                            }
                        }
                    }
                } else Modifier
            )
            .drawWithContent {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@drawWithContent

                val exit = state.packExit
                if (exit >= 0.999f) return@drawWithContent

                layer.record { this@drawWithContent.drawContent() }
                layer.alpha = 1f - exit

                val t = state.tear.coerceIn(0f, 1f)
                val fall = state.fall
                val dragY = (state.packDrag * DRAG_DISTANCE + exit * EXIT_EXTRA) * h

                if (t <= 0.001f && fall == 0f) {
                    drawLayer(layer)
                    return@drawWithContent
                }

                val stripH = h * state.stripFraction
                val tearX = t * w
                buildTearPaths(
                    attached = attachedPath,
                    flap = flapPath,
                    w = w,
                    h = h,
                    stripH = stripH,
                    tearX = tearX,
                    step = JAG_STEP_DP.dp.toPx(),
                    jag = JAG_AMOUNT_DP.dp.toPx(),
                )

                val lift = min(1f, t * 3f)

                // 1) Pack body (vertical slide)
                withTransform({
                    translate(0f, dragY)
                }) {
                    clipPath(attachedPath) { drawLayer(layer) }
                }

                // 2) Peel strip
                if (tearX > 1f && fall < 1f) {
                    flapLayer.record { clipPath(flapPath) { drawLayer(layer) } }
                    flapLayer.alpha = 1f - fall * fall
                    drawPeel(
                        flapLayer = flapLayer,
                        tearX = tearX,
                        stripH = stripH,
                        flapH = stripH + JAG_AMOUNT_DP.dp.toPx(),
                        lift = lift,
                        fall = fall,
                        h = h,
                    )
                }

                // 3) Zip Glow
                val fingerGlow = state.pressStrength * min(1f, t * 4f) * (1f - fall)
                val burstProgress = (1f - state.burst).coerceIn(0f, 1f)
                val openingGlow = if (state.burst > 0f && fall < 0.999f) {
                    0.38f * burstProgress * burstProgress * (1f - fall * 0.25f)
                } else {
                    0f
                }
                val glowStrength = max(fingerGlow, openingGlow)

                if (glowStrength > 0.01f && glowShader != null && Build.VERSION.SDK_INT >= 33) {
                    drawZipGlowShader(
                        shader = glowShader,
                        rarity = state.rarity,
                        head = Offset(tearX, stripH),
                        strength = glowStrength,
                        timeSec = timeSec,
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    if (shader != null && Build.VERSION.SDK_INT >= 33) {
                        try {
                            val press = state.pressPx
                            val pressNormX = if (press == Offset.Unspecified) 0.5f else press.x / size.width
                            val pressNormY = if (press == Offset.Unspecified) 0.5f else press.y / size.height
                            val effectivePressStrength = if (state.tear > 0.001f) 0f else state.pressStrength

                            shader.setFloatUniform("size", size.width, size.height)
                            shader.setFloatUniform("strip", state.stripFraction)
                            shader.setFloatUniform("press", pressNormX, pressNormY, effectivePressStrength)

                            renderEffect = RenderEffect
                                .createRuntimeShaderEffect(shader, "image")
                                .asComposeRenderEffect()
                        } catch (_: Exception) {
                            // Fallback if uniform setting fails
                        }
                    }
                }
                .drawWithContent {
                    drawContent()
                }
        ) {
            content()
        }
    }
}

/**
 * Peel strip sliced into linked segments curling smoothly from hinge.
 */
private fun DrawScope.drawPeel(
    flapLayer: GraphicsLayer,
    tearX: Float,
    stripH: Float,
    flapH: Float,
    lift: Float,
    fall: Float,
    h: Float,
) {
    val slice = PEEL_SLICE_DP.dp.toPx()
    val n = max(1, ceil(tearX / slice).toInt())
    val bendPerSlice = PEEL_TOTAL_CURL_DEG * lift / n
    val toRad = (PI / 180.0).toFloat()

    var px = tearX
    var py = stripH
    var angle = PEEL_BASE_DEG * lift

    withTransform({
        translate(0f, h * 0.9f * fall)
    }) {
        for (k in 0 until n) {
            val x1 = tearX - k * slice
            val x0 = max(0f, x1 - slice)
            val len = x1 - x0
            if (len <= 0f) break

            withTransform({
                translate(px - x1, py - stripH)
                rotate(angle, Offset(x1, stripH))
            }) {
                clipRect(x0, 0f, x1 + 1f, flapH) { drawLayer(flapLayer) }
            }

            px -= len * cos(angle * toRad)
            py -= len * sin(angle * toRad)
            angle += bendPerSlice * (len / slice)
        }
    }
}

private fun jagged(i: Int): Float {
    val s = sin(i * 12.9898f) * 43758.547f
    return s - floor(s)
}

/**
 * Zigzag tear path along tear line (y = stripH).
 */
private fun buildTearPaths(
    attached: Path,
    flap: Path,
    w: Float,
    h: Float,
    stripH: Float,
    tearX: Float,
    step: Float,
    jag: Float,
) {
    attached.rewind()
    flap.rewind()

    val n = max(1, ceil(tearX / step).toInt())
    fun ex(i: Int) = min(i * step, tearX)
    fun ey(i: Int) = stripH + (jagged(i) - 0.5f) * jag

    attached.moveTo(0f, ey(0))
    for (i in 1..n) attached.lineTo(ex(i), ey(i))
    attached.lineTo(tearX, 0f)
    attached.lineTo(w, 0f)
    attached.lineTo(w, h)
    attached.lineTo(0f, h)
    attached.close()

    flap.moveTo(0f, 0f)
    flap.lineTo(tearX, 0f)
    for (i in n downTo 0) flap.lineTo(ex(i), ey(i))
    flap.close()
}

fun Modifier.tearLine(state: BoosterPackState): Modifier = onGloballyPositioned {
    state.lineCoords = it
    state.updateStripFraction()
}
