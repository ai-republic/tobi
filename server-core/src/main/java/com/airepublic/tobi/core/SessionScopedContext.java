package com.airepublic.tobi.core;

import java.lang.annotation.Annotation;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.airepublic.tobi.core.spi.BeanContextStorage;

/**
 * The {@link SessionScoped} CDI context.
 * 
 * @author Torsten Oltmanns
 *
 */
public class SessionScopedContext implements AlterableContext {
    private final static ThreadLocal<BeanContextStorage> sessionContext = new ThreadLocal<>();


    @Override
    public Class<? extends Annotation> getScope() {
        return SessionScoped.class;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        validateContext();

        final T bean = contextual.create(creationalContext);
        sessionContext.get().addBean((Contextual<Object>) contextual, (CreationalContext<Object>) creationalContext, bean);
        creationalContext.push(bean);

        return bean;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual) {
        validateContext();
        return (T) sessionContext.get().getBean((Contextual<Object>) contextual);
    }


    /**
     * Activate this context.
     * 
     * @param requestContext the {@link BeanContextStorage}
     */
    public void activate(final BeanContextStorage sessionContext) {
        SessionScopedContext.sessionContext.set(sessionContext);
        validateContext();
    }


    /**
     * Deactivates this context and destroys all the associated beans.
     */
    public void deactivate() {
        validateContext();

        sessionContext.get().destroy();
        sessionContext.remove();
    }


    @Override
    public boolean isActive() {
        return sessionContext.get() != null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void destroy(final Contextual<?> contextual) {
        validateContext();

        sessionContext.get().destroy((Contextual<Object>) contextual);
    }


    /**
     * Validates this context.
     */
    private void validateContext() {
        if (sessionContext.get() == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " has not been activated!");
        }
    }

}
