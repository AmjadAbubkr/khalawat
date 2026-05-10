package com.khalawat.android.content

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File

class SpiritualContentTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var content: SpiritualContent

    @Before
    fun setUp() {
        content = SpiritualContentImpl()
    }

    // --- Load and query ayat ---

    @Test
    fun `nextAyah returns content item with arabic and translation`() {
        content.loadContent(createContentFile())
        val item = content.nextAyah(Language.EN)

        assertThat(item.arabic).isNotEmpty()
        assertThat(item.translation).isNotEmpty()
        assertThat(item.source).isNotEmpty()
    }

    @Test
    fun `nextAyah returns English translation when language is EN`() {
        content.loadContent(createContentFile())
        val item = content.nextAyah(Language.EN)

        assertThat(item.translation).contains("forgiv")
    }

    @Test
    fun `nextAyah returns Arabic translation when language is AR`() {
        content.loadContent(createContentFile())
        val item = content.nextAyah(Language.AR)

        // Arabic language should return the arabic field as both arabic and translation
        assertThat(item.translation).isEqualTo(item.arabic)
    }

    // --- Rotation without replacement ---

    @Test
    fun `rotation does not repeat until all items shown`() {
        content.loadContent(createContentFile())

        val seen = mutableSetOf<String>()
        // We have 3 ayat in the test file
        repeat(3) {
            val item = content.nextAyah(Language.EN)
            assertThat(seen).doesNotContain(item.arabic)
            seen.add(item.arabic)
        }
    }

    @Test
    fun `rotation resets after all items exhausted`() {
        content.loadContent(createContentFile())

        // Exhaust all 3 ayat
        val first = content.nextAyah(Language.EN)
        content.nextAyah(Language.EN)
        content.nextAyah(Language.EN)

        // 4th call should restart rotation
        val fourth = content.nextAyah(Language.EN)
        assertThat(fourth.arabic).isEqualTo(first.arabic)
    }

    // --- Hadith ---

    @Test
    fun `nextHadith returns content item`() {
        content.loadContent(createContentFile())
        val item = content.nextHadith(Language.EN)

        assertThat(item.arabic).isNotEmpty()
        assertThat(item.translation).isNotEmpty()
    }

    @Test
    fun `hadith rotation works independently from ayah rotation`() {
        content.loadContent(createContentFile())

        // Get an ayah
        val ayah = content.nextAyah(Language.EN)
        // Get a hadith — should not affect ayah rotation
        val hadith = content.nextHadith(Language.EN)

        // Next ayah should be the second one, not reset
        val nextAyah = content.nextAyah(Language.EN)
        assertThat(nextAyah.arabic).isNotEqualTo(ayah.arabic)
    }

    // --- Dhikr ---

    @Test
    fun `nextDhikr returns content item`() {
        content.loadContent(createContentFile())
        val item = content.nextDhikr(Language.EN)

        assertThat(item.arabic).isNotEmpty()
    }

    // --- Multi-language ---

    @Test
    fun `different languages have independent rotation`() {
        content.loadContent(createContentFile())

        val enItem = content.nextAyah(Language.EN)
        val arItem = content.nextAyah(Language.AR)

        // AR should return arabic as translation; EN should return English
        assertThat(arItem.translation).isEqualTo(arItem.arabic)
        assertThat(enItem.translation).isNotEqualTo(enItem.arabic)
    }

    // --- Reset ---

    @Test
    fun `resetRotation restarts all counters`() {
        content.loadContent(createContentFile())

        val first = content.nextAyah(Language.EN)
        content.nextAyah(Language.EN)
        content.resetRotation()
        val afterReset = content.nextAyah(Language.EN)

        assertThat(afterReset.arabic).isEqualTo(first.arabic)
    }

    // --- Helpers ---

    private fun createContentFile(): String {
        val file = tempFolder.newFile("spiritual_content.json")
        file.writeText("""
        {
            "ayat": [
                {
                    "arabic": "وَمَن يَغْفِرُ مَا ذَنۢبًا إِلَّا اللهُ",
                    "translations": {
                        "en": "And who forgives sin except Allah?",
                        "ar": "وَمَن يَغْفِرُ مَا ذَنۢبًا إِلَّا اللهُ",
                        "ur": "اور اللہ کے سوا کون گناہ معاف کرتا ہے؟",
                        "ms": "Dan siapa lagi yang dapat mengampunkan dosa selain daripada Allah?",
                        "tr": "Allah'tan başka kim günahları bağışlayabilir?",
                        "fr": "Et qui pardonne les péchés en dehors d'Allah?"
                    },
                    "source": "Quran 3:135"
                },
                {
                    "arabic": "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ",
                    "translations": {
                        "en": "Indeed, prayer prohibits immorality and wrongdoing",
                        "ar": "إِنَّ الصَّلَاةَ تَنْهَىٰ عَنِ الْفَحْشَاءِ وَالْمُنكَرِ",
                        "ur": "بے شک نماز بے حیائی اور بری باتوں سے روکتی ہے",
                        "ms": "Sesungguhnya solat itu mencegah dari perbuatan keji dan mungkar",
                        "tr": "Şüphesiz namaz, çirkinliklerden ve kötülüklerden alıkoyar",
                        "fr": "En vérité, la prière préserve de la turpitude et du blâmable"
                    },
                    "source": "Quran 29:45"
                },
                {
                    "arabic": "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللهِ",
                    "translations": {
                        "en": "Say: My servants who have transgressed against yourselves, do not despair of the mercy of Allah",
                        "ar": "قُلْ يَا عِبَادِيَ الَّذِينَ أَسْرَفُوا عَلَىٰ أَنفُسِهِمْ لَا تَقْنَطُوا مِن رَّحْمَةِ اللهِ",
                        "ur": "کہو اے میرے بندو جن پر اپنی طرف سے زیادتی کی اللہ کی رحمت سے مایوس نہ ہو",
                        "ms": "Katakanlah: Wahai hamba-hambaKu yang telah melampaui batas terhadap diri mereka sendiri, janganlah kamu berputus asa dari rahmat Allah",
                        "tr": "De ki: Ey kendilerine karşı sınırı aşan kullarım! Allah'ın rahmetinden ümidinizi kesmeyin",
                        "fr": "Dis: Ô Mes serviteurs qui avez commis des excès à votre propre détriment, ne désespérez pas de la miséricorde d'Allah"
                    },
                    "source": "Quran 39:53"
                }
            ],
            "hadith": [
                {
                    "arabic": "كُلُّ ابْنِ آدَمَ خَطَّاءٌ وَخَيْرُ الْخَطَّائِينَ التَّوَّابُونَ",
                    "translations": {
                        "en": "Every son of Adam sins, and the best of sinners are those who repent",
                        "ar": "كُلُّ ابْنِ آدَمَ خَطَّاءٌ وَخَيْرُ الْخَطَّائِينَ التَّوَّابُونَ",
                        "ur": "ہر آدمی گناہ کرتا ہے اور گناہ گاروں میں بہترین توبہ کرنے والے ہیں",
                        "ms": "Setiap anak Adam berbuat dosa, dan sebaik-baik orang yang berdosa adalah yang bertaubat",
                        "tr": "Âdemoğlunun hepsi günah işler, günah işleyenlerin en hayırlısı tevbe edenlerdir",
                        "fr": "Tout fils d'Adam commet des péchés, et les meilleurs pécheurs sont ceux qui se repentent"
                    },
                    "source": "Tirmidhi 2499"
                },
                {
                    "arabic": "اتَّقِ اللهَ حَيْثُمَا كُنتَ",
                    "translations": {
                        "en": "Fear Allah wherever you are",
                        "ar": "اتَّقِ اللهَ حَيْثُمَا كُنتَ",
                        "ur": "جہاں بھی ہو اللہ سے ڈرو",
                        "ms": "Bertakwalah kepada Allah di mana sahaja kamu berada",
                        "tr": "Nerede olursan ol Allah'tan kork",
                        "fr": "Crains Allah où que tu sois"
                    },
                    "source": "Tirmidhi 1987"
                }
            ],
            "dhikr": [
                {
                    "arabic": "سُبْحَانَ اللهِ",
                    "translations": {
                        "en": "Glory be to Allah",
                        "ar": "سُبْحَانَ اللهِ",
                        "ur": "اللہ پاک ہے",
                        "ms": "Maha Suci Allah",
                        "tr": "Allah'ı tenzih ederim",
                        "fr": "Gloire à Allah"
                    },
                    "source": "Dhikr"
                },
                {
                    "arabic": "الْحَمْدُ لِلَّهِ",
                    "translations": {
                        "en": "All praise is for Allah",
                        "ar": "الْحَمْدُ لِلَّهِ",
                        "ur": "سب تعریفیں اللہ کے لیے ہیں",
                        "ms": "Segala puji bagi Allah",
                        "tr": "Hamd Allah'a mahsustur",
                        "fr": "Louange à Allah"
                    },
                    "source": "Dhikr"
                }
            ]
        }
        """.trimIndent())
        return file.absolutePath
    }
}
