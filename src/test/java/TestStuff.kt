import io.github.natanfudge.impl.utils.flatten
import net.fabricmc.devlaunchinjector.Main
import org.junit.jupiter.api.Test

class TestStuff {
    @Test
    fun test() {
//        AtomicReference<Throwable> mcError = new AtomicReference<>(null);
//        val titleScreenLock = getInstance()

        //TODO: make the framework take a method reference (or even lambda), and run it in the proper mc classloader.
        startMinecraft()

//        waitUntilTestCanBeCompleted(titleScreenLock);
    }

    private fun startMinecraft() {
        System.setProperty("fabric.dli.config", "launch.cfg")
        System.setProperty("fabric.dli.env", "client")
        System.setProperty("fabric.dli.main", "net.fabricmc.loader.launch.knot.KnotClient")
        //        new Thread(() -> {
        try {
            //TODO: consider PR'ing a change to Fabric Loader that allows just straight up crashing instead of showing the swing GUI when there's an error.
            Main.main(arrayOf())
        } catch (e: Exception) {
            val involved = allInvolvedExceptions(e)
            if (involved.any { it.javaClass.name == "io.github.natanfudge.impl.EndTestException" }) return
            throw e
            //                if (!(throwable instanceof EndTestException)) {
//                    throw throwable;
//                }
//                mcError.set(throwable);
//                synchronized (titleScreenLock) {
//                    titleScreenLock.notify();
//                }
        }
        //        }).start();
    }

//    private fun waitUntilTestCanBeCompleted(titleScreenLock: Any) {
//        synchronized(titleScreenLock) {
//            try {
//                // Calling wait() will block this thread until another thread
//                // calls notify() on the object.
//                titleScreenLock.wait()
//            } catch (e: InterruptedException) {
//                // Happens if someone interrupts your thread.
//            }
//        }
//    }

    companion object {
        private fun allInvolvedExceptions(e: Throwable) = flatten(e) { cause }
    }
}