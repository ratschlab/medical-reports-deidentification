package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import gate.*;
import gate.annotation.AnnotationSetImpl;
import gate.annotation.ImmutableAnnotationSetImpl;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CreoleResource(
        name = "AdjustSentenceBoundaries",
        comment = "Adjust Sentence Boundaries, s.t. it doesn go over fields")
public class AdjustSentenceBoundaries extends AbstractLanguageAnalyser {
    private static final Logger log = LoggerFactory.getLogger(AdjustSentenceBoundaries.class);

    /**
     * Annotation set name from which this PR will take its input annotations.
     */
    private String inputASName;

    public String getInputASName() {
        return inputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for input annotations")
    public void setInputASName(String inputASName) {
        this.inputASName = inputASName;
    }

    public String getStructuredFieldMarkup() {
        return structuredFieldMarkup;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation containing the structured information")
    public void setStructuredFieldMarkup(String structuredFieldMarkup) {
        this.structuredFieldMarkup = structuredFieldMarkup;
    }

    private String structuredFieldMarkup = GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME;

    public String getOutputASName() {
        return outputASName;
    }

    @Optional
    @RunTime
    @CreoleParameter(comment = "The annotation set used for output annotations")
    public void setOutputASName(String outputASName) {
        this.outputASName = outputASName;
    }

    private String outputASName;

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
        AnnotationSet outputAS = doc.getAnnotations(outputASName);

        AnnotationSet sentences = inputAS.get("Sentence"); // TODO parametrize

        AnnotationSet newlines = new ImmutableAnnotationSetImpl(doc, inputAS.get("SpaceToken").stream().
                filter(an -> an.getFeatures().getOrDefault("string", "").toString().endsWith("\n")).
                collect(Collectors.toSet()));

        // leave fields
        List<Annotation> leaves = computeLeaves(doc.getAnnotations(structuredFieldMarkup));

        AnnotationSet tokens = inputAS.get("Token");

        for(Annotation leave : leaves) {
            AnnotationSet overlappingSentences = gate.Utils.getOverlappingAnnotations(sentences, leave);

            for(Annotation sentence : overlappingSentences) {
                try {
                    List<Long> breakpoints = new ArrayList<>();
                    long start = Math.max(sentence.getStartNode().getOffset(), leave.getStartNode().getOffset());
                    long end = Math.min(sentence.getEndNode().getOffset(), leave.getEndNode().getOffset());

                    breakpoints.add(start);
                    gate.Utils.getContainedAnnotations(newlines, sentence).stream().
                            map(an -> an.getStartNode().getOffset()).
                            filter(off -> start < off && off < end).
                            sorted().
                            forEach(off -> breakpoints.add(off));
                    breakpoints.add(end);

                    for(int i = 0; i < breakpoints.size() - 1; i++) {
                        AnnotationSet toksInSent = tokens.getContained(breakpoints.get(i), breakpoints.get(i+1));

                        FeatureMap fm = Factory.newFeatureMap();
                        fm.put("tokenCnt", toksInSent.size());
                        fm.put("workTokenCnt", toksInSent.stream().filter(a -> a.getFeatures().getOrDefault("kind", "").equals("word")).count());
                        outputAS.add(breakpoints.get(i), breakpoints.get(i+1), "SentenceAdj", fm);
                    }

                } catch (InvalidOffsetException e) {
                    e.printStackTrace();
                }
            }
        }


        // TODO: only for test purposes, todo make option, assert?

        for(Annotation sent : sentences) {
            List<Annotation> adjs = outputAS.get("SentenceAdj", sent.getStartNode().getOffset(), sent.getEndNode().getOffset()).inDocumentOrder();

            if(adjs.size() == 0) {
                log.info("No SentenceAdj in " + doc.getName() + " " + gate.Utils.stringFor(doc, sent));
                continue;
            }

            List<Long> annotCoords = adjs.stream().map(a -> ImmutableList.of(a.getStartNode().getOffset(), a.getEndNode().getOffset())).flatMap(List::stream).collect(Collectors.toList());

            if(annotCoords.get(0) != sent.getStartNode().getOffset()) {
                log.info("No SentenceAdj at start " + doc.getName() + " " + gate.Utils.stringFor(doc, sent) + " " + annotCoords.get(0) + " instead of " + sent.getStartNode().getOffset());
            }

            if(annotCoords.get(annotCoords.size() - 1) != sent.getEndNode().getOffset()) {
                log.info("No SentenceAdj at end " + doc.getName() + " " + gate.Utils.stringFor(doc, sent) + " " + annotCoords.get(annotCoords.size() - 1) + " instead of " + sent.getEndNode().getOffset());
            }

            for(int i = 1; i < annotCoords.size() - 1; i+=2) {
                if(annotCoords.get(i) != annotCoords.get(i + 1) && !gate.Utils.stringFor(doc, annotCoords.get(i), annotCoords.get(i + 1)).matches("\\s*")) {
                    log.info("Not covering " + doc.getName() + " " + gate.Utils.stringFor(doc, sent) + " " + annotCoords.get(i) + " instead of " + annotCoords.get(i + 1));
                }
            }
        }
    }

    private List<Annotation> computeLeaves(AnnotationSet structuredFieldsOrig) {
        AnnotationSet structuredFields = new AnnotationSetImpl(structuredFieldsOrig);

        List<Annotation> structuredFieldList = structuredFields.inDocumentOrder();

        structuredFieldList.sort((a1, a2) -> {
            if(a1.getStartNode().getOffset() == a2.getStartNode().getOffset()) {
                if(a1.getEndNode().getOffset() == a2.getEndNode().getOffset()) {
                    return a1.getId() - a2.getId();
                }
                return (int) (a2.getEndNode().getOffset() - a1.getEndNode().getOffset());
            }
            return (int) (a1.getStartNode().getOffset() - a2.getStartNode().getOffset());

        });

        List<Annotation> removeStructured = new ArrayList<>();
        for(int i = 0; i < structuredFieldList.size() - 1; i++) {
            Annotation a = structuredFieldList.get(i);
            Annotation b = structuredFieldList.get(i+1);
            if(a.getStartNode().equals(b.getStartNode()) && a.getEndNode().equals(b.getEndNode()) && a.getId() < b.getId()) {
                removeStructured.add(a);
            }
        }

        //structuredFields.removeAll(removeStructured);

        return structuredFields.stream().
            filter(an -> gate.Utils.getContainedAnnotations(structuredFields, an).stream().filter(anc -> !removeStructured.contains(anc) && anc != an).count() == 0).
            collect(Collectors.toList());
    }


}
