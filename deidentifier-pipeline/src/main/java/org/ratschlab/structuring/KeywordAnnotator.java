package org.ratschlab.structuring;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.*;
import gate.creole.metadata.Optional;
import gate.util.InvalidOffsetException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CreoleResource(
    name = "KeywordAnnotator",
    comment = "Annotate based on keywords")
public class KeywordAnnotator extends AbstractLanguageAnalyser {
    private static final Logger log = LoggerFactory.getLogger(KeywordAnnotator.class);

    public String getConfigPath() {
        return configPath;
    }

    @HiddenCreoleParameter
    private String configPath = null;

    @Optional
    @RunTime
    @CreoleParameter(comment = "Keywords Configuration path")
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    /**
     * Annotation set name from which this PR will take its input annotations.
     */
    private String inputASName;

    /**
     * Annotation set name into which this PR will create new annotations.
     */
    private String outputASName;

    public String getInputASName() {
        return inputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for input annotations")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getOutputASName() {
        return outputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for output annotations")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public Map<Pattern, List<KeywordAnnotatorCfgRecord>> getPatternDict() {
        return patternDict;
    }

    private Map<Pattern, List<KeywordAnnotatorCfgRecord>> patternDict;

    // make it sharable, s.t. it gets duplicated when the PR gets duplicated
    @Sharable
    public void setPatternDict(Map<Pattern, List<KeywordAnnotatorCfgRecord>> patternDict) {
        this.patternDict = patternDict;
    }

    @Override
    public Resource init() throws ResourceInstantiationException {
        if(configPath != null) {
            File f = org.ratschlab.deidentifier.annotation.Utils.createFileFromUrlOrPath(configPath);
            try {
                setPatternDict(this.parseConfigFile(f));
            } catch (IOException e) {
                throw new ResourceInstantiationException(e);
            }
        }

        return super.init();
    }
    // TODO: keyword map
    // in principle a gazetter, but could become more complex with inclusion and exclusion lists.

    public static  Map<Pattern, List<KeywordAnnotatorCfgRecord>> parseConfigFile(File f) throws IOException {
        log.info(String.format("Reading keyword annotation file from %s", f.getAbsoluteFile()));

        CSVParser records = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.newFormat(';').withCommentMarker('#'));

        Map<Pattern, List<KeywordAnnotatorCfgRecord>>  ret = new HashMap<>();

        for(CSVRecord r : records) {
            if (r.size() <= 1) {
                continue;
            }

            List<PathConstraint> path = new ArrayList<>();
            if(r.size() > 2) {
                String paths = r.get(2);

                path = Arrays.stream(paths.split(",")).map(p -> PathConstraint.constructPath(p)).collect(Collectors.toList());
            }

            for(String keyword : r.get(1).split(",")) {
                Pattern pat = Pattern.compile(keyword.trim());

                List<KeywordAnnotatorCfgRecord> lst;
                if(ret.containsKey(pat)) {
                    lst = ret.get(pat);
                } else {
                    lst = new ArrayList<>();
                    ret.put(pat, lst);
                }

                lst.add(new KeywordAnnotatorCfgRecord(r.get(0).trim(), path));
            }
        }

        return ret;
    }

    public void execute() {
        Document doc = getDocument();
        if (doc == null) {
            return;
        }

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet outputAS = doc.getAnnotations(outputASName);
        // java

        String content = doc.getContent().toString();
        for(Map.Entry<Pattern, List<KeywordAnnotatorCfgRecord>> e : this.patternDict.entrySet()) {
            Pattern keyword = e.getKey();
            List<KeywordAnnotatorCfgRecord> rec = e.getValue();

            Matcher keywordMatcher = keyword.matcher(content);

            while (keywordMatcher.find()) {
                for(KeywordAnnotatorCfgRecord r : rec) {
                    FeatureMap featureMap = Factory.newFeatureMap();
                    featureMap.put("code", r.getCode());
                    // TODO: rule or source feature
                    Integer id = null;
                    try {
                       id = outputAS.add((long) keywordMatcher.start(), (long) keywordMatcher.end(), "Diagnosis", featureMap);
                    } catch (InvalidOffsetException ex) {
                        ex.printStackTrace();
                    }

                    if(id != null) {
                        Annotation a = outputAS.get(id);

                        if(r.getBlacklistPath().stream().anyMatch(p ->
                                PathConstraint.parentConstraintsValid(doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME), a,
                                        p.getExpectedParents(), Collections.EMPTY_SET))) {
                            outputAS.remove(a);
                        }
                    }


                }

                keywordMatcher.start();
            }
        }

        // TODO: add codes in doc diagnosis!
    }
}
