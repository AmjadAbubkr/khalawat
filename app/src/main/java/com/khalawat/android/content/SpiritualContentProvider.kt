package com.khalawat.android.content

import android.content.Context

object SpiritualContentProvider {
    @Volatile
    private var cached: SpiritualContentImpl? = null

    fun get(context: Context): SpiritualContentImpl {
        return cached ?: synchronized(this) {
            cached ?: SpiritualContentImpl().also { content ->
                val json = context.assets.open("spiritual_content.json")
                    .bufferedReader()
                    .use { it.readText() }
                content.loadJson(json)
                cached = content
            }
        }
    }
}
