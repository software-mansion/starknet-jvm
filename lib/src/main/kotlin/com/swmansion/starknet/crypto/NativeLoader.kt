package com.swmansion.starknet.crypto

import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.util.*

internal object NativeLoader {
    fun load(name: String) = load(name, operatingSystem, architecture)

    private fun load(name: String, operatingSystem: SystemType, architecture: String) {
        try {
            // Used for tests, on android and in case someone wants to use a library from
            // a class path.
            System.loadLibrary(name)
        } catch (e: UnsatisfiedLinkError) {
            // Find the package bundled in this jar
            val path = getLibPath(operatingSystem, architecture, "lib" + name)
            val resource =
                NativeLoader::class.java.getResource(path) ?: throw UnsupportedPlatform(
                    operatingSystem.name,
                    architecture,
                )
            loadFromJar(name, resource)
        }
    }

    @Suppress("UnsafeDynamicallyLoadedCode")
    private fun loadFromJar(name: String, resource: URL) {
        // Android lint complains about it, but android should never reach this code
        val tmpDir = Files.createTempDirectory("$name-dir").toFile().apply {
            deleteOnExit()
        }
        val tmpFilePath = FileSystems.getDefault().getPath(tmpDir.absolutePath, name)
        resource.openStream().use { Files.copy(it, tmpFilePath) }
        System.load(tmpFilePath.toString())
    }

    private enum class SystemType {
        Windows, MacOS, Linux, Other
    }

    private val operatingSystem: SystemType by lazy {
        val system = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)
        when {
            system.contains("mac") || system.contains("darwin") -> SystemType.MacOS
            system.contains("win") -> SystemType.Windows
            system.contains("nux") -> SystemType.Linux
            else -> SystemType.Other
        }
    }

    private val architecture: String by lazy {
        System.getProperty("os.arch")
    }

    class UnsupportedPlatform(system: String, architecture: String) :
        Exception("Unsupported platfrom $system:$architecture")

    private fun getLibPath(system: SystemType, architecture: String, name: String): String {
        return when (system) {
            SystemType.MacOS -> "/darwin/$name.dylib"
            SystemType.Linux -> "/linux/$architecture/$name.so"
            else -> throw UnsupportedPlatform(operatingSystem.name, architecture)
        }
    }
}
