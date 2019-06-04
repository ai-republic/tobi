package com.airepublic.microprofile.core;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.config.WebBeansFinder;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap implements IConfigConstants {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    private static SeContainer cdiContainer;

    @Inject
    private JavaServer javaServer;
    @Inject
    private ServerContext serverContext;


    private void startServer() throws IOException {
        serverContext.setCdiContainer(cdiContainer);
        javaServer.init();
        javaServer.run();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                javaServer.stop();
            }
        });

    }


    public static void start() throws IOException {
        try {

            WebBeansFinder.setSingletonService(new JavaServerDefaultSingletonService());
            cdiContainer = SeContainerInitializer.newInstance().setClassLoader(new ClassLoader("bootstrap", LOG.getClass().getClassLoader()) {
            }).initialize();

            LOG.info("Booting microprofile-server ...");
            final Bootstrap bootstrap = cdiContainer.select(Bootstrap.class).get();
            bootstrap.startServer();
        } catch (final Exception e) {
            LOG.error("Failed to start server: ", e);
        }
    }

    static class JavaServerDefaultSingletonService extends DefaultSingletonService {
        /**
         * Keys --> ClassLoaders Values --> WebBeansContext
         */
        private final Map<String, WebBeansContext> singletonMap = new WeakHashMap<>();


        /**
         * Gets singleton instance for deployment.
         * 
         * @return signelton instance for this deployment
         */
        @Override
        public WebBeansContext get(final Object key) {
            assertClassLoaderKey(key);
            final ClassLoader classLoader = (ClassLoader) key;
            synchronized (singletonMap) {
                // util.Track.sync(key);

                WebBeansContext webBeansContext = singletonMap.get(classLoader.getName());
                // util.Track.get(key);

                if (webBeansContext == null) {
                    webBeansContext = new WebBeansContext();
                    singletonMap.put(classLoader.getName(), webBeansContext);
                }

                return webBeansContext;

            }
        }


        @Override
        public void register(final ClassLoader key, final WebBeansContext context) {
            singletonMap.putIfAbsent(key.getName(), context);
        }


        /**
         * Clear all deployment instances when the application is undeployed.
         * 
         * @param classLoader of the deployment
         */
        @Override
        public void clearInstances(final ClassLoader classLoader) {
            Asserts.assertNotNull(classLoader, "classloader");
            synchronized (singletonMap) {
                singletonMap.remove(classLoader.getName());
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void clear(final Object classLoader) {
            assertClassLoaderKey(classLoader);
            clearInstances((ClassLoader) classLoader);
        }


        /**
         * Assert that key is classloader instance.
         * 
         * @param key key
         */
        private void assertClassLoaderKey(final Object key) {
            if (!(key instanceof ClassLoader)) {
                throw new IllegalArgumentException("Key instance must be ClassLoader for using DefaultSingletonService");
            }
        }

    }
}
