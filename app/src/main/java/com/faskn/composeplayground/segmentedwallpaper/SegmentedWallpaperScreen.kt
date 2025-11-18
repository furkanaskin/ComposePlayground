package com.faskn.composeplayground.segmentedwallpaper

import android.content.Context
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.rememberAsyncImagePainter
import com.faskn.composeplayground.R
import com.faskn.composeplayground.ui.theme.TechBlack
import com.faskn.composeplayground.ui.theme.TransparentWhite600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private const val LOG_TAG = "SegmentedWallpaper"
private const val CLOCK_LETTER_SPACING = -10f
private const val MIN_TEXT_SIZE = 40f
private const val IMAGE_SCALE_WITH_SEGMENTATION = 1.1f

private fun Context.getWallpaperResourceId(index: Int): Int =
    resources.getIdentifier("wallpaper${index + 1}", "drawable", packageName)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SegmentedWallpaperScreen(
    viewModel: SegmentedWallpaperViewModel = viewModel()
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 5 })
    var segmentationResults by rememberSaveable {
        mutableStateOf<Map<Int, SegmentationResult>>(
            emptyMap()
        )
    }
    val backgroundFillState by viewModel.uiState.collectAsState()
    val segmentationDataSource = remember { ImageSegmentationFactory.create(context) }

    // Track current segmentation job to cancel when page changes
    val currentSegmentationJob = remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(pagerState.currentPage) {
        val currentPage = pagerState.currentPage

        // Cancel any ongoing segmentation from previous page
        currentSegmentationJob.value?.cancel()
        currentSegmentationJob.value = null

        viewModel.resetState()

        // Debounce: Wait 1000ms to see if user is still swiping
        delay(1000)

        if (!segmentationResults.containsKey(currentPage)) {
            val resourceId = context.getWallpaperResourceId(currentPage)

            if (resourceId != 0) {
                currentSegmentationJob.value = launch {
                    try {
                        Log.d(LOG_TAG, "Starting segmentation for page $currentPage")

                        val bitmap = withContext(Dispatchers.IO) {
                            BitmapFactory.decodeResource(context.resources, resourceId)
                        }

                        // Check if still active after loading bitmap
                        if (!isActive) {
                            Log.d(
                                LOG_TAG,
                                "Segmentation cancelled after bitmap load for page $currentPage"
                            )
                            return@launch
                        }

                        bitmap?.let {
                            val result = withContext(Dispatchers.Default) {
                                segmentationDataSource.segmentImage(it)
                            }

                            // Only save result if we're still on the same page
                            if (isActive && pagerState.currentPage == currentPage) {
                                segmentationResults = segmentationResults + (currentPage to result)
                                Log.d(LOG_TAG, "Segmentation completed for page $currentPage")
                            } else {
                                Log.d(
                                    LOG_TAG,
                                    "Segmentation discarded (page changed) for page $currentPage"
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Error during segmentation for page $currentPage", e)
                    } finally {
                        currentSegmentationJob.value = null
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            WallpaperPageContent(
                wallpaperIndex = page,
                segmentationResult = segmentationResults[page],
                currentPage = pagerState.currentPage,
                backgroundFillState = if (page == pagerState.currentPage) backgroundFillState else null
            )
        }

        val currentSegmentationResult = segmentationResults[pagerState.currentPage]
        val isLoading = backgroundFillState is BackgroundFillState.Loading

        AnimatedVisibility(
            visible = currentSegmentationResult?.background != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
        ) {
            val rotation = if (isLoading) {
                val infiniteAnimation = rememberInfiniteTransition(label = "rotation")
                infiniteAnimation.animateFloat(
                    initialValue = 0f,
                    targetValue = 720f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = EaseOutQuint),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "rotation"
                ).value
            } else {
                0f
            }

            Button(
                onClick = {
                    viewModel.generativeFill(
                        image = currentSegmentationResult?.background,
                        prompt = "Extend the given background image to fill transparent area. " +
                                "Do not introduce any objects or identifiable forms. " +
                                "No vehicles, people, animals, structures, shadows, or reflections. " +
                                "Match the surrounding environment's light, texture, depth, " +
                                "and colors so the area appears naturally empty."
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TransparentWhite600),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.gemini_24dp),
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = TechBlack
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Create Depth Effect",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TechBlack
                )
            }
        }

        (backgroundFillState as? BackgroundFillState.Error)?.let { errorState ->
            Text(
                text = errorState.message,
                color = Color.Red,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp, start = 32.dp, end = 32.dp)
            )
        }
    }
}

@Composable
fun WallpaperPageContent(
    wallpaperIndex: Int,
    segmentationResult: SegmentationResult?,
    currentPage: Int,
    backgroundFillState: BackgroundFillState?
) {
    val context = LocalContext.current
    var clockOffset by remember { mutableStateOf(Offset.Zero) }
    var textSize by remember { mutableStateOf(92f) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showFrontLayer by remember { mutableStateOf(true) }
    var gyroOffsetX by remember { mutableFloatStateOf(0f) }
    var gyroOffsetY by remember { mutableFloatStateOf(0f) }
    val hasDepthEffect = backgroundFillState is BackgroundFillState.Success

    LaunchedEffect(backgroundFillState) {
        if (backgroundFillState is BackgroundFillState.Idle && currentPage == wallpaperIndex) {
            clockOffset = Offset.Zero
            showFrontLayer = true
            gyroOffsetX = 0f
            gyroOffsetY = 0f
        }
    }

    DisposableEffect(hasDepthEffect) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (hasDepthEffect && event != null) {
                    gyroOffsetX = (gyroOffsetX + event.values[1] * 2f).coerceIn(-10f, 10f)
                    gyroOffsetY = (gyroOffsetY + event.values[0] * 2f).coerceIn(-10f, 10f)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (hasDepthEffect && gyroscope != null) {
            sensorManager.registerListener(
                listener,
                gyroscope,
                SensorManager.SENSOR_DELAY_GAME
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(currentTime) { timeFormat.format(Date(currentTime)) }

    val textMeasurer = rememberTextMeasurer()
    var containerWidthPx by remember { mutableStateOf(0) }

    // Helper function to measure text width
    val measureTextWidth = remember(textMeasurer) {
        { text: String, size: Float ->
            textMeasurer.measure(
                text = text,
                style = TextStyle(
                    fontSize = size.sp,
                    letterSpacing = TextUnit(CLOCK_LETTER_SPACING, TextUnitType.Sp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                ),
                maxLines = 1
            ).size.width
        }
    }

    // Calculate max text size when container width is available
    LaunchedEffect(containerWidthPx, timeString) {
        if (containerWidthPx > 0) {
            var testSize = 12f
            var lastFittingSize = 12f

            while (testSize <= 500f) {
                if (measureTextWidth(timeString, testSize) <= containerWidthPx) {
                    lastFittingSize = testSize
                    testSize += 5f
                } else {
                    break
                }
            }

            textSize = lastFittingSize
        }
    }

    val hasSegmentation = segmentationResult != null
    val imageScale = if (hasSegmentation) IMAGE_SCALE_WITH_SEGMENTATION else 1f

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    containerWidthPx = coords.size.width
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (hasSegmentation) {
                                showFrontLayer = !showFrontLayer
                            }
                        }
                    )
                }
        ) {
            if (hasSegmentation) {
                val backgroundBitmap = when {
                    backgroundFillState is BackgroundFillState.Success -> backgroundFillState.bitmap
                    else -> segmentationResult.background
                }

                backgroundBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                IntOffset(
                                    -gyroOffsetX.roundToInt(),
                                    -gyroOffsetY.roundToInt()
                                )
                            }
                            .graphicsLayer {
                                scaleX = imageScale
                                scaleY = imageScale
                            },
                        contentScale = ContentScale.Crop
                    )
                }
            } else {
                Image(
                    painter = rememberAsyncImagePainter(
                        context.getWallpaperResourceId(wallpaperIndex)
                    ),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .offset {
                        IntOffset(
                            clockOffset.x.roundToInt(),
                            clockOffset.y.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            clockOffset = Offset(
                                x = clockOffset.x + pan.x,
                                y = clockOffset.y + pan.y
                            )

                            if (containerWidthPx > 0) {
                                val proposedSize = (textSize * zoom).coerceAtLeast(MIN_TEXT_SIZE)
                                val measuredWidth = measureTextWidth(timeString, proposedSize)

                                textSize = if (measuredWidth <= containerWidthPx) {
                                    proposedSize
                                } else {
                                    textSize
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = timeString,
                    fontSize = textSize.sp,
                    letterSpacing = TextUnit(CLOCK_LETTER_SPACING, TextUnitType.Sp),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip
                )
            }

            if (hasSegmentation && showFrontLayer) {
                Image(
                    bitmap = segmentationResult.foreground.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                gyroOffsetX.roundToInt(),
                                gyroOffsetY.roundToInt()
                            )
                        }
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}