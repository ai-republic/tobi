/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.airepublic.microprofile.plugin.http.websocket.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.airepublic.microprofile.feature.logging.java.SerializableLogger;
import com.airepublic.microprofile.plugin.http.websocket.util.res.StringManager;

/**
 * Utils for introspection and reflection
 */
public final class IntrospectionUtils {

    private static final Logger log = new SerializableLogger(IntrospectionUtils.class.getName());
    private static final StringManager sm = StringManager.getManager(IntrospectionUtils.class);


    /**
     * Find a method with the right name If found, call the method ( if param is int or boolean
     * we'll convert value to the right type before) - that means you can have setDebug(1).
     * 
     * @param o The object to set a property on
     * @param name The property name
     * @param value The property value
     * @return <code>true</code> if operation was successful
     */
    public static boolean setProperty(final Object o, final String name, final String value) {
        return setProperty(o, name, value, true);
    }


    @SuppressWarnings("null") // setPropertyMethodVoid is not null when used
    public static boolean setProperty(final Object o, final String name, final String value,
            final boolean invokeSetProperty) {
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "IntrospectionUtils: setProperty(" + o.getClass() + " " + name + "=" + value + ")");
        }

        final String setter = "set" + capitalize(name);

        try {
            final Method methods[] = findMethods(o.getClass());
            Method setPropertyMethodVoid = null;
            Method setPropertyMethodBool = null;

            // First, the ideal case - a setFoo( String ) method
            for (final Method method : methods) {
                final Class<?> paramT[] = method.getParameterTypes();
                if (setter.equals(method.getName()) && paramT.length == 1
                        && "java.lang.String".equals(paramT[0].getName())) {

                    method.invoke(o, new Object[] { value });
                    return true;
                }
            }

            // Try a setFoo ( int ) or ( boolean )
            for (final Method method : methods) {
                boolean ok = true;
                if (setter.equals(method.getName())
                        && method.getParameterTypes().length == 1) {

                    // match - find the type and invoke it
                    final Class<?> paramType = method.getParameterTypes()[0];
                    final Object params[] = new Object[1];

                    // Try a setFoo ( int )
                    if ("java.lang.Integer".equals(paramType.getName())
                            || "int".equals(paramType.getName())) {
                        try {
                            params[0] = Integer.valueOf(value);
                        } catch (final NumberFormatException ex) {
                            ok = false;
                        }
                        // Try a setFoo ( long )
                    } else if ("java.lang.Long".equals(paramType.getName())
                            || "long".equals(paramType.getName())) {
                        try {
                            params[0] = Long.valueOf(value);
                        } catch (final NumberFormatException ex) {
                            ok = false;
                        }

                        // Try a setFoo ( boolean )
                    } else if ("java.lang.Boolean".equals(paramType.getName())
                            || "boolean".equals(paramType.getName())) {
                        params[0] = Boolean.valueOf(value);

                        // Try a setFoo ( InetAddress )
                    } else if ("java.net.InetAddress".equals(paramType
                            .getName())) {
                        try {
                            params[0] = InetAddress.getByName(value);
                        } catch (final UnknownHostException exc) {
                            if (log.isLoggable(Level.FINEST)) {
                                log.log(Level.FINEST, "IntrospectionUtils: Unable to resolve host name:" + value);
                            }
                            ok = false;
                        }

                        // Unknown type
                    } else {
                        if (log.isLoggable(Level.FINEST)) {
                            log.log(Level.FINEST, "IntrospectionUtils: Unknown type " +
                                    paramType.getName());
                        }
                    }

                    if (ok) {
                        method.invoke(o, params);
                        return true;
                    }
                }

                // save "setProperty" for later
                if ("setProperty".equals(method.getName())) {
                    if (method.getReturnType() == Boolean.TYPE) {
                        setPropertyMethodBool = method;
                    } else {
                        setPropertyMethodVoid = method;
                    }

                }
            }

            // Ok, no setXXX found, try a setProperty("name", "value")
            if (invokeSetProperty && (setPropertyMethodBool != null ||
                    setPropertyMethodVoid != null)) {
                final Object params[] = new Object[2];
                params[0] = name;
                params[1] = value;
                if (setPropertyMethodBool != null) {
                    try {
                        return ((Boolean) setPropertyMethodBool.invoke(o,
                                params)).booleanValue();
                    } catch (final IllegalArgumentException biae) {
                        // the boolean method had the wrong
                        // parameter types. lets try the other
                        if (setPropertyMethodVoid != null) {
                            setPropertyMethodVoid.invoke(o, params);
                            return true;
                        } else {
                            throw biae;
                        }
                    }
                } else {
                    setPropertyMethodVoid.invoke(o, params);
                    return true;
                }
            }

        } catch (IllegalArgumentException | SecurityException | IllegalAccessException e) {
            log.log(Level.WARNING, sm.getString("introspectionUtils.setPropertyError", name, value, o.getClass()), e);
        } catch (final InvocationTargetException e) {
            ExceptionUtils.handleThrowable(e.getCause());
            log.log(Level.WARNING, sm.getString("introspectionUtils.setPropertyError", name, value, o.getClass()), e);
        }
        return false;
    }


    public static Object getProperty(final Object o, final String name) {
        final String getter = "get" + capitalize(name);
        final String isGetter = "is" + capitalize(name);

        try {
            final Method methods[] = findMethods(o.getClass());
            Method getPropertyMethod = null;

            // First, the ideal case - a getFoo() method
            for (final Method method : methods) {
                final Class<?> paramT[] = method.getParameterTypes();
                if (getter.equals(method.getName()) && paramT.length == 0) {
                    return method.invoke(o, (Object[]) null);
                }
                if (isGetter.equals(method.getName()) && paramT.length == 0) {
                    return method.invoke(o, (Object[]) null);
                }

                if ("getProperty".equals(method.getName())) {
                    getPropertyMethod = method;
                }
            }

            // Ok, no setXXX found, try a getProperty("name")
            if (getPropertyMethod != null) {
                final Object params[] = new Object[1];
                params[0] = name;
                return getPropertyMethod.invoke(o, params);
            }

        } catch (IllegalArgumentException | SecurityException | IllegalAccessException e) {
            log.log(Level.WARNING, sm.getString("introspectionUtils.getPropertyError", name, o.getClass()), e);
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof NullPointerException) {
                // Assume the underlying object uses a storage to represent an unset property
                return null;
            }
            ExceptionUtils.handleThrowable(e.getCause());
            log.log(Level.WARNING, sm.getString("introspectionUtils.getPropertyError", name, o.getClass()), e);
        }
        return null;
    }


    /**
     * Replace ${NAME} with the property value.
     * 
     * @param value The value
     * @param staticProp Replacement properties
     * @param dynamicProp Replacement properties
     * @return the replacement value
     */
    public static String replaceProperties(final String value,
            final Hashtable<Object, Object> staticProp, final PropertySource dynamicProp[]) {
        if (value.indexOf('$') < 0) {
            return value;
        }
        final StringBuilder sb = new StringBuilder();
        int prev = 0;
        // assert value!=nil
        int pos;
        while ((pos = value.indexOf('$', prev)) >= 0) {
            if (pos > 0) {
                sb.append(value.substring(prev, pos));
            }
            if (pos == value.length() - 1) {
                sb.append('$');
                prev = pos + 1;
            } else if (value.charAt(pos + 1) != '{') {
                sb.append('$');
                prev = pos + 1; // XXX
            } else {
                final int endName = value.indexOf('}', pos);
                if (endName < 0) {
                    sb.append(value.substring(pos));
                    prev = value.length();
                    continue;
                }
                final String n = value.substring(pos + 2, endName);
                String v = null;
                if (staticProp != null) {
                    v = (String) staticProp.get(n);
                }
                if (v == null && dynamicProp != null) {
                    for (final PropertySource element : dynamicProp) {
                        v = element.getProperty(n);
                        if (v != null) {
                            break;
                        }
                    }
                }
                if (v == null) {
                    v = "${" + n + "}";
                }

                sb.append(v);
                prev = endName + 1;
            }
        }
        if (prev < value.length()) {
            sb.append(value.substring(prev));
        }
        return sb.toString();
    }


    /**
     * Reverse of Introspector.decapitalize.
     * 
     * @param name The name
     * @return the capitalized string
     */
    public static String capitalize(final String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        final char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }


    // -------------------- other utils --------------------
    public static void clear() {
        objectMethods.clear();
    }

    private static final Hashtable<Class<?>, Method[]> objectMethods = new Hashtable<>();


    public static Method[] findMethods(final Class<?> c) {
        Method methods[] = objectMethods.get(c);
        if (methods != null) {
            return methods;
        }

        methods = c.getMethods();
        objectMethods.put(c, methods);
        return methods;
    }


    @SuppressWarnings("null") // params cannot be null when comparing lengths
    public static Method findMethod(final Class<?> c, final String name,
            final Class<?> params[]) {
        final Method methods[] = findMethods(c);
        for (final Method method : methods) {
            if (method.getName().equals(name)) {
                final Class<?> methodParams[] = method.getParameterTypes();
                if (params == null && methodParams.length == 0) {
                    return method;
                }
                if (params.length != methodParams.length) {
                    continue;
                }
                boolean found = true;
                for (int j = 0; j < params.length; j++) {
                    if (params[j] != methodParams[j]) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return method;
                }
            }
        }
        return null;
    }


    public static Object callMethod1(final Object target, final String methodN,
            final Object param1, final String typeParam1, final ClassLoader cl) throws Exception {
        if (target == null || methodN == null || param1 == null) {
            throw new IllegalArgumentException(sm.getString("introspectionUtils.nullParameter"));
        }
        if (log.isLoggable(Level.FINEST)) {
            log.log(Level.FINEST, "IntrospectionUtils: callMethod1 " + target.getClass().getName() + " " + param1.getClass().getName() + " " + typeParam1);
        }

        final Class<?> params[] = new Class[1];
        if (typeParam1 == null) {
            params[0] = param1.getClass();
        } else {
            params[0] = cl.loadClass(typeParam1);
        }
        final Method m = findMethod(target.getClass(), methodN, params);
        if (m == null) {
            throw new NoSuchMethodException(target.getClass().getName() + " "
                    + methodN);
        }
        try {
            return m.invoke(target, new Object[] { param1 });
        } catch (final InvocationTargetException ie) {
            ExceptionUtils.handleThrowable(ie.getCause());
            throw ie;
        }
    }


    public static Object callMethodN(final Object target, final String methodN,
            final Object params[], final Class<?> typeParams[]) throws Exception {
        Method m = null;
        m = findMethod(target.getClass(), methodN, typeParams);
        if (m == null) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "IntrospectionUtils: Can't find method " + methodN + " in " + target + " CLASS " + target.getClass());
            }
            return null;
        }
        try {
            final Object o = m.invoke(target, params);

            if (log.isLoggable(Level.FINEST)) {
                // debug
                final StringBuilder sb = new StringBuilder();
                sb.append(target.getClass().getName()).append('.')
                        .append(methodN).append("( ");
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(params[i]);
                }
                sb.append(")");
                log.log(Level.FINEST, "IntrospectionUtils:" + sb.toString());
            }
            return o;
        } catch (final InvocationTargetException ie) {
            ExceptionUtils.handleThrowable(ie.getCause());
            throw ie;
        }
    }


    public static Object convert(final String object, final Class<?> paramType) {
        Object result = null;
        if ("java.lang.String".equals(paramType.getName())) {
            result = object;
        } else if ("java.lang.Integer".equals(paramType.getName())
                || "int".equals(paramType.getName())) {
            try {
                result = Integer.valueOf(object);
            } catch (final NumberFormatException ex) {
            }
            // Try a setFoo ( boolean )
        } else if ("java.lang.Boolean".equals(paramType.getName())
                || "boolean".equals(paramType.getName())) {
            result = Boolean.valueOf(object);

            // Try a setFoo ( InetAddress )
        } else if ("java.net.InetAddress".equals(paramType
                .getName())) {
            try {
                result = InetAddress.getByName(object);
            } catch (final UnknownHostException exc) {
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "IntrospectionUtils: Unable to resolve host name:" + object);
                }
            }

            // Unknown type
        } else {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "IntrospectionUtils: Unknown type " + paramType.getName());
            }
        }
        if (result == null) {
            throw new IllegalArgumentException(sm.getString("introspectionUtils.conversionError", object, paramType.getName()));
        }
        return result;
    }


    /**
     * Checks to see if the specified class is an instance of or assignable from the specified type.
     * The class <code>clazz</code>, all its superclasses, interfaces and those superinterfaces are
     * tested for a match against the type name <code>type</code>.
     *
     * This is similar to <code>instanceof</code> or {@link Class#isAssignableFrom} except that the
     * target type will not be resolved into a Class object, which provides some security and memory
     * benefits.
     *
     * @param clazz The class to test for a match.
     * @param type The name of the type that <code>clazz</code> must be.
     *
     * @return <code>true</code> if the <code>clazz</code> tested is an instance of the specified
     *         <code>type</code>, <code>false</code> otherwise.
     */
    public static boolean isInstance(final Class<?> clazz, final String type) {
        if (type.equals(clazz.getName())) {
            return true;
        }

        final Class<?>[] ifaces = clazz.getInterfaces();
        for (final Class<?> iface : ifaces) {
            if (isInstance(iface, type)) {
                return true;
            }
        }

        final Class<?> superClazz = clazz.getSuperclass();
        if (superClazz == null) {
            return false;
        } else {
            return isInstance(superClazz, type);
        }
    }

    // -------------------- Get property --------------------
    // This provides a layer of abstraction

    public interface PropertySource {

        String getProperty(String key);

    }

}
