package org.ratschlab.deidentifier.substitution;

import gate.*;
import org.apache.commons.lang3.tuple.Pair;
import org.json.XML;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.FilterDocuments;

import java.util.ArrayList;
import java.util.List;
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

            // TODO: factor out to different concern
            Document origDoc = FilterDocuments.filterDocument(rawDoc, filterTags);

            Factory.deleteResource(rawDoc);

            AnnotationSet markups = origDoc.getAnnotations(Gate.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            String phiAnnotationsCopyName = "phiAnnotationsCopy";
            AnnotationSet phiAnnotationsCopy = origDoc.getAnnotations(phiAnnotationsCopyName);

            cleanAnnotations(origDoc.getAnnotations(phiAnnotationSetName), phiAnnotationsCopy, markups, origDoc);

            List<Annotation> ls = new ArrayList(markups);

            // proper sorting
            ls.sort((a1, a2) -> {
                int result = a1.getStartNode().getOffset().compareTo(a2.getStartNode().getOffset());
                if (result == 0) {
                    result = -a1.getEndNode().getOffset().compareTo(a2.getEndNode().getOffset());

                    if (result == 0) {
                        result = a1.getId().compareTo(a2.getId());
                    }
                }

                return result;
            });

            DeidentificationSubstituter substituter = substituterFactory.apply(origDoc);

            StringBuffer buf = new StringBuffer();
            buf.append("<?xml version='1.0' encoding='UTF-8'?>");

            try {
                emit(origDoc, phiAnnotationsCopy, 0, ls, buf, substituter);
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }

            String newContent = buf.toString();

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

    private void cleanAnnotations(AnnotationSet phiAnnots, AnnotationSet copy, AnnotationSet markups, Document doc) {
        phiAnnots.forEach(a -> copy.add(a));

        List<Annotation> toRemove = new ArrayList<>();
        for(Annotation a1 : phiAnnots) {
            for(Annotation a2: phiAnnots) {
                // TODO: verify conditions
                if(!a1.equals(a2) && (a1.withinSpanOf(a2) || a1.coextensive(a2)) && a2.getId() < a1.getId()) {
                    toRemove.add(a1);
                }
            }
        }

        toRemove.forEach(a -> copy.remove(a));

        List<Annotation> toRemoveOverlaps = new ArrayList<>();

        String overlapTmps = "overlapsTmp";
        AnnotationSet newOverlaps = doc.getAnnotations(overlapTmps);

        for(Annotation a : copy) {
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

        toRemoveOverlaps.forEach(a -> copy.remove(a));

        newOverlaps.forEach(a -> copy.add(a));
        doc.removeAnnotationSet(overlapTmps);
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

