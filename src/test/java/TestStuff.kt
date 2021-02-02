import io.github.natanfudge.MinecraftLifecycle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test


class TestStuff {

    @Test
    fun test() {
        MinecraftLifecycle.startClient {
            launch {
                println("Running user code!")
                waitForGameToLoad {
                    throw RuntimeException("oh no i fucked up")
                }
//                println("Game loaded")
//                openDemoWorld()
//                println("World opened")
            }
        }
    }


    @Test
    fun tryToThrow1() {
        GlobalScope.launch {
            throw RuntimeException()
        }
        Thread.sleep(500)
    }


    @Test
    fun tryToThrow2() {
        GlobalScope.launch(CoroutineExceptionHandler { _, throwable -> throw throwable }) {
            throw RuntimeException()
        }
        Thread.sleep(500)
    }

    @Test
    fun tryToThrow3() {
        GlobalScope.launch(CoroutineExceptionHandler { _, throwable -> throw throwable }) {
            Thread.setDefaultUncaughtExceptionHandler { _, e -> throw e }
            throw RuntimeException()
        }
        Thread.sleep(1000)
    }
}