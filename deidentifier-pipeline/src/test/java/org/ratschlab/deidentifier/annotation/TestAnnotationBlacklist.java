package org.ratschlab.deidentifier.annotation;

import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestAnnotationBlacklist extends AnalyserTestBase{
    private AbstractLanguageAnalyser pr = null;

    private String ANNOTATION_SET_NAME = "SomeAnnotationSet";

    @BeforeEach
    public void initialize() {
        super.initialize();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        try {
            pr = (AbstractLanguageAnalyser) Factory.createResource("org.ratschlab.deidentifier.annotation.AnnotationBlacklist");
            pr.setParameterValue("inputASName", ANNOTATION_SET_NAME);
            pr.setParameterValue("configPath", new File(classloader.getResource("annotation_blacklist.txt").toURI()).getAbsolutePath());

            pr.init();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBlacklisting() throws ResourceInstantiationException, ExecutionException {
        String simpleXml = String.format("<doc><Id>hello ANNOTATE_ME</Id>" + // should remove here
                "<other>ANNOTATE_ME_OK</other>" +
                "<Nested>ANNOTATE_ME_OK</Nested>" +
                "<Parent><Deeply><Nested>hello ANNOTATE_ME</Nested></Deeply></Parent>" + // and also here
                "</doc>");

        Document doc = TestUtils.fromXmlString(simpleXml);

        AnnotationSet as = doc.getAnnotations(ANNOTATION_SET_NAME);

        String docStr = doc.getContent().toString();

        Pattern pat =  Pattern.compile("ANNOTATE_ME[A-Z_]*");

        Matcher matcher = pat.matcher(docStr); //.results().map(r -> r.start()).collect(Collectors.toList());

        while(matcher.find()) {
            try {
                as.add((long) matcher.start(), (long) matcher.end(), "ForbiddenAnnot", Factory.newFeatureMap());
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }

        pr.setDocument(doc);
        pr.execute();

        AnnotationSet forbidden = as.get("ForbiddenAnnot");

        Assert.assertEquals(2, forbidden.size());
        for (Annotation an : forbidden) {
            Assert.assertEquals("ANNOTATE_ME_OK", gate.Utils.cleanStringFor(doc, an));
        }
    }
}
