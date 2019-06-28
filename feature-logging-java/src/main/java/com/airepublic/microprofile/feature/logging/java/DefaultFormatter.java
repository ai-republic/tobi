package com.airepublic.microprofile.feature.logging.java;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class DefaultFormatter extends SimpleFormatter implements Serializable {
    private static final long serialVersionUID = 1L;
    private String format = "[%1$tF %1$tT] [%4$-7s] %2$s: %5$s%6$s%n";


    public DefaultFormatter() {
    }


    public DefaultFormatter(final String format) {
        this.format = format;
    }


    public String getFormat() {
        return format;
    }


    public void setFormat(final String format) {
        this.format = format;
    }


    /**
     * Format the given LogRecord.
     * <p>
     * The formatting can be customized by specifying the <a href="../Formatter.html#syntax">format
     * string</a> in the <a href="#formatting"> {@code java.util.logging.SimpleFormatter.format}</a>
     * property. The given {@code LogRecord} will be formatted as if by calling:
     * 
     * <pre>
     *    {@link String#format String.format}(format, date, source, logger, level, message, thrown);
     * </pre>
     * 
     * where the arguments are:<br>
     * <ol>
     * <li>{@code format} - the {@link java.util.Formatter java.util.Formatter} format string
     * specified in the {@code java.util.logging.SimpleFormatter.format} property or the default
     * format.</li>
     * <li>{@code date} - a {@link ZonedDateTime} object representing
     * {@linkplain LogRecord#getInstant() event time} of the log record in the
     * {@link ZoneId#systemDefault()} system time zone.</li>
     * <li>{@code source} - a string representing the caller, if available; otherwise, the logger's
     * name.</li>
     * <li>{@code logger} - the logger's name.</li>
     * <li>{@code level} - the {@linkplain Level#getLocalizedName log level}.</li>
     * <li>{@code message} - the formatted log message returned from the
     * {@link Formatter#formatMessage(LogRecord)} method. It uses {@link java.text.MessageFormat
     * java.text} formatting and does not use the {@code java.util.Formatter
     *     format} argument.</li>
     * <li>{@code thrown} - a string representing the {@linkplain LogRecord#getThrown throwable}
     * associated with the log record and its backtrace beginning with a newline character, if any;
     * otherwise, an empty string.</li>
     * </ol>
     *
     * <p>
     * Some example formats:<br>
     * <ul>
     * <li>{@code java.util.logging.SimpleFormatter.format="%4$s: %5$s [%1$tc]%n"}
     * <p>
     * This prints 1 line with the log level ({@code 4$}), the log message ({@code 5$}) and the
     * timestamp ({@code 1$}) in a square bracket.
     * 
     * <pre>
     *     WARNING: warning message [Tue Mar 22 13:11:31 PDT 2011]
     * </pre>
     * 
     * </li>
     * <li>{@code java.util.logging.SimpleFormatter.format="%1$tc %2$s%n%4$s: %5$s%6$s%n"}
     * <p>
     * This prints 2 lines where the first line includes the timestamp ({@code 1$}) and the source
     * ({@code 2$}); the second line includes the log level ({@code 4$}) and the log message
     * ({@code 5$}) followed with the throwable and its backtrace ({@code 6$}), if any:
     * 
     * <pre>
     *     Tue Mar 22 13:11:31 PDT 2011 MyClass fatal
     *     SEVERE: several message with an exception
     *     java.lang.IllegalArgumentException: invalid argument
     *             at MyClass.mash(MyClass.java:9)
     *             at MyClass.crunch(MyClass.java:6)
     *             at MyClass.main(MyClass.java:3)
     * </pre>
     * 
     * </li>
     * <li>
     * {@code java.util.logging.SimpleFormatter.format="%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS %1$Tp %2$s%n%4$s: %5$s%n"}
     * <p>
     * This prints 2 lines similar to the example above with a different date/time formatting and
     * does not print the throwable and its backtrace:
     * 
     * <pre>
     *     Mar 22, 2011 1:11:31 PM MyClass fatal
     *     SEVERE: several message with an exception
     * </pre>
     * 
     * </li>
     * <li>
     * {@code java.util.logging.SimpleFormatter.format="%1$tb %1$td, %1$tY %1$tl:%1$tM:%1$tS.%1$tN %1$Tp %2$s%n%4$s: %5$s%6$s%n"}
     * <p>
     * Since JDK 9, {@code java.util.logging} uses {@link java.time.Clock#systemUTC() java.time} to
     * create more precise time stamps. The format above can be used to add a {@code .%1$tN} to the
     * date/time formatting so that nanoseconds will also be printed:
     * 
     * <pre>
     *     Feb 06, 2015 5:33:10.279216000 PM example.Main main
     *     INFO: This is a test
     * </pre>
     * 
     * </li>
     * </ul>
     * <p>
     * This method can also be overridden in a subclass. It is recommended to use the
     * {@link Formatter#formatMessage} convenience method to localize and format the message field.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public String format(final LogRecord record) {
        final ZonedDateTime zdt = ZonedDateTime.ofInstant(
                record.getInstant(), ZoneId.systemDefault());
        String source;
        if (record.getSourceClassName() != null) {
            source = record.getSourceClassName();
            if (record.getSourceMethodName() != null) {
                source += " " + record.getSourceMethodName();
            }
        } else {
            source = record.getLoggerName();
        }
        final String message = formatMessage(record);
        String throwable = "";
        if (record.getThrown() != null) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw);
            pw.println();
            record.getThrown().printStackTrace(pw);
            pw.close();
            throwable = sw.toString();
        }
        return String.format(getFormat(), zdt, shortenClassname(source), shortenClassname(record.getLoggerName()), record.getLevel().getLocalizedName(), message, throwable);
    }


    protected String shortenClassname(final String name) {
        final StringTokenizer tokenizer = new StringTokenizer(name, ".");
        final StringBuffer buffer = new StringBuffer();

        while (tokenizer.hasMoreTokens()) {
            final String part = tokenizer.nextToken();

            if (tokenizer.hasMoreTokens()) {
                buffer.append(part.charAt(0)).append(".");
            } else {
                buffer.append(part);
            }
        }

        return buffer.toString();
    }

}
