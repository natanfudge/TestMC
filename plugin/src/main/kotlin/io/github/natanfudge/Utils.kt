package io.github.natanfudge

import groovy.lang.Closure
import org.gradle.api.Task
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestResult
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File

internal fun Test.fixConsoleOutput() {
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

private val cachedValues = mutableMapOf<String, Any?>()
internal fun <T> cachedValue(key: String, ctr: () -> T): T {
    return cachedValues.computeIfAbsent(key) { ctr() } as T
}

internal fun File.deleteEverythingExceptNamedSame(except: List<File>) {
    val exceptSet = except.map { it.name }.toSet()
    walk().forEach {
        if (!it.isDirectory && it.name !in exceptSet) it.delete()
    }
}

internal class DestinationTask(val destinationDir: File, val task: Task)

internal fun ProjectContext.getAllAssemblyTasks() =
    (project.tasks.withType<ProcessResources>()).map { DestinationTask(it.destinationDir, it) } +
            project.tasks.withType<AbstractCompile>().map { DestinationTask(it.destinationDir, it) }

internal fun Task.isTestAssemblyTask() = name.startsWith("processTestResources") || name.startsWith("compileTest")
