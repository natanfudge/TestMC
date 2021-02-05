package io.github.natanfudge

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getValue

class NamedEnvironment(override val project: Project) : ProjectContext {
    private val namedTestsRunDir = createBuildDirectory("named_tests")

    private val supplyDevLaunchConfig by testMcTask<Copy> {
        from(getDevLaunchConfig())
        into(namedTestsRunDir)
    }

    init {
        createTestMcTask<Test>("namedTest") {
            configureForIntegrationTesting()
            workingDir = namedTestsRunDir
            dependsOn(supplyDevLaunchConfig)
        }
    }


}