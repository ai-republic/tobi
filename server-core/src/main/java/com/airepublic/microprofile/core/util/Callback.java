package com.airepublic.microprofile.core.util;

@FunctionalInterface
public interface Callback<R> {
    void process(R result, final Throwable exception, boolean hasException);

}
