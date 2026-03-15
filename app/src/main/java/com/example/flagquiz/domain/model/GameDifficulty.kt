package com.example.flagquiz.domain.model

enum class GameDifficulty(
    val title: String,
    val poolFraction: Double
) {
    EASY("Easy", 0.30),
    NORMAL("Normal", 0.55),
    HARD("Hard", 0.80),
    EXPERT("Expert", 1.00)
}
