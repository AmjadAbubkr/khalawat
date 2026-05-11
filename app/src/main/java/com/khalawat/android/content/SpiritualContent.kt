package com.khalawat.android.content

data class ContentItem(
    val arabic: String,
    val translation: String,
    val source: String
)

enum class Language { AR, EN, UR, MS, TR, FR }

interface SpiritualContent {
    fun loadContent(path: String)
    fun nextAyah(language: Language): ContentItem
    fun nextHadith(language: Language): ContentItem
    fun nextDhikr(language: Language): ContentItem
    fun resetRotation()
}
