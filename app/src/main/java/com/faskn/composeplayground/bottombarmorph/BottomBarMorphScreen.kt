package com.faskn.composeplayground.bottombarmorph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun BottomBarMorphScreen(padding: PaddingValues) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = remember {
        listOf(
            BottomTab(Icons.Rounded.Home, "Home"),
            BottomTab(Icons.Rounded.SyncAlt, "Transfers"),
            BottomTab(Icons.Rounded.QrCode, "QR Payment"),
            BottomTab(Icons.Rounded.Person, "Profile"),
        )
    }

    val recentContacts = remember {
        listOf(
            QuickContact("1", "Aron Finch", "AF", BrandOrange),
            QuickContact("2", "Krystal Smith", "KS", Color(0xFFFF4081)),
            QuickContact("3", "George Clark", "GC", Color(0xFF3DD6A3)),
            QuickContact("4", "Maisy Williams", "MW", Color(0xFFAB47BC)),
            QuickContact("5", "John Doe", "JD", Color(0xFFFFB74D)),
        )
    }

    val transactions = remember {
        listOf(
            TransactionItem(
                "Salary",
                "10:00",
                "+ $45,000.00",
                true,
                Icons.Rounded.ArrowDownward,
                Color(0xFF1B382B)
            ),
            TransactionItem(
                "Lunch",
                "12:30",
                "- $240.00",
                false,
                Icons.Rounded.Restaurant,
                Color(0xFF2B221B)
            ),
            TransactionItem(
                "Coffee",
                "14:15",
                "- $85.00",
                false,
                Icons.Rounded.LocalCafe,
                Color(0xFF2B251B)
            ),
            TransactionItem(
                "Transport",
                "17:45",
                "- $65.50",
                false,
                Icons.Rounded.DirectionsCar,
                Color(0xFF1B252B)
            ),
            TransactionItem(
                "Groceries",
                "19:30",
                "- $820.00",
                false,
                Icons.Rounded.ShoppingCart,
                Color(0xFF281B2B)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(padding),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 16.dp,
                bottom = 90.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WalletHeader()
            }

            item {
                MainVisaWalletCard()
            }

            item {
                WalletActionButtonsRow()
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Last Transactions",
                        color = BrandWhite,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "View all",
                        color = BrandMutedText,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.clickable { },
                    )
                }
            }

            items(transactions.size) { index ->
                TransactionCard(transaction = transactions[index])
                if (index < transactions.lastIndex) {
                    HorizontalDivider(
                        color = TransactionCardBorder,
                        thickness = 1.dp
                    )
                }
            }
        }

        WalletBottomBar(
            tabs = tabs,
            selectedTabIndex = selectedTab,
            onTabSelected = { selectedTab = it },
            expandedContent = { onClose ->
                QuickContactList(
                    recentContacts = recentContacts,
                    onContactSelected = { _ ->
                        onClose()
                    },
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomBarMorphScreenPreview() {
    MaterialTheme {
        BottomBarMorphScreen(PaddingValues.Zero)
    }
}
