package io.github.natanfudge

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import java.io.File
import kotlin.reflect.KClass


internal interface ProjectContext {
    val project: Project

    fun createBuildDirectory(name: String): File {
        val file = project.buildDir.resolve("intermediary_tests")
        file.mkdirs()
        return file
    }

}

internal inline fun <reified T : Task> ProjectContext.testMcTask(crossinline config: T.() -> Unit) = project.tasks.creating(T::class) {
    group = "TestMC"
    config()
}

internal inline fun <reified T : Task> ProjectContext.createTestMcTask(name: String, crossinline config: T.() -> Unit) = project.tasks.create(name, T::class) {
    group = "TestMC"
    config()
}

internal  fun < T : Task> ProjectContext.testMcTask(type: KClass<T>, config: T.() -> Unit) = project.tasks.creating(type) {
    group = "TestMC"
    config()
}

//internal inline fun <T : Task> ProjectContext.createTestMcTask(type: Class<T>,  crossinline config: T.() -> Unit): T {
//    return project.tasks.creating(name, type) {
//        group = "TestMC"
//        config()
//    }
//}


internal fun ProjectContext.transformTask(fromFile: File, toFile: File, transform: (String) -> String) = testMcTask<Task> {
    inputs.file(fromFile)
    outputs.file(toFile)

    doLast {
        toFile.writeText(transform(fromFile.readText()))
    }
}