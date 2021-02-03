import io.github.natanfudge.MinecraftLifecycle
import io.github.natanfudge.impl.mixinhandlers.ServerMixinHandler
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test


class TestStuff {
    @Test
    fun testClient() {
        MinecraftLifecycle.startClient {
            waitForGameToLoad()
            openDemoWorld()
        }
    }
}

class TestStuff2 {
    @Test
    fun testServer() {
        MinecraftLifecycle.startServer {
            waitForWorldToLoad()
        }
    }
}
