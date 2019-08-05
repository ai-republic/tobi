package com.airepublic.tobi.core;

import java.lang.annotation.Annotation;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.AlterableContext;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;

public class RequestScopedContext implements AlterableContext {
    private final static ThreadLocal<RequestContext> requestContext = new ThreadLocal<>();


    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Contextual<T> contextual, final CreationalContext<T> creationalContext) {
        validateSession();

        final T bean = contextual.create(creationalContext);
        requestContext.get().addBean((Contextual<Object>) contextual, (CreationalContext<Object>) creationalContext, bean);
        creationalContext.push(bean);

        return bean;
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final Contextual<T> contextual) {
        validateSession();
        return (T) requestContext.get().getBean((Contextual<Object>) contextual);
    }


    public void activate(final RequestContext requestContext) {
        RequestScopedContext.requestContext.set(requestContext);
        validateSession();
    }


    public void deactivate() {
        validateSession();

        requestContext.get().destroy();
        requestContext.remove();
    }


    @Override
    public boolean isActive() {
        return requestContext.get() != null;
    }


    @SuppressWarnings("unchecked")
    @Override
    public void destroy(final Contextual<?> contextual) {
        validateSession();

        requestContext.get().destroy((Contextual<Object>) contextual);
    }


    private void validateSession() {
        if (requestContext.get() == null) {
            throw new IllegalStateException(getClass().getSimpleName() + " has not been activated!");
        }
    }

}
