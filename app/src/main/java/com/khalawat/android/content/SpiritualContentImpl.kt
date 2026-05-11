package com.khalawat.android.content

import org.json.JSONObject
import java.io.File

class SpiritualContentImpl : SpiritualContent {

    private data class ContentEntry(
        val arabic: String,
        val translations: Map<String, String>,
        val source: String
    )

    private var ayat: List<ContentEntry> = emptyList()
    private var hadith: List<ContentEntry> = emptyList()
    private var dhikr: List<ContentEntry> = emptyList()

    // Rotation indices per content type per language
    private val ayahIndices = mutableMapOf<Language, Int>()
    private val hadithIndices = mutableMapOf<Language, Int>()
    private val dhikrIndices = mutableMapOf<Language, Int>()

    override fun loadContent(path: String) {
        val file = File(path)
        if (!file.exists()) return

        val json = JSONObject(file.readText())

        ayat = parseEntries(json.optJSONArray("ayat") ?: org.json.JSONArray())
        hadith = parseEntries(json.optJSONArray("hadith") ?: org.json.JSONArray())
        dhikr = parseEntries(json.optJSONArray("dhikr") ?: org.json.JSONArray())

        // Reset indices on reload
        ayahIndices.clear()
        hadithIndices.clear()
        dhikrIndices.clear()
    }

    override fun nextAyah(language: Language): ContentItem {
        return getNext(ayat, ayahIndices, language)
    }

    override fun nextHadith(language: Language): ContentItem {
        return getNext(hadith, hadithIndices, language)
    }

    override fun nextDhikr(language: Language): ContentItem {
        return getNext(dhikr, dhikrIndices, language)
    }

    override fun resetRotation() {
        ayahIndices.clear()
        hadithIndices.clear()
        dhikrIndices.clear()
    }

    private fun getNext(
        entries: List<ContentEntry>,
        indices: MutableMap<Language, Int>,
        language: Language
    ): ContentItem {
        if (entries.isEmpty()) {
            return ContentItem("", "", "")
        }

        val currentIndex = indices.getOrDefault(language, 0)
        val entry = entries[currentIndex % entries.size]
        indices[language] = (currentIndex + 1) % entries.size

        // If we've wrapped around, the next call starts at 0 again
        // But since we use modulo, it naturally cycles

        val translation = if (language == Language.AR) {
            entry.arabic
        } else {
            entry.translations[language.name.lowercase()] ?: entry.translations["en"] ?: entry.arabic
        }

        return ContentItem(
            arabic = entry.arabic,
            translation = translation,
            source = entry.source
        )
    }

    private fun parseEntries(array: org.json.JSONArray): List<ContentEntry> {
        val entries = mutableListOf<ContentEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val arabic = obj.getString("arabic")
            val source = obj.getString("source")
            val translationsObj = obj.getJSONObject("translations")
            val translations = mutableMapOf<String, String>()
            for (key in translationsObj.keys()) {
                translations[key] = translationsObj.getString(key)
            }
            entries.add(ContentEntry(arabic, translations, source))
        }
        return entries
    }
}
