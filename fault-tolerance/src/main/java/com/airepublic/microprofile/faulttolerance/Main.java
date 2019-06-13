package com.airepublic.microprofile.faulttolerance;

import java.lang.reflect.Method;

public class Main {
    static class MyCL extends ClassLoader {
        public MyCL() {
        }


        public MyCL(final ClassLoader parent) {
            super(parent);
        }
    }


    public static void main(final String[] args) throws Exception {
        final MyCL cl = new MyCL();
        Thread.currentThread().setContextClassLoader(cl);
        final Class<?> clazz = cl.loadClass("com.airepublic.microprofile.faulttolerance.Test");
        final Method main = clazz.getMethod("main", String[].class);
        main.invoke(clazz.getConstructor().newInstance(), (String) null);
    }
}
