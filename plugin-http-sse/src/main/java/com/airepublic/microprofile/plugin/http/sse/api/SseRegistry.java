package com.airepublic.microprofile.plugin.http.sse.api;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import javax.enterprise.inject.spi.CDI;

public class SseRegistry implements Closeable {
    private final Map<String, Method> producerMethods = new ConcurrentHashMap<>();
    private final Map<String, Object> producerObjects = new ConcurrentHashMap<>();
    private final Map<URI, Future<Void>> consumers = new ConcurrentHashMap<>();


    public void addSseProducer(final String path, final Class clazz, final Method method) {
        if (producerMethods.containsKey(path)) {
            throw new IllegalArgumentException("Path '" + path + "' is already registered as SseProducer!");
        }

        if (!producerObjects.containsKey(clazz)) {
            producerObjects.put(path, CDI.current().select(clazz).get());
        }

        producerMethods.put(path, method);
    }


    public void removeSseProducer(final String path) {
        producerMethods.remove(path);
    }


    public Method getSseProducer(final String path) {
        return producerMethods.get(path);
    }


    public Object getObject(final String path) {
        return producerObjects.get(path);
    }


    public void addSseConsumer(final URI uri, final Future<Void> sseConsumer) {
        consumers.put(uri, sseConsumer);
    }


    public Future<Void> getSseConsumer(final URI uri) {
        return consumers.get(uri);
    }


    public Future<Void> removeConsumer(final URI uri) {
        return consumers.remove(uri);
    }


    public Map<URI, Future<Void>> getAllConsumers() {
        return consumers;
    }


    @Override
    public void close() {
        getAllConsumers().forEach((uri, future) -> future.cancel(true));
        consumers.clear();
        producerMethods.clear();
        producerObjects.clear();
    }
}
