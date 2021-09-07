package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableMap;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import org.apache.log4j.Logger;
import scala.Int;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@CreoleResource(
        name = "Remove Overlapping Annotations",
        comment = "Removes annotations overlapping")
public class RemoveOverlappingAnnotations extends AbstractLanguageAnalyser {

    private static final Logger log = Logger.getLogger(RemoveOverlappingAnnotations.class);

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

        // TODO: strong and weak: overlapping vs contained
        List<Annotation> annotationsWithOverlaps = inputAS.stream().filter(a -> !a.getType().equals("Address")).
                filter(a -> gate.Utils.getOverlappingAnnotations(inputAS, a).stream().filter(x -> !x.getType().equals("Address")).count() > 1).collect(Collectors.toList());

        // TODO: enforce postcondotion
    }

    private boolean shouldBeRemoved(Annotation a, AnnotationSet inputAS) {
        AnnotationSet overlaps = gate.Utils.getOverlappingAnnotations(inputAS, a);
        List<Annotation> candidates = overlaps.stream().filter(c -> !c.getType().equals("Address") && !a.equals(c) && (c.coextensive(a) || a.withinSpanOf(c))).collect(Collectors.toList());

        // check if "stronger" candidates
        return candidates.stream().anyMatch(c -> rankAnnotation(a, c));
    }

    private static Map<String, Integer> annotationRanking = ImmutableMap.of("Name", 0, "Age", 1, "Date", 2,"Contact", 3);

    private static boolean rankAnnotation(Annotation a1, Annotation a2) {
        double confidence1 = getConfidence(a1); // TODO: tweak default value
        double confidence2 = getConfidence(a2);

        if(Double.compare(confidence1, confidence2) == 0) {
            // same confidence
            if(gate.Utils.length(a1) == gate.Utils.length(a2)) {
                int typeRank1 = annotationRanking.getOrDefault(a1.getType(), Int.MaxValue());
                int typeRank2 = annotationRanking.getOrDefault(a2.getType(), Int.MaxValue());

                if(typeRank1 == typeRank2) {
                    return a1.getId() < a2.getId(); // same type rank and same length, break by id
                }
                return typeRank1 > typeRank2; // the lower the better
            }

            return gate.Utils.length(a1) < gate.Utils.length(a2); // take the longer one
        }

        return confidence1 < confidence2;
    }

    private static double getConfidence(Annotation a) {
        double defaultConfidence = 50.0; // TODO tweak confidence

        Object c = a.getFeatures().getOrDefault("confidence", defaultConfidence);

        if(c instanceof Number) {
            return ((Number) c).doubleValue();
        }

        return org.ratschlab.util.Utils.maybeParseDouble(c.toString()).orElse(defaultConfidence);
    }
}
