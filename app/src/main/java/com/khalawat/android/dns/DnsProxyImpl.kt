package com.khalawat.android.dns

import com.khalawat.android.blocklist.BlocklistStore
import java.net.InetAddress

class DnsProxyImpl(
    private val blocklistStore: BlocklistStore
) : DnsProxy {

    companion object {
        val REDIRECT_IP: InetAddress = InetAddress.getByName("0.0.0.0")
    }

    override fun resolve(query: DnsQuery): DnsResponse {
        val domain = DnsPacketParser.extractDomain(query.data)

        // If we can't parse the domain, forward it rather than drop it
        if (domain == null) {
            return DnsResponse.Forwarded(query.data)
        }

        return if (blocklistStore.isBlocked(domain)) {
            DnsResponse.Blocked(REDIRECT_IP)
        } else {
            DnsResponse.Forwarded(query.data)
        }
    }
}
