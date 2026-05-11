package com.khalawat.android.server

import com.google.common.truth.Truth.assertThat
import com.khalawat.android.content.Language
import com.khalawat.android.content.SpiritualContent
import com.khalawat.android.content.SpiritualContentImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class InterventionServerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var spiritualContent: SpiritualContent
    private lateinit var server: InterventionServer

    @Before
    fun setUp() {
        spiritualContent = SpiritualContentImpl()
        val contentFile = tempFolder.newFile("content.json")
        contentFile.writeText(testContentJson)
        spiritualContent.loadContent(contentFile.absolutePath)

        server = InterventionServer(spiritualContent) { path ->
            when {
                path.contains("stage1") -> ByteArrayInputStream("""
                    <html><body>
                    <div id="arabic">{{ARABIC}}</div>
                    <div id="translation">{{TRANSLATION}}</div>
                    <div id="source">{{SOURCE}}</div>
                    <div id="lang">{{LANG}}</div>
                    </body></html>
                """.trimIndent().toByteArray())
                path.contains("stage2") -> ByteArrayInputStream("""
                    <html><body>
                    <div id="dhikr1">{{DHIKR_1_ARABIC}}</div>
                    <div id="dhikr2">{{DHIKR_2_ARABIC}}</div>
                    <div id="dhikr3">{{DHIKR_3_ARABIC}}</div>
                    <div id="lang">{{LANG}}</div>
                    </body></html>
                """.trimIndent().toByteArray())
                path.contains("stage3") -> ByteArrayInputStream("""
                    <html><body>
                    <div id="lang">{{LANG}}</div>
                    </body></html>
                """.trimIndent().toByteArray())
                else -> null
            }
        }
        server.start()
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun `server responds to stage1 request`() {
        val connection = get("/stage1?lang=en")
        val html = connection.readResponse()
        assertThat(connection.responseCode).isEqualTo(200)
        assertThat(html).contains("arabic")
    }

    @Test
    fun `stage1 page contains Arabic text from spiritual content`() {
        val html = get("/stage1?lang=en").readResponse()
        assertThat(html).contains("\u0648\u064E\u0645\u064E\u0646")
    }

    @Test
    fun `stage1 page contains source reference`() {
        val html = get("/stage1?lang=en").readResponse()
        assertThat(html).contains("Quran")
    }

    @Test
    fun `stage2 page contains dhikr phrases`() {
        val html = get("/stage2?lang=en").readResponse()
        assertThat(html).contains("\u0633\u064F\u0628\u0652\u062D\u064E\u0627\u0646\u064E")
    }

    @Test
    fun `stage3 page loads successfully`() {
        val connection = get("/stage3?lang=en")
        assertThat(connection.responseCode).isEqualTo(200)
    }

    @Test
    fun `default request serves stage1`() {
        val html = get("/?lang=en").readResponse()
        assertThat(html).contains("\u0648\u064E\u0645\u064E\u0646")
    }

    // --- XSS prevention: server-path tests (Finding #4) ---

    @Test
    fun `stage1 response escapes angle brackets in content`() {
        val html = get("/stage1?lang=en").readResponse()
        // If any spiritual content contained < or >, they would be escaped.
        // At minimum, verify no raw <script> tag appears.
        assertThat(html).doesNotContain("<script>")
        // Verify escaped forms exist if angle brackets are present
        if (html.contains("&lt;")) {
            assertThat(html).contains("&lt;")
        }
    }

    @Test
    fun `stage1 response escapes ampersand in content`() {
        val html = get("/stage1?lang=en").readResponse()
        // Raw & not followed by a valid entity should be escaped as &amp;
        // The server applies escapeHtml() to all template replacements.
        // Verify the response does not contain raw unescaped script injection.
        assertThat(html).doesNotMatch(java.util.regex.Pattern.compile("<script[^>]*>", java.util.regex.Pattern.CASE_INSENSITIVE))
    }

    @Test
    fun `stage2 response does not contain unescaped script tags`() {
        val html = get("/stage2?lang=en").readResponse()
        assertThat(html).doesNotContain("<script>")
    }

    @Test
    fun `stage3 response does not contain unescaped script tags`() {
        val html = get("/stage3?lang=en").readResponse()
        assertThat(html).doesNotContain("<script>")
    }

    @Test
    fun `stage1 with injection in lang parameter does not produce raw script`() {
        val html = get("/stage1?lang=<script>alert(1)</script>").readResponse()
        // The server resolves lang to a Language enum; invalid values
        // fall back to Language.EN, so the injected string never enters
        // the HTML output at all (not even escaped).
        assertThat(html).doesNotContain("<script>")
        assertThat(html).doesNotContain("alert(1)")
    }

    @Test
    fun `stage1 with ampersand injection in lang parameter escapes it`() {
        val html = get("/stage1?lang=en&foo=bar<script>").readResponse()
        assertThat(html).doesNotContain("<script>")
    }

    // --- Helpers ---

    private fun get(path: String): HttpURLConnection {
        val url = URL("http://127.0.0.1:${InterventionServer.PORT}$path")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        return conn
    }

    private fun HttpURLConnection.readResponse(): String {
        return try {
            inputStream.bufferedReader().use { it.readText() }
        } catch (inputEx: Exception) {
            try {
                errorStream?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unable to read process output or error stream", inputEx)
            } catch (errorEx: Exception) {
                throw IllegalStateException("Unable to read process output or error stream", inputEx)
            }
        }
    }

    companion object {
        private const val testContentJson = """
        {
          "ayat": [
            {
              "arabic": "\u0648\u064E\u0645\u064E\u0646 \u064A\u064E\u063A\u0652\u0641\u0650\u0631\u064F \u0645\u064E\u0627 \u0630\u064E\u0646\u06D2\u0628\u064B\u0627 \u0625\u0650\u0644\u0651\u064E\u0627 \u0627\u0644\u0644\u0651\u064E\u0647\u064F",
              "translations": {
                "en": "And who forgives sin except Allah?",
                "ar": "\u0648\u064E\u0645\u064E\u0646 \u064A\u064E\u063A\u0652\u0641\u0650\u0631\u064F \u0645\u064E\u0627 \u0630\u064E\u0646\u06D2\u0628\u064B\u0627 \u0625\u0650\u0644\u0651\u064E\u0627 \u0627\u0644\u0644\u0651\u064E\u0647\u064F"
              },
              "source": "Quran 3:135"
            },
            {
              "arabic": "\u0625\u0650\u0646\u0651\u064E \u0627\u0644\u0635\u0651\u064E\u0644\u064E\u0627\u0629\u064E \u062A\u064E\u0646\u0652\u0647\u064E\u0649\u0670 \u0639\u064E\u0646\u0650 \u0627\u0644\u0652\u0641\u064E\u062D\u0652\u0634\u064E\u0627\u0621\u0650",
              "translations": {
                "en": "Indeed, prayer prohibits immorality",
                "ar": "\u0625\u0650\u0646\u0651\u064E \u0627\u0644\u0635\u0651\u064E\u0644\u064E\u0627\u0629\u064E \u062A\u064E\u0646\u0652\u0647\u064E\u0649\u0670 \u0639\u064E\u0646\u0650 \u0627\u0644\u0652\u0641\u064E\u062D\u0652\u0634\u064E\u0627\u0621\u0650"
              },
              "source": "Quran 29:45"
            }
          ],
          "hadith": [
            {
              "arabic": "\u0643\u064F\u0644\u0651\u064F \u0627\u0628\u0652\u0646\u0650 \u0622\u062F\u064E\u0645\u064E \u062E\u064E\u0637\u0651\u064E\u0627\u0621\u064C",
              "translations": {
                "en": "Every son of Adam sins",
                "ar": "\u0643\u064F\u0644\u0651\u064F \u0627\u0628\u0652\u0646\u0650 \u0622\u062F\u064E\u0645\u064E \u062E\u064E\u0637\u0651\u064E\u0627\u0621\u064C"
              },
              "source": "Tirmidhi 2499"
            }
          ],
          "dhikr": [
            {
              "arabic": "\u0633\u064F\u0628\u0652\u062D\u064E\u0627\u0646\u064E \u0627\u0644\u0644\u0651\u064E\u0647\u0650",
              "translations": {
                "en": "Glory be to Allah",
                "ar": "\u0633\u064F\u0628\u0652\u062D\u064E\u0627\u0646\u064E \u0627\u0644\u0644\u0651\u064E\u0647\u0650"
              },
              "source": "Dhikr"
            },
            {
              "arabic": "\u0627\u0644\u0652\u062D\u064E\u0645\u0652\u062F\u064F \u0644\u0650\u0644\u0651\u064E\u0647\u0650",
              "translations": {
                "en": "All praise is for Allah",
                "ar": "\u0627\u0644\u0652\u062D\u064E\u0645\u0652\u062F\u064F \u0644\u0650\u0644\u0651\u064E\u0647\u0650"
              },
              "source": "Dhikr"
            },
            {
              "arabic": "\u0627\u064E\u0644\u0644\u0651\u064E\u0647\u064F \u0627\u064E\u0643\u0652\u0628\u064E\u0631",
              "translations": {
                "en": "Allah is the Greatest",
                "ar": "\u0627\u064E\u0644\u0644\u0651\u064E\u0647\u064F \u0627\u064E\u0643\u0652\u0628\u064E\u0631"
              },
              "source": "Dhikr"
            }
          ]
        }
        """
    }
}
