import groovy.lang.Closure
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.Constants.MOD_COMPILE_ENTRIES
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.IOException
import java.nio.file.Files
import java.util.zip.ZipFile

plugins {
    id("fabric-loom") version ("0.5-SNAPSHOT")
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}



fun prop(str: String) = project.property(str).toString()

base {
    archivesBaseName = prop("archives_base_name")
}

version = prop("mod_version")
group = prop("maven_group")


val intermediaryTestsRunDir = file("$buildDir/intermediary_tests")
val namedTestsRunDir = file("$buildDir/named_tests")
if (!intermediaryTestsRunDir.exists()) intermediaryTestsRunDir.mkdirs()
if (!namedTestsRunDir.exists()) namedTestsRunDir.mkdirs()

val copyModIntJars by tasks.creating(Copy::class) {
    group = "integrated Tests"
    dependsOn("remapJar")
    val remapJarTask = tasks.withType<RemapJarTask>().find { it.name == "remapJar" }
        ?: error("Could not find remapJar task. Make sure Loom is applied!")
    afterEvaluate {
        from(remapJarTask.archiveFile, *getOriginalModDependencies().toTypedArray())
    }

    val modDir = "$intermediaryTestsRunDir/mods"
    into(modDir)

    val currentModJarName = remapJarTask.archiveFile.get().asFile.name
    doLast {
        // Make sure we don't leave any old mod jars
        for (fileName in file(modDir).list()) {
            if (fileName == currentModJarName) continue
            if (fileName.startsWith("${base.archivesBaseName}-") && fileName.endsWith(".jar")) {
                val oldFile = file("$modDir/$fileName")
                println("Deleting old mc jar: $oldFile")
                oldFile.delete()
            }
        }
    }
}


val copyMinecraftIntJar by tasks.creating(Copy::class) {
    group = "integrated tests"
    afterEvaluate {
        val intJar = getIntermediaryMcJar()!!
        // When not in development mode, Loader looks at the INTERMEDIARY jar as if it's the official jar, in an attempt to find something to remap.
        // It first checks if it has already remapped the jar to intermediary,
        // in <run_dir>/.fabric/remappedJars/minecraft-<mc_version>//intermediary-<official_jar_name>.
        // SO if we already put the intermediary jar in there, it won't need to remap anything and will just use the intermediary jar straightaway.
        val newName = "intermediary-${intJar.name}"
        from(intJar)
        val mcVersion = getLoom().minecraftProvider.minecraftVersion
        val mappedMcDir = "$intermediaryTestsRunDir/.fabric/remappedJars/"
        into("$mappedMcDir/minecraft-$mcVersion")

        rename { newName }

        doLast {
            Files.walk(File(mappedMcDir).toPath()).forEach {
                if (Files.isDirectory(it)) return@forEach
                if (it.fileName.toString() != newName) Files.delete(it)
            }
        }
    }
}
val originalConfigFile = getLoom().devLauncherConfig

val addProductionLaunchConfig by tasks.creating {
    group = "integrated tests"

    val newConfigFile = File("$intermediaryTestsRunDir/launch.cfg")
    inputs.file(originalConfigFile)
    outputs.file(newConfigFile)

    doLast {
        val originalConfig = originalConfigFile.readText()
        val newConfig = originalConfig.replace("fabric.development=true","fabric.development=false")
        newConfigFile.writeText(newConfig)
    }
}



val intermediaryTest by tasks.creating(Test::class){
    group = "integrated tests"

    workingDir = intermediaryTestsRunDir
    afterEvaluate {
        setClasspathToIntermediary(this, this@Build_gradle, this@creating)
    }

    dependsOn(copyModIntJars, copyMinecraftIntJar,addProductionLaunchConfig)
}

val addDevLaunchConfig by tasks.creating(Copy::class) {
    group = "integrated tests"
    from(originalConfigFile)
    into(namedTestsRunDir)
}

tasks.withType<Test> {
    fixConsoleOutput()
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

tasks.getByName<Test>("test"){
    group = "integrated tests"
    workingDir = namedTestsRunDir
    dependsOn(addDevLaunchConfig)
}

//val namedTest by tasks.creating


//TODO: find the original launch.cfg, and then with a regex replace fabric.development to false, and inject that into the mod jar and use it in the test as a resource.

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    mappings("net.fabricmc:yarn:${prop("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    testImplementation("net.fabricmc:dev-launch-injector:0.2.1+build.8")

    // PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
    // You may need to force-disable transitiveness on them.
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

fun Test.fixConsoleOutput() {
    testLogging {
        // set options for log level LIFECYCLE
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT
        )
        exceptionFormat = (TestExceptionFormat.FULL)
        showExceptions = (true)
        showCauses = true
        showStackTraces = true

        // set options for log level DEBUG and INFO
        debug {
            events(
                TestLogEvent.STARTED,
                TestLogEvent.FAILED,
                TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_ERROR
            )
            TestLogEvent.STANDARD_OUT
            exceptionFormat = TestExceptionFormat.FULL
        }
        info.events = debug.events
        info.exceptionFormat = debug.exceptionFormat

        afterSuite(object : Closure<Any?>(null) {
            fun doCall(desc: TestDescriptor, result: TestResult) {
                if (desc.parent == null) { // will match the outermost suite
                    val output =
                        "Results: ${result.resultType} (${result.testCount} tests, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped)"
                    val startItem = "|  "
                    val endItem = "  |"
                    val repeatLength = startItem.length + output.length + endItem.length
                    println(
                        '\n' + ("-".repeat(repeatLength)) + '\n' + startItem + output + endItem + '\n' + ("-".repeat(
                            repeatLength
                        ))
                    )
                }
            }
        })
    }
}

fun getIntermediaryMcJar(): File? {
    return getLoom().minecraftMappedProvider.intermediaryJar
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

fun setClasspathToIntermediary(
    project: Project,
    buildGradle: Build_gradle,
    test: Test
) {
    val remappedDependencyMods = getLoom().remappedModCache

    val namedJarsToBeFilteredOut = listOf(
        project.tasks.processResources.get().destinationDir,
        getNamedMcJar()!!,
        remappedDependencyMods
    ) + project.tasks.withType<AbstractCompile>()
        .filter { !it.name.startsWith("compileTest") }
        .map {
            it.destinationDir
        }


    fun File.filtersOutClasspathEntry(entry: File): Boolean {
        val filters = if (this.isDirectory) entry.absolutePath.startsWith(this.absolutePath)
        else this.absolutePath == entry.absolutePath
        return filters
    }

    val addedIntJars = project.files(getIntermediaryMcJar()!!, buildGradle.getIntermediaryLoaderJar())

    println("Adding jars to classpath: ${addedIntJars.map { it.name }}")

    test.classpath = test.classpath.filter { cpEntry ->
        val filtered = namedJarsToBeFilteredOut.any { it.filtersOutClasspathEntry(cpEntry) }
        !filtered
    } + addedIntJars
}