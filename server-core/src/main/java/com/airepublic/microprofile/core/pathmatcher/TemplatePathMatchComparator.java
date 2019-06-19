package com.airepublic.microprofile.core.pathmatcher;

import java.util.Comparator;

/**
 * This Comparator implementation is thread-safe so only create a single instance.
 */
public class TemplatePathMatchComparator<T> implements Comparator<TemplatePathMatch<T>> {

    @Override
    public int compare(final TemplatePathMatch<T> tpm1, final TemplatePathMatch<T> tpm2) {
        return tpm1.getUriTemplate().getNormalizedPath().compareTo(tpm2.getUriTemplate().getNormalizedPath());
    }
}