package com.faskn.composeplayground.product.view

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.faskn.composeplayground.ui.theme.*
import io.github.sceneview.Scene
import io.github.sceneview.math.*
import io.github.sceneview.model.model
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader

@Composable
fun ProductDetailScreen(
    productId: Int,
    navController: NavController,
    padding: PaddingValues
) {
    val product = productList.find { it.id == productId }
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)

    if (product == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("Product not found", color = Color.Red)
        }
        return
    }


    val centerNode = rememberNode(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = -0.75f, z = -0.75f)
        lookAt(Position(0f, 0f, 0f))
        centerNode.addChildNode(this)
    }

    var isNavigatingBack by remember { mutableStateOf(false) }
    var isModelLoading by remember { mutableStateOf(true) }

    val targetAlpha = when {
        isModelLoading -> 0f
        isNavigatingBack -> 0f
        else -> 1f
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 5000, easing = LinearEasing),
        label = "scene_alpha"
    )

    var modelInstance by remember {
        mutableStateOf(
            modelLoader.createModelInstance(
                assetFileLocation = "models/${product.modelName}"
            )
        )
    }
    var modelNode by remember {
        mutableStateOf(
            ModelNode(
                modelInstance = modelInstance,
                scaleToUnits = 0.5f,
                centerOrigin = Position(0f, 0f, 0f)
            )
        )
    }

    var animationProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(durationMillis = 2500, easing = EaseInOutCubic),
        label = "rotation_animation",
        finishedListener = {
            centerNode.position = Position(0f, 0f, 0f)
            cameraNode.position = Position(x = 0f, y = 0f, z = 1f)
            centerNode.lookAt(Position(0f, 0.01f, 0.01f))
            cameraNode.lookAt(Position(0f, 0.01f, 0.0f))
        }
    )

    val rotationX by remember {
        derivedStateOf {
            animatedProgress * -120f
        }
    }

    val rotationY by remember {
        derivedStateOf {
            animatedProgress * -360f
        }
    }

    val rotationZ by remember {
        derivedStateOf {
            animatedProgress * -180f
        }
    }

    // Apply rotation animation to model node using transform
    LaunchedEffect(rotationX, rotationY, modelNode) {
        modelNode.transform(
            rotation = Rotation(x = rotationX, y = rotationY, z = rotationZ)
        )
    }

    // Start animation when model is loaded
    LaunchedEffect(isModelLoading) {
        if (!isModelLoading) {
            animationProgress = 1f
        }
    }

    // Handle back navigation with cleanup
    fun handleBackNavigation() {
        if (!isNavigatingBack) {
            isNavigatingBack = true
            try {
                // Clean up model instance
                modelNode.let { centerNode.removeChildNode(it) }
                modelInstance.model.popRenderable()
            } catch (e: Exception) {
                Log.e("ProductDetail", "Error cleaning up: ${e.message}")
            }
            navController.popBackStack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black900)
    ) {
        val containerSize = LocalWindowInfo.current.containerSize

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            TransparentWhite600,
                            Black900,
                        ),
                        center = Offset(containerSize.width / 2f, 0f),
                        radius = 1000f
                    )
                )
                .graphicsLayer {
                    alpha = animatedAlpha
                }
                .padding(padding)

        ) {
            Box(
                modifier = Modifier.weight(0.5f)
            ) {
                if (!isNavigatingBack) {
                    Scene(
                        modifier = Modifier.fillMaxSize(),
                        engine = engine,
                        isOpaque = false,
                        cameraNode = cameraNode,
                        mainLightNode = rememberMainLightNode(engine),
                        onFrame = {
                            if (isModelLoading) {
                                isModelLoading = false
                            }
                        },
                        cameraManipulator = rememberCameraManipulator(
                            targetPosition = centerNode.worldPosition
                        ),
                        childNodes = listOf(
                            centerNode,
                            modelNode
                        )
                    )
                } else {
                    // Empty Box to avoid Scene dispose visual bugs
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(0.5f)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = White900,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = product.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Gray400
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    text = LoremIpsum(80).values.first().replace("\n", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray400
                )
            }
        }

        IconButton(
            onClick = { handleBackNavigation() },
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = White900
            )
        }
    }
}