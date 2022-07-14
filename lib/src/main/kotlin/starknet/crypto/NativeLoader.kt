package starknet.crypto

import java.nio.file.FileSystems
import java.nio.file.Files


internal object NativeLoader {
    private val sharedLibExtensions = listOf("dylib", "so", "dll")

    fun load(name: String) {
        try {
            // Used for tests and in case someone wants to use a library from
            // a class path.
            System.loadLibrary(name);
        } catch (e: UnsatisfiedLinkError) {
            // Find the package bundled in this jar
            val resource = sharedLibExtensions.map {
                val fileName = "/lib$name.$it"
                object {}::class.java.getResource(fileName)
            }.firstOrNull { it != null } ?: throw e

            val tmpDir = Files.createTempDirectory("$name-dir").toFile().apply {
                deleteOnExit()
            }
            val tmpFilePath = FileSystems.getDefault().getPath(tmpDir.absolutePath, name)
            Files.copy(resource.openStream(), tmpFilePath)
            System.load(tmpFilePath.toString())
        }
    }

}