import io.github.natanfudge.FabricPlatform
import io.github.natanfudge.startClient
import io.github.natanfudge.startServer
import org.junit.jupiter.api.Test


class TestStuff {
    @Test
    fun testClient() {
        FabricPlatform().startClient {
            waitForGameToLoad()
            openDemoWorld()
        }

    }
}

class TestStuff2 {
    @Test
    fun testServer() {
        FabricPlatform().startServer {
            waitForWorldToLoad()
        }
    }
}
