package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.Optional;
import gate.creole.metadata.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

@CreoleResource(
        name = "AnnotationBlacklist",
        comment = "Removes blacklisted annotations")
public class AnnotationBlacklist extends AbstractLanguageAnalyser {
    private static final Logger log = LoggerFactory.getLogger(AnnotationBlacklist.class);


    /**
     * Annotation set name from which this PR will take its input annotations.
     */
    private String inputASName;

    public String getInputASName() {
        return inputASName;
    }

    @gate.creole.metadata.Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for input annotations")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getConfigPath() {
        return configPath;
    }

    @HiddenCreoleParameter
    private String configPath = null;

    @Optional
    @RunTime
    @CreoleParameter(comment = "Blacklist Configuration path")
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public Map<String, List<List<String>>> getBlacklistMap() {
        return blacklistMap;
    }

    // make it sharable, s.t. it gets duplicated when the PR gets duplicated
    @Sharable
    public void setBlacklistMap(Map<String, List<List<String>>> blacklistMap) {
        this.blacklistMap = blacklistMap;
    }

    private Map<String, List<List<String>>> blacklistMap;

    @Override
    public Resource init() throws ResourceInstantiationException {
        initMap();
        return super.init();
    }

    private void initMap() {
        if (configPath != null) {
            try {
                setBlacklistMap(parseConfigFile(Utils.createFileFromUrlOrPath(configPath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Map<String, List<List<String>>> parseConfigFile(File f) throws IOException {
        log.info(String.format("Reading blacklist map from %s", f.getAbsoluteFile()));

        Map<String, List<List<String>>> ret = new HashMap<>();

        CSVParser records = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.newFormat(';').withCommentMarker('#'));

        for (CSVRecord r : records) {
            if (r.size() <= 1) {
                continue;
            }

            String blackListPath = r.get(0);

            List<String> path = Arrays.stream(blackListPath.split(("/"))).map(s -> s.trim()).collect(Collectors.toList());

            // add empty root for single fields
            if(path.size() == 1) {
                path.add(0, ""); //prefix
                path.add("");
            } else if(path.get(0).isEmpty()) {
                // otherwise removing empty ""
                path.remove(0);
            }

            for (String forbiddenAnnotation : r.get(1).split(",")) {
                forbiddenAnnotation = forbiddenAnnotation.trim();

                List<List<String>> lst;
                if (ret.containsKey(forbiddenAnnotation)) {
                    lst = ret.get(forbiddenAnnotation);
                } else {
                    lst = new ArrayList<>();
                    ret.put(forbiddenAnnotation, lst);
                }

                lst.add(path);
            }
        }

        return ret;
    }

    @Override
    public void execute() throws ExecutionException {
        Document doc = getDocument();
        if (doc == null) {
            return;
        }

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet contextAS = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        for (Map.Entry<String, List<List<String>>> el : blacklistMap.entrySet()) {
            Set<Annotation> toBeRemoved = inputAS.get(el.getKey()).stream().
                    filter(an -> el.getValue().stream().anyMatch(forbiddenPath ->
                            AnnotationNormalizer.parentConstraintsValid(contextAS, an, forbiddenPath, Collections.emptySet()))).
                    collect(Collectors.toSet());

            toBeRemoved.forEach(a -> inputAS.remove(a));
        }
    }
}
