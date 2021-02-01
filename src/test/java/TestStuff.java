import io.github.natanfudge.impl.EndTestException;
import io.github.natanfudge.impl.utils.TestLock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestStuff {
    @Test
    public void test() throws Throwable {
//        AtomicReference<Throwable> mcError = new AtomicReference<>(null);

        Object titleScreenLock = TestLock.getInstance();

        //TODO: make the framework take a method reference (or even lambda), and run it in the proper mc classloader.
        startMinecraft();

//        waitUntilTestCanBeCompleted(titleScreenLock);


    }

    private void startMinecraft() throws Throwable {
        System.setProperty("fabric.dli.config", "launch.cfg");
        System.setProperty("fabric.dli.env", "client");
        System.setProperty("fabric.dli.main", "net.fabricmc.loader.launch.knot.KnotClient");
//        new Thread(() -> {
        try {
            //TODO: consider PR'ing a change to Fabric Loader that allows just straight up crashing instead of showing the swing GUI when there's an error.
            net.fabricmc.devlaunchinjector.Main.main(new String[]{});
        } catch (Exception e) {
            List<Throwable> involved = allInvolvedExceptions(e);
            if (involved.stream().anyMatch(t -> t.getClass().getName().equals("io.github.natanfudge.impl.EndTestException"))) return;
            throw e;
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

    private static List<Throwable> allInvolvedExceptions(Throwable e) {
        List<Throwable> involvedExceptions = new ArrayList<>();
        allInvolvedExceptionsRecur(e, involvedExceptions);
        return involvedExceptions;
    }

    private static void allInvolvedExceptionsRecur(Throwable e, List<Throwable> accumulatedList) {
        accumulatedList.add(e);
        if (e.getCause() != null) {
            allInvolvedExceptionsRecur(e.getCause(), accumulatedList);
        }
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
