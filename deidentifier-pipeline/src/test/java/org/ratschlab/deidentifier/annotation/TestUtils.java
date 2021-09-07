package org.ratschlab.deidentifier.annotation;

import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import java.net.URL;

public class TestUtils {
    public static Document runTest(String xmlStr, AbstractLanguageAnalyser pr) throws ResourceInstantiationException, ExecutionException {
        Document doc = fromXmlString(xmlStr);

        pr.setDocument(doc);
        pr.execute();

        return doc;
    }

    // TODO refactor into utility function
    public static Document fromXmlString(String xmlStr) throws ResourceInstantiationException {
        FeatureMap params = Factory.newFeatureMap();
        params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, xmlStr);
        params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/xml");
        Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        doc.setSourceUrl(null);

        doc.setMarkupAware(true);
        doc.setPreserveOriginalContent(true);

        return doc;
    }
}
