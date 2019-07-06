package com.airepublic.microprofile.util.http.common;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CookieParser {
    private static final String ancientDate = formatOldCookie(new Date(10000));
    /**
     * US locale - all HTTP dates are in english
     */
    private static final Locale LOCALE_US = Locale.US;
    private static final String tspecials = ",; ";
    private static final String tspecials2 = "()<>@,;:\\\"/[]?={} \t";
    private static final String OLD_COOKIE_PATTERN = "EEE, dd-MMM-yyyy HH:mm:ss z";
    private static final DateFormat oldCookieFormat = new SimpleDateFormat(OLD_COOKIE_PATTERN, LOCALE_US);


    public static String formatOldCookie(final Date d) {
        String ocf = null;
        synchronized (oldCookieFormat) {
            ocf = oldCookieFormat.format(d);
        }
        return ocf;
    }


    public static void formatOldCookie(final Date d, final StringBuffer sb, final FieldPosition fp) {
        synchronized (oldCookieFormat) {
            oldCookieFormat.format(d, sb, fp);
        }
    }


    public static void appendCookieValue(final StringBuffer headerBuf, final int version, final String name, final String value, final String path, final String domain, final String comment, final int maxAge, final boolean isSecure) {
        final StringBuffer buf = new StringBuffer();
        // Servlet implementation checks name
        buf.append(name);
        buf.append("=");
        // Servlet implementation does not check anything else

        maybeQuote2(version, buf, value);

        // Add version 1 specific information
        if (version == 1) {
            // Version=1 ... required
            buf.append("; Version=1");

            // Comment=comment
            if (comment != null) {
                buf.append("; Comment=");
                maybeQuote2(version, buf, comment);
            }
        }

        // Add domain information, if present
        if (domain != null) {
            buf.append("; Domain=");
            maybeQuote2(version, buf, domain);
        }

        // Max-Age=secs ... or use old "Expires" format
        // TODO RFC2965 Discard
        if (maxAge >= 0) {
            if (version == 0) {
                // Wdy, DD-Mon-YY HH:MM:SS GMT ( Expires Netscape format )
                buf.append("; Expires=");
                // To expire immediately we need to set the time in past
                if (maxAge == 0) {
                    buf.append(ancientDate);
                } else {
                    formatOldCookie(new Date(System.currentTimeMillis() +
                            maxAge * 1000L), buf,
                            new FieldPosition(0));
                }

            } else {
                buf.append("; Max-Age=");
                buf.append(maxAge);
            }
        }

        // Path=path
        if (path != null) {
            buf.append("; Path=");
            maybeQuote2(version, buf, path);
        }

        // Secure
        if (isSecure) {
            buf.append("; Secure");
        }

        headerBuf.append(buf);
    }


    /**
     * Quotes values using rules that vary depending on Cookie version.
     *
     * @param version cookie version
     * @param buf buffer
     * @param value value
     */
    public static void maybeQuote2(final int version, final StringBuffer buf, final String value) {
        if (value == null || value.length() == 0) {
            buf.append("\"\"");
        } else if (containsCTL(value, version)) {
            throw new IllegalArgumentException("Control character in cookie value");
        } else if (alreadyQuoted(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 1, value.length() - 1));
            buf.append('"');
        } else if (version == 0 && !isToken(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 0, value.length()));
            buf.append('"');
        } else if (version == 1 && !isToken2(value)) {
            buf.append('"');
            buf.append(escapeDoubleQuotes(value, 0, value.length()));
            buf.append('"');
        } else {
            buf.append(value);
        }
    }


    public static boolean alreadyQuoted(final String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        return value.charAt(0) == '\"' && value.charAt(value.length() - 1) == '\"';
    }


    /**
     * Escapes any double quotes in the given string.
     *
     * @param s the input string
     * @param beginIndex start index inclusive
     * @param endIndex exclusive
     * @return The (possibly) escaped string
     */
    private static String escapeDoubleQuotes(final String s, final int beginIndex, final int endIndex) {

        if (s == null || s.length() == 0 || s.indexOf('"') == -1) {
            return s;
        }

        final StringBuffer b = new StringBuffer();
        for (int i = beginIndex; i < endIndex; i++) {
            final char c = s.charAt(i);
            if (c == '\\') {
                b.append(c);
                // ignore the character after an escape, just append it
                if (++i >= endIndex) {
                    throw new IllegalArgumentException("Invalid escape character in cookie value");
                }
                b.append(s.charAt(i));
            } else if (c == '"') {
                b.append('\\').append('"');
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }


    public static boolean containsCTL(final String value, final int version) {
        if (value == null) {
            return false;
        }
        final int len = value.length();
        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (c < 0x20 || c >= 0x7f) {
                if (c == 0x09) {
                    continue; // allow horizontal tabs
                }
                return true;
            }
        }
        return false;
    }


    /**
     * Tests a string and returns true if the string counts as a reserved token in the Java
     * language.
     *
     * @param value the <code>String</code> to be tested
     *
     * @return <code>true</code> if the <code>String</code> is a reserved token; <code>false</code>
     *         if it is not
     */
    public static boolean isToken(final String value) {
        if (value == null) {
            return true;
        }
        final int len = value.length();

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);

            if (tspecials.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }


    public static boolean isToken2(final String value) {
        if (value == null) {
            return true;
        }
        final int len = value.length();

        for (int i = 0; i < len; i++) {
            final char c = value.charAt(i);
            if (tspecials2.indexOf(c) != -1) {
                return false;
            }
        }
        return true;
    }
}
