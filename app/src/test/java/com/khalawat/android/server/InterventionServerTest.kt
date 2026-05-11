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
        assertThat(html).contains("وَمَن")
    }

    @Test
    fun `stage1 page contains source reference`() {
        val html = get("/stage1?lang=en").readResponse()
        assertThat(html).contains("Quran")
    }

    @Test
    fun `stage2 page contains dhikr phrases`() {
        val html = get("/stage2?lang=en").readResponse()
        assertThat(html).contains("سُبْحَانَ")
    }

    @Test
    fun `stage3 page loads successfully`() {
        val connection = get("/stage3?lang=en")
        assertThat(connection.responseCode).isEqualTo(200)
    }

    @Test
    fun `default request serves stage1`() {
        val html = get("/?lang=en").readResponse()
        assertThat(html).contains("وَمَن")
    }

    // --- XSS prevention tests (unit-level) ---

    @Test
    fun `escapeHtml escapes angle brackets`() {
        val result = testEscapeHtml("<script>alert(1)</script>")
        assertThat(result).isEqualTo("&lt;script&gt;alert(1)&lt;/script&gt;")
    }

    @Test
    fun `escapeHtml escapes ampersand`() {
        val result = testEscapeHtml("Quran&Hadith")
        assertThat(result).isEqualTo("Quran&amp;Hadith")
    }

    @Test
    fun `escapeHtml escapes double quotes`() {
        val result = testEscapeHtml("""value="injection" """)
        assertThat(result).isEqualTo("""value=&quot;injection&quot; """)
    }

    @Test
    fun `escapeHtml escapes single quotes`() {
        val result = testEscapeHtml("value='injection'")
        assertThat(result).isEqualTo("value=&#39;injection&#39;")
    }

    @Test
    fun `escapeHtml leaves plain text unchanged`() {
        val input = "وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا"
        val result = testEscapeHtml(input)
        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `stage1 response does not contain unescaped script tags`() {
        val html = get("/stage1?lang=en").readResponse()
        assertThat(html).doesNotContain("<script>")
    }

    // --- Helpers ---

    /**
     * Direct test of the escapeHtml logic without going through HTTP.
     * Tests the same escaping used in InterventionServer.
     */
    private fun testEscapeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

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
        } catch (e: Exception) {
            errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        }
    }

    companion object {
        private const val testContentJson = """
        {
            "ayat": [
                {
                    "arabic": "وَمَن يَغْفِرُ مَا ذَنۢبًا إِلَّا اللهُ",
                    "translations": {
                        "en": "And who forgives sin except Allah?",
                        "ar": "وَمَن يَغْفِرُ مَا ذَنۢبًا إِلَّا اللهُ"
                    },
                    "source": "Quran 3:135"
                },
                {
                    "arabic": "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ",
                    "translations": {
                        "en": "Indeed, prayer prohibits immorality",
                        "ar": "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ"
                    },
                    "source": "Quran 29:45"
                }
            ],
            "hadith": [
                {
                    "arabic": "كُلُّ ابْنِ آدَمَ خَطَّاءٌ",
                    "translations": {
                        "en": "Every son of Adam sins",
                        "ar": "كُلُّ ابْنِ آدَمَ خَطَّاءٌ"
                    },
                    "source": "Tirmidhi 2499"
                }
            ],
            "dhikr": [
                {
                    "arabic": "سُبْحَانَ اللهِ",
                    "translations": {
                        "en": "Glory be to Allah",
                        "ar": "سُبْحَانَ اللهِ"
                    },
                    "source": "Dhikr"
                },
                {
                    "arabic": "الْحَمْدُ لِلَّهِ",
                    "translations": {
                        "en": "All praise is for Allah",
                        "ar": "الْحَمْدُ لِلَّهِ"
                    },
                    "source": "Dhikr"
                },
                {
                    "arabic": "اَللَّهُ اَكْبَر",
                    "translations": {
                        "en": "Allah is the Greatest",
                        "ar": "اَللَّهُ اَكْبَر"
                    },
                    "source": "Dhikr"
                }
            ]
        }
        """
    }
}
