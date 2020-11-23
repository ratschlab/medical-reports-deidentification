package org.ratschlab.deidentifier.substitution;

import gate.FeatureMap;

public class ScrubberSubstitution extends DeidentificationSubstituter {
    private boolean keepPatientIDs = false;
    private boolean keepCaseIDs = false;
    private boolean keepDates = false;

    public ScrubberSubstitution(boolean keepPatientIDs, boolean keepCaseIDs, boolean keepDates) {
        this.keepPatientIDs = keepPatientIDs;
        this.keepCaseIDs = keepCaseIDs;
        this.keepDates = keepDates;
    }

    public ScrubberSubstitution() {
        this(false, false, false);
    }

    @Override
    protected String substituteDate(String origStr, FeatureMap features) {
        if(!keepDates) {
            return "DATE";
        } else {
            return origStr;
        }
    }

    @Override
    protected String substituteName(String origStr, FeatureMap features) {
        return "NAME";
    }

    @Override
    protected String substituteLocation(String origStr, FeatureMap features) {
        switch (features.getOrDefault("type", "").toString()) {
            case "organisation":
                return "ORGANISATION";
            case "organisational unit":
                return "ORGANISATIONAL UNIT";
            case "street":
                return "STREET";
            case "zip":
                return "PLZ";
            case "city":
                return "CITY";
            case "canton":
                return "ZH";
            case "country":
                return "COUNTRY";
            default:
                return "LOCATION";
        }
    }

    @Override
    protected String substituteAddress(String origStr, FeatureMap features) {
        return "ADDRESS";
    }

    @Override
    protected String substituteID(String origStr, FeatureMap features) {
        if (
                (keepPatientIDs && features.getOrDefault("type", "").equals("patient ID")) ||
                (keepCaseIDs && features.getOrDefault("type", "").equals("case ID"))
        ) {
            return origStr;
        }

        switch (features.getOrDefault("type", "").toString()) {
            case "patient ID":
                return "PATIENT_ID";
            case "case ID":
                return "CASE_ID";
            case "social security number":
                return "SOCIAL_SECURITY_ID";
            case "medical insurance number":
                return "INSURANCE_ID";
        }

        return "ID";
    }

    @Override
    protected String substituteContact(String origStr, FeatureMap features) {
        switch (features.getOrDefault("type", "").toString()) {
            case "phone number":
                return "PHONE";
            case "fax number":
                return "FAX";
            case "email":
                return "EMAIL";
            case "website":
                return "WEBSITE";
            default:
                return "CONTACT_INFO";
        }
    }

    @Override
    protected String substituteOccupation(String origStr, FeatureMap features) {
        return "OCCUPATION";
    }

    @Override
    protected String substituteAge(String origStr, FeatureMap features) {
        return "AGE";
    }

    public boolean isKeepPatientIDs() {
        return keepPatientIDs;
    }

    public void setKeepPatientIDs(boolean keepPatientIDs) {
        this.keepPatientIDs = keepPatientIDs;
    }

    public boolean isKeepCaseIDs() {
        return keepCaseIDs;
    }

    public void setKeepCaseIDs(boolean keepCaseIDs) {
        this.keepCaseIDs = keepCaseIDs;
    }
}
