package com.airepublic.microprofile.faulttolerance;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.eclipse.microprofile.faulttolerance.Retry;

public class Test {

    @Retry
    public void test(final boolean throwException) {
        if (throwException) {
            throw new RuntimeException();
        }

    }


    public static void main(final String[] args) throws ClassNotFoundException {
        System.out.println(Test.class.getClassLoader());

        final SeContainer cdiContainer = SeContainerInitializer.newInstance().initialize();

        final Test test = cdiContainer.select(Test.class).get();

        test.test(true);
        test.test(false);

    }

}
