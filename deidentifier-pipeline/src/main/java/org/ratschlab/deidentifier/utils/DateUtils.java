package org.ratschlab.deidentifier.utils;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DateUtils {

    public DateUtils() {
        this(Locale.GERMAN);
    }

    public DateUtils(Locale locale) {
        this.locale = locale;

        this.monthReprToFormat = buildMonthReprToFormat();
    }
    
    public Optional<SimpleDateFormat> determineMonthFormat(String orig) {
        return Optional.ofNullable(this.monthReprToFormat.get(orig));
    }

    public Optional<SimpleDateFormat> determineYearFormat(String orig) {
        int length = orig.length();
        switch(length) {
            case 2: return Optional.of(new SimpleDateFormat("yy", locale));
            case 4: return Optional.of(new SimpleDateFormat("yyyy", locale));
            default: return Optional.empty();
        }
    }

    public Locale getLocale() {
        return locale;
    }

    public Map<String, SimpleDateFormat> getMonthReprToFormat() {
        return monthReprToFormat;
    }

    private Map<String, SimpleDateFormat> buildMonthReprToFormat() {
        DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);

        Map<String, SimpleDateFormat> monthReprMap = new HashMap();

        int nrMonths = 12;
        int nrSingleDigitMonths = 9;

        monthReprMap.putAll(IntStream.rangeClosed(1, nrMonths).boxed().
            collect(Collectors.toMap(i -> String.valueOf(i), i -> new SimpleDateFormat("MM", locale))));
        monthReprMap.putAll(IntStream.rangeClosed(1, nrSingleDigitMonths).boxed().
            collect(Collectors.toMap(i -> "0" + (i), i -> new SimpleDateFormat("MM", locale))));

        monthReprMap.putAll(IntStream.rangeClosed(1, nrMonths).boxed().
            collect(Collectors.toMap(i -> symbols.getShortMonths()[i - 1].replaceAll("\\.$", ""), i -> new SimpleDateFormat("MMM", locale))));
        monthReprMap.putAll(IntStream.rangeClosed(1, nrMonths).boxed().
            collect(Collectors.toMap(i -> symbols.getMonths()[i - 1], i -> new SimpleDateFormat("MMMM", locale))));

        monthReprMap.put("Sept", new SimpleDateFormat("MMM", locale));

        // adding upper case version
        monthReprMap.putAll(monthReprMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toUpperCase(), e -> e.getValue())));
        // adding lower case version
        monthReprMap.putAll(monthReprMap.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> e.getValue(), (x, y) -> x)));

        return monthReprMap;
    }

    public static Optional<Date> maybeParseDate(String s, SimpleDateFormat fmt) {
        try {
            return Optional.of(fmt.parse(s));
        } catch (ParseException ex) {
        }
        return Optional.empty();
    }

    private Locale locale;
    private Map<String, SimpleDateFormat> monthReprToFormat;
}
