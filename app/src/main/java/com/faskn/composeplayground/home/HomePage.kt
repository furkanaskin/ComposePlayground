package com.faskn.composeplayground.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.faskn.composeplayground.ui.theme.*

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