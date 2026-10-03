package com.faskn.composeplayground.bottombarmorph

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class BottomTab(
    val icon: ImageVector,
    val label: String,
)

@Immutable
data class QuickContact(
    val id: String,
    val name: String,
    val initials: String,
    val accentColor: Color,
)

@Immutable
data class TransactionItem(
    val title: String,
    val time: String,
    val amount: String,
    val isPositive: Boolean,
    val icon: ImageVector,
    val iconBg: Color,
)
