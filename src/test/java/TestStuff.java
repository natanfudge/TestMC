import net.fabricmc.devlaunchinjector.Main;
import io.github.natanfudge.impl.utils.TestLock;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

public class TestStuff {
    @Test
    public void test() throws Throwable {
        AtomicReference<Throwable> mcError = new AtomicReference<>(null);

        Object titleScreenLock = TestLock.getInstance();

        startMinecraft(mcError, titleScreenLock);

        waitUntilTestCanBeCompleted(titleScreenLock);

        if (mcError.get() != null) {
            throw mcError.get();
        }

    }

    private void startMinecraft(AtomicReference<Throwable> mcError, Object titleScreenLock) {
        System.setProperty("fabric.dli.config", "launch.cfg");
        System.setProperty("fabric.dli.env", "client");
        System.setProperty("fabric.dli.main", "net.fabricmc.loader.launch.knot.KnotClient");
        new Thread(() -> {
            try {
                Main.main(new String[]{});
            } catch (Throwable throwable) {
                mcError.set(throwable);
                synchronized (titleScreenLock) {
                    titleScreenLock.notify();
                }
            }
        }).start();
    }

    private void waitUntilTestCanBeCompleted(Object titleScreenLock) {
        synchronized (titleScreenLock) {
            try {
                // Calling wait() will block this thread until another thread
                // calls notify() on the object.
                titleScreenLock.wait();
            } catch (InterruptedException e) {
                // Happens if someone interrupts your thread.
            }
        }
    }


}
