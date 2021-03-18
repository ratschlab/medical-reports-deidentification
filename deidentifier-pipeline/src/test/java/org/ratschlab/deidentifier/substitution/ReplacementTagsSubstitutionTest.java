package org.ratschlab.deidentifier.substitution;

import gate.Factory;
import gate.FeatureMap;
import org.junit.jupiter.api.Test;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysGeneral;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplacementTagsSubstitutionTest {

    @Test
    void substituteDate() {
        ReplacementTagsSubstitution subst = new ReplacementTagsSubstitution();
        String origStr = "20.02.2012";

        String expected = String.format("%sDate;%s;;day=;format=;month=;rule=;type=;year=%s", ReplacementTagsSubstitution.START_TAG,
            origStr,
            ReplacementTagsSubstitution.END_TAG);

        assertEquals(expected, subst.substituteDate(origStr, Factory.newFeatureMap()));
    }

    @Test
    void substituteId() {
        ReplacementTagsSubstitution subst = new ReplacementTagsSubstitution();
        String origStr = "1234";
        String ruleName =  "MyRule";
        FeatureMap map = Factory.newFeatureMap();
        map.put(FeatureKeysGeneral.RULE, ruleName);

        String expected = String.format("%sID;%s;;rule=%s;type=%s", ReplacementTagsSubstitution.START_TAG, origStr, ruleName, ReplacementTagsSubstitution.END_TAG);

        assertEquals(expected, subst.substituteID(origStr, map));
    }

    @Test
    void substituteNameSplit() {
        ReplacementTagsSubstitution subst = new ReplacementTagsSubstitution();
        String origStr = "Peter";
        String origAnnotStr = "Peter Huber";
        String ruleName =  "MyRule";
        FeatureMap map = Factory.newFeatureMap();
        map.put(FeatureKeysGeneral.RULE, ruleName);
        map.put(FeatureKeysGeneral.ORIG_ANNOTATED_STR, origAnnotStr);

        String expected = String.format("%sName;%s;%s;firstname=;format=;lastname=;rule=%s;salutation=;signature=;type=%s",
                ReplacementTagsSubstitution.START_TAG, origStr, origAnnotStr, ruleName, ReplacementTagsSubstitution.END_TAG);

        assertEquals(expected, subst.substituteName(origStr, map));
    }
}
