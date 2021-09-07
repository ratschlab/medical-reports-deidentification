package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import gate.FeatureMap;
import org.ratschlab.deidentifier.annotation.FeatureKeys;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplacementTagsSubstitution extends DeidentificationSubstituter {

    public static final String TYPE_KEY = "type";

    String sep = ";"; // TODO parametrize
    String sepName = "SEMICOL";

    // TODO: also parametrize start and end?

    @Override
    protected String substituteAddress(String origStr, FeatureMap features) {
        return genericSubst("Address", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteDate(String origStr, FeatureMap features) {
        return genericSubst("Date", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteName(String origStr, FeatureMap features) {
        return genericSubst("Name", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteLocation(String origStr, FeatureMap features) {
        return genericSubst("Location", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteID(String origStr, FeatureMap features) {
        return genericSubst("ID", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteContact(String origStr, FeatureMap features) {
        return genericSubst("Contact", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteOccupation(String origStr, FeatureMap features) {
        return genericSubst("Occupation", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteAge(String origStr, FeatureMap features) {
        return genericSubst("Age", origStr, extractRelevantProperties(features));
    }

    protected String genericSubst(String annotationName, String origStr, Map<String, String> properties) {
        StringBuilder b = new StringBuilder();

        b.append("[[[");

        b.append(annotationName);
        b.append(sep);

        b.append(origStr.replaceAll(sep, sepName));

        for (Map.Entry<String, String> e : properties.entrySet()) {
            b.append(sep);
            b.append(e.getKey());
            b.append('=');
            b.append(e.getValue());
        }

        b.append("]]]");
        return b.toString();
    }

    private Map<String, String> extractRelevantProperties(FeatureMap features) {
        Map<String, String> ret = new HashMap<>();
        List<String> propName = ImmutableList.of(FeatureKeys.TYPE, FeatureKeys.RULE,
                FeatureKeys.DATE_FORMAT, FeatureKeys.DAY_FORMAT, FeatureKeys.MONTH_FORMAT, FeatureKeys.YEAR_FORMAT,
                FeatureKeys.FIRSTNAME, FeatureKeys.LASTNAME, FeatureKeys.NAME_SIGNATURE, FeatureKeys.NAME_FORMAT);

        propName.stream().filter(n -> features.containsKey(n)).forEach(n -> ret.put(n, features.get(n).toString()));

        return ret;
    }
}