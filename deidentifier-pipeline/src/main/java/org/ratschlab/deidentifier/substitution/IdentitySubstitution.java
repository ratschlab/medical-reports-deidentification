package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;

public class IdentitySubstitution extends DeidentificationSubstituter {
    public IdentitySubstitution() {

    }

    @Override
    protected String substituteDate(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteName(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteLocation(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteID(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteContact(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteOccupation(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteAge(String origStr, FeatureMap features) {
        return origStr;
    }

    @Override
    protected String substituteAddress(String origStr, FeatureMap features) { return origStr; }
}
