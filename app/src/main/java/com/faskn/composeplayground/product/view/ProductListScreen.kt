package com.faskn.composeplayground.product.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.faskn.composeplayground.navigation.Screen
import com.faskn.composeplayground.ui.theme.*
import com.google.android.filament.Engine
import io.github.sceneview.Scene
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.*
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader

data class Product(
    val id: Int,
    val title: String,
    val subtitle: String,
    val modelName: String
)

val productList = listOf(
    Product(
        1,
        "Xbox Wireless Controller\nWhite Edition",
        "Hybrid D-pad | Button mapping | Bluetooth® technology",
        "xbox_white.glb"
    ),
    Product(
        2,
        "Xbox Wireless Controller\nBlack Edition",
        "Hybrid D-pad | Button mapping | Bluetooth® technology",
        "xbox_black.glb"
    )
)

@Composable
fun ProductListScreen(
    padding: PaddingValues,
    navController: NavController
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    var isNavigating by remember { mutableStateOf(false) }

    // Handle navigation with cleanup
    fun handleNavigation(selectedProduct: Product) {
        isNavigating = true
        navController.navigate(Screen.ProductDetail.createRoute(selectedProduct.id))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black900)
            .padding(padding)
    ) {
        Text(
            text = "Select a Product",
            style = MaterialTheme.typography.headlineSmall,
            color = White900,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(productList) { product ->
                ProductCard(
                    product = product,
                    engine = engine,
                    modelLoader = modelLoader,
                    isNavigating = isNavigating,
                    onProductClick = ::handleNavigation
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    engine: Engine,
    modelLoader: ModelLoader,
    isNavigating: Boolean,
    onProductClick: (Product) -> Unit
) {
    val cardCenterNode = rememberNode(engine)
    val cardCameraNode = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 0.25f, z = 0.75f)
        lookAt(Position(0f, 0f, 0f))
    }
    var isSceneReady by remember { mutableStateOf(false) }
    var modelNode by remember { mutableStateOf<ModelNode?>(null) }

    LaunchedEffect(product.modelName) {
        if (!isNavigating) {
            try {
                val newModelNode = ModelNode(
                    modelInstance = modelLoader.createModelInstance(
                        assetFileLocation = "models/${product.modelName}"
                    ),
                    scaleToUnits = 0.35f,
                    centerOrigin = Position(0f, 0f, 0f)
                ).apply {
                    cardCenterNode.addChildNode(this)
                }
                modelNode = newModelNode
            } catch (e: Exception) {
                Log.e("ProductCard", "Error creating model: ${e.message}")
            }
        }
    }

    // Clean up when navigation starts
    LaunchedEffect(isNavigating) {
        if (isNavigating) {
            try {
                modelNode?.model?.popRenderable()
                modelNode?.let { cardCenterNode.removeChildNode(it) }
                modelNode = null
            } catch (e: Exception) {
                Log.e("ProductCard", "Error cleaning up scene: ${e.message}")
            }
        }
    }

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(280.dp)
            .clickable {
                if (!isNavigating && isSceneReady) {
                    onProductClick(product)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = Black700
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box {
                if (!isNavigating) {
                    Scene(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Black800),
                        engine = engine,
                        modelLoader = modelLoader,
                        isOpaque = false,
                        onTouchEvent = { _, _ -> true },
                        cameraNode = cardCameraNode,
                        onFrame = {
                            if (!isSceneReady) {
                                isSceneReady = true
                            }
                        },
                        childNodes = listOf(cardCenterNode)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(Black600),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = White900,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = product.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = White900,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                maxLines = 2
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = product.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Gray400,
                modifier = Modifier.padding(horizontal = 12.dp),
                maxLines = 2
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}
