package org.ratschlab.deidentifier.annotation;

import java.util.List;

public class TriggerContextEntry {
    private final List<String> triggerTokens;
    private final String annotationType;
    private final int contextLengthLeft;
    private final int contextLengthRight;

    public TriggerContextEntry(List<String> triggerTokens, String annotationType, int contextLengthLeft, int contextLengthRight) {
        this.triggerTokens = triggerTokens;
        this.annotationType = annotationType;
        this.contextLengthLeft = contextLengthLeft;
        this.contextLengthRight = contextLengthRight;
    }

    public List<String> getTriggerTokens() {
        return triggerTokens;
    }

    public String getAnnotationType() {
        return annotationType;
    }

    public int getContextLengthLeft() {
        return contextLengthLeft;
    }

    public int getContextLengthRight() {
        return contextLengthRight;
    }
}
