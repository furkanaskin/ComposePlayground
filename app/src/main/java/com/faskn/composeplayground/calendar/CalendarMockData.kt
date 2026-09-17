package com.faskn.composeplayground.calendar

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal

fun getMockCalendarEvents() = listOf(
    CalendarEvent(
        "1",
        "Slack Triage & Coffee",
        "09:00 AM - Morning routine",
        9,
        10,
        1,
        EventColors[0],
        Icons.Default.Coffee
    ),
    CalendarEvent(
        "2",
        "Fintech Sync (Wallet API)",
        "10:00 AM - Ledger Integration",
        10,
        11,
        1,
        EventColors[3],
        Icons.Default.AccountBalanceWallet
    ),
    CalendarEvent(
        "3",
        "Android Daily Standup",
        "11:00 AM - Google Meet",
        11,
        12,
        1,
        EventColors[4],
        Icons.Default.Groups
    ),
    CalendarEvent(
        "4",
        "Product Align: H2 Goals",
        "11:00 AM - Roadmap Sync",
        11,
                12,
        3,
        EventColors[2],
        Icons.Default.Sync
    ),
    CalendarEvent(
        "5",
        "CR: Core Payment Module",
        "01:00 PM - Reviewing PR #442",
        13,
        14,
        1,
        EventColors[3],
        Icons.Default.Code
    ),
    CalendarEvent(
        "6",
        "Deep Work: Compose Perf",
        "01:00 PM - Profiling & Optimization",
        13,
        15,
        2,
        EventColors[6],
        Icons.Default.Terminal
    ),
    CalendarEvent(
        "7",
        "Bug Scrub: Checkout Flow",
        "09:00 AM - P1 Fixes only",
        9,
        11,
        2,
        EventColors[7],
        Icons.Default.BugReport
    ),
    CalendarEvent(
        "8",
        "Design Handover (New UI)",
        "01:00 PM - Figma to Compose",
        13,
        14,
        4,
        EventColors[4],
        Icons.Default.Palette
    ),
    CalendarEvent(
        "9",
        "1:1 with Engineering Lead",
        "02:00 PM - Career Growth",
        14,
        15,
        5,
        EventColors[0],
        Icons.Default.ChatBubble
    ),
    CalendarEvent(
        "10",
        "Security Layer Audit",
        "01:00 PM - Biometric Auth",
        13,
        14,
        4,
        EventColors[7],
        Icons.Default.Security
    ),
    CalendarEvent(
        "11",
        "Backend Architecture Review",
        "11:00 AM - Service Mesh Discussion",
        11,
        12,
        0,
        EventColors[0],
        Icons.Default.Code
    ),
    CalendarEvent(
        "12",
        "UI/UX Design Handover",
        "11:00 AM - Figma Component Library",
        11,
        12,
        0,
        EventColors[1],
        Icons.Default.Palette
    ),
    CalendarEvent(
        "13",
        "Android Memory Leaks Triage",
        "11:00 AM - LeakCanary Log Audit",
        11,
        12,
        0,
        EventColors[6],
        Icons.Default.BugReport
    ),
    CalendarEvent(
        "14",
        "Weekend Dev Marathon",
        "11:00 AM - Open Source Contributions",
        11,
        14,
        6,
        EventColors[2],
        Icons.Default.Terminal
    ),
    CalendarEvent(
        "15",
        "Coffee Tasting & Chat",
        "11:00 AM - Barista Workshop Sync",
        11,
        12,
        6,
        EventColors[5],
        Icons.Default.Coffee
    ),
    CalendarEvent(
        "16",
        "Algorithm Practice",
        "11:00 AM - LeetCode Weekly Contest",
        11,
        13,
        6,
        EventColors[4],
        Icons.Default.Groups
    )
)
