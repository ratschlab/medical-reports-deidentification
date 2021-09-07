package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;
import org.ratschlab.deidentifier.annotation.FeatureKeys;

import java.time.Period;
import java.util.Random;

public class DateShiftSubstitution extends ScrubberSubstitution {
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

        if(features.containsKey(FeatureKeys.DATE_FORMAT)) {
            try {
                DateAnnotation da = DateAnnotation.fromAnnotation(features, defaultYear);
                ret = da.shift(shiftLength).format();
            } catch(Exception ex){
                ex.printStackTrace();
            }
        }

        return ret;
    }
}
