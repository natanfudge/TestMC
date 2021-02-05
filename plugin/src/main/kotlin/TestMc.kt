import io.github.natanfudge.IntermediaryEnvironment
import io.github.natanfudge.NamedEnvironment
import io.github.natanfudge.ProjectContext
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class TestMc(private val prodJar: Jar, override val project: Project) : ProjectContext {

    fun configure() {
        NamedEnvironment(project)
        IntermediaryEnvironment(prodJar, project)
    }
}