package com.airepublic.microprofile.feature.cdi.owb;

import java.util.Map;
import java.util.WeakHashMap;

import org.apache.webbeans.config.WebBeansContext;
import org.apache.webbeans.corespi.DefaultSingletonService;
import org.apache.webbeans.util.Asserts;

public class JavaServerDefaultSingletonService extends DefaultSingletonService {
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