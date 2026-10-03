package com.faskn.composeplayground.bottombarmorph

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.ui.theme.Black800
import com.faskn.composeplayground.ui.theme.CarouselGradientEnd
import com.faskn.composeplayground.ui.theme.CarouselGradientStart
import com.faskn.composeplayground.ui.theme.RacingWhite
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

enum class TabPanelState {
    Collapsed,
    Morphing,
    Expanded
}

private var MorphMillis = 80
private var BlurInMillis = 400
private val MaxBlur = 14.dp
private val ExpandedCorner = 24.dp

private val MeshCorePalette = listOf(
    Color(0xFFFE6940),
    Color(0xFFFF3366),
    Color(0xFFFF1818),
    Color(0xFFEC407A),
    Color(0xFFFF8A65),
    Color(0xFFFF9696),
)
private val MeshTealGlowColor = Color(0xFF64B5F6)

private fun <T> settleSpring() = spring<T>(
    dampingRatio = 0.6f,
    stiffness = 600f,
)

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick,
    )
}

private fun Modifier.blurIfNeeded(radius: Dp): Modifier =
    if (radius > 0.dp) blur(radius, BlurredEdgeTreatment.Unbounded) else this

@Composable
fun WalletBottomBar(
    tabs: List<BottomTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    collapsedHeight: Dp = 56.dp,
    expandedHeight: Dp = Dp.Unspecified,
    expandedContent: @Composable (onClose: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    var phase by remember {
        mutableStateOf(if (expanded) TabPanelState.Expanded else TabPanelState.Collapsed)
    }
    var origin by remember { mutableStateOf(expanded) }

    val transition = updateTransition(targetState = phase, label = "tabPanelMorph")

    LaunchedEffect(expanded) {
        if (phase != TabPanelState.Morphing && expanded == origin) return@LaunchedEffect

        phase = TabPanelState.Morphing
        delay(MorphMillis.milliseconds)
        phase = if (expanded) TabPanelState.Expanded else TabPanelState.Collapsed
        origin = expanded
    }

    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val expandedMeasurables = subcompose("expandedContentMeasure") {
            expandedContent {}
        }
        val unconstrainedHeight = expandedMeasurables.firstOrNull()?.measure(
            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
        )?.height?.toDp() ?: 330.dp

        val targetExpandedHeight =
            if (expandedHeight != Dp.Unspecified) expandedHeight else unconstrainedHeight

        val mainMeasurables = subcompose("mainContent") {
            val animHeight by transition.animateDp(
                label = "containerHeight",
                transitionSpec = {
                    if (targetState == TabPanelState.Morphing) tween(
                        MorphMillis,
                        easing = LinearEasing
                    )
                    else settleSpring()
                },
            ) { state ->
                when (state) {
                    TabPanelState.Collapsed -> collapsedHeight
                    TabPanelState.Expanded -> targetExpandedHeight
                    TabPanelState.Morphing -> targetExpandedHeight / 2
                }
            }

            val animWidthFraction by transition.animateFloat(
                label = "containerWidthFraction",
                transitionSpec = {
                    if (targetState == TabPanelState.Morphing) tween(
                        MorphMillis,
                        easing = LinearEasing
                    )
                    else settleSpring()
                },
            ) { state ->
                when (state) {
                    TabPanelState.Collapsed -> 1f
                    TabPanelState.Expanded -> 1f
                    TabPanelState.Morphing -> if (origin) 1f else 0.33f
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    MorphingTabPanel(
                        transition = transition,
                        expanded = expanded,
                        origin = origin,
                        tabContent = {
                            TabBarTabIconsRow(
                                tabs = tabs,
                                selectedTabIndex = selectedTabIndex,
                                onTabSelected = onTabSelected,
                            )
                        },
                        expandedContent = {
                            expandedContent { expanded = false }
                        },
                        modifier = Modifier
                            .fillMaxWidth(animWidthFraction.coerceIn(0f, 1f))
                            .height(animHeight.coerceAtLeast(0.dp)),
                    )
                }

                QuickSendFab(
                    expanded = expanded,
                    onClick = { expanded = !expanded },
                    onLongClick = {
                        MorphMillis = 200
                        BlurInMillis = 1000
                    }
                )
            }
        }

        val placeables = mainMeasurables.map { it.measure(constraints) }
        val maxWidth = placeables.maxOfOrNull { it.width } ?: constraints.minWidth
        val maxHeight = placeables.maxOfOrNull { it.height } ?: constraints.minHeight

        layout(maxWidth, maxHeight) {
            placeables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@Composable
private fun TabBarTabIconsRow(
    tabs: List<BottomTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            TabIcon(
                tab = tab,
                selected = index == selectedTabIndex,
                onClick = { onTabSelected(index) },
            )
        }
    }
}

@Composable
fun MorphingTabPanel(
    transition: Transition<TabPanelState>,
    expanded: Boolean,
    origin: Boolean,
    tabContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cornerRadiusState = transition.animateDp(
        label = "cornerRadius",
        transitionSpec = {
            if (targetState == TabPanelState.Morphing) tween(MorphMillis, easing = LinearEasing)
            else tween(320, easing = LinearEasing)
        },
    ) { state ->
        when (state) {
            TabPanelState.Collapsed -> 50.dp
            TabPanelState.Expanded -> ExpandedCorner
            TabPanelState.Morphing -> if (origin) ExpandedCorner else 24.dp
        }
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(cornerRadiusState.value),
                spotColor = Color.Black,
                ambientColor = Color.Black
            )
            .graphicsLayer {
                val cornerRadius = cornerRadiusState.value.coerceAtLeast(0.dp)
                shape = RoundedCornerShape(cornerRadius)
                clip = true
            }
            .drawWithCache {
                val cornerPx = cornerRadiusState.value.coerceAtLeast(0.dp).toPx()
                val cornerRadius = CornerRadius(cornerPx)
                val strokeStyle = Stroke(width = 1.dp.toPx())
                onDrawBehind {
                    drawRoundRect(color = BottomBarPanelBackground, cornerRadius = cornerRadius)
                    drawRoundRect(
                        color = PanelBorderColor,
                        cornerRadius = cornerRadius,
                        style = strokeStyle
                    )
                }
            },
    ) {
        MorphLayer(
            visible = !expanded,
            hiddenScale = 0.6f,
            contentAlignment = Alignment.Center,
            content = tabContent,
        )
        MorphLayer(
            visible = expanded,
            hiddenScale = 0.85f,
            contentAlignment = Alignment.TopStart,
            content = expandedContent,
        )
    }
}

@Composable
private fun MorphLayer(
    visible: Boolean,
    hiddenScale: Float,
    contentAlignment: Alignment,
    content: @Composable () -> Unit,
) {
    val alphaState = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) MorphMillis * 2 else MorphMillis / 2,
            easing = LinearEasing,
        ),
        label = "layerAlpha",
    )

    val blurState = animateDpAsState(
        targetValue = if (visible) 0.dp else MaxBlur,
        animationSpec = tween(
            durationMillis = BlurInMillis,
            easing = LinearEasing,
        ),
        label = "layerBlur",
    )

    val scaleState = animateFloatAsState(
        targetValue = if (visible) 1f else hiddenScale,
        animationSpec = if (visible) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        } else {
            tween(MorphMillis, easing = LinearEasing)
        },
        label = "layerScale",
    )

    val isVisible by remember { derivedStateOf { alphaState.value > 0f } }

    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = alphaState.value
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                }
                .blurIfNeeded(blurState.value),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

@Composable
fun QuickContactList(
    recentContacts: List<QuickContact>,
    onContactSelected: (QuickContact) -> Unit,
    modifier: Modifier = Modifier,
    contactRowHeight: Dp = 56.dp,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Quick Transfer Contacts",
            color = Color.White.copy(alpha = 0.45f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp),
        )
        recentContacts.forEach { contact ->
            QuickContactRow(
                contact = contact,
                rowHeight = contactRowHeight,
                onClick = { onContactSelected(contact) },
            )
        }
    }
}

@Composable
private fun TabIcon(
    tab: BottomTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) Color.White else MutedIconColor,
        animationSpec = tween(180),
        label = "tabTint",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = if (selected) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "indicatorWidth"
    )

    Column(
        modifier = Modifier
            .height(56.dp)
            .padding(horizontal = 12.dp)
            .noRippleClickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(indicatorWidth)
                .height(3.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (selected) ActiveTabIndicatorColor else Color.Transparent)
        )
    }
}

@Composable
private fun QuickContactRow(
    contact: QuickContact,
    rowHeight: Dp,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(rowHeight)
            .clip(RoundedCornerShape(14.dp))
            .noRippleClickable(onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContactAvatar(
            initials = contact.initials,
            accentColor = contact.accentColor,
        )
        Text(
            text = contact.name,
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ContactAvatar(
    initials: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = accentColor,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun QuickSendFab(
    expanded: Boolean,
    onClick: () -> Unit,
    onLongClick : () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val transition = updateTransition(targetState = expanded, label = "fabIconMorph")
    val scaleTransition = updateTransition(targetState = isPressed, label = "fabIconScale")

    val scaleState = scaleTransition.animateFloat(
        label = "fabScale",
        transitionSpec = {
            tween(
                durationMillis = 200,
                easing = LinearEasing,
            )
        },
    ) { pressed ->
        if (pressed) 1.32f else 1f
    }

    val iconProgressState = transition.animateFloat(
        label = "iconProgress",
        transitionSpec = {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            )
        },
    ) { isExpanded -> if (isExpanded) 1f else 0f }

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scaleState.value
                scaleY = scaleState.value
            }
            .clip(CircleShape)
            .background(RacingWhite)
            .innerShadow(
                shape = CircleShape,
                shadow = Shadow(
                    radius = 2.dp,
                    spread = 1.5.dp,
                    brush = Brush.linearGradient(
                        start = Offset(0f, 0f),
                        end = Offset(135f, 135f),
                        colors = listOf(
                            Black800.copy(alpha = 0.75f),
                            Color.Transparent,
                            Black800.copy(alpha = 0.5f),
                        ),
                    ),
                ),
            )
            .combinedClickable(
                interactionSource = interactionSource,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        val progress = iconProgressState.value

        val sendAlpha = (1f - progress * 2f).coerceIn(0f, 1f)
        val sendScale = 1f - progress * 0.5f
        val sendRotation = progress * 180f

        val closeAlpha = ((progress - 0.3f) * 2f).coerceIn(0f, 1f)
        val closeScale = 0.5f + progress * 0.5f
        val closeRotation = (1f - progress) * -180f

        if (sendAlpha > 0f) {
            Icon(
                painter = painterResource(R.drawable.ic_send),
                contentDescription = "Send",
                tint = Color(0xFF0F0F0F),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        alpha = sendAlpha
                        scaleX = sendScale
                        scaleY = sendScale
                        rotationZ = sendRotation
                    },
            )
        }

        if (closeAlpha > 0f) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close",
                tint = Color(0xFF0F0F0F),
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        alpha = closeAlpha
                        scaleX = closeScale
                        scaleY = closeScale
                        rotationZ = closeRotation
                    },
            )
        }
    }
}

@Composable
fun AnimatedOrangeMeshGradient(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "colorFlowMovement")
    val progressState = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "progress",
    )

    Box(
        modifier = modifier.drawWithCache {
            onDrawBehind {
                val progress = progressState.value
                val painter = createMeshGradientPainter(progress)
                with(painter) {
                    draw(size)
                }
            }
        },
    )
}

private fun createMeshGradientPainter(progress: Float): MeshGradientPainter {
    return MeshGradientPainter(rows = 3, columns = 3) {
        val rows = 4
        val cols = 4
        val angle = (progress * 2 * Math.PI).toFloat()

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val x = c / (cols - 1).toFloat()
                val y = r / (rows - 1).toFloat()

                val dx = abs(x - 0.5f) * 2f
                val dy = abs(y - 0.5f) * 2f
                val cornerFactor = sqrt(dx * dx + dy * dy).coerceAtMost(1.0f)

                val wave1 = sin(angle + r * 1.4f + c * 0.9f)
                val wave2 = cos(angle * 0.8f - r * 0.7f + c * 1.5f)
                val phaseOffset = (wave1 + wave2) * 0.25f

                val baseColor = getCoreFlowColor(progress, phaseOffset)

                val glowPulse = (sin(angle + (r + c) * 0.9f) + 1f) / 2f
                val tealOpacity =
                    (cornerFactor * 0.75f * (0.4f + 0.6f * glowPulse)).coerceIn(0f, 1f)

                val finalColor = lerp(baseColor, MeshTealGlowColor, tealOpacity)

                setVertex(r, c, Offset(x, y), finalColor)
            }
        }
    }
}

private fun getCoreFlowColor(progress: Float, phaseOffset: Float): Color {
    val total = MeshCorePalette.size
    val normalizedPhase = (phaseOffset % 1f + 1f) % 1f
    val scaled = ((progress + normalizedPhase) * total) % total
    val index = scaled.toInt()
    val nextIndex = (index + 1) % total
    val fraction = scaled - index
    return lerp(MeshCorePalette[index], MeshCorePalette[nextIndex], fraction)
}

@Composable
fun WalletHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(CarouselGradientStart, CarouselGradientEnd),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "FA",
                    color = BrandWhite,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Morning, Furkan",
                    color = BrandWhite,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
fun MainVisaWalletCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
    ) {
        AnimatedOrangeMeshGradient(
            modifier = Modifier
                .fillMaxSize()
                .blur(1.dp, BlurredEdgeTreatment.Rectangle),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$3,200",
                    color = BrandWhite,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = ".00",
                    color = BrandWhite.copy(alpha = 0.65f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        text = "06/2028",
                        color = BrandWhite.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "4466 98** **** 8841",
                        color = BrandWhite.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.sp,
                    )
                }

                Image(
                    modifier = Modifier
                        .height(16.dp)
                        .align(Alignment.Bottom),
                    painter = painterResource(R.drawable.visa_logo),
                    contentDescription = "Visa",
                    colorFilter = ColorFilter.tint(Color.White),
                )
            }
        }
    }
}

@Composable
fun WalletActionButtonsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WalletActionButton(
            label = "Top Up",
            icon = Icons.Rounded.Add,
            modifier = Modifier.weight(1f),
        )
        WalletActionButton(
            label = "Send",
            icon = Icons.AutoMirrored.Rounded.CallMade,
            modifier = Modifier.weight(1f),
        )
        WalletActionButton(
            label = "Request",
            icon = Icons.AutoMirrored.Rounded.CallReceived,
            modifier = Modifier.weight(1f),
        )
        WalletActionButton(
            label = "More",
            icon = Icons.Rounded.GridView,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WalletActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ActionButtonBackground)
            .border(
                1.dp,
                PanelBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .noRippleClickable { }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = BrandWhite,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = BrandWhite.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(transaction.iconBg)
                        .border(
                            1.dp,
                            Color.White.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = transaction.icon,
                        contentDescription = transaction.title,
                        tint = if (transaction.isPositive) PositiveGreen else BrandWhite,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = transaction.title,
                        color = BrandWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.time,
                        color = BrandMutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = transaction.amount,
                    color = if (transaction.isPositive) PositiveGreen else BrandWhite,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (transaction.isPositive) "Income" else "Expense",
                    color = BrandMutedText.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
