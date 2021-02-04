package io.github.fudge

import org.gradle.api.PolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.kotlin.dsl.PolymorphicDomainObjectContainerCreatingDelegateProvider
import org.gradle.kotlin.dsl.creating
import java.io.File
import kotlin.reflect.KClass


interface ProjectContext {
    val project: Project

    fun createBuildDirectory(name: String): File {
        val file = project.buildDir.resolve("intermediary_tests")
        file.mkdirs()
        return file
    }

}

inline fun <reified T : Task> ProjectContext.testMcTask(crossinline config: T.() -> Unit) = project.tasks.creating(T::class) {
    group = "TestMC"
    config()
}
