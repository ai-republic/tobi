package com.airepublic.microprofile.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionContext {
    private final static Logger LOG = LoggerFactory.getLogger(SessionContext.class);

    private final long sessionId;
    private final Map<Contextual<Object>, Object> beans = new HashMap<>();
    private final Map<Contextual<Object>, CreationalContext<Object>> creationalContexts = new HashMap<>();


    public SessionContext(final long sessionId) {
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
            LOG.error("Error destroying bean: ", e);
        }

    }
}