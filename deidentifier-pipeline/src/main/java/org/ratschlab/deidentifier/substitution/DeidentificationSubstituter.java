package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;

public abstract class DeidentificationSubstituter implements Substituter {
    abstract protected String substituteAddress(String origStr, FeatureMap features);
    abstract protected String substituteDate(String origStr, FeatureMap features);
    abstract protected String substituteName(String origStr, FeatureMap features);
    abstract protected String substituteLocation(String origStr, FeatureMap features);
    abstract protected String substituteID(String origStr, FeatureMap features);
    abstract protected String substituteContact(String origStr, FeatureMap features);
    abstract protected String substituteOccupation(String origStr, FeatureMap features);
    abstract protected String substituteAge(String origStr, FeatureMap features);
}
