package com.airepublic.tobi.javaserver.boot;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import com.airepublic.tobi.core.Bootstrap;

public class Boot {

    public static void main(final String[] args) throws IOException {
        final LocalDate start = LocalDate.of(2018, 10, 31);
        final LocalDate now = LocalDate.now();
        System.out.println(ChronoUnit.WEEKS.between(start, now) + " weeks \r\n or exactly " + start.until(now).getMonths() + " months " + start.until(now).getDays() + " days \r\n or calculated birthday on " + start.plusWeeks(40) + "\r\n or " + ChronoUnit.DAYS.between(now, start.plusWeeks(40)) + " days to go!");
        Bootstrap.start();
    }
}
