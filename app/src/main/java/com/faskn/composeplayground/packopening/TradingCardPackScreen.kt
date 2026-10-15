package com.faskn.composeplayground.packopening

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.ui.theme.Pink100

private val Stage = Color(0xFF050506)

@Preview
@Composable
fun TradingCardPackScreenPreview() {
    TradingCardPackScreen()
}

@Composable
fun TradingCardPackScreen(
    padding: PaddingValues = PaddingValues(0.dp)
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val packState = rememberBoosterPackState()
        val rarity = packState.rarity

        val timeSec by produceState(0f) {
            val startTime = System.nanoTime()
            while (true) {
                withFrameNanos { frameTime ->
                    value = (frameTime - startTime) / 1_000_000_000f
                }
            }
        }

        val smokeShader = remember {
            try {
                RuntimeShader(SCENE_SMOKE_AGSL)
            } catch (_: Exception) {
                null
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Stage)
                .padding(padding)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Scene ambient smoke & glow radiating behind the card in rarity.color when revealed/opening
                if (packState.torn) {
                    val baseGlowAlpha = if (packState.revealed) 1f else (packState.packDrag * 1.2f).coerceIn(0f, 1f)
                    val glowAlpha = baseGlowAlpha * packState.smokeFade

                    Canvas(Modifier.fillMaxSize()) {
                        if (smokeShader != null) {
                            drawSceneSmokeShader(
                                shader = smokeShader,
                                rarity = rarity,
                                alpha = glowAlpha,
                                timeSec = timeSec,
                            )
                        } else {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        rarity.color.copy(alpha = 0.35f * glowAlpha),
                                        rarity.color.copy(alpha = 0.12f * glowAlpha),
                                        Color.Transparent
                                    ),
                                    center = center,
                                    radius = size.width * 0.85f
                                ),
                                radius = size.width * 0.85f,
                                center = center
                            )
                        }
                    }

                    CollectibleCardView(packState)
                }

                BoosterPackView(
                    state = packState,
                    modifier = Modifier.size(
                        PackConstants.PACK_WIDTH_DP.dp,
                        PackConstants.PACK_HEIGHT_DP.dp
                    )
                ) {
                    BoosterPackContent(state = packState)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        packState.revealed -> "DOUBLE TAP TO FLIP"
                        packState.torn -> "SWIPE PACK DOWN TO REVEAL"
                        else -> "PULL THE CORNER TO OPEN"
                    },
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (packState.revealed) rarity.color else Color.White.copy(alpha = 0.4f),
                    letterSpacing = 4.sp
                )
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { if (packState.torn) packState.openAnother() else packState.reset() },
                    enabled = (!packState.torn || packState.revealed) && !packState.isTransitioning,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (packState.torn) rarity.color else Pink100,
                        disabledContentColor = Color.White.copy(alpha = 0.15f)
                    )
                ) {
                    Text(
                        text = if (packState.torn) "OPEN ANOTHER" else "RESET",
                        fontSize = 10.sp,
                        letterSpacing = 3.sp
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Stage)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Trading Card Pack Opening simulation requires\nAndroid 13 and above.",
                textAlign = TextAlign.Center,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
