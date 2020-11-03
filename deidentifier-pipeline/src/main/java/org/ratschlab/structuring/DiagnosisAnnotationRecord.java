package org.ratschlab.structuring;

public class DiagnosisAnnotationRecord {
    private String annotationText;
    private String code;
    private String rank;

    public DiagnosisAnnotationRecord(String annotationText, String code, String rank) {
        this.annotationText = annotationText;
        this.code = code;
        this.rank = rank;
    }

    public String getAnnotationText() {
        return annotationText;
    }

    public String getCode() {
        return code;
    }

    public String getRank() {
        return rank;
    }


}
