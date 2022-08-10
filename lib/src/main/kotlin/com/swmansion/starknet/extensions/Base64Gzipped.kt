package com.swmansion.starknet.extensions

import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

fun String.base64Gzipped(): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(this) }
    val base64Encoded = ByteArrayOutputStream().use { Base64.encode(bos.toByteArray(), it) }
    return base64Encoded.toString()
}
