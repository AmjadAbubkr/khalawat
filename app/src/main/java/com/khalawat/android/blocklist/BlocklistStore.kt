package com.khalawat.android.blocklist

interface BlocklistStore {
    fun loadBlocklist(path: String)
    fun isBlocked(domain: String): Boolean
    fun size(): Int
}
