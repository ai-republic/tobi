/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.airepublic.tobi.plugin.http.websocket;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.websocket.CloseReason.CloseCode;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Decoder;
import javax.websocket.Decoder.Binary;
import javax.websocket.Decoder.BinaryStream;
import javax.websocket.Decoder.Text;
import javax.websocket.Decoder.TextStream;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.PongMessage;
import javax.websocket.Session;

import com.airepublic.tobi.plugin.http.websocket.pojo.PojoMessageHandlerPartialBinary;
import com.airepublic.tobi.plugin.http.websocket.pojo.PojoMessageHandlerWholeBinary;
import com.airepublic.tobi.plugin.http.websocket.pojo.PojoMessageHandlerWholeText;
import com.airepublic.tobi.plugin.http.websocket.util.res.StringManager;

/**
 * Utility class for internal use only within this package.
 */
public class Util {

    private static final StringManager sm = StringManager.getManager(Util.class);
    private static final Queue<SecureRandom> randoms = new ConcurrentLinkedQueue<>();


    private Util() {
        // Hide default constructor
    }


    static boolean isControl(final byte opCode) {
        return (opCode & 0x08) != 0;
    }


    static boolean isText(final byte opCode) {
        return opCode == Constants.OPCODE_TEXT;
    }


    static boolean isContinuation(final byte opCode) {
        return opCode == Constants.OPCODE_CONTINUATION;
    }


    static CloseCode getCloseCode(final int code) {
        if (code > 2999 && code < 5000) {
            return CloseCodes.getCloseCode(code);
        }
        switch (code) {
            case 1000:
                return CloseCodes.NORMAL_CLOSURE;
            case 1001:
                return CloseCodes.GOING_AWAY;
            case 1002:
                return CloseCodes.PROTOCOL_ERROR;
            case 1003:
                return CloseCodes.CANNOT_ACCEPT;
            case 1004:
                // Should not be used in a close frame
                // return CloseCodes.RESERVED;
                return CloseCodes.PROTOCOL_ERROR;
            case 1005:
                // Should not be used in a close frame
                // return CloseCodes.NO_STATUS_CODE;
                return CloseCodes.PROTOCOL_ERROR;
            case 1006:
                // Should not be used in a close frame
                // return CloseCodes.CLOSED_ABNORMALLY;
                return CloseCodes.PROTOCOL_ERROR;
            case 1007:
                return CloseCodes.NOT_CONSISTENT;
            case 1008:
                return CloseCodes.VIOLATED_POLICY;
            case 1009:
                return CloseCodes.TOO_BIG;
            case 1010:
                return CloseCodes.NO_EXTENSION;
            case 1011:
                return CloseCodes.UNEXPECTED_CONDITION;
            case 1012:
                // Not in RFC6455
                // return CloseCodes.SERVICE_RESTART;
                return CloseCodes.PROTOCOL_ERROR;
            case 1013:
                // Not in RFC6455
                // return CloseCodes.TRY_AGAIN_LATER;
                return CloseCodes.PROTOCOL_ERROR;
            case 1015:
                // Should not be used in a close frame
                // return CloseCodes.TLS_HANDSHAKE_FAILURE;
                return CloseCodes.PROTOCOL_ERROR;
            default:
                return CloseCodes.PROTOCOL_ERROR;
        }
    }


    static byte[] generateMask() {
        // SecureRandom is not thread-safe so need to make sure only one thread
        // uses it at a time. In theory, the pool could grow to the same size
        // as the number of request processing threads. In reality it will be
        // a lot smaller.

        // Get a SecureRandom from the pool
        SecureRandom sr = randoms.poll();

        // If one isn't available, generate a new one
        if (sr == null) {
            try {
                sr = SecureRandom.getInstance("SHA1PRNG");
            } catch (final NoSuchAlgorithmException e) {
                // Fall back to platform default
                sr = new SecureRandom();
            }
        }

        // Generate the mask
        final byte[] result = new byte[4];
        sr.nextBytes(result);

        // Put the SecureRandom back in the poll
        randoms.add(sr);

        return result;
    }


    static Class<?> getMessageType(final MessageHandler listener) {
        return Util.getGenericType(MessageHandler.class,
                listener.getClass()).getClazz();
    }


    private static Class<?> getDecoderType(final Class<? extends Decoder> decoder) {
        return Util.getGenericType(Decoder.class, decoder).getClazz();
    }


    static Class<?> getEncoderType(final Class<? extends Encoder> encoder) {
        return Util.getGenericType(Encoder.class, encoder).getClazz();
    }


    private static <T> TypeResult getGenericType(final Class<T> type,
            final Class<? extends T> clazz) {

        // Look to see if this class implements the interface of interest

        // Get all the interfaces
        final Type[] interfaces = clazz.getGenericInterfaces();
        for (final Type iface : interfaces) {
            // Only need to check interfaces that use generics
            if (iface instanceof ParameterizedType) {
                final ParameterizedType pi = (ParameterizedType) iface;
                // Look for the interface of interest
                if (pi.getRawType() instanceof Class) {
                    if (type.isAssignableFrom((Class<?>) pi.getRawType())) {
                        return getTypeParameter(
                                clazz, pi.getActualTypeArguments()[0]);
                    }
                }
            }
        }

        // Interface not found on this class. Look at the superclass.
        @SuppressWarnings("unchecked")
        final Class<? extends T> superClazz = (Class<? extends T>) clazz.getSuperclass();
        if (superClazz == null) {
            // Finished looking up the class hierarchy without finding anything
            return null;
        }

        TypeResult superClassTypeResult = getGenericType(type, superClazz);
        final int dimension = superClassTypeResult.getDimension();
        if (superClassTypeResult.getIndex() == -1 && dimension == 0) {
            // Superclass implements interface and defines explicit type for
            // the interface of interest
            return superClassTypeResult;
        }

        if (superClassTypeResult.getIndex() > -1) {
            // Superclass implements interface and defines unknown type for
            // the interface of interest
            // Map that unknown type to the generic types defined in this class
            final ParameterizedType superClassType = (ParameterizedType) clazz.getGenericSuperclass();
            final TypeResult result = getTypeParameter(clazz,
                    superClassType.getActualTypeArguments()[superClassTypeResult.getIndex()]);
            result.incrementDimension(superClassTypeResult.getDimension());
            if (result.getClazz() != null && result.getDimension() > 0) {
                superClassTypeResult = result;
            } else {
                return result;
            }
        }

        if (superClassTypeResult.getDimension() > 0) {
            final StringBuilder className = new StringBuilder();
            for (int i = 0; i < dimension; i++) {
                className.append('[');
            }
            className.append('L');
            className.append(superClassTypeResult.getClazz().getCanonicalName());
            className.append(';');

            Class<?> arrayClazz;
            try {
                arrayClazz = Class.forName(className.toString());
            } catch (final ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }

            return new TypeResult(arrayClazz, -1, 0);
        }

        // Error will be logged further up the call stack
        return null;
    }


    /*
     * For a generic parameter, return either the Class used or if the type is unknown, the index
     * for the type in definition of the class
     */
    private static TypeResult getTypeParameter(final Class<?> clazz, final Type argType) {
        if (argType instanceof Class<?>) {
            return new TypeResult((Class<?>) argType, -1, 0);
        } else if (argType instanceof ParameterizedType) {
            return new TypeResult((Class<?>) ((ParameterizedType) argType).getRawType(), -1, 0);
        } else if (argType instanceof GenericArrayType) {
            final Type arrayElementType = ((GenericArrayType) argType).getGenericComponentType();
            final TypeResult result = getTypeParameter(clazz, arrayElementType);
            result.incrementDimension(1);
            return result;
        } else {
            final TypeVariable<?>[] tvs = clazz.getTypeParameters();
            for (int i = 0; i < tvs.length; i++) {
                if (tvs[i].equals(argType)) {
                    return new TypeResult(null, i, 0);
                }
            }
            return null;
        }
    }


    public static boolean isPrimitive(final Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return true;
        } else if (clazz.equals(Boolean.class) ||
                clazz.equals(Byte.class) ||
                clazz.equals(Character.class) ||
                clazz.equals(Double.class) ||
                clazz.equals(Float.class) ||
                clazz.equals(Integer.class) ||
                clazz.equals(Long.class) ||
                clazz.equals(Short.class)) {
            return true;
        }
        return false;
    }


    public static Object coerceToType(final Class<?> type, final String value) {
        if (type.equals(String.class)) {
            return value;
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.valueOf(value);
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Byte.valueOf(value);
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return Character.valueOf(value.charAt(0));
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.valueOf(value);
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.valueOf(value);
        } else if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.valueOf(value);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.valueOf(value);
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Short.valueOf(value);
        } else {
            throw new IllegalArgumentException(sm.getString(
                    "util.invalidType", value, type.getName()));
        }
    }


    public static List<DecoderEntry> getDecoders(
            final List<Class<? extends Decoder>> decoderClazzes)
            throws DeploymentException {

        final List<DecoderEntry> result = new ArrayList<>();
        if (decoderClazzes != null) {
            for (final Class<? extends Decoder> decoderClazz : decoderClazzes) {
                // Need to instantiate decoder to ensure it is valid and that
                // deployment can be failed if it is not
                @SuppressWarnings("unused")
                Decoder instance;
                try {
                    instance = decoderClazz.getConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    throw new DeploymentException(
                            sm.getString("pojoMethodMapping.invalidDecoder",
                                    decoderClazz.getName()),
                            e);
                }
                final DecoderEntry entry = new DecoderEntry(
                        Util.getDecoderType(decoderClazz), decoderClazz);
                result.add(entry);
            }
        }

        return result;
    }


    static Set<MessageHandlerResult> getMessageHandlers(final Class<?> target,
            final MessageHandler listener, final EndpointConfig endpointConfig,
            final Session session) {

        // Will never be more than 2 types
        final Set<MessageHandlerResult> results = new HashSet<>(2);

        // Simple cases - handlers already accepts one of the types expected by
        // the frame handling code
        if (String.class.isAssignableFrom(target)) {
            final MessageHandlerResult result = new MessageHandlerResult(listener,
                    MessageHandlerResultType.TEXT);
            results.add(result);
        } else if (ByteBuffer.class.isAssignableFrom(target)) {
            final MessageHandlerResult result = new MessageHandlerResult(listener,
                    MessageHandlerResultType.BINARY);
            results.add(result);
        } else if (PongMessage.class.isAssignableFrom(target)) {
            final MessageHandlerResult result = new MessageHandlerResult(listener,
                    MessageHandlerResultType.PONG);
            results.add(result);
            // Handler needs wrapping and optional decoder to convert it to one of
            // the types expected by the frame handling code
        } else if (byte[].class.isAssignableFrom(target)) {
            final boolean whole = MessageHandler.Whole.class.isAssignableFrom(listener.getClass());
            final MessageHandlerResult result = new MessageHandlerResult(
                    whole ? new PojoMessageHandlerWholeBinary(listener,
                            getOnMessageMethod(listener), session,
                            endpointConfig, matchDecoders(target, endpointConfig, true),
                            new Object[1], 0, true, -1, false, -1)
                            : new PojoMessageHandlerPartialBinary(listener,
                                    getOnMessagePartialMethod(listener), session,
                                    new Object[2], 0, true, 1, -1, -1),
                    MessageHandlerResultType.BINARY);
            results.add(result);
        } else if (InputStream.class.isAssignableFrom(target)) {
            final MessageHandlerResult result = new MessageHandlerResult(
                    new PojoMessageHandlerWholeBinary(listener,
                            getOnMessageMethod(listener), session,
                            endpointConfig, matchDecoders(target, endpointConfig, true),
                            new Object[1], 0, true, -1, true, -1),
                    MessageHandlerResultType.BINARY);
            results.add(result);
        } else if (Reader.class.isAssignableFrom(target)) {
            final MessageHandlerResult result = new MessageHandlerResult(
                    new PojoMessageHandlerWholeText(listener,
                            getOnMessageMethod(listener), session,
                            endpointConfig, matchDecoders(target, endpointConfig, false),
                            new Object[1], 0, true, -1, -1),
                    MessageHandlerResultType.TEXT);
            results.add(result);
        } else {
            // Handler needs wrapping and requires decoder to convert it to one
            // of the types expected by the frame handling code
            final DecoderMatch decoderMatch = matchDecoders(target, endpointConfig);
            final Method m = getOnMessageMethod(listener);
            if (decoderMatch.getBinaryDecoders().size() > 0) {
                final MessageHandlerResult result = new MessageHandlerResult(
                        new PojoMessageHandlerWholeBinary(listener, m, session,
                                endpointConfig,
                                decoderMatch.getBinaryDecoders(), new Object[1],
                                0, false, -1, false, -1),
                        MessageHandlerResultType.BINARY);
                results.add(result);
            }
            if (decoderMatch.getTextDecoders().size() > 0) {
                final MessageHandlerResult result = new MessageHandlerResult(
                        new PojoMessageHandlerWholeText(listener, m, session,
                                endpointConfig,
                                decoderMatch.getTextDecoders(), new Object[1],
                                0, false, -1, -1),
                        MessageHandlerResultType.TEXT);
                results.add(result);
            }
        }

        if (results.size() == 0) {
            throw new IllegalArgumentException(
                    sm.getString("wsSession.unknownHandler", listener, target));
        }

        return results;
    }


    private static List<Class<? extends Decoder>> matchDecoders(final Class<?> target,
            final EndpointConfig endpointConfig, final boolean binary) {
        final DecoderMatch decoderMatch = matchDecoders(target, endpointConfig);
        if (binary) {
            if (decoderMatch.getBinaryDecoders().size() > 0) {
                return decoderMatch.getBinaryDecoders();
            }
        } else if (decoderMatch.getTextDecoders().size() > 0) {
            return decoderMatch.getTextDecoders();
        }
        return null;
    }


    private static DecoderMatch matchDecoders(final Class<?> target,
            final EndpointConfig endpointConfig) {
        DecoderMatch decoderMatch;
        try {
            final List<Class<? extends Decoder>> decoders = endpointConfig.getDecoders();
            final List<DecoderEntry> decoderEntries = getDecoders(decoders);
            decoderMatch = new DecoderMatch(target, decoderEntries);
        } catch (final DeploymentException e) {
            throw new IllegalArgumentException(e);
        }
        return decoderMatch;
    }


    public static void parseExtensionHeader(final List<Extension> extensions,
            final String header) {
        // The relevant ABNF for the Sec-WebSocket-Extensions is as follows:
        // extension-list = 1#extension
        // extension = extension-token *( ";" extension-param )
        // extension-token = registered-token
        // registered-token = token
        // extension-param = token [ "=" (token | quoted-string) ]
        // ; When using the quoted-string syntax variant, the value
        // ; after quoted-string unescaping MUST conform to the
        // ; 'token' ABNF.
        //
        // The limiting of parameter values to tokens or "quoted tokens" makes
        // the parsing of the header significantly simpler and allows a number
        // of short-cuts to be taken.

        // Step one, split the header into individual extensions using ',' as a
        // separator
        final String unparsedExtensions[] = header.split(",");
        for (final String unparsedExtension : unparsedExtensions) {
            // Step two, split the extension into the registered name and
            // parameter/value pairs using ';' as a separator
            final String unparsedParameters[] = unparsedExtension.split(";");
            final WsExtension extension = new WsExtension(unparsedParameters[0].trim());

            for (int i = 1; i < unparsedParameters.length; i++) {
                final int equalsPos = unparsedParameters[i].indexOf('=');
                String name;
                String value;
                if (equalsPos == -1) {
                    name = unparsedParameters[i].trim();
                    value = null;
                } else {
                    name = unparsedParameters[i].substring(0, equalsPos).trim();
                    value = unparsedParameters[i].substring(equalsPos + 1).trim();
                    final int len = value.length();
                    if (len > 1) {
                        if (value.charAt(0) == '\"' && value.charAt(len - 1) == '\"') {
                            value = value.substring(1, value.length() - 1);
                        }
                    }
                }
                // Make sure value doesn't contain any of the delimiters since
                // that would indicate something went wrong
                if (containsDelims(name) || containsDelims(value)) {
                    throw new IllegalArgumentException(sm.getString(
                            "util.notToken", name, value));
                }
                if (value != null &&
                        (value.indexOf(',') > -1 || value.indexOf(';') > -1 ||
                                value.indexOf('\"') > -1 || value.indexOf('=') > -1)) {
                    throw new IllegalArgumentException(sm.getString("", value));
                }
                extension.addParameter(new WsExtensionParameter(name, value));
            }
            extensions.add(extension);
        }
    }


    private static boolean containsDelims(final String input) {
        if (input == null || input.length() == 0) {
            return false;
        }
        for (final char c : input.toCharArray()) {
            switch (c) {
                case ',':
                case ';':
                case '\"':
                case '=':
                    return true;
                default:
                    // NO_OP
            }

        }
        return false;
    }


    private static Method getOnMessageMethod(final MessageHandler listener) {
        try {
            return listener.getClass().getMethod("onMessage", Object.class);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    sm.getString("util.invalidMessageHandler"), e);
        }
    }


    private static Method getOnMessagePartialMethod(final MessageHandler listener) {
        try {
            return listener.getClass().getMethod("onMessage", Object.class, Boolean.TYPE);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    sm.getString("util.invalidMessageHandler"), e);
        }
    }

    public static class DecoderMatch {

        private final List<Class<? extends Decoder>> textDecoders = new ArrayList<>();
        private final List<Class<? extends Decoder>> binaryDecoders = new ArrayList<>();
        private final Class<?> target;


        public DecoderMatch(final Class<?> target, final List<DecoderEntry> decoderEntries) {
            this.target = target;
            for (final DecoderEntry decoderEntry : decoderEntries) {
                if (decoderEntry.getClazz().isAssignableFrom(target)) {
                    if (Binary.class.isAssignableFrom(
                            decoderEntry.getDecoderClazz())) {
                        binaryDecoders.add(decoderEntry.getDecoderClazz());
                        // willDecode() method means this decoder may or may not
                        // decode a message so need to carry on checking for
                        // other matches
                    } else if (BinaryStream.class.isAssignableFrom(
                            decoderEntry.getDecoderClazz())) {
                        binaryDecoders.add(decoderEntry.getDecoderClazz());
                        // Stream decoders have to process the message so no
                        // more decoders can be matched
                        break;
                    } else if (Text.class.isAssignableFrom(
                            decoderEntry.getDecoderClazz())) {
                        textDecoders.add(decoderEntry.getDecoderClazz());
                        // willDecode() method means this decoder may or may not
                        // decode a message so need to carry on checking for
                        // other matches
                    } else if (TextStream.class.isAssignableFrom(
                            decoderEntry.getDecoderClazz())) {
                        textDecoders.add(decoderEntry.getDecoderClazz());
                        // Stream decoders have to process the message so no
                        // more decoders can be matched
                        break;
                    } else {
                        throw new IllegalArgumentException(
                                sm.getString("util.unknownDecoderType"));
                    }
                }
            }
        }


        public List<Class<? extends Decoder>> getTextDecoders() {
            return textDecoders;
        }


        public List<Class<? extends Decoder>> getBinaryDecoders() {
            return binaryDecoders;
        }


        public Class<?> getTarget() {
            return target;
        }


        public boolean hasMatches() {
            return textDecoders.size() > 0 || binaryDecoders.size() > 0;
        }
    }

    private static class TypeResult {
        private final Class<?> clazz;
        private final int index;
        private int dimension;


        public TypeResult(final Class<?> clazz, final int index, final int dimension) {
            this.clazz = clazz;
            this.index = index;
            this.dimension = dimension;
        }


        public Class<?> getClazz() {
            return clazz;
        }


        public int getIndex() {
            return index;
        }


        public int getDimension() {
            return dimension;
        }


        public void incrementDimension(final int inc) {
            dimension += inc;
        }
    }
}