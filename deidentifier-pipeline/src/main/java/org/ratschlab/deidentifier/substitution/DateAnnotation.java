package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;
import org.apache.log4j.Logger;
import org.ratschlab.deidentifier.annotation.FeatureKeys;
import org.ratschlab.util.Utils;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateAnnotation {
    private static final Logger log = Logger.getLogger(DateAnnotation.class);

    private String format;
    private LocalDate date;

    public DateAnnotation(String format, LocalDate ts) {
        this.format = format;
        this.date = ts;
    }

    public String getFormat() {
        return format;
    }

    public LocalDate getDate() {
        return date;
    }

    public DateAnnotation shift(Period delta) {
        return new DateAnnotation(format, date.plus(delta));
    }

    public String format() {
        // TODO: handle exception?
        return DateTimeFormatter.ofPattern(format).format(date);
    }

    public static DateAnnotation fromAnnotation(FeatureMap features, int defaultYear) throws DateTimeException {
        String format = features.get(FeatureKeys.DATE_FORMAT).toString();

        Optional<Integer> yearOpt = Utils.maybeParseInt(features.getOrDefault(FeatureKeys.YEAR_FORMAT, "").toString());

        if(!yearOpt.isPresent()) {
            log.warn(String.format("No date present, substitute %d", defaultYear));
        }

        int year = yearOpt.orElse(defaultYear);
        int month = Utils.maybeParseInt(features.getOrDefault(FeatureKeys.MONTH_FORMAT, "").toString()).orElse(1);
        int day = Utils.maybeParseInt(features.getOrDefault(FeatureKeys.DAY_FORMAT, "").toString()).orElse(1);

        LocalDate d = LocalDate.of(year, month, day);
        return new DateAnnotation(format, d);
    }
}
