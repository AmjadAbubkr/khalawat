package com.khalawat.android.blocklist

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File

class BlocklistStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var blocklistStore: BlocklistStore

    @Before
    fun setUp() {
        blocklistStore = BlocklistStoreImpl()
    }

    // --- RED: Load blocklist and query domains ---

    @Test
    fun `blocked domain returns true`() {
        val blocklistFile = createBlocklistFile("bad.com\nevil.org\nharam.net")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.isBlocked("bad.com")).isTrue()
    }

    @Test
    fun `clean domain returns false`() {
        val blocklistFile = createBlocklistFile("bad.com\nevil.org")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.isBlocked("google.com")).isFalse()
    }

    @Test
    fun `subdomain of blocked domain is also blocked`() {
        val blocklistFile = createBlocklistFile("bad.com")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.isBlocked("sub.bad.com")).isTrue()
    }

    @Test
    fun `empty blocklist blocks nothing`() {
        val blocklistFile = createBlocklistFile("")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.isBlocked("anything.com")).isFalse()
    }

    @Test
    fun `blocklist size reports correctly`() {
        val blocklistFile = createBlocklistFile("a.com\nb.com\nc.com")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.size()).isEqualTo(3)
    }

    @Test
    fun `duplicate domains counted once`() {
        val blocklistFile = createBlocklistFile("bad.com\nbad.com\nbad.com")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.size()).isEqualTo(1)
    }

    @Test
    fun `blank lines and whitespace are ignored`() {
        val blocklistFile = createBlocklistFile("\n  bad.com  \n\n\nevil.org\n")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.size()).isEqualTo(2)
        assertThat(blocklistStore.isBlocked("bad.com")).isTrue()
        assertThat(blocklistStore.isBlocked("evil.org")).isTrue()
    }

    @Test
    fun `domains with comments are stripped`() {
        val blocklistFile = createBlocklistFile("# This is a comment\nbad.com # inline comment\n# another comment")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        assertThat(blocklistStore.size()).isEqualTo(1)
        assertThat(blocklistStore.isBlocked("bad.com")).isTrue()
    }

    @Test
    fun `lookup performance under 5ms for 100k domains`() {
        // Generate a large blocklist
        val domains = StringBuilder()
        for (i in 1..100_000) {
            domains.append("domain$i.com\n")
        }
        // Add a known domain to search for
        domains.append("target.com\n")

        val blocklistFile = createBlocklistFile(domains.toString())
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        // Warm up
        blocklistStore.isBlocked("target.com")

        // Measure
        val start = System.nanoTime()
        for (i in 1..100) {
            blocklistStore.isBlocked("target.com")
            blocklistStore.isBlocked("notfound$i.com")
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0 / 100.0

        assertThat(elapsedMs).isLessThan(5.0)
    }

    // --- Helper ---

    private fun createBlocklistFile(content: String): File {
        val file = tempFolder.newFile("blocklist.txt")
        file.writeText(content)
        return file
    }
}
