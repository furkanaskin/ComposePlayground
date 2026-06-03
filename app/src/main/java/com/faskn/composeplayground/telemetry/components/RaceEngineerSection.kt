package com.faskn.composeplayground.telemetry.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutQuint
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.telemetry.RacingColors
import com.faskn.composeplayground.telemetry.data.ChatMessage
import com.faskn.composeplayground.telemetry.data.ReplayMarker
import com.faskn.composeplayground.telemetry.data.Role
import com.faskn.composeplayground.telemetry.racingColors
import com.faskn.composeplayground.ui.theme.Black950
import com.faskn.composeplayground.ui.theme.ComposePlaygroundTheme

@Composable
fun RaceEngineerHeader(
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    val colors = racingColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(24.dp).padding(end = 4.dp),
                imageVector = Icons.Default.HeadsetMic,
                contentDescription = null,
                tint = colors.raceEngineerSoft
            )
            Text(
                "Race Engineer",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                ),
                color = colors.raceEngineerSoft
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
            contentDescription = null,
            tint = colors.neutral
        )
    }
}

@Composable
fun RaceEngineerSection(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onSendMessage: (String) -> Unit,
    onMarkerClick: (ReplayMarker) -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    val colors = racingColors

    val submit = {
        if (inputText.isNotBlank() && !isLoading) {
            onSendMessage(inputText)
            inputText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Black950)
            .clip(RoundedCornerShape(16.dp))
    ) {
        RaceEngineerHeader(isExpanded = isExpanded, onToggleExpanded = onToggleExpanded)

        if (isExpanded) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                val listState = rememberLazyListState()
                LaunchedEffect(messages.size, isLoading) {
                    if (messages.isNotEmpty()) {
                        listState.animateScrollToItem(messages.size)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message, onMarkerClick)
                    }
                    if (isLoading) {
                        item {
                            EngineerLoadingIndicator()
                        }
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                HorizontalDivider(color = colors.divider)

                // Input Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                "Chat with engineer...",
                                color = colors.muted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = colors.raceEngineerSoft
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() })
                    )

                    IconButton(
                        onClick = { submit() },
                        enabled = inputText.isNotBlank() && !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (inputText.isNotBlank() && !isLoading) colors.raceEngineerSoft else colors.muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, onMarkerClick: (ReplayMarker) -> Unit = {}) {
    val colors = racingColors
    val isUser = message.role == Role.USER

    val annotatedText = buildAnnotatedString {
        if (!isUser) {
            appendMarkerAnnotations(message.text, message.markers, colors)
        } else {
            append(message.text)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isUser) 12.dp else 0.dp,
                        bottomEnd = if (isUser) 0.dp else 12.dp
                    )
                )
                .background(
                    if (isUser) colors.raceEngineerDark.copy(alpha = 0.15f)
                    else colors.divider.copy(alpha = 0.5f)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
            Text(
                modifier = Modifier
                    .animateContentSize()
                    .pointerInput(annotatedText) {
                        detectTapGestures { offset ->
                            layoutResult?.let { result ->
                                val position = result.getOffsetForPosition(offset)
                                annotatedText
                                    .getStringAnnotations(
                                        tag = "marker",
                                        start = position,
                                        end = position
                                    )
                                    .firstOrNull()
                                    ?.let { annotation ->
                                        val parts = annotation.item.split(",")
                                        if (parts.size == 3) {
                                            onMarkerClick(
                                                ReplayMarker(
                                                    parts[0].toInt(),
                                                    parts[1].toInt(),
                                                    parts[2].toInt()
                                                )
                                            )
                                        }
                                    }
                            }
                        }
                    },
                text = annotatedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    color = if (isUser) colors.raceEngineerSoft else Color.White
                ),
                onTextLayout = { layoutResult = it }
            )
        }
    }
}

private fun AnnotatedString.Builder.appendMarkerAnnotations(
    text: String,
    markers: List<ReplayMarker>,
    colors: RacingColors
) {
    val markerSpanStyle = SpanStyle(
        color = colors.raceEngineerSoft,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline
    )

    append(text)

    markers.forEach { marker ->
        val targets = listOf("${marker.dist}m", "${marker.dist} m")
        for (target in targets) {
            var startIndex = text.indexOf(target, ignoreCase = true)
            while (startIndex != -1) {
                val endIndex = startIndex + target.length
                addStringAnnotation(
                    tag = "marker",
                    annotation = "${marker.lap},${marker.dist},${marker.frame}",
                    start = startIndex,
                    end = endIndex
                )
                addStyle(
                    style = markerSpanStyle,
                    start = startIndex,
                    end = endIndex
                )
                startIndex = text.indexOf(target, startIndex + 1, ignoreCase = true)
            }
        }
    }
}

@Composable
private fun EngineerLoadingIndicator() {
    val colors = racingColors
    val infiniteAnimation = rememberInfiniteTransition(label = "rotation")
    val rotation = infiniteAnimation.animateFloat(
        initialValue = 0f,
        targetValue = 720f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseOutQuint),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    ).value

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomEnd = 12.dp))
            .background(colors.divider.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.gemini_24dp),
            contentDescription = null,
            modifier = Modifier
                .size(20.dp)
                .rotate(rotation)
        )
    }
}

@Preview
@Composable
private fun RaceEngineerHeaderPreview() {
    ComposePlaygroundTheme {
        Box(
            modifier = Modifier
                .background(Black950)
                .padding(16.dp)
        ) {
            RaceEngineerHeader(
                isExpanded = false
            ) { }
        }
    }
}

@Preview
@Composable
private fun ChatBubbleUserPreview() {
    ComposePlaygroundTheme {
        Box(
            modifier = Modifier
                .background(Black950)
                .padding(16.dp)
        ) {
            ChatBubble(
                message = ChatMessage(
                    role = Role.USER,
                    text = "How's my pace compared to the leader?"
                )
            )
        }
    }
}

@Preview
@Composable
private fun ChatBubbleEngineerPreview() {
    ComposePlaygroundTheme {
        Box(
            modifier = Modifier
                .background(Black950)
                .padding(16.dp)
        ) {
            ChatBubble(
                message = ChatMessage(
                    role = Role.ENGINEER,
                    text = "You're doing well. Your pace is consistent, but you're losing about 0.2s in Turn 4(1200m). Check the replay to see your line.",
                    markers = listOf(
                        ReplayMarker(lap = 5, dist = 1200, frame = 5000)
                    )
                )
            )
        }
    }
}
