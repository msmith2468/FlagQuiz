package com.lavaegg.flagquiz.domain.model

enum class GameRegion(
    val title: String,
    val subtitle: String
) {
    WORLD("World", "A mix of flags from every continent"),
    AFRICA("Africa", "Explore flags across the African continent"),
    ASIA("Asia", "Play with countries across Asia"),
    EUROPE("Europe", "Guess flags from European countries"),
    NORTH_AMERICA("North America", "Play with North American flags"),
    SOUTH_AMERICA("South America", "Guess flags from South America"),
    OCEANIA("Australia", "Play with Australia and nearby Pacific nations")
}

