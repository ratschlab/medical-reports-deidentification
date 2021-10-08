package org.ratschlab.deidentifier.annotation;

import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotationNormalizer extends AnalyserTestBase {
    private AbstractLanguageAnalyser pr = null;

    @BeforeEach
    public void initialize() {
        super.initialize();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        try {

            pr = (AbstractLanguageAnalyser) Factory.createResource("org.ratschlab.deidentifier.annotation.AnnotationNormalizer");
            pr.setParameterValue("inputASName", GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
            pr.setParameterValue("configPath", new File(classloader.getResource("annotation_normalizer.txt").toURI()).getAbsolutePath());

            pr.init();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSimpleNormalization() throws ResourceInstantiationException, ExecutionException {
        String normalizeText = "hello";
        String simpleXml = String.format("<doc><SomeField>%s</SomeField></doc>", normalizeText);

        Document doc = TestUtils.runTest(simpleXml, pr);

        AnnotationSet original = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("SomeField");
        AnnotationSet normalized = doc.getAnnotations().get("NormalizedField");

        assertEquals(original.size(), normalized.size());

        for (Annotation an : normalized) {
            assertEquals(normalizeText, gate.Utils.cleanStringFor(doc, an));
        }
    }

    @Test
    public void testNestNormalization() throws ResourceInstantiationException, ExecutionException {
        String normalizeText = "only normalize here";

        String simpleXml = String.format("<doc>" +
                "<Deeper><FirstLevelOnly>don't here</FirstLevelOnly></Deeper>" +
                "<Parent><AnotherField><NestedField>don't normalize here</NestedField></AnotherField></Parent>" +
                "<NestedField>hello</NestedField>" +
                "<Parent><NestedField>%s</NestedField></Parent>" +
                "<NestedField><Parent>don't normalize here</Parent></NestedField>" +
                "<GrandParent><Parent><NestedField2>don't normalize here</NestedField2></Parent></GrandParent>" +
                "<Parent2><AnotherField><NestedField>%s</NestedField></AnotherField></Parent2>" +
                "<BlackListedParent><SomeField>don't normalize here</SomeField></BlackListedParent>" +
                "<Spread><Whatever><Constraint>%s</Constraint></Whatever></Spread>" +
                "<Spread><Constraint>%s</Constraint></Spread>" +
                "<Spread>don't normalize here</Spread>" +
                "<Constraint>don't normalize here</Constraint>" +
                "<FirstLevelOnly>%s</FirstLevelOnly>" +
                "</doc>", normalizeText, normalizeText, normalizeText, normalizeText, normalizeText); //, normalizeText, normalizeText);
        Document doc = TestUtils.runTest(simpleXml, pr);

        AnnotationSet normalized = doc.getAnnotations().get("NormalizedField");


        for (Annotation an : normalized) {
            String s = gate.Utils.cleanStringFor(doc, an);
            assertEquals(normalizeText, gate.Utils.cleanStringFor(doc, an));
        }

        assertEquals(5, normalized.size());

        assertEquals(1, doc.getAnnotations().get("DifferentNormalizedField").size());
    }

    @Test
    public void testNormalizationWithFeatures() throws ResourceInstantiationException, ExecutionException {
        String normalizeText = "hello";
        String simpleXml = String.format("<doc><WithFeatures>%s</WithFeatures></doc>", normalizeText);

        Document doc = TestUtils.runTest(simpleXml, pr);

        AnnotationSet original = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("WithFeatures");
        AnnotationSet normalized = doc.getAnnotations().get("NormalizedField");

        assertEquals(original.size(), normalized.size());

        for (Annotation an : normalized) {
            assertEquals(normalizeText, gate.Utils.cleanStringFor(doc, an));
        }

        FeatureMap features = normalized.iterator().next().getFeatures();
        assertEquals(3, features.size());
        assertTrue(features.containsKey("hello"));
        assertEquals("world", features.get("hello"));
        assertTrue(features.containsKey("rule"));
    }

    @Test
    public void testNormalizationLeavesOnly() {
        String text = "<doc><SomeField><other>content</other><another>more</another></SomeField></doc>";

        try {
            pr.setParameterValue("leavesOnly", Boolean.TRUE);
        } catch (ResourceInstantiationException e) {
            fail(e.getMessage());
        }

        Document doc = TestUtils.runTest(text, pr);
        assertTrue(doc.getAnnotations().get("NormalizedField").isEmpty());
    }
}
