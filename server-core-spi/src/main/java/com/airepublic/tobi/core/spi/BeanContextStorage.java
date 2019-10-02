package com.airepublic.tobi.core.spi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.airepublic.logging.java.SerializableLogger;

/**
 * The CDI context bean storage.
 * 
 * @author Torsten Oltmanns
 *
 */
public class BeanContextStorage {
    private final static Logger LOG = new SerializableLogger(BeanContextStorage.class.getName());
    private final Map<Contextual<Object>, Object> beans = new HashMap<>();
    private final Map<Contextual<Object>, CreationalContext<Object>> creationalContexts = new HashMap<>();


    /**
     * Gets the bean associated with the {@link Contextual}.
     * 
     * @param contextual the {@link Contextual}
     * @return the bean instance
     */
    public Object getBean(final Contextual<Object> contextual) {
        return beans.get(contextual);
    }


    /**
     * Adds a bean associated with the {@link Contextual} and {@link CreationalContext}.
     * 
     * @param contextual the {@link Contextual}
     * @param creationalContext the {@link CreationalContext}
     * @param bean the bean instance
     */
    public void addBean(final Contextual<Object> contextual, final CreationalContext<Object> creationalContext, final Object bean) {
        beans.put(contextual, bean);
        creationalContexts.put(contextual, creationalContext);
    }


    /**
     * Gets all registered {@link Contextual}s.
     * 
     * @return the set of all {@link Contextual}s
     */
    public Set<Contextual<Object>> getContextuals() {
        return new HashSet<>(beans.keySet());
    }


    /**
     * Destroys this context bean storage and destroys all beans associated with this context.
     */
    public void destroy() {
        getContextuals().forEach(c -> destroy(c));
        beans.clear();
        creationalContexts.clear();
    }


    /**
     * Destroys the bean associated with the {@link Contextual}.
     * 
     * @param contextual the {@link Contextual}
     */
    public void destroy(final Contextual<Object> contextual) {
        try {

            final CreationalContext<Object> creationalContext = creationalContexts.remove(contextual);
            final Object instance = beans.remove(contextual);
            contextual.destroy(instance, creationalContext);
            creationalContext.release();
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Error destroying bean: ", e);
        }

    }
}
