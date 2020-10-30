package org.ratschlab.structuring;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

@CreoleResource(
    name = "Annotation Consolidation",
    comment = "Cleans up Annotations")
public class AnnotationConsolidation extends AbstractLanguageAnalyser {

    private static final Logger log = LoggerFactory.getLogger(AnnotationConsolidation.class);

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

        Set<Annotation> toRemove = inputAS.stream().
            filter(a -> shouldBeRemoved(a, inputAS)).
            collect(Collectors.toSet());

        toRemove.forEach(a -> inputAS.remove(a));

        // if no rank has been determined, use confirmed as default rank
        inputAS.get("Diagnosis").stream().
            filter(a -> !a.getFeatures().containsKey("rank")).
            forEach(a -> a.getFeatures().put("rank", "confirmed"));
    }

    private boolean shouldBeRemoved(Annotation annot, AnnotationSet inputAS) {
        if(annot.getFeatures().containsKey("rank")) {
            return false;
        }

        AnnotationSet overlaps  = inputAS.get("Diagnosis", annot.getStartNode().getOffset(), annot.getEndNode().getOffset());

        // keep other with a rank
        return overlaps.stream().anyMatch(a -> a.getFeatures().containsKey("rank"));
    }
}
