package starknet.crypto

import java.io.File
import java.nio.file.Files


internal object NativeLoader {
    private val sharedLibExtensions = listOf("dylib", "so", "dll")

    fun load(name: String) {
        try {
            System.out.println("LOADING DEFAULT")
            // Used for tests and in case someone wants to use a library from
            // a class path.
            System.loadLibrary(name);
        } catch (e: UnsatisfiedLinkError) {
            System.out.println("LOADING OTHER")
            // Find the package bundled in this jar
            val resource = sharedLibExtensions.map {
                val fileName = "/lib$name.$it"
                object {}::class.java.getResource(fileName)
            }.firstOrNull { it != null } ?: throw e

            val tmpDir = Files.createTempDirectory("$name-dir").toFile().apply {
                deleteOnExit()
            }
            val tmpFile = File(tmpDir, name)
            resource.openStream().use { Files.copy(it, tmpFile.toPath()) }
            System.load(tmpFile.absolutePath)
        }
    }

}