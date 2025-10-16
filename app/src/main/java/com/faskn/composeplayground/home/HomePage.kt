package com.faskn.composeplayground.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faskn.composeplayground.R
import com.faskn.composeplayground.navigation.Screen
import com.faskn.composeplayground.ui.theme.Black500
import com.faskn.composeplayground.ui.theme.Black700
import com.faskn.composeplayground.ui.theme.Black900
import com.faskn.composeplayground.ui.theme.Gray400
import com.faskn.composeplayground.ui.theme.White900

data class Tutorial(
    val title: String,
    val description: String,
    val screen: Screen
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    onTutorialClick: (Screen) -> Unit,
    padding: PaddingValues
) {
    val tutorials = listOf(
        Tutorial(
            title = "Credit Card Selection Animation",
            description = "Interactive credit card selection with drag gestures.",
            screen = Screen.CreditCard
        ),
        Tutorial(
            title = "3D Product Viewer (Experimental)",
            description = "Product list and detail screens with 3D models and camera animations.",
            screen = Screen.ProductList
        ), Tutorial(
            title = "AGSL Sample - Spirograph",
            description = "Drawing 2 rotating Axes like Spirograph using Android Graphics Shading Language (AGSL).",
            screen = Screen.AGSLSample
        ), Tutorial(
            title = "Circular Carousel",
            description = "Circular carousel using LazyRow and graphicsLayer transformations.",
            screen = Screen.CircularCarousel
        ), Tutorial(
            title = "Shadows",
            description = "Various inner and drop shadow effects using Jetpack Compose.",
            screen = Screen.Shadows
        ), Tutorial(
            title = "Side Panel Layout",
            description = "An animated custom side panel layout with drag handle in Jetpack Compose.",
            screen = Screen.SidePanel
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black900)
            .padding(padding)
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = White900
            )

            Image(
                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                contentDescription = stringResource(id = R.string.app_name),
                modifier = Modifier.height(48.dp),
            )
        }

        LazyColumn {
            items(tutorials) { tutorial ->
                Card(
                    onClick = { onTutorialClick(tutorial.screen) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Black900.copy(alpha = 0.50f),
                        contentColor = Color.Transparent
                    ),
                    border = BorderStroke(
                        2.dp, Brush.linearGradient(colors = listOf(Black500, Black700))
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = tutorial.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = White900
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tutorial.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Gray400
                        )
                    }
                }
            }
        }
    }
}