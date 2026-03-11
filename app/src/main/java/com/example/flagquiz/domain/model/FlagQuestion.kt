package com.example.flagquiz.domain.model

data class FlagQuestion(
    val flagEmoji: String,
    val correctCountry: String,
    val options: List<String>
)
