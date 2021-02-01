package io.github.natanfudge.impl.utils;


import java.lang.reflect.Field;

public class TestLock {


    public static Object instance = null;

    /**
     * Retrieve an instance of AbsoluteSingleton from the original classloader. This is a true
     * Singleton, in that there will only be one instance of this object in the virtual machine,
     * even though there may be several copies of its class file loaded in different classloaders.
     */
    public synchronized static Object getInstance() {
        ClassLoader myClassLoader = Object.class.getClassLoader();

        if (instance == null) {
            ClassLoader rootClassLoader = getRootClassLoader();
            if (myClassLoader == rootClassLoader) {
                instance = new Object();
            } else {
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