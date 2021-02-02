import groovy.lang.Closure
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.Constants.MOD_COMPILE_ENTRIES
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.IOException
import java.util.zip.ZipFile

plugins {
    id("fabric-loom")
    id("maven-publish")
    kotlin("jvm")
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


repositories{
//    jcenter()
    maven ( url  ="https://dl.bintray.com/shedaniel/cloth" )
    maven (url = "https://jitpack.io"){
        metadataSources {
            mavenPom()
            artifact()
        }
        content {
            includeGroupByRegex ("com.github.Chocohead")
        }
    }
}


dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    mappings("net.fabricmc:yarn:${prop("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop("loader_version")}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
    modImplementation("net.fabricmc:dev-launch-injector:0.2.1+build.8")
    modImplementation("me.shedaniel:architectury:${prop("architectury_version")}:fabric")
    modRuntime ("com.github.Chocohead:Data-Breaker-Lower:24be1a2"){
        exclude (module = "fabric-loader")
        exclude (group = "net.fabricmc.fabric-api")
    }


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    testImplementation("net.fabricmc:dev-launch-injector:0.2.1+build.8")

    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("flk_version")}")

    // PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
    // You may need to force-disable transitiveness on them.
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}


/**
 * Named tests are just normal tests using a different directory.
 * Intermediary tests are special. They remove everything that uses 'named' identifiers, and replace it with the equivalent that uses intermediary identifiers.
 * For Minecraft: that is the mc jar in the global loom cache that is mapped to intermediary;
 * For mod dependencies: that is the modCompile dependencies that were fetched by gradle and put in the global loom cache, before Loom touched them;
 * For the mod itself: that is the output of the remapJar task.
 */

val intermediaryTestsRunDir = buildDir.resolve("intermediary_tests")
val namedTestsRunDir = buildDir.resolve("named_tests")
intermediaryTestsRunDir.mkdirs()
namedTestsRunDir.mkdirs()


val supplyIntermediaryModJars by tasks.creating(Copy::class) {
    group = "integrated Tests"
    dependsOn("remapJar")

    val modIntermediaryJar = getModIntermediaryJar()
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
    group = "integrated tests"
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
    group = "integrated tests",
    fromFile = getDevLaunchConfig(),
    toFile = intermediaryTestsRunDir.resolve("launch.cfg")
) {
    it.replace("fabric.development=true", "fabric.development=false")
}


val intermediaryTest by tasks.creating(Test::class) {
    group = "integrated tests"

    workingDir = intermediaryTestsRunDir
    afterEvaluate {
        setClasspathToIntermediary(this, this@Build_gradle, this@creating)
    }

    dependsOn(supplyIntermediaryModJars, supplyMinecraftIntermediaryJar, supplyProductionLaunchConfig)
}

val supplyDevLaunchConfig by tasks.creating(Copy::class) {
    group = "integrated tests"
    from(getDevLaunchConfig())
    into(namedTestsRunDir)
}

// Apply to all test types
tasks.withType<Test> {
    // Without this tests don't actually print without the -i flag
    fixConsoleOutput()
    // Required
    useJUnitPlatform()
    // Without this tests won't rerun when no changes occur
    outputs.upToDateWhen { false }
}

tasks.getByName<Test>("test") {
    group = "integrated tests"
    workingDir = namedTestsRunDir
    dependsOn(supplyDevLaunchConfig)
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


fun setClasspathToIntermediary(
    project: Project,
    buildGradle: Build_gradle,
    test: Test
) {
    val remappedDependencyMods = getLoom().remappedModCache

    val nonTestResourceDirectories = project.tasks.withType<ProcessResources>()
        .filter { !it.name.startsWith("processTestResources") }
        .map { it.destinationDir }
    val nonTestBinaryDirectories = project.tasks.withType<AbstractCompile>()
        .filter { !it.name.startsWith("compileTest") }
        .map { it.destinationDir }

    val namedJarsToBeFilteredOut = listOf(
        getNamedMcJar()!!,
        remappedDependencyMods
    ) + nonTestResourceDirectories + nonTestBinaryDirectories


    val addedIntJars = project.files(getIntermediaryMcJar()!!, buildGradle.getIntermediaryLoaderJar(), getModIntermediaryJar())

    fun File.filtersOutClasspathEntry(entry: File): Boolean {
        return if (isDirectory) entry.absolutePath.startsWith(absolutePath)
        else absolutePath == entry.absolutePath
    }

    // Remove named cp entries and add intermediary cp entries
    test.classpath = test.classpath.filter { cpEntry ->
       namedJarsToBeFilteredOut.all { !it.filtersOutClasspathEntry(cpEntry) }
    } + addedIntJars
}

fun getModIntermediaryJar(): File {
    val remapJarTask = tasks.withType<RemapJarTask>().find { it.name == "remapJar" }
        ?: error("Could not find remapJar task. Make sure Loom is applied!")
    return remapJarTask.archiveFile.get().asFile
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