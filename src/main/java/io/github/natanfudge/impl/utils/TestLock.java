package io.github.natanfudge.impl.utils;


import java.lang.reflect.Field;

public class TestLock {


    public static Object instance = null;

    /**
     * Retrieve some TRUELY singleton Object. There can only be one, even across different class loaders.
     */
    public synchronized static Object getInstance() {
        ClassLoader myClassLoader = Object.class.getClassLoader();

        if (instance == null) {
            // The point is that we will only use ONE classloader.
            // the instance will be stored in the "root class loader", aka the appClassLoader.
            ClassLoader rootClassLoader = getRootClassLoader();
            if (myClassLoader == rootClassLoader) {
                // If this is the appClassLoader then we can just use normal java to do this, woohoo.
                instance = new Object();
            } else {
                // If this is not appClassLoader, then we need to get the instance from appClassLoader using reflection.
                // If there is no instance yet, we will put one there.
                try {
                    Class<?> absoluteSingletonClassInAppLoader = rootClassLoader.loadClass(TestLock.class.getName());
                    Field instanceFieldInAppLoader = absoluteSingletonClassInAppLoader.getField("instance");
                    Object instanceInAppLoader =  instanceFieldInAppLoader.get(null);
                    if (instanceInAppLoader == null) {
                        Object newInstance = new Object();
                        instanceFieldInAppLoader.set(null, newInstance);
                        instance = newInstance;
                    } else {
                        instance = instanceInAppLoader;
                    }
                } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }

        return instance;
    }

    private static ClassLoader getRootClassLoader() {
        // Minecraft class loader parent hierarchy looks like this:
        // null -> platformClassLoader -> appClassLoader -> someFLoaderBsLoader1 -> someFLoaderBsLoader2 -> minecraftClassLoader
        // Test class loader parent hierarchy looks like this:
        // null -> platformClassLoader -> appClassLoader.
        // Therefore the best way to find the common ancestor (appClassLoader) is going up and up until loader.getParent().getParent() == null
        ClassLoader currentLoader = TestLock.class.getClassLoader();
        while(!isRootClassLoader(currentLoader)) {
            currentLoader = currentLoader.getParent();
        }
        return currentLoader;
    }

    private static boolean isRootClassLoader(ClassLoader loader) {
        return loader.getParent().getParent() == null;
    }

}