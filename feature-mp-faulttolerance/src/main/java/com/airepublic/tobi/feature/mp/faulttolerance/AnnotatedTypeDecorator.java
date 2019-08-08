package com.airepublic.tobi.feature.mp.faulttolerance;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;

/**
 * Decorates the annotated type with another annotation. This is to break the restriction if an
 * annotation is only declared for, i.e. type scope.
 * 
 * @author Torsten Oltmanns
 *
 * @param <X> the type of the original annotation
 * @param <A> the type of the annotation used to decorate the occurrence
 */
public class AnnotatedTypeDecorator<X, A extends Annotation> implements AnnotatedType<X> {
    private final AnnotatedType<X> decoratedType;
    private final Class<A> annotationClass;
    private final Annotation decoratingAnnotation;
    private final Set<Annotation> annotations;


    /**
     * Constructor.
     * 
     * @param decoratedType the {@link AnnotatedType} of the original annotation
     * @param annotationClass the annotation used to decorate the occurrence
     * @param decoratingAnnotation the annotation literal used to decorate the occurrence
     */
    public AnnotatedTypeDecorator(final AnnotatedType<X> decoratedType, final Class<A> annotationClass, final Annotation decoratingAnnotation) {
        this.decoratedType = decoratedType;
        this.annotationClass = annotationClass;
        this.decoratingAnnotation = decoratingAnnotation;

        final Set<Annotation> annotations = new HashSet<>(decoratedType.getAnnotations());
        annotations.add(decoratingAnnotation);
        this.annotations = Collections.unmodifiableSet(annotations);
    }


    @Override
    public Class<X> getJavaClass() {
        return decoratedType.getJavaClass();
    }


    @Override
    public Set<AnnotatedConstructor<X>> getConstructors() {
        return decoratedType.getConstructors();
    }


    @Override
    public Set<AnnotatedMethod<? super X>> getMethods() {
        return decoratedType.getMethods();
    }


    @Override
    public Set<AnnotatedField<? super X>> getFields() {
        return decoratedType.getFields();
    }


    @Override
    public Type getBaseType() {
        return decoratedType.getBaseType();
    }


    @Override
    public Set<Type> getTypeClosure() {
        return decoratedType.getTypeClosure();
    }


    @SuppressWarnings("unchecked")
    @Override
    public <T extends Annotation> T getAnnotation(final Class<T> aClass) {
        if (annotationClass.equals(aClass)) {
            return (T) decoratingAnnotation;
        }
        return decoratedType.getAnnotation(aClass);
    }


    @Override
    public Set<Annotation> getAnnotations() {

        return annotations;
    }


    @Override
    public boolean isAnnotationPresent(final Class<? extends Annotation> aClass) {
        if (aClass.equals(annotationClass)) {
            return true;
        }

        return decoratedType.isAnnotationPresent(aClass);
    }
}