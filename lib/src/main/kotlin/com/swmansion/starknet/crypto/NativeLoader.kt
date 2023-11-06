package com.swmansion.starknet.crypto

import java.io.File
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

internal object NativeLoader {
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

    private val jarPath: Path? by lazy {
        val envProperty = "com.swmansion.starknet.library.jarPath"
        return@lazy System.getProperty(envProperty)?.let {
            val file = File(it)
            if (!file.exists() || file.isDirectory) {
                throw InvalidJarPath(it, envProperty)
            }
            file.toPath()
        }
    }

    fun load(name: String) = load(name, operatingSystem, architecture)

    private fun load(name: String, operatingSystem: SystemType, architecture: String) {
        try {
            // Used for tests, on android and in case someone wants to use a library from
            // a class path.
            System.loadLibrary(name)
        } catch (e: UnsatisfiedLinkError) {
            // Find the package bundled in this jar
            val path = getLibRelativePath(operatingSystem, architecture, "lib$name")
            val resource = resolveLibResource(path)
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

    class UnsupportedPlatform(system: String, architecture: String) :
        RuntimeException("Unsupported platfrom $system:$architecture")
    class InvalidJarPath(path: String, systemProperty: String) :
        RuntimeException("File $path does not exist or is not a file. Remove $systemProperty or set it to a valid path.")

    private fun resolveLibResource(path: String): URL = when (jarPath) {
        null -> NativeLoader::class.java.getResource(path)
            ?: throw UnsupportedPlatform(operatingSystem.name, architecture)
        else -> URL("jar:file:$jarPath!$path")
    }

    private fun getLibRelativePath(system: SystemType, architecture: String, name: String): String {
        return when (system) {
            SystemType.MacOS -> "/darwin/$name.dylib"
            SystemType.Linux -> "/linux/$architecture/$name.so"
            else -> throw UnsupportedPlatform(operatingSystem.name, architecture)
        }
    }
}
