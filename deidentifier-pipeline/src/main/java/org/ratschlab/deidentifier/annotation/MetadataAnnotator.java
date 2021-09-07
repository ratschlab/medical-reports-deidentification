package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@CreoleResource(
        name = "Metadata Annotator",
        comment = "Annotates tokens based on previous metadata")
public class MetadataAnnotator extends AbstractLanguageAnalyser {
    private static final Logger log = Logger.getLogger(MetadataAnnotator.class);

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

    // TODO: where to put this??
    private void add(Map<String, Set<String>> m, String k, String v) {
        if(!m.containsKey(k)) {
            m.put(k, new HashSet<>());
        }
        m.get(k).add(v);
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

        FeatureMap docFeatures = doc.getFeatures();

        Map<String, Set<String>> annotationMap = new HashMap<>();
        docFeatures.forEach((k, v) -> {
            String ks = k.toString();
            if(ks.startsWith("metaAnnot_")) {
                String cat = ks.split("_")[1];
                Set<String> vs = (Set<String>) v;

                vs.forEach(s -> {
                    // "normalize" strings assuming we are treating proper names, i.e. either DOE or Doe
                    add(annotationMap, s.toUpperCase(), cat);

                    String capitalized = s.toLowerCase();
                    if(!capitalized.isEmpty()) {
                        capitalized = capitalized.substring(0, 1).toUpperCase() + capitalized.substring(1);
                    }
                    add(annotationMap, capitalized, cat);
                });
            }
        });

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet outputAS = doc.getAnnotations(outputASName);
        for (Annotation a : inputAS.get("Token")) {
            String t = gate.Utils.stringFor(doc, a);

            if (annotationMap.containsKey(t)) {
                FeatureMap m = Factory.newFeatureMap();
                m.put("rule", "meta_annotator");

                annotationMap.get(t).forEach(s -> outputAS.add(a.getStartNode(), a.getEndNode(), s, m));
            }
        }
    }
}
