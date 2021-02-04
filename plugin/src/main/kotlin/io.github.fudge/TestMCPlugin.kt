package io.github.fudge

import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

class FFLoomPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val loom = project.extensions.getByType(LoomGradleExtension::class.java)
    }
}

class TestMc(private val prodJarTask: Jar, override val project: Project) : ProjectContext {
    private val intermediaryTestsRunDir = createBuildDirectory("intermediary_tests")
    private val namedTestsRunDir = createBuildDirectory("named_tests")

    private fun Test.configureForIntegrationTesting() {
        // Without this tests don't actually print without the -i flag
        fixConsoleOutput()
        // Required
        useJUnitPlatform()
        // Without this tests won't rerun when no changes occur
        outputs.upToDateWhen { false }

        setForkEvery(1)
        this.maxParallelForks = 2
    }

    @Suppress("unused")
    private val namedTest by testMcTask<Test> {
        configureForIntegrationTesting()
        workingDir = namedTestsRunDir
        dependsOn(supplyDevLaunchConfig)
    }

    private val testJar by testMcTask<Jar> {
        prodJarTask.inputs.files.forEach { from(it) }
        getAllAssemblyTasks().filter { it.task.isTestAssemblyTask() }
            .forEach { from(it.destinationDir) }
        archiveClassifier.set("test-named")
    }

    private val testRemapJar by testMcTask<RemapJarTask> {
        dependsOn(testJar)
        input.set(testJar.outputs.files.singleFile)
        archiveClassifier.set("test-intermediary")
        addNestedDependencies.set(false)
    }


    /**
     * Named tests are just normal tests using a different directory.
     * Intermediary tests are special. They remove everything that uses 'named' identifiers, and replace it with the equivalent that uses intermediary identifiers.
     * For Minecraft: that is the mc jar in the global loom cache that is mapped to intermediary;
     * For mod dependencies: that is the modCompile dependencies that were fetched by gradle and put in the global loom cache, before Loom touched them;
     * For the mod itself: that is the output of the remapJar task.
     */


    //TODO: seperate intermediary-based tasks to their own file
    val supplyIntermediaryModJars by testMcTask<Copy> {
        dependsOn(testRemapJar)

        val modIntermediaryJar = getModTestIntermediaryJar()
        afterEvaluate {
            val dependencyModJars = getOriginalModDependencies()
            from(modIntermediaryJar, *dependencyModJars.toTypedArray())

            val intermediaryModDir = intermediaryTestsRunDir.resolve("mods")
            into(intermediaryModDir)

            doLast {
                // Make sure we don't leave any old jars from older versions
                intermediaryModDir.deleteEverythingExceptNamedSame(dependencyModJars + modIntermediaryJar)
            }
        }

    }


    val supplyMinecraftIntermediaryJar by tasks.creating(Copy::class) {
        group = "integration tests"
        afterEvaluate {
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

    val supplyProductionLaunchConfig by transformTask(
        group = "integration tests",
        fromFile = getDevLaunchConfig(),
        toFile = intermediaryTestsRunDir.resolve("launch.cfg")
    ) {
        it.replace("fabric.development=true", "fabric.development=false")
    }


    val intermediaryTest by testMcTask<Test> {
        configureForIntegrationTesting()
        workingDir = intermediaryTestsRunDir
        project.afterEvaluate {
            setClasspathToIntermediary(this, this@testMcTask)
        }

        dependsOn(supplyIntermediaryModJars, supplyMinecraftIntermediaryJar, supplyProductionLaunchConfig)
    }

    val supplyDevLaunchConfig by tasks.creating(Copy::class) {
        group = "integration tests"
        from(getDevLaunchConfig())
        into(namedTestsRunDir)
    }


//TODO: use unique directories for each game launch so many instances can run at the same time.
// (first make sure that doing that will allow multiple instances though, it may lock on some global stuff)


    /** Must only be called in afterEvaluate! */
    fun getIntermediaryMcJar(): File? {
        return try {
            getLoom().minecraftMappedProvider.intermediaryJar
        } catch (e: NullPointerException) {
            null
        }
    }

    fun getNamedMcJar(): File? {
        return getLoom().minecraftMappedProvider.mappedJar
    }

    fun getLoom() = extensions.getByType(LoomGradleExtension::class.java)

    var _originalModsCache: List<File>? = null

    fun getOriginalModDependencies(): List<File> {
        if (_originalModsCache == null) {
            _originalModsCache = MOD_COMPILE_ENTRIES.flatMap { modCompileEntry ->
                val sourceConfig = project.configurations.getByName(modCompileEntry.sourceConfiguration)
                sourceConfig.resolvedConfiguration.resolvedArtifacts.filter { isFabricMod(it) }
                    .map { it.file }
            }
        }
        return _originalModsCache!!
    }

    fun getIntermediaryLoaderJar() = getOriginalModDependencies().first { it.name.startsWith("fabric-loader-") }

    /**
     * Checks if an artifact is a fabric mod, according to the presence of a fabric.mod.json.
     */
    fun isFabricMod(artifact: ResolvedArtifact): Boolean {
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

    class DestinationTask(val destinationDir: File, val task: Task)

    fun getAllAssemblyTasks() =
        (project.tasks.withType<ProcessResources>()).map { DestinationTask(it.destinationDir, it) } +
                project.tasks.withType<AbstractCompile>().map { DestinationTask(it.destinationDir, it) }

    fun Task.isTestAssemblyTask() = name.startsWith("processTestResources") || name.startsWith("compileTest")

    fun setClasspathToIntermediary(project: Project, test: Test) {
        val remappedDependencyMods = getLoom().remappedModCache

        // These contain the named binaries of our mod
        val modAssemblyDirectories = getAllAssemblyTasks().map { it.destinationDir }

        val namedJarsToBeFilteredOut = listOf(
            getNamedMcJar()!!,
            remappedDependencyMods
        ) + modAssemblyDirectories


        val addedIntJars =
            project.files(getIntermediaryMcJar()!!, getIntermediaryLoaderJar(), getModTestIntermediaryJar())

        fun File.filtersOutClasspathEntry(entry: File): Boolean {
            return if (isDirectory) entry.absolutePath.startsWith(absolutePath)
            else absolutePath == entry.absolutePath
        }

        // Remove named cp entries and add intermediary cp entries
        test.classpath = test.classpath.filter { cpEntry ->
            namedJarsToBeFilteredOut.all { !it.filtersOutClasspathEntry(cpEntry) }
        } + addedIntJars
    }

    fun getModTestIntermediaryJar(): File {
        return testRemapJar.archiveFile.get().asFile
    }

    fun getMinecraftVersion() = getLoom().minecraftProvider.minecraftVersion

    fun getDevLaunchConfig() = getLoom().devLauncherConfig

    fun File.deleteEverythingExceptNamedSame(except: List<File>) {
        val exceptSet = except.map { it.name }.toSet()
        walk().forEach {
            if (!it.isDirectory && it.name !in exceptSet) it.delete()
        }
    }

    fun transformTask(group: String, fromFile: File, toFile: File, transform: (String) -> String) = tasks.creating {
        this.group = group
        inputs.file(fromFile)
        outputs.file(toFile)

        doLast {
            toFile.writeText(transform(fromFile.readText()))
        }
    }
}