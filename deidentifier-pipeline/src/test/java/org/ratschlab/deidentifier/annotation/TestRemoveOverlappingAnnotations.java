package org.ratschlab.deidentifier.annotation;

import com.google.common.collect.ImmutableList;
import gate.*;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

public class TestRemoveOverlappingAnnotations extends AnalyserTestBase {
    private AbstractLanguageAnalyser pr = null;

    private Document doc = null;

    @BeforeEach
    public void initialize() {
        super.initialize();
        doc = createDummyDoc();

        try {

            pr = (AbstractLanguageAnalyser) Factory.createResource("org.ratschlab.deidentifier.annotation.RemoveOverlappingAnnotations");
            pr.setParameterValue("inputASName", "");

            pr.init();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest()
    @MethodSource("testMergingParameters")
    public void testMerging(List<AnnotTuple> annots, AnnotTuple expected) {
        AnnotationSet as = doc.getAnnotations();
        annots.forEach(a -> {
            try {
                a.addToAnnotationSet(as);
            } catch (Exception e){
                Assert.fail();
            }
        });

        AnnotationSet out = runTest();
        Assert.assertEquals(1, out.size());

        Annotation an = out.iterator().next();
        Assert.assertEquals(expected.getStart(), an.getStartNode().getOffset().intValue());
        Assert.assertEquals(expected.getEnd(), an.getEndNode().getOffset().intValue());
        Assert.assertEquals(expected.getTag(), an.getType());
    }

    static Stream<Arguments> testMergingParameters() {
        FeatureMap highConfidence = Factory.newFeatureMap();
        highConfidence.put("confidence", 100);

        return Stream.of(
                Arguments.of(ImmutableList.of(new AnnotTuple(0, 5, "Name"), new AnnotTuple(0, 5, "Age")), new AnnotTuple(0, 5, "Name")),
                Arguments.of(ImmutableList.of(new AnnotTuple(0, 5, "Name"), new AnnotTuple(3, 4, "Location")), new AnnotTuple(0, 5, "Name")),
                Arguments.of(ImmutableList.of(new AnnotTuple(0, 5, "Name"), new AnnotTuple(0, 5, "Location", highConfidence)), new AnnotTuple(0, 5, "Location"))
        );
    }

    private AnnotationSet runTest() {
        pr.setDocument(doc);
        try {
            pr.execute();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return doc.getAnnotations("");
    }
}
