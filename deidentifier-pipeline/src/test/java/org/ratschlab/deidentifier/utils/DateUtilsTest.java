package org.ratschlab.deidentifier.utils;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateUtilsTest {

    Locale defaultLocale = Locale.GERMAN;

    DateUtils dateUtils = new DateUtils(defaultLocale);

    @Test
    void determineMonthFormat() {
        assertEquals(expected("MM"), dateUtils.determineMonthFormat("09"));
        assertEquals(expected("MM"), dateUtils.determineMonthFormat("9"));
        assertEquals(expected("MMM"), dateUtils.determineMonthFormat("sep"));
        assertEquals(expected("MMM"), dateUtils.determineMonthFormat("Sep"));
        assertEquals(expected("MMM"), dateUtils.determineMonthFormat("Sept"));
        assertEquals(expected("MMM"), dateUtils.determineMonthFormat("Dez"));
        assertEquals(expected("MMM"), dateUtils.determineMonthFormat("SEP"));
        assertEquals(expected("MMMM"), dateUtils.determineMonthFormat("september"));
        assertEquals(expected("MMMM"), dateUtils.determineMonthFormat("September"));
        assertEquals(expected("MMMM"), dateUtils.determineMonthFormat("SEPTEMBER"));
    }

    @Test
    void determineMonthFormatEnglish() {
        Locale eng = Locale.ENGLISH;
        DateUtils dateUtils = new DateUtils(Locale.ENGLISH);

        // english dates
        assertEquals(expected("MMMM", eng), dateUtils.determineMonthFormat("January"));
        assertEquals(expected("MMM", eng), dateUtils.determineMonthFormat("Mar"));
        assertEquals(expected("MMM", eng), dateUtils.determineMonthFormat("Oct"));
        assertEquals(expected("MMMM", eng), dateUtils.determineMonthFormat("May"));
    }

    @Test
    void determineYearFormat() {
        assertEquals(expected("yyyy"), dateUtils.determineYearFormat("2000"));
        assertEquals(expected("yy"), dateUtils.determineYearFormat("01"));
        assertEquals(expected("yy"), dateUtils.determineYearFormat("00"));
    }

    private Optional<SimpleDateFormat> expected(String s) {
        return expected(s, defaultLocale);
    }

    private Optional<SimpleDateFormat> expected(String s, Locale loc) {
        return Optional.of(new SimpleDateFormat(s, loc));
    }
}
