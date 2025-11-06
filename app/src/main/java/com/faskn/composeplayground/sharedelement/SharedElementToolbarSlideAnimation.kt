package com.faskn.composeplayground.sharedelement

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.delay

/**
 * Slide Animation: Title slides horizontally into the toolbar as the user scrolls.
 *
 * - Title starts large (48sp) in the content area
 * - When scroll threshold is reached, toolbar title slides in from the left
 * - Animation progress is driven by scroll position (0f to 1f)
 * - Transition point: content title midpoint reaches toolbar bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.SharedElementToolbarSlideAnimation(
    onNavigateToScale: () -> Unit
) {
    val key = "key_shared_transition_toolbar_slide_anim"
    val gridState = rememberLazyStaggeredGridState()
    val density = LocalDensity.current

    var scaffoldPaddingTopPx by remember { mutableFloatStateOf(0f) } // (top app bar + status bar)
    var contentTitleY by remember { mutableFloatStateOf(0f) }
    var contentTitleHeight by remember { mutableFloatStateOf(0f) }

    // Determines if layout measurements are ready for transition calculations
    val isLayoutReady by remember {
        derivedStateOf {
            contentTitleHeight > 0f && contentTitleY > 0f && scaffoldPaddingTopPx > 0f
        }
    }

    val config = remember {
        object : SharedContentConfig {
            override val SharedContentState.isEnabled: Boolean
                get() = isLayoutReady
        }
    }

    // Calculate scroll threshold: distance to scroll until transition occurs
    // Transition occurs when content title's midpoint reaches the toolbar's bottom edge
    val scrollThreshold by remember {
        derivedStateOf {
            // Convert content title's Y position to scrollable content coordinates
            val contentTitleRelativeY = contentTitleY - scaffoldPaddingTopPx
            // Calculate the midpoint position (where transition should complete)
            val contentTitleMidpointRelative = contentTitleRelativeY + (contentTitleHeight / 2f)
            contentTitleMidpointRelative
        }
    }

    // Tracks scroll-driven animation progress (0f to 1f)
    // Progress reaches 1f when content title's midpoint aligns with toolbar bottom
    val transitionProgress by remember {
        derivedStateOf {
            val firstItemIndex = gridState.firstVisibleItemIndex
            val firstItemOffset = gridState.firstVisibleItemScrollOffset

            when {
                firstItemIndex > 0 -> 1f
                else -> (firstItemOffset / scrollThreshold).coerceIn(0f, 1f)
            }
        }
    }

    // Determines if title should be shown in toolbar or content area
    val isTitleInAppBar by remember {
        derivedStateOf { transitionProgress >= 1f }
    }

    val images = remember {
        List(30) { index ->
            val imageHeight = if (index % 2 == 0) 300 else 200
            imageHeight to "https://picsum.photos/id/${index + 10}/${imageHeight}/${imageHeight * 2}"
        }
    }

    // Image loading state with 500ms delay
    var showImages by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500)
        showImages = true
    }

    // Animated alpha for images
    val imagesAlpha by animateFloatAsState(
        targetValue = if (showImages) 1f else 0f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "imagesAlpha"
    )

    // Content that moves between toolbar and content area
    val titleContent = remember {
        movableContentOf {
            Text(
                text = "Photos",
                fontSize = if (isTitleInAppBar) 20.sp else 48.sp
            )
        }
    }

    Scaffold(
        containerColor = TechBlack,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechBlack,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateToScale) {
                        Icon(
                            imageVector = Icons.Default.Animation,
                            contentDescription = "Switch to Scale Animation"
                        )
                    }
                },
                title = {
                    val slideInAnimation by animateFloatAsState(
                        targetValue = if (isTitleInAppBar) 1f else 0f,
                        animationSpec = keyframes {
                            durationMillis = 300
                            0f at 0
                            1f at 300 using FastOutSlowInEasing
                        },
                        label = "slideInAnimation"
                    )

                    val alphaAnimation by animateFloatAsState(
                        targetValue = if (isTitleInAppBar) 1f else 0f,
                        animationSpec = keyframes {
                            durationMillis = 500
                            0f at 0
                            1f at 500 using FastOutSlowInEasing
                        },
                        label = "alphaAnimation"
                    )

                    Box(
                        modifier = Modifier
                            .clipToBounds()
                            .sharedElementWithCallerManagedVisibility(
                                renderInOverlayDuringTransition = false,
                                sharedContentState = rememberSharedContentState(
                                    key = key,
                                    config = config
                                ),
                                visible = isTitleInAppBar
                            )
                            .skipToLookaheadPosition()
                            .graphicsLayer {
                                alpha = alphaAnimation
                                translationX = if (isTitleInAppBar) {
                                    lerp(-size.width, 0f, slideInAnimation)
                                } else {
                                    0f
                                }
                            }
                    ) {
                        if (isTitleInAppBar) titleContent()
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                }
            )
        }
    ) { padding ->
        // Capture scaffold's top padding (top app bar + status bar) in pixels
        if (scaffoldPaddingTopPx == 0f) {
            scaffoldPaddingTopPx = with(density) { padding.calculateTopPadding().toPx() }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            state = gridState,
            contentPadding = padding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "title_header",
                span = StaggeredGridItemSpan.FullLine
            ) {
                Box(
                    modifier = Modifier
                        .clipToBounds()
                        .padding(top = 32.dp)
                        .height(60.dp)
                        .onPlaced { coordinates ->
                            // Measure content title position and height only once
                            if (contentTitleY == 0f || contentTitleHeight == 0f) {
                                contentTitleY = coordinates.positionInRoot().y
                                contentTitleHeight = coordinates.size.height.toFloat()
                            }
                        }
                        .sharedElementWithCallerManagedVisibility(
                            renderInOverlayDuringTransition = false,
                            sharedContentState = rememberSharedContentState(
                                key = key,
                                config = config
                            ),
                            visible = !isTitleInAppBar
                        )
                        .skipToLookaheadSize()
                        .skipToLookaheadPosition()
                        .graphicsLayer { alpha = 1f - transitionProgress.fastCoerceAtMost(0.75f) }
                ) {
                    if (!isTitleInAppBar) titleContent()
                }
            }

            items(
                items = images,
                key = { index -> "photo_$index" }
            ) { item ->
                ImageItem(
                    height = item.first,
                    url = item.second,
                    alpha = imagesAlpha
                )
            }
        }
    }
}