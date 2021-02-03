import io.github.natanfudge.MinecraftLifecycle
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Test


class TestStuff {

    @Test
    fun test() {
        MinecraftLifecycle.startClient {
            println("Running user code!")
            onGameLoaded {
                openDemoWorld {
                    closeMinecraft()
                }
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