@file:JvmName("Base64Gzipped")

package com.swmansion.starknet.extensions

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.GZIPOutputStream

fun String.base64Gzipped(): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(this) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}
