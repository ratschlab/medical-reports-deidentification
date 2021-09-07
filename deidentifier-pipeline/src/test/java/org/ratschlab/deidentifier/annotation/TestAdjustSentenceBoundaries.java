package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class TestAdjustSentenceBoundaries extends AnalyserTestBase{

    private AbstractLanguageAnalyser pr = null;

    private String INPUT_AS_NAME = "inputas";
    private String OUTPUT_AS_NAME = "outputas";

    @Before
    public void initialize() {
        super.initialize();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        try {
            pr = (AbstractLanguageAnalyser) Factory.createResource("org.ratschlab.deidentifier.annotation.AdjustSentenceBoundaries");
            pr.setParameterValue("inputASName", INPUT_AS_NAME);
            pr.setParameterValue("outputASName", OUTPUT_AS_NAME);

            pr.init();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testBasicAdjustment() throws GateException {
        List<String> testCases = ImmutableList.of(
            "<doc><Field1>Hello, this is </Field1><Field2>a test</Field2></doc>",
            "<doc><nested><Field1>Hello, this is </Field1></nested><Field2>a test</Field2></doc>",
            "<doc><nested><nested2><Field1>Hello, this is </Field1></nested2></nested><Field2>a test</Field2></doc>");
        for(String docXmlStr : testCases) {
            Document doc = TestUtils.fromXmlString(docXmlStr);
            String docStr = doc.getContent().toString();

            AnnotationSet inputAs = doc.getAnnotations(INPUT_AS_NAME);

            // adding a sentence spanning both fields
            inputAs.add(0L, (long) docStr.length(), "Sentence", Factory.newFeatureMap());

            pr.setDocument(doc);
            pr.execute();

            AnnotationSet output = doc.getAnnotations(OUTPUT_AS_NAME);

            // expecting sentence annotation to be split up
            Assert.assertEquals(2, output.size());

            AnnotationSet origAnnots = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            for (String field : ImmutableList.of("Field1", "Field2")) {
                Annotation fieldAn = origAnnots.get(field).iterator().next(); // assume there is exactly one
                AnnotationSet adjSents = output.get(fieldAn.getStartNode().getOffset(), fieldAn.getEndNode().getOffset());

                Assert.assertEquals(1, adjSents.size());

                adjSents.stream().forEach(a -> Assert.assertEquals("SentenceAdj", a.getType()));
            }
        }
    }
}
