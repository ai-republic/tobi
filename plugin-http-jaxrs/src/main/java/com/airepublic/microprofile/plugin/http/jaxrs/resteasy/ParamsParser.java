package com.airepublic.microprofile.plugin.http.jaxrs.resteasy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.PathSegment;

/**
 * Parses path params from the resource {@link Path} method annotation and maps it to the URI
 * segment.
 * 
 * @author Torsten Oltmanns
 *
 */
public class ParamsParser {
    /**
     * Parses path params from the resource {@link Path} method annotation and maps it to the URI
     * segment.
     *
     * @param request the {@link RestEasyHttpRequestWrapper}
     * @param method the {@link Method}
     * @param contextPath the context path of the {@link Application}
     */
    public static void parse(final RestEasyHttpRequestWrapper request, final Method method, final String contextPath) {
        final List<PathSegment> uriPathSegments = request.getUri().getPathSegments();

        // segment class path
        Path annotation = method.getDeclaringClass().getAnnotation(Path.class);
        String path = "";

        if (annotation != null) {
            path = annotation.value();

            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
        }

        // segment method path
        annotation = method.getAnnotation(Path.class);

        if (annotation != null) {
            if (annotation.value().startsWith("/")) {
                path = path + annotation.value();
            } else if (path.length() > 0) {
                path = path + "/" + annotation.value();
            } else {
                path = annotation.value();
            }
        }

        // remove preceeding /
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        path = path.replace('/', ' ');

        final List<String> methodPathSegments = Arrays.asList(path.split(" "));

        // compare path segments
        for (int i = 0; i < uriPathSegments.size(); i++) {
            final String uriPathSegment = uriPathSegments.get(i).getPath();
            final String methodPathSegment = methodPathSegments.get(i);

            if (methodPathSegment.startsWith("{")) {
                final String param = methodPathSegment.replace("{", "").replace("}", "");

                if (!request.getUri().getPathParameters().containsKey(param)) {
                    request.getUri().getPathParameters().add(param, uriPathSegment);
                }
            } else if (uriPathSegment.equals(methodPathSegment)) {

            } else {
                throw new IllegalArgumentException("Request URI path and method path do not match!");
            }
        }
    }
}
