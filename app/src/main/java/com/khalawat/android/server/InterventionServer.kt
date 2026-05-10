package com.khalawat.android.server

import com.khalawat.android.content.Language
import com.khalawat.android.content.SpiritualContent
import com.khalawat.android.escalation.EscalationStage
import fi.iki.elonen.NanoHTTPD
import java.io.InputStream

class InterventionServer(
    private val spiritualContent: SpiritualContent,
    private val assetLoader: (String) -> InputStream?
) : NanoHTTPD(8080) {

    companion object {
        const val PORT = 8080
    }

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val params = session.parms ?: emptyMap()

        val langCode = params["lang"] ?: "en"
        val language = Language.entries.find { it.name.equals(langCode, ignoreCase = true) } ?: Language.EN

        val html = when {
            uri.startsWith("/stage1") || uri == "/" -> buildStage1Page(language, params)
            uri.startsWith("/stage2") -> buildStage2Page(language, params)
            uri.startsWith("/stage3") -> buildStage3Page(language, params)
            else -> buildStage1Page(language, params)
        }

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun buildStage1Page(language: Language, params: Map<String, String>): String {
        // Prefer ayah for stage 1, fall back to hadith
        val content = try {
            spiritualContent.nextAyah(language)
        } catch (_: Exception) {
            spiritualContent.nextHadith(language)
        }

        val html = loadAsset("intervention/stage1.html")
        return html
            .replace("{{ARABIC}}", content.arabic)
            .replace("{{TRANSLATION}}", content.translation)
            .replace("{{SOURCE}}", content.source)
            .replace("{{LANG}}", language.name.lowercase())
    }

    private fun buildStage2Page(language: Language, params: Map<String, String>): String {
        val dhikr1 = spiritualContent.nextDhikr(language)
        val dhikr2 = spiritualContent.nextDhikr(language)
        val dhikr3 = spiritualContent.nextDhikr(language)

        val html = loadAsset("intervention/stage2.html")
        return html
            .replace("{{ARABIC}}", params["arabic"] ?: "")
            .replace("{{TRANSLATION}}", params["translation"] ?: "")
            .replace("{{SOURCE}}", params["source"] ?: "")
            .replace("{{LANG}}", language.name.lowercase())
            .replace("{{DHIKR_1_ARABIC}}", dhikr1.arabic)
            .replace("{{DHIKR_1_TRANS}}", dhikr1.translation)
            .replace("{{DHIKR_2_ARABIC}}", dhikr2.arabic)
            .replace("{{DHIKR_2_TRANS}}", dhikr2.translation)
            .replace("{{DHIKR_3_ARABIC}}", dhikr3.arabic)
            .replace("{{DHIKR_3_TRANS}}", dhikr3.translation)
    }

    private fun buildStage3Page(language: Language, params: Map<String, String>): String {
        val html = loadAsset("intervention/stage3.html")
        return html
            .replace("{{LANG}}", language.name.lowercase())
    }

    private fun loadAsset(path: String): String {
        val stream = assetLoader(path) ?: return "<html><body>Intervention page not found</body></html>"
        return stream.bufferedReader().use { it.readText() }
    }
}
