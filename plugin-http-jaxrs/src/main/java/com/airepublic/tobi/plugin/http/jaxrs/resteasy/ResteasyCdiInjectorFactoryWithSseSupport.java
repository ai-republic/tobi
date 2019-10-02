package com.airepublic.tobi.plugin.http.jaxrs.resteasy;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.jboss.resteasy.cdi.CdiConstructorInjector;
import org.jboss.resteasy.cdi.CdiPropertyInjector;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.cdi.i18n.LogMessages;
import org.jboss.resteasy.cdi.i18n.Messages;
import org.jboss.resteasy.core.InjectorFactoryImpl;
import org.jboss.resteasy.core.ValueInjector;
import org.jboss.resteasy.spi.ConstructorInjector;
import org.jboss.resteasy.spi.InjectorFactory;
import org.jboss.resteasy.spi.MethodInjector;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.metadata.Parameter;
import org.jboss.resteasy.spi.metadata.ResourceClass;
import org.jboss.resteasy.spi.metadata.ResourceConstructor;
import org.jboss.resteasy.spi.metadata.ResourceLocator;

/**
 * A customized {@link InjectorFactory} to use CDI and supporting SSE.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ResteasyCdiInjectorFactoryWithSseSupport implements InjectorFactory {
    public static final String BEAN_MANAGER_ATTRIBUTE_PREFIX = "org.jboss.weld.environment.servlet.";
    private final BeanManager manager;
    private final InjectorFactory delegate = new InjectorFactoryImpl();
    private final ResteasyCdiExtension extension;
    private final Map<Class<?>, Type> sessionBeanInterface;


    /**
     * Constructor.
     */
    public ResteasyCdiInjectorFactoryWithSseSupport() {
        manager = lookupBeanManager();
        extension = lookupResteasyCdiExtension();
        sessionBeanInterface = extension.getSessionBeanInterface();
    }


    /**
     * Constructor.
     * 
     * @param manager the {@link BeanManager}
     */
    public ResteasyCdiInjectorFactoryWithSseSupport(final BeanManager manager) {
        this.manager = manager;
        extension = lookupResteasyCdiExtension();
        sessionBeanInterface = extension.getSessionBeanInterface();
    }


    @Override
    public ValueInjector createParameterExtractor(final Parameter parameter, final ResteasyProviderFactory providerFactory) {
        return delegate.createParameterExtractor(parameter, providerFactory);
    }


    @Override
    public MethodInjector createMethodInjector(final ResourceLocator method, final ResteasyProviderFactory factory) {
        return delegate.createMethodInjector(method, factory);
    }


    @Override
    public PropertyInjector createPropertyInjector(final ResourceClass resourceClass, final ResteasyProviderFactory providerFactory) {
        return new CdiPropertyInjector(delegate.createPropertyInjector(resourceClass, providerFactory), resourceClass.getClazz(), sessionBeanInterface, manager);
    }


    @Override
    public ConstructorInjector createConstructor(final ResourceConstructor constructor, final ResteasyProviderFactory providerFactory) {
        final Class<?> clazz = constructor.getConstructor().getDeclaringClass();

        final ConstructorInjector injector = cdiConstructor(clazz);
        if (injector != null) {
            return injector;
        }

        LogMessages.LOGGER.debug(Messages.MESSAGES.noCDIBeansFound(clazz));
        return delegate.createConstructor(constructor, providerFactory);
    }


    @Override
    public ConstructorInjector createConstructor(final Constructor constructor, final ResteasyProviderFactory factory) {
        final Class<?> clazz = constructor.getDeclaringClass();

        final ConstructorInjector injector = cdiConstructor(clazz);
        if (injector != null) {
            return injector;
        }

        LogMessages.LOGGER.debug(Messages.MESSAGES.noCDIBeansFound(clazz));
        return delegate.createConstructor(constructor, factory);
    }


    /**
     * Creates the {@link CdiConstructorInjector} for the specified class.
     * 
     * @param clazz the class
     * @return the {@link CdiConstructorInjector} or null
     */
    protected ConstructorInjector cdiConstructor(final Class<?> clazz) {
        if (!manager.getBeans(clazz).isEmpty()) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.usingCdiConstructorInjector(clazz));
            return new CdiConstructorInjector(clazz, manager);
        }

        if (sessionBeanInterface.containsKey(clazz)) {
            final Type intfc = sessionBeanInterface.get(clazz);
            LogMessages.LOGGER.debug(Messages.MESSAGES.usingInterfaceForLookup(intfc, clazz));
            return new CdiConstructorInjector(intfc, manager);
        }

        return null;
    }


    @Override
    public PropertyInjector createPropertyInjector(final Class resourceClass, final ResteasyProviderFactory factory) {
        return new CdiPropertyInjector(delegate.createPropertyInjector(resourceClass, factory), resourceClass, sessionBeanInterface, manager);
    }


    @Override
    @Deprecated
    public ValueInjector createParameterExtractor(final Class injectTargetClass, final AccessibleObject injectTarget, final Class type,
            final Type genericType, final Annotation[] annotations, final ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, type, genericType, annotations, factory);
    }


    @Override
    public ValueInjector createParameterExtractor(final Class injectTargetClass, final AccessibleObject injectTarget, final String defaultName, final Class type, final Type genericType, final Annotation[] annotations, final ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, factory);
    }


    @Override
    @Deprecated
    public ValueInjector createParameterExtractor(final Class injectTargetClass, final AccessibleObject injectTarget, final Class type,
            final Type genericType, final Annotation[] annotations, final boolean useDefault, final ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, type, genericType, annotations, useDefault, factory);
    }


    @Override
    public ValueInjector createParameterExtractor(final Class injectTargetClass, final AccessibleObject injectTarget, final String defaultName, final Class type,
            final Type genericType, final Annotation[] annotations, final boolean useDefault, final ResteasyProviderFactory factory) {
        return delegate.createParameterExtractor(injectTargetClass, injectTarget, defaultName, type, genericType, annotations, useDefault, factory);
    }


    /**
     * Do a lookup for BeanManager instance.
     *
     * @return the {@link BeanManager}
     */
    protected BeanManager lookupBeanManager() {
        BeanManager beanManager = null;

        beanManager = lookupBeanManagerCDIUtil();
        if (beanManager != null) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.foundBeanManagerViaCDI());
            return beanManager;
        }

        throw new RuntimeException(Messages.MESSAGES.unableToLookupBeanManager());
    }


    /**
     * Looks up the BeanManager of the current CDI instance.
     * 
     * @return the {@link BeanManager}
     */
    public static BeanManager lookupBeanManagerCDIUtil() {
        BeanManager bm = null;
        try {
            bm = CDI.current().getBeanManager();
        } catch (final NoClassDefFoundError e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.unableToFindCDIClass(), e);
        } catch (final Exception e) {
            LogMessages.LOGGER.debug(Messages.MESSAGES.errorOccurredLookingUpViaCDIUtil(), e);
        }
        return bm;
    }


    /**
     * Lookup ResteasyCdiExtension instance that was instantiated during CDI bootstrap
     *
     * @return ResteasyCdiExtension instance
     */
    private ResteasyCdiExtension lookupResteasyCdiExtension() {
        final Set<Bean<?>> beans = manager.getBeans(ResteasyCdiExtension.class);
        final Bean<?> bean = manager.resolve(beans);
        if (bean == null) {
            throw new IllegalStateException(Messages.MESSAGES.unableToObtainResteasyCdiExtension());
        }
        final CreationalContext<?> context = manager.createCreationalContext(bean);
        return (ResteasyCdiExtension) manager.getReference(bean, ResteasyCdiExtension.class, context);
    }

}
