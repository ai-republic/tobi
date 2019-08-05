package com.airepublic.tobi.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.airepublic.logging.java.SerializableLogger;

public class RequestContext {
    private final static Logger LOG = new SerializableLogger(RequestContext.class.getName());

    private final long sessionId;
    private final Map<Contextual<Object>, Object> beans = new HashMap<>();
    private final Map<Contextual<Object>, CreationalContext<Object>> creationalContexts = new HashMap<>();


    public RequestContext(final long sessionId) {
        this.sessionId = sessionId;
    }


    public long getSessionId() {
        return sessionId;
    }


    public Object getBean(final Contextual<Object> contextual) {
        return beans.get(contextual);
    }


    public void addBean(final Contextual<Object> contextual, final CreationalContext<Object> creationalContext, final Object bean) {
        beans.put(contextual, bean);
        creationalContexts.put(contextual, creationalContext);
    }


    public Set<Contextual<Object>> getContextuals() {
        return new HashSet<>(beans.keySet());
    }


    public void destroy() {
        getContextuals().forEach(c -> destroy(c));
        beans.clear();
        creationalContexts.clear();
    }


    protected void destroy(final Contextual<Object> contextual) {
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
