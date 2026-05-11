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

    /** HTML-escapes a string to prevent XSS in template replacements. */
    private fun escapeHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri ?: "/"
        val params = session.parms ?: emptyMap()
        val langCode = params["lang"] ?: "en"
        val language = Language.entries.find { it.name.equals(langCode, ignoreCase = true) }
            ?: Language.EN

        val html = when {
            uri.startsWith("/stage1") || uri == "/" -> buildStage1Page(language, params)
            uri.startsWith("/stage2") -> buildStage2Page(language, params)
            uri.startsWith("/stage3") -> buildStage3Page(language, params)
            else -> buildStage1Page(language, params)
        }

        return newFixedLengthResponse(Response.Status.OK, "text/html", html)
    }

    private fun buildStage1Page(language: Language, params: Map<String, String>): String {
        val content = try {
            spiritualContent.nextAyah(language)
        } catch (_: Exception) {
            spiritualContent.nextHadith(language)
        }
        val html = loadAsset("intervention/stage1.html")
        return html
            .replace("{{ARABIC}}", escapeHtml(content.arabic))
            .replace("{{TRANSLATION}}", escapeHtml(content.translation))
            .replace("{{SOURCE}}", escapeHtml(content.source))
            .replace("{{LANG}}", escapeHtml(language.name.lowercase()))
    }

    private fun buildStage2Page(language: Language, params: Map<String, String>): String {
        val dhikr1 = spiritualContent.nextDhikr(language)
        val dhikr2 = spiritualContent.nextDhikr(language)
        val dhikr3 = spiritualContent.nextDhikr(language)
        val html = loadAsset("intervention/stage2.html")
        return html
            .replace("{{ARABIC}}", escapeHtml(params["arabic"] ?: ""))
            .replace("{{TRANSLATION}}", escapeHtml(params["translation"] ?: ""))
            .replace("{{SOURCE}}", escapeHtml(params["source"] ?: ""))
            .replace("{{LANG}}", escapeHtml(language.name.lowercase()))
            .replace("{{DHIKR_1_ARABIC}}", escapeHtml(dhikr1.arabic))
            .replace("{{DHIKR_1_TRANS}}", escapeHtml(dhikr1.translation))
            .replace("{{DHIKR_2_ARABIC}}", escapeHtml(dhikr2.arabic))
            .replace("{{DHIKR_2_TRANS}}", escapeHtml(dhikr2.translation))
            .replace("{{DHIKR_3_ARABIC}}", escapeHtml(dhikr3.arabic))
            .replace("{{DHIKR_3_TRANS}}", escapeHtml(dhikr3.translation))
    }

    private fun buildStage3Page(language: Language, params: Map<String, String>): String {
        val html = loadAsset("intervention/stage3.html")
        return html
            .replace("{{LANG}}", escapeHtml(language.name.lowercase()))
    }

    private fun loadAsset(path: String): String {
        val stream = assetLoader(path)
            ?: return "<html><body>Intervention page not found</body></html>"
        return stream.bufferedReader().use { it.readText() }
    }
}
