package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.Optional;
import gate.creole.metadata.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.ratschlab.deidentifier.utils.AnnotationUtils;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@CreoleResource(
        name = "AnnotationNormalizer",
        comment = "Adds annotation syonyoms")
public class AnnotationNormalizer extends AbstractLanguageAnalyser {
    private static final Logger log = LoggerFactory.getLogger(AnnotationNormalizer.class);

    private static final String tmpAnnotationName = "__AnnotationNormalizer_Temp_Annotation";

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

    public String getConfigPath() {
        return configPath;
    }

    @HiddenCreoleParameter
    private String configPath = null;

    @Optional
    @RunTime
    @CreoleParameter(comment = "Normalizer Configuration path")
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public List<NormalizationEntry> getNormalizationEntries() {
        return normalizationEntries;
    }

    // make it sharable, s.t. it gets duplicated when the PR gets duplicated
    @Sharable
    public void setNormalizationEntries(List<NormalizationEntry> normalizationEntries) {
        this.normalizationEntries = normalizationEntries;
    }

    private List<NormalizationEntry> normalizationEntries;

    @Override
    public Resource init() throws ResourceInstantiationException {
        initMap();
        return super.init();
    }

    private void initMap() {
        if (configPath != null) {
            try {
                setNormalizationEntries(parseConfigFile(org.ratschlab.util.Utils.createFileFromUrlOrPath(configPath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<NormalizationEntry> parseConfigFile(File f) throws IOException {
        log.info(String.format("Reading annotation mapping from %s", f.getAbsoluteFile()));

        CSVParser records = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.newFormat(';').withCommentMarker('#'));

        List<NormalizationEntry> ret = new ArrayList<>();

        for(CSVRecord r : records) {
            if(r.size() <= 1) {
                continue;
            }

            PathConstraint p = PathConstraint.constructPath(r.get(0));
            List<String> completePath = p.getExpectedParents();

            String originalFieldNamePattern = p.getAnnotationName();
            String normalizedFieldName = r.get(1).trim();

            FeatureMap features = Factory.newFeatureMap();
            features.put("rule", "AnnotationNormalizer-" + originalFieldNamePattern);

            if(r.size() > 2) {
                String featureStrs = r.get(2);

                Arrays.stream(featureStrs.split(",")).forEach(s -> {
                    String[] arr = s.split("=");
                    if(arr.length == 2) {
                        features.put(arr[0].trim(), arr[1].trim());
                    }
                });
            }

            Set<String> blacklist = ImmutableSet.of();
            if(r.size() > 3) {
                String blackList = r.get(3);
                blacklist = Arrays.stream(blackList.split(",")).
                        map(s -> s.trim()).
                        filter(s -> !s.isEmpty()).
                        collect(Collectors.toSet());
            }

            ret.add(new NormalizationEntry(Pattern.compile(originalFieldNamePattern), normalizedFieldName, completePath, blacklist, features));
        }

        return ret;
    }

    /**
     * Execute this this is some experiment over the current document.
     *
     * @throws ExecutionException if an error occurs during processing.
     */
    public void execute() throws ExecutionException {

        Document doc = getDocument();
        if (doc == null) {
            return;
        }


        AnnotationSet inputAS = doc.getAnnotations(inputASName);

        // doing temporary annotation just in case inputASName = outputASName
        AnnotationSet tmpAS = doc.getAnnotations(tmpAnnotationName);

        for (Annotation a : inputAS) {
            for(NormalizationEntry normalizationEntry : normalizationEntries) {
                if (normalizationEntry.getOriginal().matcher(a.getType()).matches()) {
                    if (parentConstraintsValid(inputAS, a, normalizationEntry.getParents(), normalizationEntry.getBlackListedParents())) {
                        FeatureMap feat = Factory.newFeatureMap();
                        feat.putAll(normalizationEntry.getFeatures());
                        tmpAS.add(a.getStartNode(), a.getEndNode(), normalizationEntry.getNormalized(), feat);
                    }
                }
            }
        }

        AnnotationSet outputAS = doc.getAnnotations(outputASName);
        outputAS.addAll(tmpAS);

        doc.removeAnnotationSet(tmpAnnotationName);
    }

    // TODO: move out into utility function
    public static boolean parentConstraintsValid(AnnotationSet inputAS, Annotation a, List<String> expectedParents, Set<String> blacklistedParents) {
        AnnotationSet overlapping = gate.Utils.getOverlappingAnnotations(inputAS, a);

        List<String> completePath = AnnotationUtils.sortOverlappingAnnotations(overlapping);
        if(completePath.size() > 0) {
            completePath.remove(0); // remove root
        }

        List<String> parents = completePath;

        Set<String> parentSet = new HashSet(parents);
        boolean noBlacklistedParent = !blacklistedParents.stream().anyMatch(s -> parentSet.contains(s));

        if(!noBlacklistedParent) {
            return false;
        }

        int expected_ind = 0;

        if(!expectedParents.isEmpty()) {
            boolean acceptingAny = false;

            if(expectedParents.get(expected_ind).isEmpty()) {
                acceptingAny = true;
                expected_ind++;
            }

            for(String p : parents) {

                if(expected_ind >= expectedParents.size()) {
                    return acceptingAny;  // "consumed" all constraints, if accepting any it is ok if still some children left
                }

                String curParent = expectedParents.get(expected_ind);


                if (p.matches(curParent)) {
                    expected_ind++;
                    acceptingAny = false;

                    if (expected_ind < expectedParents.size() && expectedParents.get(expected_ind).isEmpty()) {
                        acceptingAny = true;
                        expected_ind++;
                    }
                } else if (!acceptingAny) {
                    return false; // violated constraint
                }
            }
        }

        return expected_ind >= expectedParents.size() || (expected_ind == expectedParents.size() - 1 && expectedParents.get(expected_ind).isEmpty()); // check if all constraints consumed
    }
}
