package com.faskn.composeplayground.sidepanel

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.faskn.composeplayground.R
import com.faskn.composeplayground.ui.theme.AndroidGreen
import com.faskn.composeplayground.ui.theme.TechBlack
import com.faskn.composeplayground.ui.theme.TransparentWhite600
import com.faskn.composeplayground.ui.theme.White800
import com.faskn.composeplayground.ui.theme.White900
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch

@Composable
fun SidePanelScreen() {
    val sidePanelState = rememberSidePanelState(initialState = SidePanelState.Collapsed)
    var productImageHeightDp by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val containerColor = Color(0xFFF2F6FF)
    var selectedProductIndex by remember { mutableIntStateOf(0) }
    val selectedProduct = productList[selectedProductIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(TechBlack, Color(0xFF1A1A1A)),
                        startY = size.height,
                        endY = 0f
                    )
                )
            },
    ) {
        // Animated Image with crossfade
        AnimatedContent(
            targetState = selectedProduct.imageRes,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith
                        fadeOut(animationSpec = tween(600)) using
                        SizeTransform { _, _ ->
                            tween(600, easing = FastOutSlowInEasing)
                        }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp)
                .zIndex(2f),
            label = "image_animation"
        ) { imageRes ->
            AsyncImage(
                model = imageRes,
                contentDescription = selectedProduct.model,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1536f / 1024f)
                    .onGloballyPositioned { coordinates ->
                        val positionY = coordinates.positionInRoot().y
                        val height = coordinates.size.height.toFloat()
                        val bottomPx = positionY + height
                        val bottomDp = with(density) { bottomPx.toDp() }
                        productImageHeightDp = bottomDp
                    }
            )
        }

        Column(
            modifier = Modifier
                .padding(top = productImageHeightDp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.weight(0.2f))

            Text(
                text = "FLYINGSHOES",
                color = White800,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 42.dp),
                text = "Pegasus ${selectedProduct.model}",
                color = TransparentWhite600,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.weight(0.2f))

            Box(
                modifier = Modifier
                    .size(58.dp, 4.dp)
                    .background(selectedProduct.colors.first(), RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.weight(0.2f))

            Text(
                modifier = Modifier
                    .padding(horizontal = 32.dp),
                text = selectedProduct.description,
                color = Color(0xFFDDDDDD),
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.weight(0.3f))

            Text(
                text = "STARTING FROM",
                color = White900.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraLight,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-2).sp,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.weight(0.1f))

            Text(
                text = "$140.97",
                color = White900,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = (-2).sp,
                style = MaterialTheme.typography.displaySmall
            )

            Spacer(modifier = Modifier.weight(0.5f))
        }

        SidePanelLayout(
            state = sidePanelState,
            arrangement = SidePanelArrangement.End,
            containerColor = containerColor,
            dragHandleContent = { isExpanded, progress ->
                CustomDragHandle(
                    sidePanelState = sidePanelState,
                    containerColor = containerColor,
                    productColor = selectedProduct.colors.first()
                )
            }
        ) { progress ->
            SheetContent(
                progress = progress,
                productImageHeightDp = productImageHeightDp,
                selectedProductIndex = selectedProductIndex,
                onProductSelected = { selectedProductIndex = it }
            )
        }
    }
}

@Composable
fun CustomDragHandle(
    sidePanelState: SidePanelStateHolder,
    containerColor: Color,
    productColor: Color
) {
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(
        targetValue = if (sidePanelState.currentState == SidePanelState.Expanded) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "handle_scale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                drawCircle(
                    productColor,
                    radius = size.minDimension / 1.75f,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Fill
                )

                drawCircle(
                    containerColor,
                    radius = size.minDimension / 2f,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Fill
                )

                drawCircle(
                    containerColor,
                    radius = size.minDimension / 4,
                    center = Offset(size.width / 2, size.height / 2),
                    style = Fill
                )
            }
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                scope.launch { sidePanelState.toggle() }
            }
    ) {

        Icon(
            imageVector = Icons.Default.AddShoppingCart,
            contentDescription = "",
            tint = productColor,
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp)
                .align(Alignment.Center)
        )
    }
}

@Composable
fun SheetContent(
    progress: Float,
    productImageHeightDp: Dp,
    selectedProductIndex: Int,
    onProductSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .padding(top = productImageHeightDp)
            .padding(24.dp)
    ) {
        AnimatedVisibility(
            progress in 0.40f..1f,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut(tween(150))
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "FLYINGSHOES",
                    color = TechBlack,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Choose Your Style",
                    color = TechBlack,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    productList.forEachIndexed { index, product ->
                        val isSelected = index == selectedProductIndex
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "circle_scale_$index"
                        )

                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .clip(CircleShape)
                                .background(
                                    brush = if (product.colors.size > 1)
                                        Brush.linearGradient(
                                            colors = product.colors,
                                            start = Offset.Zero,
                                            end = Offset.Infinite
                                        )
                                    else Brush.linearGradient(
                                        colors = listOf(
                                            product.colors.first(),
                                            product.colors.first()
                                        )
                                    )
                                )
                                .border(
                                    width = 4.dp,
                                    color = if (isSelected) AndroidGreen else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onProductSelected(index)
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                // Animated Product Model in Sheet
                AnimatedContent(
                    targetState = productList[selectedProductIndex].model,
                    transitionSpec = {
                        slideInHorizontally() { it / 2 } + fadeIn() togetherWith
                                slideOutHorizontally { -it / 2 } + fadeOut()
                    },
                    label = "sheet_model_animation"
                ) { model ->
                    Text(
                        text = model,
                        color = TechBlack,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                OutlinedButton(
                    onClick = { /* TODO: Handle click */ },
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(2.dp, TechBlack),
                ) {
                    Text(
                        text = "Order Now",
                        color = TechBlack,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

data class Product(
    val id: Int,
    val imageRes: Int,
    val model: String,
    val colors: ImmutableList<Color>,
    val description: String
)

val productList = listOf(
    Product(
        id = 0,
        imageRes = R.drawable.ai_gen_running_sneaker1,
        model = "UltraStep Aurora Blaze",
        colors = persistentListOf(
            Color(0xFFE33B08), // Aurora Orange
            Color(0xFFF5AD08), // Solar Yellow
            Color(0xFF54C697)  // Mint Green
        ),
        description = "Designed for energetic and innovative runners. Featuring an advanced mesh upper and AuroraCushion™ midsole for maximum breathability and comfort. Whether on the track or in the city, every step combines performance and style."
    ),
    Product(
        id = 1,
        imageRes = R.drawable.ai_gen_running_sneaker2,
        model = "UltraStep Crimson Pulse",
        colors = persistentListOf(Color(0xFF910E13)), // Crimson Red
        description = "Stands out with powerful red tones. Its lightweight structure and flexible sole reduce fatigue even during long runs. Perfect for those who want a bold and dynamic look."
    ),
    Product(
        id = 2,
        imageRes = R.drawable.ai_gen_running_sneaker3,
        model = "UltraStep Spectrum Runner",
        colors = persistentListOf(
            Color(0xFF863CC7), // Violet
            Color(0xFFC21326), // Ruby
            Color(0xFF0EB2E5), // Sky Blue
            Color(0xFFDC5509)  // Tangerine
        ),
        description = "Stands out with its colorful and energetic design. Multi-color transitions and a durable outsole provide maximum grip both in the city and outdoors. Ideal for those who love to be different."
    ),
    Product(
        id = 3,
        imageRes = R.drawable.ai_gen_running_sneaker4,
        model = "UltraStep Phantom Black",
        colors = persistentListOf(Color(0xFF0D0D0D)), // Phantom Black
        description = "Timeless elegance of black meets modern technology. With cushioning and support features, it offers top-level comfort for daily use and training."
    )
)