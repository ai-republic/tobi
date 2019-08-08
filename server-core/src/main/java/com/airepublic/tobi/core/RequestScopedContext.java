package com.airepublic.tobi.core;

import java.lang.annotation.Annotation;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

import com.airepublic.tobi.core.spi.BeanContextStorage;

/**
 * The {@link RequestScoped} CDI context.
 * 
 * @author Torsten Oltmanns
 *
 */
public class RequestScopedContext implements AlterableContext {
    private final static ThreadLocal<BeanContextStorage> requestContext = new ThreadLocal<>();


    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        validateContext();

        final T bean = contextual.create(creationalContext);
        requestContext.get().addBean((Contextual<Object>) contextual, (CreationalContext<Object>) creationalContext, bean);
        creationalContext.push(bean);

        return bean;
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(final Contextual<T> contextual) {
        validateContext();
        return (T) requestContext.get().getBean((Contextual<Object>) contextual);
    }


    /**
     * Activate this context.
     * 
     * @param requestContext the {@link BeanContextStorage}
     */
    public void activate(final BeanContextStorage requestContext) {
        RequestScopedContext.requestContext.set(requestContext);
        validateContext();
    }


    /**
     * Deactivates this context and destroys all the associated beans.
     */
    public void deactivate() {
        validateContext();

        requestContext.get().destroy();
        requestContext.remove();
    }


    @Override
    public boolean isActive() {
        return requestContext.get() != null;
    }


    @Override
    @SuppressWarnings("unchecked")
    public void destroy(final Contextual<?> contextual) {
        validateContext();

        requestContext.get().destroy((Contextual<Object>) contextual);
    }


    /**
     * Validates this context.
     */
    private void validateContext() {
        if (requestContext.get() == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " has not been activated!");
        }
    }

}
