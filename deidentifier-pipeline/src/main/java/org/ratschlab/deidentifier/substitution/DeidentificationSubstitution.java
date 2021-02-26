package org.ratschlab.deidentifier.substitution;

import gate.*;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;
import org.json.XML;
import org.ratschlab.deidentifier.utils.AnnotationUtils;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.FilterDocuments;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DeidentificationSubstitution implements SubstitutionStrategy {
    private String phiAnnotationSetName;
    private Function<Document, DeidentificationSubstituter> substituterFactory;
    private boolean substituteWholeAddress;
    private List<PathConstraint> filterTags;

    public DeidentificationSubstitution(String phiAnnotationSetName, Function<Document, DeidentificationSubstituter> substituterFactory,
                                        boolean substituteWholeAddress, List<PathConstraint> filterTags) {
        this.phiAnnotationSetName = phiAnnotationSetName;
        this.substituterFactory = substituterFactory;
        this.substituteWholeAddress = substituteWholeAddress;
        this.filterTags = filterTags;
    }

    @Override
    public Document substitute(Document rawDoc) {
        try {
            Document origDoc = FilterDocuments.filterDocument(rawDoc, filterTags);
            Factory.deleteResource(rawDoc);

            AnnotationSet markups = origDoc.getAnnotations(Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            String phiAnnotationsCopyName = "phiAnnotationsCopy";
            AnnotationSet phiAnnotationsCopy = origDoc.getAnnotations(phiAnnotationsCopyName);

            origDoc.getAnnotations(phiAnnotationSetName).forEach(a -> phiAnnotationsCopy.add(a));
            cleanAnnotations(phiAnnotationsCopy, markups);

            String newContent = generateNewDocumentStr(origDoc, phiAnnotationsCopy, substituterFactory.apply(origDoc));

            origDoc.removeAnnotationSet(phiAnnotationsCopyName);

            try {
                Document substDoc = Factory.newDocument(newContent);

                substDoc.setName(origDoc.getName());

                substDoc.setMarkupAware(true);
                substDoc.setPreserveOriginalContent(true);

                substDoc.setFeatures(origDoc.getFeatures());

                Factory.deleteResource(origDoc);
                return substDoc;
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    protected String substitute(String origStr, Annotation an, Document doc, DeidentificationSubstituter subst) {
        switch(an.getType()) {
            case "Address":
                return subst.substituteAddress(origStr, an.getFeatures());
            case "Date":
                return subst.substituteDate(origStr, an.getFeatures());
            case "Name":
                return subst.substituteName(origStr, an.getFeatures());
            case "Location":
                return subst.substituteLocation(origStr, an.getFeatures());
            case "ID":
                return subst.substituteID(origStr, an.getFeatures());
            case "Contact":
                return subst.substituteContact(origStr, an.getFeatures());
            case "Occupation":
                return subst.substituteOccupation(origStr, an.getFeatures());
            case "Age":
                return subst.substituteAge(origStr, an.getFeatures());
            default: {
                return origStr;
            }
        }
    }

    private String generateNewDocumentStr(Document origDoc, AnnotationSet phiAnnotations, DeidentificationSubstituter substituter) {
        List<Annotation> markupList = new ArrayList(origDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME));

        AnnotationUtils.sortAnnotations(markupList);

        StringBuffer buf = new StringBuffer();
        buf.append("<?xml version='1.0' encoding='UTF-8'?>");

        try {
            emit(origDoc, phiAnnotations, 0, markupList, buf, substituter);
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }

        return buf.toString();
    }

    public static void splitAnnotationsAcrossMarkupBoundaries(AnnotationSet as, AnnotationSet markups) {
        List<Annotation> toRemoveOverlaps = new ArrayList<>();

        String overlapTmps = "overlapsTmp";

        AnnotationSet newOverlaps = as.getDocument().getAnnotations(overlapTmps);

        for(Annotation a : as) {
            AnnotationSet overlaps = gate.Utils.getOverlappingAnnotations(markups, a);

            if(overlaps.size() > 1) {
                List<Annotation> leaves = overlaps.stream().
                        filter(an -> gate.Utils.getContainedAnnotations(overlaps, an).size() <= 1).
                        collect(Collectors.toList());

                List<Annotation> completelyContained = overlaps.stream().filter(an -> an.withinSpanOf(a)).collect(Collectors.toList());

                if(leaves.size() > 1 || completelyContained.size() >= 1) {
                    toRemoveOverlaps.add(a);

                    for (Annotation over : leaves) {
                        newOverlaps.add(over.getStartNode(), over.getEndNode(), a.getType(), a.getFeatures());
                    }
                }

            }
        }

        toRemoveOverlaps.forEach(a -> as.remove(a));

        newOverlaps.forEach(a -> as.add(a));
        as.getDocument().removeAnnotationSet(overlapTmps);
    }

    protected static Range<Long> determineNewRange(Annotation annot, Set<Annotation> overlaps) {
        // precondition on overlap: partial only; coextensive and contained annotation removed through prior processing

        Optional<Annotation> left = overlaps.stream().filter(a -> a.getStartNode().getOffset() < annot.getStartNode().getOffset()).
            max(Comparator.comparingInt(a -> a.getEndNode().getOffset().intValue()));
        Optional<Annotation> right = overlaps.stream().filter(a -> a.getStartNode().getOffset() > annot.getStartNode().getOffset()).
            min(Comparator.comparingInt(a -> a.getStartNode().getOffset().intValue()));

        Range<Long> ret = Range.between(
            left.map(a -> a.getEndNode().getOffset()).orElse(annot.getStartNode().getOffset()),
            right.map(a -> a.getStartNode().getOffset()).orElse(annot.getEndNode().getOffset())
        );

        assert ret.getMinimum() >= annot.getStartNode().getOffset() && ret.getMaximum() <= annot.getEndNode().getOffset();

        return ret;
    }

    public static void splitOverlappingAnnotations(AnnotationSet as) {
        // greedy, may not be optimal

        Map<Annotation, Set<Annotation>> neighborMap = as.stream().map(a -> new AbstractMap.SimpleEntry<Annotation, Set<Annotation>>(a,
                as.stream().filter(a2 -> !a.equals(a2) && a.overlaps(a2)).collect(Collectors.toSet()))).
            filter(e -> e.getValue().size() > 0).
            collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        while(!neighborMap.isEmpty()) {
            Optional<Annotation> annot = neighborMap.entrySet().stream().
                max(Comparator.comparingInt(e -> e.getValue().size())).
                map(e -> e.getKey());

            annot.ifPresent(an -> {

                Set<Annotation> neighors = neighborMap.get(an);

                if(!neighors.isEmpty()) {
                    Range<Long> newRange = determineNewRange(an, neighors);

                    if (newRange.getMaximum() > newRange.getMinimum()) {
                        try {
                            as.add(newRange.getMinimum(), newRange.getMaximum(), an.getType(), an.getFeatures());
                        } catch (InvalidOffsetException e) {
                            throw new AssertionError(e); // should not happen
                        }
                    }

                    neighors.forEach(a -> neighborMap.get(a).remove(an));
                    as.remove(an);
                }

                neighborMap.remove(an);
            });
        }
    }

    private void cleanAnnotations(AnnotationSet as, AnnotationSet markups) {
        if(!substituteWholeAddress) {
            // we are ignoring Address annotation, so removing them already here
            List<Annotation> toRemove = as.stream().filter(a -> a.getType().equals("Address")).collect(Collectors.toList());
            toRemove.forEach(a -> as.remove(a));
        }

        // construct to save original annotations to be used to check postconditions at the end only if asserts are enabled
        // see also https://docs.oracle.com/javase/8/docs/technotes/guides/language/assert.html
        class DataCopy {
            List<Annotation> annotCopy;
            DataCopy() { annotCopy = as.stream().collect(Collectors.toList()); }
        }
        DataCopy originalAs = null;

        assert ((originalAs = new DataCopy()) != null);

        AnnotationUtils.removeRedundantAnnotations(as);

        splitAnnotationsAcrossMarkupBoundaries(as, markups);

        AnnotationUtils.removeRedundantAnnotations(as);

        splitOverlappingAnnotations(as);

        AnnotationUtils.removeRedundantAnnotations(as);

        assert AnnotationUtils.annotationRanges(originalAs.annotCopy).equals(AnnotationUtils.annotationRanges(as));
        assert !AnnotationUtils.hasOverlappingAnnotations(as);
    }

    private int emit(Document doc, AnnotationSet phiAnnots, int pos, List<Annotation> markupAn, StringBuffer buf, DeidentificationSubstituter substituter) {
        if(pos >= markupAn.size()) {
            return pos;
        }

        Annotation cur = markupAn.get(pos);

        buf.append('<').append(cur.getType()).append('>');

        int nextPos = pos + 1;
        while(nextPos < markupAn.size() && markupAn.get(nextPos).getStartNode().getOffset() < cur.getEndNode().getOffset()) {
            nextPos = emit(doc, phiAnnots, nextPos, markupAn, buf, substituter);
        }

        if(pos + 1 == nextPos) {
            // leave node
            String replacement = replaceAnnotations(cur, phiAnnots, doc, substituter);
            buf.append(replacement);
        }

        buf.append("</").append(cur.getType()).append('>').append("\n"); // end line important for conversion back to JSON, s.t. distinct offset ist generated, for consecutive empty elements

        return nextPos;
    }

    // TODO refactor and analyse why semantically different from getOverlappingAnnotations
    private List<Annotation> overlaps(AnnotationSet phiAnnots, Annotation cur) {
        long start = cur.getStartNode().getOffset();
        long end = cur.getEndNode().getOffset();
        return phiAnnots.stream().
                filter(a ->
                 (start <= a.getStartNode().getOffset() && a.getStartNode().getOffset() < end) ||
                        (start < a.getEndNode().getOffset() && a.getEndNode().getOffset() <= end)).
                sorted((a1, a2) -> a2.getStartNode().getOffset().intValue() - a1.getStartNode().getOffset().intValue()).
                collect(Collectors.toList());
    }

    private String replaceAnnotations(Annotation cur, AnnotationSet phiAnnots, Document doc, DeidentificationSubstituter substituter) {
        // TODO: why different?
        //List<Annotation> contained = gate.Utils.getOverlappingAnnotations(phiAnnots, cur).inDocumentOrder();
        List<Annotation> contained = overlaps(phiAnnots, cur);
        //Collections.reverse(contained);

        String anStr = gate.Utils.stringFor(doc, cur);

        AnnotationSet addresses = phiAnnots.get("Address");

        Pair<Integer, Integer> lastAnnotation = Pair.of(-1,-1);
        for(Annotation an : contained) {
            if(substituteWholeAddress) {
                AnnotationSet covered = gate.Utils.getOverlappingAnnotations(addresses, an);

                if(!an.getType().equals("Address") && !covered.isEmpty()) {
                    continue; // parts will be replaced
                }
            } else {
                if(an.getType().equals("Address")) {
                    continue;
                }
            }

            int start = an.getStartNode().getOffset().intValue() - cur.getStartNode().getOffset().intValue();
            if(start < 0) {
                start = 0; // TODO: verify, check if need to handle end as well
            }

            int end = an.getEndNode().getOffset().intValue() - cur.getStartNode().getOffset().intValue();

            Pair<Integer, Integer> curAnnRange = Pair.of(start, end);
            if(curAnnRange.equals(lastAnnotation)) {
                continue; // already performed some substituation at that position. TODO warning?, TODO: check for disjoint annotation set
            }

            String origStr = gate.Utils.stringFor(doc, an);
            String replacement = substitute(origStr, an, doc, substituter);

            String endStr = "";
            if(end < anStr.length()) {
                endStr = anStr.substring(end);
            }

            anStr = anStr.substring(0, start) + replacement + endStr;
            lastAnnotation = curAnnRange;
        }

        return XML.escape(anStr);
    }
}

