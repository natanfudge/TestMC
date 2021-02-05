package io.github.natanfudge

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class TestMCPlugin : Plugin<Project> {
    override fun apply(project: Project) {
    }
}


internal fun Test.configureForIntegrationTesting() {
    // Without this tests don't actually print without the -i flag
    fixConsoleOutput()
    // Required
    useJUnitPlatform()
    // Without this tests won't rerun when no changes occur
    outputs.upToDateWhen { false }

    setForkEvery(1)
    this.maxParallelForks = 2
}



//TODO: use unique directories for each game launch so many instances can run at the same time.
// (first make sure that doing that will allow multiple instances though, it may lock on some global stuff)
