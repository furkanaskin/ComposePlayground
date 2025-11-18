package com.faskn.composeplayground.segmentedwallpaper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import com.faskn.composeplayground.ui.theme.Black800
import com.faskn.composeplayground.ui.theme.Green300
import com.faskn.composeplayground.ui.theme.TechBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SegmentedWallpaperScreen(
    viewModel: SegmentedWallpaperViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var segmentationResult by remember { mutableStateOf<SegmentationResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val backgroundFillState by viewModel.uiState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        segmentationResult = null
        errorMessage = null
    }

    val segmentationDataSource = remember {
        ImageSegmentationFactory.create(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        TechBlack,
                        Black800,
                        TechBlack
                    )
                )
            )
    ) {
        if (selectedImageUri == null) {
            ImageSelectionScreen(
                onSelectImage = { imagePickerLauncher.launch("image/*") }
            )
        } else {
            ImagePreviewScreen(
                imageUri = selectedImageUri!!,
                segmentationResult = segmentationResult,
                isProcessing = isProcessing,
                errorMessage = errorMessage,
                backgroundFillState = backgroundFillState,
                viewModel = viewModel,
                onChangeImage = {
                    selectedImageUri = null
                    segmentationResult = null
                    errorMessage = null
                    viewModel.resetState()
                },
                onProcess = {
                    scope.launch {
                        isProcessing = true
                        errorMessage = null

                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                // First, decode image bounds without loading the full bitmap
                                val options = BitmapFactory.Options().apply {
                                    inJustDecodeBounds = true
                                }

                                context.contentResolver.openInputStream(selectedImageUri!!)
                                    ?.use { stream ->
                                        BitmapFactory.decodeStream(stream, null, options)
                                    }

                                // Calculate inSampleSize to reduce memory usage
                                val maxDimension = 2048 // Maximum width or height
                                var inSampleSize = 1

                                if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                                    val halfHeight = options.outHeight / 2
                                    val halfWidth = options.outWidth / 2

                                    // Calculate the largest inSampleSize value that is a power of 2
                                    while ((halfHeight / inSampleSize) >= maxDimension &&
                                        (halfWidth / inSampleSize) >= maxDimension
                                    ) {
                                        inSampleSize *= 2
                                    }
                                }

                                // Decode with inSampleSize
                                val decodingOptions = BitmapFactory.Options().apply {
                                    this.inSampleSize = inSampleSize
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                }

                                context.contentResolver.openInputStream(selectedImageUri!!)
                                    ?.use { stream ->
                                        BitmapFactory.decodeStream(stream, null, decodingOptions)
                                    }
                            }

                            if (bitmap != null) {
                                // Perform segmentation on original bitmap (no scaling)
                                val result = withContext(Dispatchers.Default) {
                                    segmentationDataSource.segmentImage(bitmap)
                                }
                                segmentationResult = result
                            } else {
                                errorMessage = "Failed to load image"
                            }
                        } catch (_: OutOfMemoryError) {
                            errorMessage = "Image too large. Please select a smaller image."
                        } catch (e: Exception) {
                            errorMessage = "Segmentation failed: ${e.message}"
                        } finally {
                            isProcessing = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ImageSelectionScreen(onSelectImage: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Select Image from Gallery",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Button(
                onClick = onSelectImage,
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green300
                )
            ) {
                Text(
                    text = "Select Image",
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ImagePreviewScreen(
    imageUri: Uri,
    segmentationResult: SegmentationResult?,
    isProcessing: Boolean,
    errorMessage: String?,
    backgroundFillState: BackgroundFillState,
    viewModel: SegmentedWallpaperViewModel,
    onChangeImage: () -> Unit,
    onProcess: () -> Unit
) {
    var clockOffset by remember { mutableStateOf(Offset.Zero) }
    var textSize by remember { mutableStateOf(92f) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showButtons by remember { mutableStateOf(true) }
    var showFrontLayer by remember { mutableStateOf(true) }

    // Hide buttons when background fill is successful
    LaunchedEffect(backgroundFillState) {
        if (backgroundFillState is BackgroundFillState.Success) {
            delay(500) // Small delay before hiding
            showButtons = false
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

    // Re-clamp text size when container width or text content changes
    val maxFitSp by remember(timeString, containerWidthPx) {
        mutableStateOf(
            run {
                if (containerWidthPx <= 0) 172f else {
                    // Binary search max font size that fits in containerWidthPx
                    var low = 12f
                    var high = 300f
                    var best = 12f
                    while (high - low > 0.5f) {
                        val mid = (low + high) / 2f
                        val layout = textMeasurer.measure(
                            text = timeString,
                            style = TextStyle(
                                fontSize = mid.sp,
                                letterSpacing = TextUnit(-10f, TextUnitType.Sp),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            softWrap = false
                        )
                        if (layout.size.width <= containerWidthPx) {
                            best = mid
                            low = mid
                        } else {
                            high = mid
                        }
                    }
                    best.coerceIn(12f, 300f)
                }
            }
        )
    }

    // Also ensure current textSize never exceeds maxFitSp
    LaunchedEffect(maxFitSp) {
        textSize = textSize.coerceAtMost(maxFitSp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        TechBlack,
                        Black800,
                        TechBlack
                    )
                )
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 19.5f),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
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
                                    // Toggle front layer visibility on double tap anywhere
                                    showFrontLayer = !showFrontLayer
                                }
                            )
                        }
                ) {
                    // Show Gemini-filled background if available, otherwise show original or segmented background
                    when (backgroundFillState) {
                        is BackgroundFillState.Success -> {
                            Image(
                                bitmap = backgroundFillState.bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        else -> {
                            if (segmentationResult != null && segmentationResult.background != null) {
                                Image(
                                    bitmap = segmentationResult.background.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Image(
                                    painter = rememberAsyncImagePainter(imageUri),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
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
                                    val proposed = (textSize * zoom).coerceAtLeast(12f)
                                    textSize = proposed.coerceAtMost(maxFitSp)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeString,
                            fontSize = textSize.sp,
                            letterSpacing = TextUnit(-10f, TextUnitType.Sp),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }

                    if (segmentationResult != null && showFrontLayer) {
                        Image(
                            bitmap = segmentationResult.foreground.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            AnimatedVisibility(errorMessage != null) {
                Text(
                    errorMessage.orEmpty(),
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        AnimatedVisibility(
            visible = showButtons,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        12.dp,
                        Alignment.CenterHorizontally
                    )
                ) {
                    Button(
                        onClick = onChangeImage,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f)
                        )
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onProcess,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green300
                        )
                    ) {
                        Text(
                            text = if (isProcessing) "Processing..." else "Process",
                            fontSize = 12.sp
                        )
                    }

                    if (segmentationResult != null && segmentationResult.background != null) {
                        Button(
                            onClick = {
                                viewModel.generativeFill(
                                    image = segmentationResult.background,
                                    prompt = "Generate a clean background continuation in the masked area. " +
                                            "Do not introduce any objects or identifiable forms." +
                                            " No vehicles, people, animals, structures, shadows, or reflections. " +
                                            "Match the surrounding environment's light, texture, depth," +
                                            " and colors so the area appears naturally empty.",
                                )
                            },
                            enabled = backgroundFillState !is BackgroundFillState.Loading,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green300
                            )
                        ) {
                            Text(
                                text = when (backgroundFillState) {
                                    is BackgroundFillState.Loading -> "Processing..."
                                    else -> "Fill Background"
                                },
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                when (backgroundFillState) {
                    is BackgroundFillState.Loading -> {
                        Text(
                            text = backgroundFillState.message,
                            color = Green300,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    is BackgroundFillState.Error -> {
                        Text(
                            text = backgroundFillState.message,
                            color = Color.Red,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .padding(horizontal = 32.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {}
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}