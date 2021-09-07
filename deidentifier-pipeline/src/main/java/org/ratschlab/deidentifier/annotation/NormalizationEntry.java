package org.ratschlab.deidentifier.annotation;

import gate.FeatureMap;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class NormalizationEntry {
    private final Pattern original;
    private final String normalized;
    private final List<String> parents;

    public Set<String> getBlackListedParents() {
        return blackListedParents;
    }

    private final Set<String> blackListedParents;
    private final FeatureMap features;

    public NormalizationEntry(Pattern original, String normalized, List<String> parents, Set<String> blackListedParents, FeatureMap features) {
        this.original = original;
        this.normalized = normalized;
        this.parents = parents;
        this.features = features;
        this.blackListedParents = blackListedParents;
    }

    public Pattern getOriginal() {
        return original;
    }

    public String getNormalized() {
        return normalized;
    }

    public List<String> getParents() {
        return parents;
    }

    public FeatureMap getFeatures() {
        return features;
    }
}
