package com.faskn.composeplayground.packopening

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.faskn.composeplayground.R
import kotlin.random.Random

/**
 * Collectible card rarity tiers governing drop rates, holographic glow intensity, and theme colors.
 */
enum class CardRarity(
    val label: String,
    val color: Color,
    val weight: Int,
    val glow: Float,
) {
    COMMON("COMMON", PackColors.RarityCommon, 40, 0.35f),
    RARE("RARE", PackColors.RarityRare, 30, 0.55f),
    EPIC("EPIC", PackColors.RarityEpic, 18, 0.75f),
    LEGENDARY("LEGENDARY", PackColors.RarityLegendary, 8, 0.90f),
    MYTHIC("MYTHIC", PackColors.RarityMythic, 4, 1.0f);

    companion object {
        fun roll(random: Random = Random): CardRarity {
            var pick = random.nextInt(entries.sumOf { it.weight })
            for (rarity in entries) {
                if (pick < rarity.weight) return rarity
                pick -= rarity.weight
            }
            return COMMON
        }
    }
}

/**
 * Trading card data model representing a collectible item with city artwork, country, description, and dynamically rolled rarity.
 */
data class TradingCard(
    val title: String,
    val subtitle: String,
    @DrawableRes val artworkRes: Int,
    val loreDescription: String,
    val rarity: CardRarity,
    val cardId: String,
) {
    data class CityTemplate(
        val title: String,
        val subtitle: String,
        @DrawableRes val artworkRes: Int,
        val loreDescription: String,
        val cardId: String,
    )

    companion object {
        private val CITY_TEMPLATES = listOf(
            CityTemplate(
                title = "Istanbul",
                subtitle = "Türkiye",
                artworkRes = R.drawable.aigen_image_istanbul,
                loreDescription = "Where continents meet beneath golden minarets & Bosphorus tides.",
                cardId = "CS-001"
            ),
            CityTemplate(
                title = "Kyoto",
                subtitle = "Japan",
                artworkRes = R.drawable.aigen_image_kyoto,
                loreDescription = "Pathways of crimson torii gates & eternal cherry blossom rain.",
                cardId = "CS-002"
            ),
            CityTemplate(
                title = "Amsterdam",
                subtitle = "Netherlands",
                artworkRes = R.drawable.aigen_image_amsterdam,
                loreDescription = "Sunset reflections over historic canals & blooming tulips.",
                cardId = "CS-003"
            ),
            CityTemplate(
                title = "London",
                subtitle = "United Kingdom",
                artworkRes = R.drawable.aigen_image_london,
                loreDescription = "Victorian lamps & red double-deckers in the misty dusk.",
                cardId = "CS-004"
            )
        )

        fun roll(forced: CardRarity? = null): TradingCard {
            val rolledRarity = forced ?: CardRarity.roll()
            val template = CITY_TEMPLATES.random()
            return TradingCard(
                title = template.title,
                subtitle = template.subtitle,
                artworkRes = template.artworkRes,
                loreDescription = template.loreDescription,
                rarity = rolledRarity,
                cardId = template.cardId
            )
        }
    }
}

/**
 * Foil palette for trading card holographic shader effects.
 */
data class FoilPalette(
    val primary: Color,
    val light: Color,
    val dark: Color,
    val side: Color,
    val face: Color,
    val intensity: Float,
    val depth: Int,
) {
    companion object {
        fun forRarity(r: CardRarity): FoilPalette = when (r) {
            CardRarity.COMMON -> FoilPalette(
                Color(0xFFD0D6E1), Color.White, Color(0xFF58616D),
                Color(0xFF67788D), Color(0xFFCCD3DF), .15f, 5
            )
            CardRarity.RARE -> FoilPalette(
                Color(0xFF547CFF), Color(0xFF8FFBFF), Color(0xFF263A9D),
                Color(0xFF334BDD), Color(0xFF6397FF), .35f, 8
            )
            CardRarity.EPIC -> FoilPalette(
                Color(0xFFB36AFF), Color(0xFFFFA7F6), Color(0xFF5A26A0),
                Color(0xFF7738DC), Color(0xFFD786FF), .55f, 11
            )
            CardRarity.LEGENDARY -> FoilPalette(
                Color(0xFFFFA81F), Color(0xFFFFF2AF), Color(0xFF9B3F16),
                Color(0xFFCB4B72), Color(0xFFFFB65C), .75f, 14
            )
            CardRarity.MYTHIC -> FoilPalette(
                Color(0xFFFF2A55), Color(0xFFFF8DA1), Color(0xFF8B001A),
                Color(0xFFC7002E), Color(0xFFFF5C7C), .90f, 16
            )
        }
    }
}
