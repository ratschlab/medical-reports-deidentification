package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DateTimeException;
import java.time.Period;
import java.util.Random;

public class DateShiftSubstitution extends ScrubberSubstitution {
    private static final Logger log = LoggerFactory.getLogger(DateShiftSubstitution.class);

    public long randomSeed;
    public int defaultYear;
    public Period shiftLength;

    public DateShiftSubstitution(long randomSeed, int defaultYear, int minShiftDays, int maxShiftDays) {
        assert maxShiftDays > minShiftDays;

        int days = new Random(randomSeed).nextInt(maxShiftDays - minShiftDays) + minShiftDays;
        this.shiftLength = Period.ofDays(days).negated();

        this.randomSeed = randomSeed;
        this.defaultYear = defaultYear;
    }

    @Override
    public String substituteDate(String origStr, FeatureMap features) {
        String ret = "DATE";

        if(features.containsKey(FeatureKeysDate.DATE_FORMAT)) {
            try {
                DateAnnotation da = DateAnnotation.fromAnnotation(features, defaultYear);
                ret = da.shift(shiftLength).format();
            } catch (DateTimeException ex) {
                log.warn(String.format("Was not able to shift date '%s' or shifted date not valid. Substituting with %s instead", origStr, ret));
            }
        }

        return ret;
    }
}
