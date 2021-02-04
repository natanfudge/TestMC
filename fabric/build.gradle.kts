import groovy.lang.Closure
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.util.Constants.MOD_COMPILE_ENTRIES
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.IOException
import java.util.zip.ZipFile

plugins {
    id("com.github.johnrengelman.shadow") version "5.0.0"
}

//val shadow: Configuration = configurations["shadow"]
//
//architectury {
//    platformSetupLoomIde()
//}
//
//java {
//    sourceCompatibility = JavaVersion.VERSION_1_8
//    targetCompatibility = JavaVersion.VERSION_1_8
//}



fun prop(str: String) = project.property(str).toString()

//base {
//    archivesBaseName = prop("archives_base_name")
//}
//
//version = prop("mod_version")
//group = prop("maven_group")


repositories {
//    jcenter()
    maven(url = "https://dl.bintray.com/shedaniel/cloth")
    maven(url = "https://jitpack.io") {
        metadataSources {
            mavenPom()
            artifact()
        }
        content {
            includeGroupByRegex("com.github.Chocohead")
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
    modRuntime("com.github.Chocohead:Data-Breaker-Lower:24be1a2") {
        exclude(module = "fabric-loader")
        exclude(group = "net.fabricmc.fabric-api")
    }


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    testImplementation("net.fabricmc:dev-launch-injector:0.2.1+build.8")

//    modImplementation("net.fabricmc:fabric-language-kotlin:${prop("flk_version")}")

    testImplementation(project(":common"))
    compileOnly(project(path = ":common")) {
        isTransitive = false
    }
    runtimeOnly(project(path = ":common", configuration = "transformDevelopmentFabric")) {
        isTransitive = false
    }
    shadow(project(path = ":common", configuration = "transformProductionFabric")) {
        isTransitive = false
    }

    // PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
    // You may need to force-disable transitiveness on them.
}

tasks.withType<ProcessResources> {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
//TODO: i'm done. create a gradle plugin.




val shadowJar by tasks.existing(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    configurations = listOf(project.configurations.shadow.get())
    classifier = "shadow"
}


//TestMc(shadowJar.get()).init()

val remapJar by tasks.existing(RemapJarTask::class) {
    dependsOn(shadowJar)
    input.set(shadowJar.get().archivePath)
    classifier = "fabric"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

