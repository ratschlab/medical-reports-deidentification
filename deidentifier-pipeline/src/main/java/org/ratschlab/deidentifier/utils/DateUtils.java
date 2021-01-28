package org.ratschlab.deidentifier.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DateUtils {

    public static Optional<Date> maybeParseDate(String s, SimpleDateFormat fmt) {
        try {
            return Optional.of(fmt.parse(s));
        } catch (ParseException ex) {
        }
        return Optional.empty();
    }

    private static final List<String> monthFormats = ImmutableList.of(
        "MMMM",
        "MMM",
        "MM",
        "M"
    );

    private static final List<String> yearFormats = ImmutableList.of(
        "yyyy",
        "yy"
    );

    private static final List<String> allFormats = Lists.newArrayList(Iterables.concat(ImmutableList.of("dd.M.yy",
        "d.M.yy", "d.M.yyyy", "d.MM.yyyy",
        "dd.MM.yy", "dd.MM.yyyy",
        "MM/yy", "MM/yyyy", //"M/yyyy",
        "MM.yyyy", "dd.MM.",
        "d.MM.", "dd.MM", "d.MM",
        "yyyy", "M yyyy", "MMMM", "yy"), monthFormats, yearFormats));

    public static Optional<SimpleDateFormat> determineDateFormat(String s) {
        return determineDateFormat(s, allFormats);
    }

    public static Optional<SimpleDateFormat> determineDateFormat(String s, Iterable<String> dateFormats) {
        return determineDateFormat(s, dateFormats, Locale.GERMAN);
    }

    public static Optional<SimpleDateFormat> determineDateFormat(String s, Iterable<String> dateFormats, Locale locale) {
        SimpleDateFormat bestFormat = null;

        for (String fmtStr : dateFormats) {
            SimpleDateFormat fmt = new SimpleDateFormat(fmtStr, locale);

            Optional<Date> d = maybeParseDate("0" + s, fmt);
            if (!d.isPresent()) {
                d = maybeParseDate(s, fmt);
            }

            if (d.isPresent()) {
                Date date = d.get();
                // attempt to parse longest
                int bestLength = Optional.ofNullable(bestFormat).map(f -> f.toPattern().length()).orElse(0);
                if (fmt.toPattern().length() > bestLength && fmt.format(date).equals(s) || fmt.format(date).equals("0" + s)) {
                    bestFormat = fmt;
                }
            }

        }

        return Optional.ofNullable(bestFormat);
    }

    public static Optional<SimpleDateFormat> determineMonthFormat(String orig) {
        return determineDateFormat(orig, monthFormats);
    }

    public static Optional<SimpleDateFormat> determineYearFormat(String orig) {
        return determineDateFormat(orig, yearFormats);
    }
}
