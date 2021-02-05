package io.github.natanfudge

import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.util.Constants
import org.gradle.api.artifacts.ResolvedArtifact
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

internal fun ProjectContext.getLoom() = project.extensions.getByType(LoomGradleExtension::class.java)

/**
 * Checks if an artifact is a fabric mod, according to the presence of a fabric.mod.json.
 */
internal fun isFabricMod(artifact: ResolvedArtifact): Boolean {
    val input = artifact.file
    try {
        ZipFile(input).use { zipFile ->
            if (zipFile.getEntry("fabric.mod.json") != null) {
                return true
            }
            return false
        }
    } catch (e: IOException) {
        return false
    }
}

internal fun ProjectContext.getUnmappedModDependencies(): List<File> = cachedValue("originalModDependencies") {
    Constants.MOD_COMPILE_ENTRIES.flatMap { modCompileEntry ->
        val sourceConfig = project.configurations.getByName(modCompileEntry.sourceConfiguration)
        sourceConfig.resolvedConfiguration.resolvedArtifacts.filter { isFabricMod(it) }
            .map { it.file }
    }
}

internal fun ProjectContext.getMinecraftVersion() = getLoom().minecraftProvider.minecraftVersion

internal fun ProjectContext.getDevLaunchConfig() = getLoom().devLauncherConfig

internal fun ProjectContext.getNamedMcJar(): File? {
    return getLoom().minecraftMappedProvider.mappedJar
}

internal fun ProjectContext.getIntermediaryLoaderJar() = getUnmappedModDependencies().first { it.name.startsWith("fabric-loader-") }

/** Must only be called in afterEvaluate! */
internal fun ProjectContext.getIntermediaryMcJar(): File? {
    return try {
        getLoom().minecraftMappedProvider.intermediaryJar
    } catch (e: NullPointerException) {
        null
    }
}
