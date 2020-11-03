package org.ratschlab.structuring;

import org.ratschlab.deidentifier.utils.paths.PathConstraint;

import java.util.List;

public class KeywordAnnotatorCfgRecord {
    private String code;
    private List<PathConstraint> blacklistPath;

    public KeywordAnnotatorCfgRecord(String code, List<PathConstraint> blacklistPath) {
        this.code = code;
        this.blacklistPath = blacklistPath;
    }

    public String getCode() {
        return code;
    }

    public List<PathConstraint> getBlacklistPath() {
        return blacklistPath;
    }
}
