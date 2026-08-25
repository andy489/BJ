package com.casino.blackjack.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

public class DateUtil {

    public static Integer calcYearsBetween(Date from, LocalDate to) {
        return Period.between(convertToLocalDate(from), to).getYears();
    }

    public static Integer calcYearsBetween(Long epochMillis, LocalDate to) {
        LocalDate date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate();
        return Period.between(date, to).getYears();
    }

    private static LocalDate convertToLocalDate(Date dateToConvert) {
        return LocalDate.ofInstant(dateToConvert.toInstant(), ZoneId.systemDefault());
    }

}