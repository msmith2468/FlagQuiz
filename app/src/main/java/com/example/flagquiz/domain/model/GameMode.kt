package com.example.flagquiz.domain.model

enum class GameMode(
    val title: String,
    val description: String
) {
    FLAG("Flags", "Guess the country from its flag"),
    OUTLINE("Outlines", "Guess the country from its outline")
}
