package com.faskn.composeplayground.sharedelement

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.SharedContentConfig
import androidx.compose.animation.SharedTransitionScope.SharedContentState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.delay

/**
 * Scale Animation: Title scales from large to small during scroll-driven transition.
 *
 * - Font size animates from 48sp (content) to 20sp (toolbar)
 * - Creates a zoom-out effect as title moves to the toolbar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTransitionScope.SharedElementToolbarScaleAnimation(
    onNavigateToSlide: () -> Unit
) {
    val key = "key_shared_transition_toolbar_scale_anim"
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

    // Font size animation creates the scaling effect (48sp → 20sp)
    val animatedFontSize by animateFloatAsState(
        targetValue = if (isTitleInAppBar) 20f else 48f,
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "titleFontSize"
    )

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
        animationSpec = tween(400, easing = LinearOutSlowInEasing),
        label = "imagesAlpha"
    )

    // Content that moves between toolbar and content area with animated font size
    val titleContent = remember {
        movableContentOf {
            Text(
                text = "Photos",
                fontSize = animatedFontSize.sp
            )
        }
    }

    Scaffold(
        containerColor = TechBlack,
        topBar = {
            CenterAlignedTopAppBar(
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
                    IconButton(onClick = onNavigateToSlide) {
                        Icon(
                            imageVector = Icons.Default.Animation,
                            contentDescription = "Change Animation"
                        )
                    }
                },
                title = {
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
                            .skipToLookaheadSize()
                            .skipToLookaheadPosition()
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
                            contentTitleHeight = coordinates.size.height.toFloat()
                            contentTitleY = coordinates.positionInRoot().y
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