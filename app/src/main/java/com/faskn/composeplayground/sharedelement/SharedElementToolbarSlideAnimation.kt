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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.delay

/**
 * Slide Animation: Title slides horizontally into the toolbar as the user scrolls.
 *
 * - Title starts large (48sp) in the content area
 * - When scroll threshold is reached, toolbar title slides in from the left
 * - Animation progress is driven by scroll position (0f to 1f)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.SharedElementToolbarSlideAnimation(
    onNavigateToScale: () -> Unit
) {
    val key = "key_shared_transition_toolbar_slide_anim"
    val gridState = rememberLazyStaggeredGridState()
    var topAppBarY by remember { mutableFloatStateOf(0f) }
    var contentTitleY by remember { mutableFloatStateOf(0f) }
    var contentTitleHeight by remember { mutableFloatStateOf(0f) }

    // Determines if layout measurements are ready for transition calculations
    val isLayoutReady by remember {
        derivedStateOf {
            contentTitleHeight > 0f && contentTitleY > 0f && topAppBarY > 0f
        }
    }

    val config = remember {
        object : SharedContentConfig {
            override val SharedContentState.isEnabled: Boolean
                get() = isLayoutReady
        }
    }

    // Scroll threshold to trigger title transition
    val scrollThreshold by remember {
        derivedStateOf {
            contentTitleY - (topAppBarY - contentTitleHeight / 2f)
        }
    }

    // Tracks scroll-driven animation progress (0f to 1f)
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
                modifier = Modifier.onPlaced { coordinates ->
                    topAppBarY = coordinates.positionInRoot().y
                },
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
                            contentTitleY = coordinates.positionInRoot().y
                            contentTitleHeight = coordinates.size.height.toFloat()
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
                        .graphicsLayer { alpha = 1f - transitionProgress }
                ) {
                    if (!isTitleInAppBar) titleContent()
                }
            }

            items(
                count = images.size,
                key = { index -> "photo_$index" }
            ) { index ->
                ImageItem(
                    height = images[index].first,
                    url = images[index].second,
                    alpha = imagesAlpha
                )
            }
        }
    }
}