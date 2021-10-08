package org.ratschlab.deidentifier.utils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import org.junit.jupiter.api.Test;
import org.ratschlab.gate.GateTools;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

;

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
            Document doc = GateTools.documentFromXmlString("<hello>abcdefghij klmn opq rstufv</hello>");

            AnnotationSet as = doc.getAnnotations();

            intervals.forEach(r -> {
                try {
                    as.add(r.lowerEndpoint(), r.upperEndpoint(), "dummy", Factory.newFeatureMap());
                } catch (InvalidOffsetException e) {
                    fail(e.getMessage());
                }
            });
            return as;
        } catch (GateException e) {
            fail(e.getMessage());
        }

        return null;
    }

    @Test
    public void testAnnotationRanges() {
        Set<Range<Long>> simpleRange = ImmutableSet.of(Range.closedOpen(3L, 5L));
        assertEquals(simpleRange, AnnotationUtils.annotationRanges(createAnnotationSet(simpleRange)));

        Set<Range<Long>> doubleRange = ImmutableSet.of(Range.closedOpen(3L, 5L),
            Range.closedOpen(10L, 15L));

        assertEquals(doubleRange, AnnotationUtils.annotationRanges(createAnnotationSet(doubleRange)));

        Set<Range<Long>> overlappingRange = ImmutableSet.of(Range.closedOpen(3L, 7L),
            Range.closedOpen(5L, 6L),
            Range.closedOpen(4L, 15L));
        assertEquals(ImmutableSet.of(Range.closedOpen(3L, 15L)), AnnotationUtils.annotationRanges(createAnnotationSet(overlappingRange)));

        Set<Range<Long>> touchingRange = ImmutableSet.of(Range.closedOpen(3L, 5L),
                Range.closedOpen(5L, 6L));
        assertEquals(ImmutableSet.of(Range.closedOpen(3L, 6L)), AnnotationUtils.annotationRanges(createAnnotationSet(touchingRange)));

        Set<Range<Long>> touchingRangeNeighbor = ImmutableSet.of(Range.closedOpen(3L, 5L),
                Range.closedOpen(6L, 7L));
        assertEquals(touchingRangeNeighbor, AnnotationUtils.annotationRanges(createAnnotationSet(touchingRangeNeighbor)));

        assertEquals(ImmutableSet.of(Range.closedOpen(3L, 7L)),
                AnnotationUtils.annotationRanges(createAnnotationSet(touchingRangeNeighbor), ImmutableSet.of(Range.closedOpen(5L,6L))));
    }
}
