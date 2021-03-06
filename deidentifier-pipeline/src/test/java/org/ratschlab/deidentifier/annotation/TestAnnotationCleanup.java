package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ratschlab.deidentifier.annotation.features.FeatureKeysName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestAnnotationCleanup extends AnalyserTestBase {
    private AbstractLanguageAnalyser pr = null;

    private static final String TYPE = "MYANNOT";
    private static final String OUTPUT_AS = "myoutput";

    private Document doc;
    private FeatureMap someMap;

    @BeforeEach
    public void initialize() {
        super.initialize();
        doc = createDummyDoc();
        someMap = Factory.newFeatureMap();
        someMap.put("type", "example_type");
        someMap.put("rule", "someRule");

        try {

            pr = (AbstractLanguageAnalyser) Factory.createResource("org.ratschlab.deidentifier.annotation.AnnotationCleanup");
            pr.setParameterValue("inputASName", "");
            pr.setParameterValue("outputASName", OUTPUT_AS);
            pr.setParameterValue("annotationTypes", ImmutableSet.of(TYPE));

            pr.init();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testMerging() {
        List<List<Pair<Integer, Integer>>> cases = ImmutableList.of(
                ImmutableList.of(Pair.of(0, 1)),
                ImmutableList.of(Pair.of(0, 1), Pair.of(0, 1)),
                ImmutableList.of(Pair.of(0, 1), Pair.of(0, 1), Pair.of(0, 1)),
                ImmutableList.of(Pair.of(3, 6), Pair.of(3, 8)),
                ImmutableList.of(Pair.of(3, 6), Pair.of(3, 8), Pair.of(3, 6)),
                ImmutableList.of(Pair.of(3, 6), Pair.of(1, 6)),
                ImmutableList.of(Pair.of(3, 6), Pair.of(1, 8))
        );

        for(List<Pair<Integer, Integer>> annotationList : cases) {
            AnnotationSet as = doc.getAnnotations();

            as.clear();
            doc.getAnnotations(OUTPUT_AS).clear();
            annotationList.forEach(p -> {
                try {
                    as.add(p.getLeft().longValue(), p.getRight().longValue(), TYPE, someMap);
                } catch (InvalidOffsetException e) {
                    e.printStackTrace();
                }
            });

            AnnotationSet oas = runTest();

            assertEquals(1, oas.size());

            Annotation mergedAn = oas.inDocumentOrder().get(0);
            long longestAnnotationExp = annotationList.stream().mapToLong(p -> p.getRight() - p.getLeft()).max().getAsLong();

            assertEquals(longestAnnotationExp,  mergedAn.getEndNode().getOffset() - mergedAn.getStartNode().getOffset());

            assertEquals(someMap, mergedAn.getFeatures());
        }
    }

    @Test
    public void testMergingTypes() throws InvalidOffsetException {
        String commonField = "common_field";
        someMap.put(commonField, "a");

        FeatureMap otherMap = Factory.newFeatureMap();
        otherMap.put(commonField, "b");

        AnnotationSet as = doc.getAnnotations();
        as.add(0L, 1L, TYPE, someMap);
        as.add(0L, 1L, TYPE, otherMap);

        AnnotationSet oas = runTest();
        assertEquals(1, oas.size());

        FeatureMap expected = Factory.newFeatureMap();
        expected.put(commonField, "b,a");
        expected.put("type", "example_type");
        expected.put("rule", "someRule");
        assertEquals(expected, oas.inDocumentOrder().get(0).getFeatures());
    }

    @Test
    public void testMergingTypesWithOther() throws InvalidOffsetException {
        FeatureMap otherMap = Factory.newFeatureMap();
        otherMap.put("type", "other");

        AnnotationSet as = doc.getAnnotations();
        as.add(0L, 1L, TYPE, someMap);
        as.add(0L, 1L, TYPE, otherMap);

        AnnotationSet oas = runTest();
        assertEquals(1, oas.size());

        FeatureMap expected = Factory.newFeatureMap();
        expected.put("type", "example_type");
        expected.put("rule", "someRule");
        assertEquals(expected, oas.inDocumentOrder().get(0).getFeatures());
    }

    @Test
    public void testMergingNoTypesSamePosition() throws InvalidOffsetException {
        FeatureMap otherMap = Factory.newFeatureMap();
        otherMap.put("rule", "otherRule");

        AnnotationSet as = doc.getAnnotations();
        as.add(0L, 1L, TYPE, someMap);
        as.add(0L, 1L, TYPE, otherMap);

        AnnotationSet oas = runTest();
        assertEquals(1, oas.size());

        FeatureMap expected = Factory.newFeatureMap();
        expected.put("type", "example_type");
        expected.put("rule", "otherRule,someRule");
        assertEquals(expected, oas.inDocumentOrder().get(0).getFeatures());
    }

    @Test
    public void testNoMerging() throws InvalidOffsetException {
        FeatureMap otherMap = Factory.newFeatureMap();
        otherMap.put("type", "different_type");

        AnnotationSet as = doc.getAnnotations();
        as.add(0L, 1L, TYPE, someMap);
        as.add(0L, 1L, TYPE, otherMap);
        as.add(1L, 2L, TYPE, someMap);
        as.add(2L, 3L, TYPE, someMap);
        as.add(7L, 8L, TYPE, someMap);

        AnnotationSet oas = runTest();
        assertEquals(as, oas); // shouldn't touch.
    }

    @Test
    public void testFeatureMapMerging() {
        FeatureMap a = Factory.newFeatureMap();
        FeatureMap b = Factory.newFeatureMap();

        FeatureMap ret = AnnotationCleanup.mergeFeatureMaps(a, b);
        assertTrue(ret.isEmpty(), "Merging empty maps should give an empty map");

        String akey = "akey";
        a.put(akey, "hello");
        b.put(akey, "world");

        a.put("a", "a");
        b.put("b", "b");

        b.put("x", "x");
        a.put("x", "");

        ret = AnnotationCleanup.mergeFeatureMaps(a, b);
        assertEquals(4, ret.size());
        assertEquals("hello,world", ret.get(akey));
        assertEquals("x", ret.get("x"));

        assertEquals("a", a.get("a"));
        assertEquals("b", b.get("b"));
    }

    @Test
    void testCleanupNameAnnotationFeature() {
        // test on empty feature map
        FeatureMap fm = gate.Factory.newFeatureMap();
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals(0, fm.size());

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ll ff, ll ff");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);

        assertEquals("ll ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ll ff");
        fm.put(FeatureKeysName.FIRSTNAME, "Hans");
        fm.put(FeatureKeysName.LASTNAME, "MUSTER");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("LL ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ll ff, LL ff");
        fm.put(FeatureKeysName.FIRSTNAME, "Hans");
        fm.put(FeatureKeysName.LASTNAME, "MUSTER");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("LL ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ll ff, LL ff");
        fm.put(FeatureKeysName.FIRSTNAME, "Hans");
        fm.put(FeatureKeysName.LASTNAME, "Muster");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("ll ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ll ff, ff");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("ll ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ff, ff, ll ff, ff");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("ll ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));

        fm = gate.Factory.newFeatureMap();
        fm.put(FeatureKeysName.NAME_FORMAT, "ff ff");
        AnnotationCleanup.cleanupNameAnnotationFeature(fm);
        assertEquals("ff ff", fm.getOrDefault(FeatureKeysName.NAME_FORMAT, ""));
    }

    private AnnotationSet runTest() {
        pr.setDocument(doc);
        try {
            pr.execute();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return doc.getAnnotations(OUTPUT_AS);
    }
}
