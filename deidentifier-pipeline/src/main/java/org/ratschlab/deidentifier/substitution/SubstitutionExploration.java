package org.ratschlab.deidentifier.substitution;

import gate.*;
import gate.corpora.DocumentContentImpl;
import gate.corpora.DocumentImpl;
import gate.util.InvalidOffsetException;

public class SubstitutionExploration {
    public static Document substitute(Document origDoc) throws InvalidOffsetException {
        Document doc = copyDocument(origDoc);

        AnnotationSet dates = doc.getAnnotations().get("Date");

        for(Annotation a : dates) {
            doc.edit(a.getStartNode().getOffset(), a.getEndNode().getOffset(), new DocumentContentImpl("31.01.1850"));
        }

        return doc;
    }

    private static Document copyDocument(Document origDoc) {
        // TODO: seems brittle, really no better way?
        DocumentImpl doc = new DocumentImpl();
        doc.setFeatures(origDoc.getFeatures());
        doc.setContent(new DocumentContentImpl(origDoc.getContent().toString()));
        doc.setPreserveOriginalContent(origDoc.getPreserveOriginalContent());
        doc.setMarkupAware(origDoc.getMarkupAware());

        for(String an : origDoc.getAnnotationSetNames()) {
            if(!an.equals(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME)) {
                AnnotationSet as = origDoc.getAnnotations(an);
                doc.getAnnotations(an).addAll(as);
            }
        }

        doc.getAnnotations().addAll(origDoc.getAnnotations());

        return doc;
    }
}
