package com.airepublic.microprofile.core.pathmatcher;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class PathMapping<T> {
    private final Map<String, T> configExactMatchMap = new ConcurrentHashMap<>();
    private final Map<Integer, SortedSet<TemplatePathMatch<T>>> configTemplateMatchMap = new ConcurrentHashMap<>();


    public void add(final String path, final T mappedObject) {
        UriTemplate uriTemplate = null;

        try {
            uriTemplate = new UriTemplate(path);
        } catch (final IOException e) {
            // Path is not valid so can't be matched
            throw new IllegalArgumentException("URI path not valid: " + path, e);
        }

        if (uriTemplate.hasParameters()) {
            final Integer key = Integer.valueOf(uriTemplate.getSegmentCount());
            SortedSet<TemplatePathMatch<T>> templateMatches = configTemplateMatchMap.get(key);

            if (templateMatches == null) {
                // Ensure that if concurrent threads execute this block they
                // both end up using the same TreeSet instance
                templateMatches = new TreeSet<>(new TemplatePathMatchComparator<T>());
                configTemplateMatchMap.putIfAbsent(key, templateMatches);
                templateMatches = configTemplateMatchMap.get(key);
            }

            if (!templateMatches.add(new TemplatePathMatch<>(mappedObject, uriTemplate))) {
                // Duplicate uriTemplate;
                throw new IllegalArgumentException("Duplicate path mapping: " + path);
            }
        } else {
            // Exact match
            final T old = configExactMatchMap.put(path, mappedObject);

            if (old != null) {
                // Duplicate path mappings
                throw new IllegalArgumentException("Duplicate path mapping: " + path);
            }
        }
    }


    public MappingResult<T> findMapping(final String path) {
        // Check an exact match. Simple case as there are no templates.
        T mappedObject = configExactMatchMap.get(path);

        if (mappedObject != null) {
            return new MappingResult<>(mappedObject, Collections.emptyMap());
        }

        // No exact match. Need to look for template matches.
        UriTemplate pathUriTemplate = null;

        try {
            pathUriTemplate = new UriTemplate(path);
        } catch (final IOException e) {
            // Path is not valid so can't be matched
            return null;
        }

        // Number of segments has to match
        final Integer key = Integer.valueOf(pathUriTemplate.getSegmentCount());
        final SortedSet<TemplatePathMatch<T>> templateMatches = configTemplateMatchMap.get(key);

        if (templateMatches == null) {
            // No templates with an equal number of segments so there will be no matches
            return null;
        }

        // List is in alphabetical order of normalised templates.
        // Correct match is the first one that matches.
        Map<String, String> pathParams = null;

        for (final TemplatePathMatch<T> templateMatch : templateMatches) {
            pathParams = templateMatch.getUriTemplate().match(pathUriTemplate);

            if (pathParams != null) {
                mappedObject = templateMatch.getConfig();
                break;
            }
        }

        if (mappedObject == null) {
            // No match
            return null;
        }

        return new MappingResult<>(mappedObject, pathParams);
    }

}
