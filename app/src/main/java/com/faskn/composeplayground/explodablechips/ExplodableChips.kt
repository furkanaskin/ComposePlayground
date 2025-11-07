package com.faskn.composeplayground.explodablechips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.Black700
import com.faskn.composeplayground.ui.theme.Gray500
import com.faskn.composeplayground.ui.theme.Gray600
import com.faskn.composeplayground.ui.theme.Green300
import com.faskn.composeplayground.ui.theme.TechBlack
import com.faskn.composeplayground.ui.theme.White900
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Constants
private val chipColors = persistentListOf(
    Color(0xFF547CE0), Color(0xFFA077EB), Color(0xFFFF6AD5), Color(0xFF547CE0),
    Color(0xFFFFA057), Color(0xFF4EE1A0), Color(0xFFFF5E5E), Color(0xFF8C6FF7),
    Color(0xFF42C6FF), Color(0xFF9CE67F), Color(0xFFFFD54F), Color(0xFF00D2FF)
)

private val allInterests = persistentListOf(
    "Kotlin", "Jetpack Compose", "Open Source", "Android", "UI/UX",
    "Mobile Dev", "Cloud", "AI/ML", "DevOps", "Game Dev",
    "Cybersecurity", "Web Dev", "Data Science", "Blockchain", "AR/VR",
    "Machine Learning", "Backend", "Frontend", "iOS Dev",
    "Flutter", "Compose MP", "Kotlin MP", "Docker", "Kubernetes", "GraphQL", "REST APIs",
    "Microservices", "Testing", "CI/CD", "Agile", "Scrum", "Product Management"
)

private const val PARTICLE_MIN_SPEED = 100f
private const val PARTICLE_MAX_SPEED = 1000f
private const val PARTICLE_SPREAD_RANGE = 100f
private const val PARTICLE_GRAVITY = 500f
private const val PARTICLE_LIFE_DECAY = 2f
private const val FRAME_TIME = 0.016f // 60 FPS

private const val SWIPE_VELOCITY_THRESHOLD = 3000f
private const val SWIPE_DISTANCE_THRESHOLD = 300f
private const val ROTATION_ANGLE_SELECTED = 5f

data class Particle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    val color: Color,
    var alpha: Float = 1f,
    var life: Float = 1f,
    var size: Float = 5f
)

private fun updateParticle(particle: Particle) {
    particle.velocityY += PARTICLE_GRAVITY * FRAME_TIME
    particle.x += particle.velocityX * FRAME_TIME
    particle.y += particle.velocityY * FRAME_TIME
    particle.life -= FRAME_TIME * PARTICLE_LIFE_DECAY
    particle.alpha = particle.life.coerceIn(0f, 1f)
}

private fun createParticles(
    centerPosition: Offset,
    particles: MutableList<Particle>,
    color: Color = chipColors.random()
) {
    repeat(40) {
        val angle = Random.nextFloat() * Math.PI.toFloat() * 2
        val speed =
            Random.nextFloat() * (PARTICLE_MAX_SPEED - PARTICLE_MIN_SPEED) + PARTICLE_MIN_SPEED
        val spreadX = (Random.nextFloat() - 0.5f) * PARTICLE_SPREAD_RANGE
        val spreadY = (Random.nextFloat() - 0.5f) * PARTICLE_SPREAD_RANGE

        particles.add(
            Particle(
                x = centerPosition.x + spreadX,
                y = centerPosition.y + spreadY,
                velocityX = cos(angle) * speed,
                velocityY = sin(angle) * speed,
                color = color
            )
        )
    }
}

@Composable
fun ExplodableChipsContainer(
    modifier: Modifier = Modifier,
    items: List<String>,
    selectedItems: Set<String> = emptySet(),
    onItemSelected: (String) -> Unit = {},
    onItemDismissed: (String, Int, Offset) -> Unit = { _, _, _ -> },
) {
    val rows = remember(items) { items.chunked(2) }
    val particles = remember { mutableStateListOf<Particle>() }
    var frameCount by remember { mutableIntStateOf(0) }
    var containerPosition by remember { mutableStateOf(Offset.Zero) }

    // Update particles
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                particles.removeAll { it.life <= 0f }
                particles.forEach { updateParticle(it) }
                particles.all { it.life <= 0f }.let { allDead ->
                    if (!allDead) {
                        frameCount++
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                containerPosition = coordinates.positionInRoot()
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
            reverseLayout = true,
            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            itemsIndexed(rows) { rowIndex, rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEachIndexed { itemIndex, item ->
                        val isSelected = selectedItems.contains(item)
                        val weight = if (rowItems.size == 2) {
                            item.length.toFloat() / rowItems.sumOf { it.length }
                        } else 1f

                        key(item) {
                            ChipItem(
                                modifier = Modifier.weight(weight),
                                item = item,
                                isSelected = isSelected,
                                rowIndex = rowIndex,
                                itemIndex = itemIndex,
                                onItemSelected = onItemSelected
                            ) { chipPosition ->
                                val globalIndex = rowIndex * 2 + itemIndex
                                val relativePosition = Offset(
                                    x = chipPosition.x - containerPosition.x,
                                    y = chipPosition.y - containerPosition.y
                                )
                                createParticles(relativePosition, particles)
                                onItemDismissed(item, globalIndex, chipPosition)
                            }
                        }
                    }
                }
            }
        }

        // Particle canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (frameCount >= 0) {
                particles.forEach { particle ->
                    drawCircle(
                        color = particle.color.copy(alpha = particle.alpha),
                        radius = particle.size,
                        center = Offset(particle.x, particle.y)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipItem(
    modifier: Modifier,
    item: String,
    isSelected: Boolean,
    rowIndex: Int,
    itemIndex: Int,
    onItemSelected: (String) -> Unit,
    onItemDismissed: (Offset) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val rotation = remember { Animatable(0f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(300f) }
    val alpha = remember { Animatable(0f) }

    val chipColor = remember(item) { chipColors[item.hashCode().mod(chipColors.size)] }
    var chipCenterPosition by remember { mutableStateOf(Offset.Zero) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }

    // Initial pop-up animation
    LaunchedEffect(Unit) {
        val delay = rowIndex * 150L + itemIndex * 75L
        launch {
            delay(delay)
            offsetY.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        launch {
            delay(delay)
            alpha.animateTo(1f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessMedium))
        }
    }

    val rotationAngle = remember(isSelected, itemIndex) {
        if (isSelected) {
            if (itemIndex % 2 == 0) -ROTATION_ANGLE_SELECTED else ROTATION_ANGLE_SELECTED
        } else 0f
    }

    LaunchedEffect(rotationAngle) {
        rotation.animateTo(
            rotationAngle,
            spring(Spring.DampingRatioHighBouncy, Spring.StiffnessMedium)
        )
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val positionInRoot = coordinates.positionInRoot()
                val size = coordinates.size
                chipCenterPosition = Offset(
                    x = positionInRoot.x + size.width / 2f,
                    y = positionInRoot.y + size.height / 2f
                )
            }
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .alpha(alpha.value)
            .rotate(rotation.value)
            .pointerInput(isSelected) {
                if (!isSelected) {
                    detectDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val shouldDismiss = abs(dragVelocity) > SWIPE_VELOCITY_THRESHOLD ||
                                        abs(offsetX.value) > SWIPE_DISTANCE_THRESHOLD

                                if (shouldDismiss) {
                                    onItemDismissed(chipCenterPosition)
                                } else {
                                    launch {
                                        offsetX.animateTo(
                                            0f,
                                            spring(Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                    launch {
                                        rotation.animateTo(
                                            rotationAngle,
                                            spring(Spring.DampingRatioMediumBouncy)
                                        )
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragVelocity = dragAmount.x
                            coroutineScope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                rotation.snapTo((offsetX.value / 10f).coerceIn(-30f, 30f))
                            }
                        }
                    )
                }
            }
    ) {
        DefaultChipItem(
            modifier = Modifier.fillMaxWidth(),
            label = item,
            isSelected = isSelected,
            chipColor = chipColor,
            onItemSelected = { onItemSelected(it) }
        )
    }
}

@Composable
private fun DefaultChipItem(
    modifier: Modifier = Modifier,
    label: String,
    isSelected: Boolean,
    chipColor: Color,
    onItemSelected: (String) -> Unit,
) {
    val backgroundColor = if (isSelected) chipColor else Gray600

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onItemSelected(label)
            }
            .background(
                backgroundColor,
                shape = RoundedCornerShape(50.dp)
            )
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = White900,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ExplodableChipsScreen() {
    var visibleItems by remember { mutableStateOf(allInterests.take(10)) }
    var remainingItems by remember { mutableStateOf(allInterests.drop(10)) }
    var selectedInterests by remember { mutableStateOf(setOf<String>()) }

    val minSelection = 3
    val maxSelection = 10
    val isButtonEnabled = selectedInterests.size in minSelection..maxSelection

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TechBlack)
            .safeDrawingPadding()
    ) {
        Text(
            "Choose your\ninterests",
            color = White900,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Normal),
            modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp)
        )

        Text(
            "Swipe fast to explode chips you don't like 💥",
            color = White900.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Light),
            modifier = Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        ExplodableChipsContainer(
            modifier = Modifier.weight(1f),
            items = visibleItems,
            selectedItems = selectedInterests,
            onItemSelected = { item ->
                selectedInterests = if (item in selectedInterests) {
                    selectedInterests - item
                } else {
                    selectedInterests + item
                }
            },
            onItemDismissed = { dismissedItem, index, _ ->
                selectedInterests -= dismissedItem

                if (remainingItems.isNotEmpty()) {
                    visibleItems = visibleItems.toMutableList().apply {
                        this[index] = remainingItems.first()
                    }
                    remainingItems = remainingItems.drop(1)
                } else {
                    visibleItems = visibleItems.filter { it != dismissedItem }
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .heightIn(30.dp)
                .padding(bottom = 8.dp)
        ) {
            AnimatedVisibility(
                visible = selectedInterests.isNotEmpty(),
            ) {
                Text(
                    text = "${selectedInterests.size}/$maxSelection Interests selected",
                    color = if (isButtonEnabled) Green300 else Green300.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        Button(
            onClick = { /* TODO */ },
            enabled = isButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = Black700
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp)
        ) {
            Text(
                text = "Get Started",
                color = if (isButtonEnabled) TechBlack else Gray500,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}