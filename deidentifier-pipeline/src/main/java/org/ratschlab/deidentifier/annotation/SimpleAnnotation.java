package org.ratschlab.deidentifier.annotation;

import gate.AnnotationSet;
import gate.Factory;
import gate.FeatureMap;
import gate.util.InvalidOffsetException;

import java.util.Objects;

public class SimpleAnnotation {
    private final int start;
    private final int end;
    private final String tag;
    private final FeatureMap features;

    public SimpleAnnotation(int start, int end, String tag) {
        this(start, end, tag, Factory.newFeatureMap());
    }

    public SimpleAnnotation(int start, int end, String tag, FeatureMap features) {
        this.start = start;
        this.end = end;
        this.tag = tag;
        this.features = features;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public String getTag() {
        return tag;
    }

    public FeatureMap getFeatures() { return features; }

    public void addToAnnotationSet(AnnotationSet as) throws InvalidOffsetException {
        as.add((long) start, (long) end, tag, features);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleAnnotation that = (SimpleAnnotation) o;
        return start == that.start &&
                end == that.end &&
                tag.equals(that.tag) &&
                features.equals(that.features);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, tag, features);
    }
}
