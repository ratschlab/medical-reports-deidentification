package org.ratschlab.gate;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Gate;
import gate.corpora.DocumentContentImpl;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;

import java.util.List;
import java.util.stream.Collectors;

public class FilterDocuments {
    // TODO: move to GateTools?
    public static Document filterDocument(Document origDoc, List<PathConstraint> filterTags) {
        Document doc = GateTools.copyDocument(origDoc);

        AnnotationSet markups = origDoc.getAnnotations(Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        List<Annotation> matchingAnnotations = markups.stream().filter(a -> filterTags.stream().anyMatch(f -> PathConstraint.checkConstraints(a, f, markups))).collect(Collectors.toList());

        // remove matching tags, which are contained in other matching tags
        List<Annotation> toRemove = matchingAnnotations.stream().filter(a -> matchingAnnotations.stream().noneMatch(b -> !a.equals(b) && a.withinSpanOf(b))).collect(Collectors.toList());

        // reverse sort annotations
        toRemove.sort((a, b) -> b.getStartNode().getOffset().intValue() - a.getStartNode().getOffset().intValue());

        for(Annotation a : toRemove) {
            try {
                doc.edit(a.getStartNode().getOffset(), a.getEndNode().getOffset(), new DocumentContentImpl(""));
            } catch (Exception ex) {
                throw new IllegalArgumentException(String.format("Could not properly remove field %s", a.getType()), ex);
            }
        }

        return doc;
    }

}
