package com.wendigo.tekrar.repo

import android.content.Context
import android.net.Uri

object FileIo {

    fun writeText(context: Context, uri: Uri, text: String) {
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Dosya açılamadı")
        stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
    }

    fun readText(context: Context, uri: Uri): String {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("Dosya açılamadı")
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }
}
