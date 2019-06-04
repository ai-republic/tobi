package com.airepublic.microprofile.core.pathmatcher;

public class TemplatePathMatch<T> {
    private final T config;
    private final UriTemplate uriTemplate;


    public TemplatePathMatch(final T config, final UriTemplate uriTemplate) {
        this.config = config;
        this.uriTemplate = uriTemplate;
    }


    public T getConfig() {
        return config;
    }


    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }
}