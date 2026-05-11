package com.khalawat.android.dns

import com.khalawat.android.blocklist.BlocklistStore
import java.net.InetAddress

data class DnsQuery(
    val data: ByteArray,
    val upstreamDns: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DnsQuery) return false
        return data.contentEquals(other.data) && upstreamDns == other.upstreamDns
    }

    override fun hashCode(): Int = 31 * data.contentHashCode() + upstreamDns.hashCode()
}

sealed class DnsResponse {
    data class Blocked(val redirectIp: InetAddress) : DnsResponse()
    data class Forwarded(val answer: ByteArray) : DnsResponse() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Forwarded) return false
            return answer.contentEquals(other.answer)
        }

        override fun hashCode(): Int = answer.contentHashCode()
    }
}

interface DnsProxy {
    fun resolve(query: DnsQuery): DnsResponse
}
