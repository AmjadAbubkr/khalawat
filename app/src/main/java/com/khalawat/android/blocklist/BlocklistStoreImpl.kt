package com.khalawat.android.blocklist

import java.io.File

class BlocklistStoreImpl : BlocklistStore {

    private val blockedDomains = HashSet<String>()

    override fun loadBlocklist(path: String) {
        val file = File(path)
        if (!file.exists()) return

        blockedDomains.clear()
        file.readLines()
            .map { it.trim() }
            .map { line ->
                // Strip comments: both full-line (# prefix) and inline
                val commentIndex = line.indexOf('#')
                if (commentIndex >= 0) line.substring(0, commentIndex).trim() else line
            }
            .filter { it.isNotBlank() }
            .forEach { blockedDomains.add(it.lowercase()) }
    }

    override fun isBlocked(domain: String): Boolean {
        val normalized = domain.lowercase().trimEnd('.')
        // Check exact match first
        if (normalized in blockedDomains) return true
        // Check if any parent domain is blocked (subdomain matching)
        // e.g., sub.bad.com → check bad.com
        var parent = normalized
        while (parent.contains('.')) {
            parent = parent.substringAfter('.')
            if (parent in blockedDomains) return true
        }
        return false
    }

    override fun size(): Int = blockedDomains.size
}
