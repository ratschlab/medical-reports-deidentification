package org.ratschlab.deidentifier.utils;

import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.Range;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.ratschlab.gate.GateTools;

import java.util.Set;

public class AnnotationUtilsTest {

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    private static AnnotationSet createAnnotationSet(Set<Range<Long>> intervals) {
        try {
            // TODO fix
            Document doc = GateTools.documentFromXmlString("<hello>world asdfas asdfasdf asdf asdfasdf asdf asd </hello>");

            AnnotationSet as = doc.getAnnotations();

            intervals.forEach(r -> {
                try {
                    as.add(r.getMinimum(), r.getMaximum(), "dummy", Factory.newFeatureMap());
                } catch (InvalidOffsetException e) {
                    Assert.fail(e.getMessage());
                }
            });
            return as;
        } catch (GateException e) {
            Assert.fail(e.getMessage());
        }

        return null;
    }

    @Test
    public void testAnnotationRanges() {
        Set<Range<Long>> simpleRange = ImmutableSet.of(Range.between(3L, 5L));
        Assert.assertEquals(simpleRange, AnnotationUtils.annotationRanges(createAnnotationSet(simpleRange)));

        Set<Range<Long>> doubleRange = ImmutableSet.of(Range.between(3L, 5L),
            Range.between(10L, 15L));

        Assert.assertEquals(doubleRange, AnnotationUtils.annotationRanges(createAnnotationSet(doubleRange)));

        Set<Range<Long>> overlappingRange = ImmutableSet.of(Range.between(3L, 7L),
            Range.between(5L, 6L),
            Range.between(4L, 15L));
        Assert.assertEquals(ImmutableSet.of(Range.between(3L, 15L)), AnnotationUtils.annotationRanges(createAnnotationSet(overlappingRange)));
    }
}
