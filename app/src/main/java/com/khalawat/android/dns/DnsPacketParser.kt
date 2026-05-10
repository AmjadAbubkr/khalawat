package com.khalawat.android.dns

object DnsPacketParser {

    /**
     * Extracts the domain name from a DNS query packet.
     * DNS QNAME format: length byte + label bytes, repeated, terminated by 0x00
     * e.g., [3][b][a][d][3][c][o][m][0] → "bad.com"
     */
    fun extractDomain(packet: ByteArray): String? {
        if (packet.size < 12) return null // Too short for a valid DNS header

        try {
            var offset = 12 // Skip header (12 bytes)
            val labels = mutableListOf<String>()

            while (offset < packet.size) {
                val length = packet[offset].toInt() and 0xFF
                if (length == 0) break // Null terminator

                offset++ // Move past length byte
                if (offset + length > packet.size) return null // Malformed

                val label = String(packet, offset, length, Charsets.US_ASCII)
                labels.add(label)
                offset += length
            }

            return if (labels.isEmpty()) null else labels.joinToString(".")
        } catch (e: Exception) {
            return null
        }
    }
}
