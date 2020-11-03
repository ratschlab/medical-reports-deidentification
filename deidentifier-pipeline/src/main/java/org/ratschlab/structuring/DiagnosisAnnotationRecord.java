package org.ratschlab.structuring;

public class DiagnosisAnnotationRecord {
    private String annotationText;
    private String code;
    private String reliability;

    public DiagnosisAnnotationRecord(String annotationText, String code, String reliability) {
        this.annotationText = annotationText;
        this.code = code;
        this.reliability = reliability;
    }

    public String getAnnotationText() {
        return annotationText;
    }

    public String getCode() {
        return code;
    }

    public String getReliability() {
        return reliability;
    }


}
