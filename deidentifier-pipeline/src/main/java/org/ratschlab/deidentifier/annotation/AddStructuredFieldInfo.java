package org.ratschlab.deidentifier.annotation;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.GateConstants;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

import java.util.List;

@CreoleResource(
        name = "AddStructuredFieldInfo",
        comment = "Add information about structured fields to Tokens")
public class AddStructuredFieldInfo extends AbstractLanguageAnalyser {

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
        AnnotationSet structuredFields = doc.getAnnotations(structuredFieldMarkup);

        AnnotationSet tokens = inputAS.get("Token"); // TODO parametrize

        for(Annotation token : tokens) {
            AnnotationSet overlapping = gate.Utils.getOverlappingAnnotations(structuredFields, token);

            List<String> parents = Utils.sortAnnotations(overlapping);

            // TODO: how to deal with empty lists?
            token.getFeatures().put("fieldName", parents.get(parents.size() - 1));
            token.getFeatures().put("fieldPath", String.join("/", parents));
        }
    }
}
