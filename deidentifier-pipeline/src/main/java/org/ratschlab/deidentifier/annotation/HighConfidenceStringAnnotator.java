package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;


@CreoleResource(
        name = "HighConfidenceStringAnnotator",
        comment = "Annotates Substrings based on high confidence annotations")
public class HighConfidenceStringAnnotator extends AbstractLanguageAnalyser {
    private static final Logger log = Logger.getLogger(HighConfidenceStringAnnotator.class);


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

    private boolean isHighConfidence(Annotation a) {
        FeatureMap f = a.getFeatures();

        java.util.Optional<Double> confidenceOpt = org.ratschlab.util.Utils.maybeParseDouble(f.getOrDefault("confidence", "").toString());

        return f.getOrDefault("rule", "").toString().startsWith("AnnotationNormalizer") ||
                confidenceOpt.filter(v -> v > 80.0).isPresent();
    }


    public void execute() throws ExecutionException {
        Document doc = getDocument();
        if (doc == null) {
            return;
        }

        AnnotationSet inputAS = doc.getAnnotations(inputASName);
        AnnotationSet outputAS = doc.getAnnotations(outputASName);

        Map<String, Annotation> stringMaps = inputAS.stream().filter(a -> isHighConfidence(a) && gate.Utils.stringFor(doc, a).contains(" ")).
                collect(Collectors.toMap(a -> gate.Utils.stringFor(doc, a), a -> a, (x, y) -> x));
                //map(a -> gate.Utils.stringFor(doc, a));

        //System.out.println("Mappings " + stringMaps.size());
        //stringMaps.forEach((k,v) -> System.out.println("\t " + k));

        String docContent = gate.Utils.contentFor(doc, outputAS).toString();

        for(Map.Entry<String, Annotation> e: stringMaps.entrySet()) {
            int idx = -1;

            while((idx = docContent.indexOf(e.getKey(), idx + 1)) >= 0) {
                FeatureMap origFeatures = e.getValue().getFeatures();
                FeatureMap fm = Factory.newFeatureMap();
                fm.putAll(origFeatures);

                String origRule = origFeatures.get("rule").toString();

                fm.put("rule", "HighConfidenceStringAnnotator-" + origRule);
                try {
                    outputAS.add((long) idx,(long) idx + e.getKey().length(), e.getValue().getType(), fm);
                } catch(InvalidOffsetException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
