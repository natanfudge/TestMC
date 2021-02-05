package io.github.natanfudge

import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import java.io.File

internal class IntermediaryEnvironment(prodJarTask: Jar, override val project: Project) : ProjectContext {
    private val namedTestJar by testMcTask(prodJarTask::class) {
        prodJarTask.inputs.files.forEach { from(it) }
        getAllAssemblyTasks().filter { it.task.isTestAssemblyTask() }
            .forEach { from(it.destinationDir) }
        archiveClassifier.set("test-named")
    }
    private val intermediaryTestJar by testMcTask<RemapJarTask> {
        dependsOn(namedTestJar)
        input.set(namedTestJar.outputs.files.singleFile)
        archiveClassifier.set("test-intermediary")
        addNestedDependencies.set(false)
    }

    private val intermediaryTestJarFile = intermediaryTestJar.archiveFile.get().asFile

    private val intermediaryTestsRunDir = createBuildDirectory("intermediary_tests")


    /**
     * Named tests are just normal tests using a different directory.
     * Intermediary tests are special. They remove everything that uses 'named' identifiers, and replace it with the equivalent that uses intermediary identifiers.
     * For Minecraft: that is the mc jar in the global loom cache that is mapped to intermediary;
     * For mod dependencies: that is the modCompile dependencies that were fetched by gradle and put in the global loom cache, before Loom touched them;
     * For the mod itself: that is the output of the remapJar task.
     */

    private val supplyIntermediaryModJars by testMcTask<Copy> {
        dependsOn(intermediaryTestJar)

        project.afterEvaluate {
            val dependencyModJars = getUnmappedModDependencies()
            from(intermediaryTestJarFile, *dependencyModJars.toTypedArray())

            val intermediaryModDir = intermediaryTestsRunDir.resolve("mods")
            into(intermediaryModDir)

            doLast {
                // Make sure we don't leave any old jars from older versions
                intermediaryModDir.deleteEverythingExceptNamedSame(dependencyModJars + intermediaryTestJarFile)
            }
        }
    }

    private val supplyMinecraftIntermediaryJar by testMcTask<Copy> {
        project.afterEvaluate {
            val intJar = getIntermediaryMcJar()!!
            val loaderIntermediaryMcJarCache = intermediaryTestsRunDir.resolve(".fabric").resolve("remappedJars")
            val currentVersionIntMcDir = loaderIntermediaryMcJarCache.resolve("minecraft-${getMinecraftVersion()}")

            from(intJar)
            into(currentVersionIntMcDir)

            // When not in development mode, Loader looks at the INTERMEDIARY jar as if it's the official jar, in an attempt to find something to remap.
            // It first checks if it has already remapped the jar to intermediary,
            // in <run_dir>/.fabric/remappedJars/minecraft-<mc_version>//intermediary-<official_jar_name>.
            // SO if we already put the intermediary jar in there, it won't need to remap anything and will just use the intermediary jar straightaway.
            val newName = "intermediary-${intJar.name}"
            rename { newName }

            doLast {
                loaderIntermediaryMcJarCache.deleteEverythingExceptNamedSame(listOf(File(newName)))
            }
        }
    }


    private val supplyProductionLaunchConfig by transformTask(
        fromFile = getDevLaunchConfig(),
        toFile = intermediaryTestsRunDir.resolve("launch.cfg")
    ) {
        it.replace("fabric.development=true", "fabric.development=false")
    }

    init {
        createTestMcTask<Test>("intermediaryTest"){
            configureForIntegrationTesting()
            workingDir = intermediaryTestsRunDir
            project.afterEvaluate {
                setClasspathToIntermediary(this, this@createTestMcTask)
            }

            dependsOn(supplyIntermediaryModJars, supplyMinecraftIntermediaryJar, supplyProductionLaunchConfig)
        }
    }

    private fun setClasspathToIntermediary(project: Project, test: Test) {
        val remappedDependencyMods = getLoom().remappedModCache

        // These contain the named binaries of our mod
        val modAssemblyDirectories = getAllAssemblyTasks().map { it.destinationDir }

        val namedJarsToBeFilteredOut = listOf(
            getNamedMcJar()!!,
            remappedDependencyMods
        ) + modAssemblyDirectories


        val addedIntJars = project.files(getIntermediaryMcJar()!!, getIntermediaryLoaderJar(), intermediaryTestJarFile)

        fun File.filtersOutClasspathEntry(entry: File): Boolean {
            return if (isDirectory) entry.absolutePath.startsWith(absolutePath)
            else absolutePath == entry.absolutePath
        }

        // Remove named cp entries and add intermediary cp entries
        test.classpath = test.classpath.filter { cpEntry ->
            namedJarsToBeFilteredOut.all { !it.filtersOutClasspathEntry(cpEntry) }
        } + addedIntJars
    }

}