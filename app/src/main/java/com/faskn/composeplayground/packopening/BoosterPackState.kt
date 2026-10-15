package com.faskn.composeplayground.packopening

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val COMPLETE_AT = 0.85f     // threshold to complete tear
private const val COMMIT_AT = 0.35f       // release threshold to reveal card

@Stable
class BoosterPackState internal constructor(private val scope: CoroutineScope) {

    /** 0..1: tear progress */
    var tear by mutableFloatStateOf(0f)
        private set

    /** 0..1: torn strip falling progress */
    var fall by mutableFloatStateOf(0f)
        private set

    /** 0..1: opening glow burst progress */
    var burst by mutableFloatStateOf(0f)
        private set

    /** 0..1: user pack drag progress */
    var packDrag by mutableFloatStateOf(0f)
        private set

    /** 0..1: pack body exit progress */
    var packExit by mutableFloatStateOf(0f)
        private set

    /** 0..1: card settling progress */
    var cardSettle by mutableFloatStateOf(0f)
        private set

    /** Card is fully revealed on center stage */
    var revealed by mutableStateOf(false)
        private set

    /** 0..1: touch press strength */
    var pressStrength by mutableFloatStateOf(0f)
        private set

    var pressPx by mutableStateOf(Offset.Unspecified)
        private set

    /** Top strip is torn; pack wrapper is ready for downward drag */
    var torn by mutableStateOf(false)
        private set

    /** 0..1: smooth smoke and card fade progress for opening another pack */
    var smokeFade by mutableFloatStateOf(1f)
        private set

    var isTransitioning by mutableStateOf(false)
        private set

    /** Trading card inside current booster pack */
    var card by mutableStateOf(TradingCard.roll())
        private set

    val rarity: CardRarity get() = card.rarity

    private var tearJob: Job? = null
    private var pressJob: Job? = null
    private var dragJob: Job? = null

    private var dragLocked = false

    /** 0..1: tear line y relative to pack height */
    var stripFraction by mutableFloatStateOf(0.1f)
        private set

    internal var packCoords: LayoutCoordinates? = null
    internal var lineCoords: LayoutCoordinates? = null

    internal fun updateStripFraction() {
        val pack = packCoords?.takeIf { it.isAttached && it.size.height > 0 } ?: return
        val line = lineCoords?.takeIf { it.isAttached } ?: return
        val centerY = pack.localPositionOf(line, Offset.Zero).y + line.size.height / 2f
        stripFraction = (centerY / pack.size.height).coerceIn(0f, 1f)
    }

    internal fun touchDown(p: Offset) {
        if (!torn && !isTransitioning) {
            pressPx = p
            animatePress(1f)
        }
    }

    internal fun touchMove(p: Offset) {
        if (!torn && !isTransitioning) {
            pressPx = p
        }
    }

    internal fun dragTear(progress: Float) {
        if (torn || isTransitioning) return
        tearJob?.cancel()
        tear = progress.coerceIn(0f, 1f)
    }

    internal fun updatePackDrag(progress: Float) {
        if (!torn || revealed || dragLocked || isTransitioning) return
        dragJob?.cancel()
        packDrag = progress.coerceIn(0f, 1f)
    }

    internal fun releasePackDrag() {
        if (!torn || revealed || dragLocked || isTransitioning) return
        dragJob?.cancel()
        val startDrag = packDrag
        dragJob = scope.launch {
            if (startDrag >= COMMIT_AT) {
                dragLocked = true
                coroutineScope {
                    launch {
                        animate(
                            initialValue = startDrag,
                            targetValue = 1f,
                            animationSpec = tween(380, easing = FastOutLinearInEasing)
                        ) { v, _ -> packDrag = v }
                    }
                    launch {
                        animate(
                            initialValue = 0f,
                            targetValue = 1f,
                            animationSpec = tween(380, easing = FastOutLinearInEasing)
                        ) { v, _ -> packExit = v }
                    }
                    launch {
                        animate(
                            initialValue = cardSettle,
                            targetValue = 1f,
                            animationSpec = tween(480, easing = FastOutSlowInEasing)
                        ) { v, _ -> cardSettle = v }
                    }
                }
                revealed = true
            } else {
                animate(
                    initialValue = startDrag,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)
                ) { v, _ -> packDrag = v }
            }
        }
    }

    internal fun release() {
        animatePress(0f)
        if (torn || isTransitioning) return
        tearJob?.cancel()
        tearJob = scope.launch {
            if (tear >= COMPLETE_AT) {
                animate(
                    initialValue = tear,
                    targetValue = 1f,
                    animationSpec = tween(140)
                ) { v, _ -> tear = v }
                coroutineScope {
                    launch {
                        animate(
                            0f,
                            1f,
                            animationSpec = tween(800, easing = LinearEasing)
                        ) { v, _ -> burst = v }
                    }
                    animate(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = tween(550, easing = FastOutLinearInEasing)
                    ) { v, _ -> fall = v }
                    torn = true
                }
            } else {
                animate(
                    initialValue = tear,
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)
                ) { v, _ -> tear = v }
            }
        }
    }

    private fun resetInternal() {
        tearJob?.cancel()
        pressJob?.cancel()
        dragJob?.cancel()
        dragLocked = false
        tear = 0f
        fall = 0f
        burst = 0f
        packDrag = 0f
        packExit = 0f
        cardSettle = 0f
        revealed = false
        pressStrength = 0f
        torn = false
    }

    /** Reset booster pack state for the same trading card */
    fun reset() {
        if (isTransitioning) return
        resetInternal()
        smokeFade = 1f
    }

    /** Reset booster pack state, smoothly fade out smoke, and roll a new trading card */
    fun openAnother(forced: CardRarity? = null) {
        if (isTransitioning) return
        scope.launch {
            isTransitioning = true
            // Smoothly animate smoke and revealed card fading out instead of instant disappearance
            animate(
                initialValue = smokeFade,
                targetValue = 0f,
                animationSpec = tween(350, easing = FastOutLinearInEasing)
            ) { v, _ -> smokeFade = v }

            resetInternal()
            card = TradingCard.roll(forced)

            // Fade smoke back in gracefully for the new booster pack
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) { v, _ -> smokeFade = v }
            isTransitioning = false
        }
    }

    private fun animatePress(target: Float) {
        pressJob?.cancel()
        pressJob = scope.launch {
            animate(
                initialValue = pressStrength,
                targetValue = target,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)
            ) { v, _ -> pressStrength = v }
        }
    }
}

@Composable
fun rememberBoosterPackState(): BoosterPackState {
    val scope = rememberCoroutineScope()
    return remember { BoosterPackState(scope) }
}
