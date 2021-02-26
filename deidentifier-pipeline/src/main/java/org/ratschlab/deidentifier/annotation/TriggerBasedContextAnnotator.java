package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.Optional;
import gate.creole.metadata.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@CreoleResource(
        name = "Trigger Based Context Annotator",
        comment = "Annotates contexts based on trigger words")
public class TriggerBasedContextAnnotator extends AbstractLanguageAnalyser {
    private static final Logger log = LoggerFactory.getLogger(AbstractLanguageAnalyser.class);

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

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for output annotations")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    public Map<String, List<TriggerContextEntry>> getTriggerEntries() {
        return triggerEntries;
    }

    // make it sharable, s.t. it gets duplicated when the PR gets duplicated
    @Sharable
    public void setTriggerEntries(Map<String, List<TriggerContextEntry>>  triggerEntries) {
        this.triggerEntries = triggerEntries;
    }

    private Map<String, List<TriggerContextEntry>> triggerEntries;

    @Override
    public Resource init() throws ResourceInstantiationException {
        initEntries();
        return super.init();
    }

    private void initEntries() {
        if (configPath != null) {
            try {
                setTriggerEntries(parseConfigFile(org.ratschlab.util.Utils.createFileFromUrlOrPath(configPath)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Map<String, List<TriggerContextEntry>> parseConfigFile(File f) throws IOException {
        log.info(String.format("Reading annotation mapping from %s", f.getAbsoluteFile()));

        CSVParser records = CSVParser.parse(f, Charset.defaultCharset(), CSVFormat.newFormat(';').withCommentMarker('#'));

        List<TriggerContextEntry> entries = StreamSupport.stream(records.spliterator(), false).map(record ->
                new TriggerContextEntry(Arrays.asList(record.get(0).trim().split(" ")),
                        record.get(1).trim(),
                        Integer.parseInt(record.get(2)),
                        Integer.parseInt(record.get(3))
                )
        ).collect(Collectors.toList());

        return entries.stream().collect(
                Collectors.toMap(e -> e.getTriggerTokens().get(0), e -> Collections.singletonList(e), (a, b) -> {
                    List<TriggerContextEntry> r = new ArrayList<>(a);
                    r.addAll(b);
                    return r;
                }));
    }

    public void execute() throws ExecutionException {
        Document doc = getDocument();
        if (doc == null) {
            return;
        }

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet outputAS = doc.getAnnotations(outputASName);

        List<Annotation> tokens = inputAS.get("Token").inDocumentOrder();

        for(int i = 0; i < tokens.size(); i++) {
            String t = gate.Utils.stringFor(doc, tokens.get(i));

            if(!triggerEntries.containsKey(t)) {
                continue;
            }

            List<TriggerContextEntry> entries = triggerEntries.get(t);

            for(TriggerContextEntry entry : entries) {
                boolean entriesMatch = true;
                for(int delta = 1; delta < entry.getTriggerTokens().size(); delta++) {
                    if(i + delta >= tokens.size() || !tokens.get(i + delta).equals(entry.getTriggerTokens().get(delta))) {
                        entriesMatch = false;
                        break;
                    }
                }

                if(!entriesMatch) {
                    continue;
                }

                Annotation startToken = tokens.get(Math.max(i - 1 - entry.getContextLengthLeft(), 0));
                Annotation endToken = tokens.get(Math.min(i + 1 + entry.getContextLengthRight(), tokens.size() - 1));

                FeatureMap m = Factory.newFeatureMap();
                m.put("rule", "TriggerBasedContextAnnotator");
                outputAS.add(startToken.getStartNode(), endToken.getEndNode(), entry.getAnnotationType(), m);
            }

        }
    }
}
