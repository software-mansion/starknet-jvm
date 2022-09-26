package com.swmansion.starknet.extensions

import org.bouncycastle.util.encoders.Base64
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPOutputStream

@JvmSynthetic
internal fun String.base64Gzipped(): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(StandardCharsets.UTF_8).use { it.write(this) }
    return Base64.toBase64String(bos.toByteArray())
}
