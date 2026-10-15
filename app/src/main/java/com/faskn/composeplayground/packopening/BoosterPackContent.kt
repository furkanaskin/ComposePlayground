package com.faskn.composeplayground.packopening

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faskn.composeplayground.telemetry.Exo2Family
import com.faskn.composeplayground.telemetry.MichromaFamily
import com.faskn.composeplayground.ui.theme.RacingWhite

/** Minimal black holographic booster pack content layout. */
@Composable
fun BoosterPackContent(
    modifier: Modifier = Modifier,
    state: BoosterPackState? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PackColors.PackBlack)
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(PackColors.PackBlack)

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(PackColors.HoloCyan.copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.22f, size.height * 0.47f),
                    radius = size.width * 0.68f,
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(PackColors.HoloPurple.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.76f, size.height * 0.53f),
                    radius = size.width * 0.62f,
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(PackColors.HoloPink.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.38f, size.height * 0.72f),
                    radius = size.width * 0.53f,
                )
            )
        }

        Column(Modifier.fillMaxSize()) {
            PackCrimpEffect(Modifier.fillMaxWidth().height(10.dp))

            Spacer(Modifier.height(17.dp))

            Text(
                text = "OPEN HERE",
                modifier = Modifier.padding(start = 12.dp),
                fontFamily = Exo2Family,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = PackColors.PackWhite.copy(alpha = 0.78f),
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(12.dp))

            PerforationLine(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .height(2.dp)
                    .then(if (state != null) Modifier.tearLine(state) else Modifier)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "CITY//SHIFT",
                            modifier = Modifier.offset(x = (-2.5).dp, y = (-1).dp),
                            fontFamily = MichromaFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PackColors.HoloPink.copy(alpha = 0.42f),
                            letterSpacing = (-1.1).sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = "CITY//SHIFT",
                            modifier = Modifier.offset(x = (-1).dp, y = (-1).dp),
                            fontFamily = MichromaFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PackColors.HoloCyan.copy(alpha = 0.38f),
                            letterSpacing = (-1.1).sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                        Text(
                            text = "CITY//SHIFT",
                            fontFamily = MichromaFamily,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PackColors.PackWhite,
                            letterSpacing = (-1.1).sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }

                    Spacer(Modifier.height(11.dp))

                    Text(
                        text = "REALITY IS COLLECTIBLE",
                        fontFamily = Exo2Family,
                        fontSize = 8.sp,
                        color = PackColors.PackWhite.copy(alpha = 0.72f),
                        letterSpacing = 2.0.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Text(
                text = "LIMITED EDITION",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 17.dp),
                textAlign = TextAlign.Center,
                fontFamily = Exo2Family,
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = PackColors.PackWhite.copy(alpha = 0.72f),
                letterSpacing = 0.6.sp,
            )

            PackCrimpEffect(Modifier.fillMaxWidth().height(10.dp), bottomEdge = true)
        }

        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.13f),
                style = Stroke(width = 0.8.dp.toPx()),
            )
        }
    }
}

@Composable
private fun PackCrimpEffect(
    modifier: Modifier = Modifier,
    bottomEdge: Boolean = false,
) {
    Canvas(modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF101216), PackColors.PackBlack, Color(0xFF0B0D10))
            )
        )

        val gap = size.width / 100f
        for (i in 1..100 step 1) {
            val x = gap * i
            drawLine(
                color = RacingWhite.copy(alpha = 0.25f),
                start = Offset(x, size.height * 0.18f),
                end = Offset(x, size.height * 0.82f),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val edgeY = if (bottomEdge) 0f else size.height
        drawLine(
            color = Color.White.copy(alpha = 0.13f),
            start = Offset(0f, edgeY),
            end = Offset(size.width, edgeY),
            strokeWidth = 0.7.dp.toPx(),
        )
    }
}

@Composable
private fun PerforationLine(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset.Zero.copy(y = size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 6.dp.toPx())
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun BoosterPackContentPreview() {
    Box(Modifier.size(width = 300.dp, height = 480.dp)) {
        BoosterPackContent()
    }
}
