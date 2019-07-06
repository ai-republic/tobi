package com.airepublic.microprofile.plugin.http.sse;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SseUriBuilder extends UriBuilder {

    private static final class URITemplateParametersMap extends HashMap<String, Object> {

        private final Object[] parameterValues;
        private int index;


        private URITemplateParametersMap(final Object... parameterValues) {
            this.parameterValues = parameterValues;
        }


        @Override
        public Object get(final Object key) {
            Object object = null;
            if (!super.containsKey(key) && index != parameterValues.length) {
                object = parameterValues[index++];
                super.put((String) key, object);
            } else {
                object = super.get(key);
            }
            return object;
        }


        @Override
        public boolean containsKey(final Object key) {
            boolean containsKey = super.containsKey(key);
            if (!containsKey && index != parameterValues.length) {
                super.put((String) key, parameterValues[index++]);
                containsKey = true;
            }
            return containsKey;
        }

    }

    private String host;
    private String scheme;
    private int port = -1;

    private String userInfo;
    private String path;
    private String query;
    private String fragment;
    private String ssp;
    private String authority;


    @Override
    public UriBuilder clone() {
        final SseUriBuilder impl = new SseUriBuilder();
        impl.host = host;
        impl.scheme = scheme;
        impl.port = port;
        impl.userInfo = userInfo;
        impl.path = path;
        impl.query = query;
        impl.fragment = fragment;
        impl.ssp = ssp;
        impl.authority = authority;

        return impl;
    }

    public static final Pattern opaqueUri = Pattern.compile("^([^:/?#{]+):([^/].*)");
    public static final Pattern hierarchicalUri = Pattern.compile("^(([^:/?#{]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
    private static final Pattern hostPortPattern = Pattern.compile("([^/:]+):(\\d+)");
    private static final Pattern squareHostBrackets = Pattern.compile("(\\[(([0-9A-Fa-f]{0,4}:){2,7})([0-9A-Fa-f]{0,4})%?.*\\]):(\\d+)");


    public static boolean compare(final String s1, final String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        return s1.equals(s2);
    }


    public static URI relativize(final URI from, final URI to) {
        if (!compare(from.getScheme(), to.getScheme())) {
            return to;
        }
        if (!compare(from.getHost(), to.getHost())) {
            return to;
        }
        if (from.getPort() != to.getPort()) {
            return to;
        }
        if (from.getPath() == null && to.getPath() == null) {
            return URI.create("");
        } else if (from.getPath() == null) {
            return URI.create(to.getPath());
        } else if (to.getPath() == null) {
            return to;
        }

        String fromPath = from.getPath();
        if (fromPath.startsWith("/")) {
            fromPath = fromPath.substring(1);
        }
        final String[] fsplit = fromPath.split("/");
        String toPath = to.getPath();
        if (toPath.startsWith("/")) {
            toPath = toPath.substring(1);
        }
        final String[] tsplit = toPath.split("/");

        int f = 0;

        for (; f < fsplit.length && f < tsplit.length; f++) {
            if (!fsplit[f].equals(tsplit[f])) {
                break;
            }
        }

        final UriBuilder builder = UriBuilder.fromPath("");
        for (int i = f; i < fsplit.length; i++) {
            builder.path("..");
        }
        for (int i = f; i < tsplit.length; i++) {
            builder.path(tsplit[i]);
        }
        return builder.build();
    }


    /**
     * You may put path parameters anywhere within the uriTemplate except port.
     *
     * @param uriTemplate uri template
     * @return uri builder
     */
    public static UriBuilder fromTemplate(final String uriTemplate) {
        final SseUriBuilder impl = new SseUriBuilder();
        impl.uriTemplate(uriTemplate);
        return impl;
    }


    /**
     * You may put path parameters anywhere within the uriTemplate except port.
     *
     * @param uriTemplate uri template
     * @return uri builder
     */
    public UriBuilder uriTemplate(final String uriTemplate) {
        if (uriTemplate == null) {
            throw new IllegalArgumentException("uriTemplate parameter null");
        }
        final Matcher opaque = opaqueUri.matcher(uriTemplate);
        if (opaque.matches()) {
            authority = null;
            host = null;
            port = -1;
            userInfo = null;
            query = null;
            scheme = opaque.group(1);
            ssp = opaque.group(2);
            return this;
        } else {
            final Matcher match = hierarchicalUri.matcher(uriTemplate);
            if (match.matches()) {
                ssp = null;
                return parseHierarchicalUri(uriTemplate, match);
            }
        }
        throw new IllegalArgumentException("Illegal uriTemplate " + uriTemplate);
    }


    protected UriBuilder parseHierarchicalUri(final String uriTemplate, final Matcher match) {
        final boolean scheme = match.group(2) != null;
        if (scheme) {
            this.scheme = match.group(2);
        }
        final String authority = match.group(4);
        if (authority != null) {
            this.authority = null;
            String host = match.group(4);
            final int at = host.indexOf('@');
            if (at > -1) {
                final String user = host.substring(0, at);
                host = host.substring(at + 1);
                userInfo = user;
            }

            final Matcher hostPortMatch = hostPortPattern.matcher(host);
            if (hostPortMatch.matches()) {
                this.host = hostPortMatch.group(1);
                final int val = 0;
                try {
                    port = Integer.parseInt(hostPortMatch.group(2));
                } catch (final NumberFormatException e) {
                    throw new IllegalArgumentException("Illegal uriTemplate " + uriTemplate, e);
                }
            } else {
                if (host.startsWith("[")) {
                    // Must support an IPv6 hostname of format "[::1]" or [0:0:0:0:0:0:0:0]
                    // and IPv6 link-local format [fe80::1234%1] [ff08::9abc%interface10]
                    final Matcher bracketsMatch = squareHostBrackets.matcher(host);
                    if (bracketsMatch.matches()) {
                        host = bracketsMatch.group(1);
                        try {
                            port = Integer.parseInt(bracketsMatch.group(5));
                        } catch (final NumberFormatException e) {
                            throw new IllegalArgumentException("Illegal uriTemplate " + uriTemplate, e);
                        }
                    }
                }
                this.host = host;
            }
        }
        if (match.group(5) != null) {
            final String group = match.group(5);
            if (!scheme && !"".equals(group) && !group.startsWith("/") && group.indexOf(':') > -1 &&
                    group.indexOf('/') > -1 && group.indexOf(':') < group.indexOf('/')) {
                throw new IllegalArgumentException("Illegal uriTemplate " + uriTemplate);
            }
            if (!"".equals(group)) {
                replacePath(group);
            }
        }
        if (match.group(7) != null) {
            replaceQuery(match.group(7));
        }
        if (match.group(9) != null) {
            fragment(match.group(9));
        }
        return this;
    }


    @Override
    public UriBuilder uri(final String uriTemplate) throws IllegalArgumentException {
        return uriTemplate(uriTemplate);
    }


    @Override
    public UriBuilder uri(final URI uri) throws IllegalArgumentException {
        if (uri == null) {
            throw new IllegalArgumentException("Illegal uri " + uri);
        }

        if (uri.getRawFragment() != null) {
            fragment = uri.getRawFragment();
        }

        if (uri.isOpaque()) {
            scheme = uri.getScheme();
            ssp = uri.getRawSchemeSpecificPart();
            return this;
        }

        if (uri.getScheme() == null) {
            if (ssp != null) {
                if (uri.getRawSchemeSpecificPart() != null) {
                    ssp = uri.getRawSchemeSpecificPart();
                    return this;
                }
            }
        } else {
            scheme = uri.getScheme();
        }

        ssp = null;
        if (uri.getRawAuthority() != null) {
            if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                authority = uri.getRawAuthority();
                userInfo = null;
                host = null;
                port = -1;
            } else {
                authority = null;
                if (uri.getRawUserInfo() != null) {
                    userInfo = uri.getRawUserInfo();
                }
                if (uri.getHost() != null) {
                    host = uri.getHost();
                }
                if (uri.getPort() != -1) {
                    port = uri.getPort();
                }
            }
        }

        if (uri.getRawPath() != null && uri.getRawPath().length() > 0) {
            path = uri.getRawPath();
        }
        if (uri.getRawQuery() != null && uri.getRawQuery().length() > 0) {
            query = uri.getRawQuery();
        }

        return this;
    }


    @Override
    public UriBuilder scheme(final String scheme) throws IllegalArgumentException {
        this.scheme = scheme;
        return this;
    }


    @Override
    public UriBuilder schemeSpecificPart(final String ssp) throws IllegalArgumentException {
        if (ssp == null) {
            throw new IllegalArgumentException("Illegal scheme specific part " + ssp);
        }

        final StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme).append(':');
        }
        if (ssp != null) {
            sb.append(ssp);
        }
        if (fragment != null && fragment.length() > 0) {
            sb.append('#').append(fragment);
        }
        final URI uri = URI.create(sb.toString());

        if (uri.getRawSchemeSpecificPart() != null && uri.getRawPath() == null) {
            this.ssp = uri.getRawSchemeSpecificPart();
        } else {
            this.ssp = null;
            userInfo = uri.getRawUserInfo();
            host = uri.getHost();
            port = uri.getPort();
            path = uri.getRawPath();
            query = uri.getRawQuery();

        }
        return this;

    }


    @Override
    public UriBuilder userInfo(final String ui) {
        userInfo = ui;
        return this;
    }


    @Override
    public UriBuilder host(final String host) throws IllegalArgumentException {
        if (host != null && host.equals("")) {
            throw new IllegalArgumentException("Illegal host " + host);
        }
        this.host = host;
        return this;
    }


    @Override
    public UriBuilder port(final int port) throws IllegalArgumentException {
        if (port < -1) {
            throw new IllegalArgumentException("Illegal port " + port);
        }
        this.port = port;
        return this;
    }


    protected static String paths(final boolean encode, final String basePath, final String... segments) {
        String path = basePath;
        if (path == null) {
            path = "";
        }
        for (String segment : segments) {
            if ("".equals(segment)) {
                continue;
            }
            if (path.endsWith("/")) {
                if (segment.startsWith("/")) {
                    segment = segment.substring(1);
                    if ("".equals(segment)) {
                        continue;
                    }
                }
                if (encode) {
                    segment = Encode.encodePath(segment);
                }
                path += segment;
            } else {
                if (encode) {
                    segment = Encode.encodePath(segment);
                }
                if ("".equals(path)) {
                    path = segment;
                } else if (segment.startsWith("/")) {
                    path += segment;
                } else {
                    path += "/" + segment;
                }
            }

        }
        return path;
    }


    @Override
    public UriBuilder path(final String segment) throws IllegalArgumentException {
        if (segment == null) {
            throw new IllegalArgumentException("Illegal path " + segment);
        }
        path = paths(true, path, segment);
        return this;
    }


    @SuppressWarnings("unchecked")
    @Override
    public UriBuilder path(final Class resource) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("Illegal path " + resource);
        }
        final Path ann = (Path) resource.getAnnotation(Path.class);
        if (ann != null) {
            final String[] segments = new String[] { ann.value() };
            path = paths(true, path, segments);
        } else {
            throw new IllegalArgumentException("Illegal path " + resource);
        }
        return this;
    }


    @SuppressWarnings("unchecked")
    @Override
    public UriBuilder path(final Class resource, final String method) throws IllegalArgumentException {
        if (resource == null) {
            throw new IllegalArgumentException("Illegal path " + method);
        }
        if (method == null) {
            throw new IllegalArgumentException("Illegal path " + method);
        }
        Method theMethod = null;
        for (final Method m : resource.getMethods()) {
            if (m.getName().equals(method)) {
                if (theMethod != null && m.isAnnotationPresent(Path.class)) {
                    throw new IllegalArgumentException("Illegal path " + method);
                }
                if (m.isAnnotationPresent(Path.class)) {
                    theMethod = m;
                }
            }
        }
        if (theMethod == null) {
            throw new IllegalArgumentException("Illegal path " + method);
        }
        return path(theMethod);
    }


    @Override
    public UriBuilder path(final Method method) throws IllegalArgumentException {
        if (method == null) {
            throw new IllegalArgumentException("Illegal path " + method);
        }
        final Path ann = method.getAnnotation(Path.class);
        if (ann != null) {
            path = paths(true, path, ann.value());
        } else {
            throw new IllegalArgumentException("Illegal path " + method);
        }
        return this;
    }


    @Override
    public UriBuilder replaceMatrix(String matrix) throws IllegalArgumentException {
        if (matrix == null) {
            matrix = "";
        }
        if (!matrix.startsWith(";")) {
            matrix = ";" + matrix;
        }
        matrix = Encode.encodePath(matrix);
        if (path == null) {
            path = matrix;
        } else {
            int start = path.lastIndexOf('/');
            if (start < 0) {
                start = 0;
            }
            final int matrixIndex = path.indexOf(';', start);
            if (matrixIndex > -1) {
                path = path.substring(0, matrixIndex) + matrix;
            } else {
                path += matrix;
            }

        }
        return this;
    }


    @Override
    public UriBuilder replaceQuery(final String query) throws IllegalArgumentException {
        if (query == null || query.length() == 0) {
            this.query = null;
            return this;
        }
        this.query = Encode.encodeQueryString(query);
        return this;
    }


    @Override
    public UriBuilder fragment(final String fragment) throws IllegalArgumentException {
        if (fragment == null) {
            this.fragment = null;
            return this;
        }
        this.fragment = Encode.encodeFragment(fragment);
        return this;
    }


    /**
     * Only replace path params in path of URI. This changes state of URIBuilder.
     *
     * @param name parameter name
     * @param value parameter value
     * @param isEncoded encoded flag
     * @return uri builder
     */
    public UriBuilder substitutePathParam(final String name, final Object value, final boolean isEncoded) {
        if (path != null) {
            final StringBuilder builder = new StringBuilder();
            replacePathParameter(name, value.toString(), isEncoded, path, builder, false);
            path = builder.toString();
        }
        return this;
    }


    @Override
    public URI buildFromMap(final Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        return buildUriFromMap(values, false, true);
    }


    @Override
    public URI buildFromEncodedMap(final Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        return buildUriFromMap(values, true, false);
    }


    @Override
    public URI buildFromMap(final Map<String, ?> values, final boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        return buildUriFromMap(values, false, encodeSlashInPath);
    }


    protected URI buildUriFromMap(final Map<String, ? extends Object> paramMap, final boolean fromEncodedMap, final boolean encodeSlash) throws IllegalArgumentException, UriBuilderException {
        final String buf = buildString(paramMap, fromEncodedMap, false, encodeSlash);
        try {
            return URI.create(buf);
        } catch (final Exception e) {
            throw new RuntimeException("Illegal parameters " + paramMap);
        }
    }


    private String buildString(final Map<String, ? extends Object> paramMap, final boolean fromEncodedMap, final boolean isTemplate, final boolean encodeSlash) {
        final StringBuilder builder = new StringBuilder();

        if (scheme != null) {
            replaceParameter(paramMap, fromEncodedMap, isTemplate, scheme, builder, encodeSlash).append(":");
        }
        if (ssp != null) {
            builder.append(ssp);
        } else if (userInfo != null || host != null || port != -1) {
            builder.append("//");
            if (userInfo != null) {
                replaceParameter(paramMap, fromEncodedMap, isTemplate, userInfo, builder, encodeSlash).append("@");
            }
            if (host != null) {
                if ("".equals(host)) {
                    throw new UriBuilderException("Illegal host " + host);
                }
                replaceParameter(paramMap, fromEncodedMap, isTemplate, host, builder, encodeSlash);
            }
            if (port != -1) {
                builder.append(":").append(Integer.toString(port));
            }
        } else if (authority != null) {
            builder.append("//");
            replaceParameter(paramMap, fromEncodedMap, isTemplate, authority, builder, encodeSlash);
        }
        if (path != null) {
            final StringBuilder tmp = new StringBuilder();
            replaceParameter(paramMap, fromEncodedMap, isTemplate, path, tmp, encodeSlash);
            final String tmpPath = tmp.toString();
            if (userInfo != null || host != null) {
                if (!tmpPath.startsWith("/")) {
                    builder.append("/");
                }
            }
            builder.append(tmpPath);
        }
        if (query != null) {
            builder.append("?");
            replaceQueryStringParameter(paramMap, fromEncodedMap, isTemplate, query, builder);
        }
        if (fragment != null) {
            builder.append("#");
            replaceParameter(paramMap, fromEncodedMap, isTemplate, fragment, builder, encodeSlash);
        }
        return builder.toString();
    }


    protected StringBuilder replacePathParameter(final String name, String value, final boolean isEncoded, final String string, final StringBuilder builder, final boolean encodeSlash) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        final Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            final String param = matcher.group(1);
            if (!param.equals(name)) {
                continue;
            }
            builder.append(string, start, matcher.start());
            if (!isEncoded) {
                if (encodeSlash) {
                    value = Encode.encodePath(value);
                } else {
                    value = Encode.encodePathSegment(value);
                }

            } else {
                value = Encode.encodeNonCodes(value);
            }
            builder.append(value);
            start = matcher.end();
        }
        builder.append(string, start, string.length());
        return builder;
    }


    public static Matcher createUriParamMatcher(final String string) {
        final Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBraces(string));
        return matcher;
    }


    protected StringBuilder replaceParameter(final Map<String, ? extends Object> paramMap, final boolean fromEncodedMap, final boolean isTemplate, final String string, final StringBuilder builder, final boolean encodeSlash) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        final Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            builder.append(string, start, matcher.start());
            final String param = matcher.group(1);
            final boolean containsValueForParam = paramMap.containsKey(param);
            if (!containsValueForParam) {
                if (isTemplate) {
                    builder.append(matcher.group());
                    start = matcher.end();
                    continue;
                }
                throw new IllegalArgumentException("Missing path " + paramMap);
            }
            final Object value = paramMap.get(param);
            String stringValue = value != null ? value.toString() : null;
            if (stringValue != null) {
                if (!fromEncodedMap) {
                    if (encodeSlash) {
                        stringValue = Encode.encodePathSegmentAsIs(stringValue);
                    } else {
                        stringValue = Encode.encodePathAsIs(stringValue);
                    }
                } else {
                    if (encodeSlash) {
                        stringValue = Encode.encodePathSegmentSaveEncodings(stringValue);
                    } else {
                        stringValue = Encode.encodePathSaveEncodings(stringValue);
                    }
                }
                builder.append(stringValue);
                start = matcher.end();
            } else {
                throw new IllegalArgumentException("Illegal parameters " + paramMap);
            }
        }
        builder.append(string, start, string.length());
        return builder;
    }


    protected StringBuilder replaceQueryStringParameter(final Map<String, ? extends Object> paramMap, final boolean fromEncodedMap, final boolean isTemplate, final String string, final StringBuilder builder) {
        if (string.indexOf('{') == -1) {
            return builder.append(string);
        }
        final Matcher matcher = createUriParamMatcher(string);
        int start = 0;
        while (matcher.find()) {
            builder.append(string, start, matcher.start());
            final String param = matcher.group(1);
            final boolean containsValueForParam = paramMap.containsKey(param);
            if (!containsValueForParam) {
                if (isTemplate) {
                    builder.append(matcher.group());
                    start = matcher.end();
                    continue;
                }
                throw new IllegalArgumentException("Missing path " + paramMap);
            }
            final Object value = paramMap.get(param);
            String stringValue = value != null ? value.toString() : null;
            if (stringValue != null) {
                if (!fromEncodedMap) {
                    stringValue = Encode.encodeQueryParamAsIs(stringValue);
                } else {
                    stringValue = Encode.encodeQueryParamSaveEncodings(stringValue);
                }
                builder.append(stringValue);
                start = matcher.end();
            } else {
                throw new IllegalArgumentException("Missing template parameter " + paramMap);
            }
        }
        builder.append(string, start, string.length());
        return builder;
    }


    /**
     * Return a unique order list of path params.
     *
     * @return list of path parameters
     */
    public List<String> getPathParamNamesInDeclarationOrder() {
        final List<String> params = new ArrayList<>();
        final HashSet<String> set = new HashSet<>();
        if (scheme != null) {
            addToPathParamList(params, set, scheme);
        }
        if (userInfo != null) {
            addToPathParamList(params, set, userInfo);
        }
        if (host != null) {
            addToPathParamList(params, set, host);
        }
        if (path != null) {
            addToPathParamList(params, set, path);
        }
        if (query != null) {
            addToPathParamList(params, set, query);
        }
        if (fragment != null) {
            addToPathParamList(params, set, fragment);
        }

        return params;
    }


    private void addToPathParamList(final List<String> params, final HashSet<String> set, final String string) {
        final Matcher matcher = PathHelper.URI_PARAM_PATTERN.matcher(PathHelper.replaceEnclosedCurlyBraces(string));
        while (matcher.find()) {
            final String param = matcher.group(1);
            if (set.contains(param)) {
                continue;
            } else {
                set.add(param);
                params.add(param);
            }
        }
    }


    @Override
    public URI build(final Object... values) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        return buildFromValues(true, false, values);
    }


    protected URI buildFromValues(final boolean encodeSlash, final boolean encoded, final Object... values) {
        String buf = null;
        try {
            buf = buildString(new URITemplateParametersMap(values), encoded, false, encodeSlash);
            return new URI(buf);
            // return URI.create(buf);
        } catch (final IllegalArgumentException iae) {
            throw iae;
        } catch (final Exception e) {
            throw new UriBuilderException("Illegal parameters " + values);
        }
    }


    @Override
    public UriBuilder matrixParam(final String name, final Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        if (path == null) {
            path = "";
        }
        for (final Object val : values) {
            if (val == null) {
                throw new IllegalArgumentException("Illegal parameters " + values);
            }
            path += ";" + Encode.encodeMatrixParam(name) + "=" + Encode.encodeMatrixParam(val.toString());
        }
        return this;
    }

    private static final Pattern PARAM_REPLACEMENT = Pattern.compile("_resteasy_uri_parameter");


    @Override
    public UriBuilder replaceMatrixParam(final String name, final Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (path == null) {
            if (values != null && values.length > 0) {
                return matrixParam(name, values);
            }
            return this;
        }

        // remove all path param expressions so we don't accidentally start replacing within a
        // regular expression
        final ArrayList<String> pathParams = new ArrayList<>();
        boolean foundParam = false;

        final CharSequence pathWithoutEnclosedCurlyBraces = PathHelper.replaceEnclosedCurlyBraces(path);
        Matcher matcher = PathHelper.URI_TEMPLATE_PATTERN.matcher(pathWithoutEnclosedCurlyBraces);
        StringBuilder newSegment = new StringBuilder();
        int from = 0;
        while (matcher.find()) {
            newSegment.append(pathWithoutEnclosedCurlyBraces, from, matcher.start());
            foundParam = true;
            final String group = matcher.group();
            pathParams.add(PathHelper.recoverEnclosedCurlyBraces(group));
            newSegment.append("_resteasy_uri_parameter");
            from = matcher.end();
        }
        newSegment.append(pathWithoutEnclosedCurlyBraces, from, pathWithoutEnclosedCurlyBraces.length());
        path = newSegment.toString();

        // Find last path segment
        int start = path.lastIndexOf('/');
        if (start < 0) {
            start = 0;
        }

        final int matrixIndex = path.indexOf(';', start);
        if (matrixIndex > -1) {

            final String matrixParams = path.substring(matrixIndex + 1);
            path = path.substring(0, matrixIndex);
            final MultivaluedMap<String, String> map = new MultivaluedHashMap<>();

            final String[] params = matrixParams.split(";");
            for (final String param : params) {
                final int idx = param.indexOf('=');
                if (idx < 0) {
                    map.add(param, null);
                } else {
                    final String theName = param.substring(0, idx);
                    String value = "";
                    if (idx + 1 < param.length()) {
                        value = param.substring(idx + 1);
                    }
                    map.add(theName, value);
                }
            }
            map.remove(name);
            for (final String theName : map.keySet()) {
                final List<String> vals = map.get(theName);
                for (final Object val : vals) {
                    if (val == null) {
                        path += ";" + theName;
                    } else {
                        path += ";" + theName + "=" + val.toString();
                    }
                }
            }
        }
        if (values != null && values.length > 0) {
            matrixParam(name, values);
        }

        // put back all path param expressions
        if (foundParam) {
            matcher = PARAM_REPLACEMENT.matcher(path);
            newSegment = new StringBuilder();
            int i = 0;
            from = 0;
            while (matcher.find()) {
                newSegment.append(path, from, matcher.start());
                newSegment.append(pathParams.get(i++));
                from = matcher.end();
            }
            newSegment.append(path, from, path.length());
            path = newSegment.toString();
        }
        return this;
    }


    /**
     * Called by ClientRequest.getUri() to add a query parameter for {@code @QueryParam} parameters.
     * We do not use UriBuilder.queryParam() because
     * <ul>
     * <li>queryParam() supports URI template processing and this method must always encode braces
     * (for parameter substitution is not possible for {@code @QueryParam} parameters).
     * <li>queryParam() supports "contextual URI encoding" (i.e., it does not encode {@code %}
     * characters that are followed by two hex characters). The JavaDoc for
     * {@code @QueryParam.value()} explicitly states that the value is specified in decoded format
     * and that "any percent encoded literals within the value will not be decoded and will instead
     * be treated as literal text". This means that it is an explicit bug to perform contextual URI
     * encoding of this method's name parameter; hence, we must always encode said parameter. This
     * method also foregoes contextual URI encoding on this method's value parameter because it
     * represents arbitrary data passed to a {@code QueryParam} parameter of a client proxy (since
     * the client proxy is nothing more than a transport layer, it should not be "interpreting" such
     * data; instead, it should faithfully transmit this data over the wire).
     * </ul>
     *
     * @param name the name of the query parameter.
     * @param value the value of the query parameter.
     * @return Returns this instance to allow call chaining.
     */
    public UriBuilder clientQueryParam(final String name, final Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (value == null) {
            throw new IllegalArgumentException("Illegal parameters " + value);
        }
        if (query == null) {
            query = "";
        } else {
            query += "&";
        }
        query += Encode.encodeQueryParamAsIs(name) + "=" + Encode.encodeQueryParamAsIs(value.toString());
        return this;
    }


    @Override
    public UriBuilder queryParam(final String name, final Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (values == null) {
            throw new IllegalArgumentException("Illegal parameters " + values);
        }
        for (final Object value : values) {
            if (value == null) {
                throw new IllegalArgumentException("Illegal parameters " + values);
            }
            if (query == null) {
                query = "";
            } else {
                query += "&";
            }
            query += Encode.encodeQueryParam(name) + "=" + Encode.encodeQueryParam(value.toString());
        }
        return this;
    }


    @Override
    public UriBuilder replaceQueryParam(final String name, final Object... values) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (query == null || query.equals("")) {
            if (values != null) {
                return queryParam(name, values);
            }
            return this;
        }

        final String[] params = query.split("&");
        query = null;

        final String replacedName = Encode.encodeQueryParam(name);

        for (final String param : params) {
            final int pos = param.indexOf('=');
            if (pos >= 0) {
                final String paramName = param.substring(0, pos);
                if (paramName.equals(replacedName)) {
                    continue;
                }
            } else {
                if (param.equals(replacedName)) {
                    continue;
                }
            }
            if (query == null) {
                query = "";
            } else {
                query += "&";
            }
            query += param;
        }
        // don't set values if values is null
        if (values == null) {
            return this;
        }
        return queryParam(name, values);
    }


    public String getHost() {
        return host;
    }


    public String getScheme() {
        return scheme;
    }


    public int getPort() {
        return port;
    }


    public String getUserInfo() {
        return userInfo;
    }


    public String getPath() {
        return path;
    }


    public String getQuery() {
        return query;
    }


    public String getFragment() {
        return fragment;
    }


    @Override
    public UriBuilder segment(final String... segments) throws IllegalArgumentException {
        if (segments == null) {
            throw new IllegalArgumentException("Illegal segments " + segments);
        }
        for (final String segment : segments) {
            if (segment == null) {
                throw new IllegalArgumentException("Illegal segments " + segments);
            }
            path(Encode.encodePathSegment(segment));
        }
        return this;
    }


    @Override
    public URI buildFromEncoded(final Object... values) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal values " + values);
        }
        return buildFromValues(false, true, values);
    }


    @Override
    public UriBuilder replacePath(final String path) {
        if (path == null) {
            this.path = null;
            return this;
        }
        this.path = Encode.encodePath(path);
        return this;
    }


    @Override
    public URI build(final Object[] values, final boolean encodeSlashInPath) throws IllegalArgumentException, UriBuilderException {
        if (values == null) {
            throw new IllegalArgumentException("Illegal values " + values);
        }
        return buildFromValues(encodeSlashInPath, false, values);
    }


    @Override
    public String toTemplate() {
        return buildString(new HashMap<String, Object>(), true, true, true);
    }


    @Override
    public UriBuilder resolveTemplate(final String name, final Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (value == null) {
            throw new IllegalArgumentException("Illegal values " + value);
        }
        final HashMap<String, Object> vals = new HashMap<>();
        vals.put(name, value);
        return resolveTemplates(vals);
    }


    @Override
    public UriBuilder resolveTemplates(final Map<String, Object> templateValues) throws IllegalArgumentException {
        if (templateValues == null) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        if (templateValues.containsKey(null)) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        final String str = buildString(templateValues, false, true, true);
        return fromTemplate(str);
    }


    @Override
    public UriBuilder resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (value == null) {
            throw new IllegalArgumentException("Illegal values " + value);
        }
        final HashMap<String, Object> vals = new HashMap<>();
        vals.put(name, value);
        final String str = buildString(vals, false, true, encodeSlashInPath);
        return fromTemplate(str);
    }


    @Override
    public UriBuilder resolveTemplateFromEncoded(final String name, final Object value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("Illegal name " + name);
        }
        if (value == null) {
            throw new IllegalArgumentException("Illegal values " + value);
        }
        final HashMap<String, Object> vals = new HashMap<>();
        vals.put(name, value);
        final String str = buildString(vals, true, true, true);
        return fromTemplate(str);
    }


    @Override
    public UriBuilder resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) throws IllegalArgumentException {
        if (templateValues == null) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        if (templateValues.containsKey(null)) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        final String str = buildString(templateValues, false, true, encodeSlashInPath);
        return fromTemplate(str);
    }


    @Override
    public UriBuilder resolveTemplatesFromEncoded(final Map<String, Object> templateValues) throws IllegalArgumentException {
        if (templateValues == null) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        if (templateValues.containsKey(null)) {
            throw new IllegalArgumentException("Illegal template values " + templateValues);
        }
        final String str = buildString(templateValues, true, true, true);
        return fromTemplate(str);
    }
}
