package com.khalawat.android.dns

import com.google.common.truth.Truth.assertThat
import com.khalawat.android.blocklist.BlocklistStore
import com.khalawat.android.blocklist.BlocklistStoreImpl
import org.junit.Before
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.Rule
import java.io.File
import java.net.InetAddress

class DnsProxyTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var blocklistStore: BlocklistStore
    private lateinit var dnsProxy: DnsProxy

    @Before
    fun setUp() {
        blocklistStore = BlocklistStoreImpl()
        val blocklistFile = tempFolder.newFile("blocklist.txt")
        blocklistFile.writeText("bad.com\nevil.org\nharam.net")
        blocklistStore.loadBlocklist(blocklistFile.absolutePath)

        dnsProxy = DnsProxyImpl(blocklistStore)
    }

    // --- Blocked domains return redirect IP ---

    @Test
    fun `blocked domain returns Blocked response`() {
        val query = buildDnsQuery("bad.com")
        val result = dnsProxy.resolve(query)

        assertThat(result).isInstanceOf(DnsResponse.Blocked::class.java)
    }

    @Test
    fun `blocked domain redirects to local intervention server`() {
        val query = buildDnsQuery("bad.com")
        val result = dnsProxy.resolve(query) as DnsResponse.Blocked

        assertThat(result.redirectIp).isEqualTo(InetAddress.getByName("127.0.0.1"))
    }

    @Test
    fun `subdomain of blocked domain returns Blocked`() {
        val query = buildDnsQuery("sub.bad.com")
        val result = dnsProxy.resolve(query)

        assertThat(result).isInstanceOf(DnsResponse.Blocked::class.java)
    }

    // --- Clean domains return Forwarded ---

    @Test
    fun `clean domain returns Forwarded response`() {
        val query = buildDnsQuery("google.com")
        val result = dnsProxy.resolve(query)

        assertThat(result).isInstanceOf(DnsResponse.Forwarded::class.java)
    }

    // --- Domain extraction from DNS packets ---

    @Test
    fun `extracts domain from standard DNS query packet`() {
        val packet = buildDnsQuery("example.com")
        val domain = DnsPacketParser.extractDomain(packet.data)

        assertThat(domain).isEqualTo("example.com")
    }

    @Test
    fun `extracts domain with multiple labels`() {
        val packet = buildDnsQuery("sub.domain.example.com")
        val domain = DnsPacketParser.extractDomain(packet.data)

        assertThat(domain).isEqualTo("sub.domain.example.com")
    }

    // --- Malformed packets ---

    @Test
    fun `malformed packet returns Forwarded with original data`() {
        val query = DnsQuery(byteArrayOf(0xFF.toByte(), 0xFE.toByte()), "8.8.8.8")
        val result = dnsProxy.resolve(query)

        // Malformed packets should be forwarded, not dropped
        assertThat(result).isInstanceOf(DnsResponse.Forwarded::class.java)
    }

    @Test
    fun `empty packet returns Forwarded`() {
        val query = DnsQuery(ByteArray(0), "8.8.8.8")
        val result = dnsProxy.resolve(query)

        assertThat(result).isInstanceOf(DnsResponse.Forwarded::class.java)
    }

    // --- Helper: Build a valid DNS query packet ---

    private fun buildDnsQuery(domain: String): DnsQuery {
        val data = buildDnsQueryPacket(domain)
        return DnsQuery(data, "8.8.8.8")
    }

    /**
     * Builds a minimal valid DNS query packet for the given domain.
     * DNS packet format:
     *   Header (12 bytes): ID(2) + Flags(2) + QDCOUNT(2) + ANCOUNT(2) + NSCOUNT(2) + ARCOUNT(2)
     *   Question section: QNAME(variable) + QTYPE(2) + QCLASS(2)
     */
    private fun buildDnsQueryPacket(domain: String): ByteArray {
        val parts = domain.split(".")
        var qnameSize = 1 // trailing null byte
        for (part in parts) {
            qnameSize += 1 + part.length // length byte + label
        }

        val packet = ByteArray(12 + qnameSize + 4) // header + qname + qtype + qclass
        var offset = 0

        // Header
        packet[offset++] = 0x00 // ID high byte
        packet[offset++] = 0x01 // ID low byte
        packet[offset++] = 0x01 // Flags: standard query
        packet[offset++] = 0x00
        packet[offset++] = 0x00 // QDCOUNT high
        packet[offset++] = 0x01 // QDCOUNT low = 1 question
        packet[offset++] = 0x00 // ANCOUNT
        packet[offset++] = 0x00
        packet[offset++] = 0x00 // NSCOUNT
        packet[offset++] = 0x00
        packet[offset++] = 0x00 // ARCOUNT
        packet[offset++] = 0x00

        // QNAME
        for (part in parts) {
            packet[offset++] = part.length.toByte()
            for (c in part) {
                packet[offset++] = c.code.toByte()
            }
        }
        packet[offset++] = 0x00 // null terminator

        // QTYPE = A (0x0001)
        packet[offset++] = 0x00
        packet[offset++] = 0x01

        // QCLASS = IN (0x0001)
        packet[offset++] = 0x00
        packet[offset++] = 0x01

        return packet
    }
}
