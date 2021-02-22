package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import gate.FeatureMap;
import org.apache.tools.ant.taskdefs.PathConvert;
import org.ratschlab.deidentifier.annotation.features.FeatureKeys;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysGeneral;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysDate;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysName;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class ReplacementTagsSubstitution extends DeidentificationSubstituter {

    public static final String TYPE_KEY = "type";

    public static final String START_TAG = "[[[";
    public static final String END_TAG = "]]]";

    String sep = ";"; // TODO parametrize
    String sepName = "SEMICOL";

    @Override
    protected String substituteAddress(String origStr, FeatureMap features) {
        return genericSubst("Address", origStr, extractRelevantProperties(features));
    }

    @Override
    protected String substituteDate(String origStr, FeatureMap features) {
        Set<String> fieldNames = FeatureKeys.getFieldNames(FeatureKeysGeneral.class);
        fieldNames.addAll(FeatureKeys.getFieldNames(FeatureKeysDate.class));
        return genericSubst("Date", origStr, extractRelevantProperties(features), fieldNames);
    }

    @Override
    protected String substituteName(String origStr, FeatureMap features) {
        Set<String> fieldNames = FeatureKeys.getFieldNames(FeatureKeysGeneral.class);
        fieldNames.addAll(FeatureKeys.getFieldNames(FeatureKeysName.class));
        return genericSubst("Name", origStr, extractRelevantProperties(features), fieldNames);
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
        return genericSubst(annotationName, origStr, properties, FeatureKeys.getFieldNames(FeatureKeysGeneral.class));
    }

    protected String genericSubst(String annotationName, String origStr, Map<String, String> properties, Set<String> allFields) {
        StringBuilder b = new StringBuilder();

        b.append(START_TAG);

        b.append(annotationName);
        b.append(sep);

        b.append(origStr.replaceAll(sep, sepName));

        Map<String, String> allEntries = new HashMap<>(properties);

        // adding missing entries
        allFields.stream().
            filter(s -> !properties.keySet().contains(s)).
            forEach(s -> allEntries.put(s, ""));

        // sort entries by key s.t. we always have the same order of fields
        List<Map.Entry<String, String>> entryPairs = allEntries.entrySet().stream().
            sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey())).collect(Collectors.toList());

        for (Map.Entry<String, String> e : entryPairs) {
            b.append(sep);
            b.append(e.getKey());
            b.append('=');
            b.append(e.getValue());
        }

        b.append(END_TAG);
        return b.toString();
    }

    private Map<String, String> extractRelevantProperties(FeatureMap features) {
        Map<String, String> ret = new HashMap<>();

        List<String> propName = ImmutableList.of(FeatureKeys.getFieldNames(FeatureKeysGeneral.class),
                FeatureKeys.getFieldNames(FeatureKeysName.class),
                FeatureKeys.getFieldNames(FeatureKeysDate.class)).stream().flatMap(Collection::stream).collect(Collectors.toList());

        propName.stream().filter(n -> features.containsKey(n)).forEach(n -> ret.put(n, features.get(n).toString()));

        return ret;
    }

    public static boolean documentValid(String content) {
        int pos = content.indexOf(START_TAG);

        boolean insideAnnoation = true;

        while(0 <= pos && pos < content.length()) {
            int posNextStartTag = content.indexOf(START_TAG, pos + 1);
            int posNextEndTag = content.indexOf(END_TAG, pos + 1);

            boolean nextIsClosing = (posNextEndTag < posNextStartTag) || (posNextEndTag >= 0 && posNextStartTag < 0);

            if(insideAnnoation && nextIsClosing) {
                pos = posNextEndTag;
                insideAnnoation = false;
            }
            else if(!insideAnnoation && !nextIsClosing) {
                pos = posNextStartTag;
                insideAnnoation = true;
            }
            else {
                return false;
            }
        }

        return true;
    }
}
