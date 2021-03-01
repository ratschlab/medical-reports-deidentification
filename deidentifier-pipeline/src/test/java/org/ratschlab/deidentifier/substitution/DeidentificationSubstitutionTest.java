package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.util.GateException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.ratschlab.deidentifier.annotation.AnnotTuple;
import org.ratschlab.deidentifier.utils.AnnotationUtils;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.GateTools;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class DeidentificationSubstitutionTest {
    private static String phiAnnotationName = "phiannotations";
    private static String DUMMY_TAG = "dummy";

    private static Set<String> pipelineAnnotationTags = ImmutableSet.of("Name", "Location", "Date", "Address");

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest
    @MethodSource("substitutionTestCases")
    public void testWithScrubberSubstitution(Document doc) {

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("TheDate");

        Assert.assertEquals(2, dateAnnots.size());

        for (Annotation an : dateAnnots) {
            Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, an));
        }
    }

    @ParameterizedTest
    @MethodSource("substitutionTestCases")
    public void testWithReplacementTagSubstitution(Document doc) {

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ReplacementTagsSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        Assert.assertTrue(ReplacementTagsSubstitution.replacementTagsValid(substDoc.getContent().toString()));
    }

    private static Stream<Arguments> substitutionTestCases() {
        Stream<Arguments> args = Stream.of(
            prepareAnnotationDoc("This is a date <Date><TheDate>21.10.2017</TheDate></Date> and <TheDate><Date>24.10.2017</Date></TheDate> should be replaced"),
            dummyDocumentWithAnnotation(ImmutableList.of(
                AnnotTuple.of(2, 5, "TheDate"),
                AnnotTuple.of(2, 5, "Date"),
                AnnotTuple.of(3, 4, "Name"), // shorter than previous annotation, should be removed
                // second block
                AnnotTuple.of(10, 15, "TheDate"),
                AnnotTuple.of(10, 15, "Name"),
                AnnotTuple.of(10, 15, "Date")
            ))
        ).map(f -> Arguments.of(f));

        return args;
    }

    @Test
    public void testOverlapSubstitutionComplex() {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 5, "Location"),
            AnnotTuple.of(3, 7, "Location"),
            AnnotTuple.of(6, 10, "Date")
        ));

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ReplacementTagsSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        String substCont = substDoc.getContent().toString();

        Assert.assertTrue(ReplacementTagsSubstitution.replacementTagsValid(substCont));
    }

    @Test
    public void testCrossingOrigMarkupSubstitution() {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 5, "Markup1"),
            AnnotTuple.of(5, 10, "Markup2"),
            AnnotTuple.of(2, 10, "Location") // adding annotation which spans Markup1 and Markup2 elements
        ));


        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        Assert.assertEquals("LOCATION", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup1").iterator().next()));
        Assert.assertEquals("LOCATION", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup2").iterator().next()));
    }

    private Document addressDoc = dummyDocumentWithAnnotation(ImmutableList.of(
        AnnotTuple.of(0, 2, "tag1"),
        AnnotTuple.of(2, 20, "PostalAddress"),
        AnnotTuple.of(2, 20, "Address"),
        AnnotTuple.of(3, 20, "Location"),
        AnnotTuple.of(20, 26, "tag2")
    ));

    @Test
    public void testWholeAddressSubstitution() {
        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), true, Collections.emptyList());
        Document substDoc = subst.substitute(addressDoc);

        AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        // the PostalAddress markup tag should be replaced with "ADDRESS" string
        Assert.assertEquals("ADDRESS", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("PostalAddress").iterator().next()));
    }

    @Test
    public void testAddressHandlingWithoutWholeAddressSubstitution() {
        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(addressDoc);

        // don't substitute Address annotation, just replace Location annotation within Address annotation
        String substDocContent = substDoc.getContent().toString();
        Assert.assertTrue(substDocContent.contains("LOCATION"));
        Assert.assertFalse(substDocContent.contains("ADDRESS"));
    }

    @Test
    public void testSimpleFilter() {
        String tag = "SecretTag";
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 20, tag),
            AnnotTuple.of(5, 10, "Name"),
            AnnotTuple.of(22, 25, tag)
        ));

        List<PathConstraint> filters = ImmutableList.of(new PathConstraint(tag, Collections.emptyList()));
        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, filters);
        Document substDoc = subst.substitute(doc);

        String substContent = substDoc.getContent().toString();

        Assert.assertFalse(substContent.contains("NAME"));

        AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get(tag);
        Assert.assertEquals(0, dateAnnots.size());
    }

    @Test
    public void testNestedFilter() {
        String tag = "SecretTag";
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 15, "ParentTag"),
            AnnotTuple.of(3, 12, tag),
            AnnotTuple.of(5, 7, "Name"),
            AnnotTuple.of(18, 25, tag),
            AnnotTuple.of(19, 22, "Location")
        ));

        // should remove 'SecretTag' fields only if they are somewhere below a 'MyParent' field.
        List<PathConstraint> filters = ImmutableList.of(new PathConstraint(tag, ImmutableList.of("", "ParentTag", tag)));
        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, filters);
        Document substDoc = subst.substitute(doc);

        String substContent = substDoc.getContent().toString();

        Assert.assertFalse(substContent.contains("NAME")); // should be filtered
        Assert.assertTrue(substContent.contains("LOCATION")); // not filtered, hence normally subsituted

        AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get(tag);
        Assert.assertEquals(1, dateAnnots.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "This is some ovelrap <Location><Name myfeautre='bla'>text</Name></Location>",
        "This is some ovelrap <Name><Location><Name myfeautre='bla'>text</Name></Location></Name> ab",
        "hello <Name> world <Location>sth</Location> <Name><Location> hello </Location></Name></Name>"
    })
    public void testNested(String content) {
        Document doc = prepareAnnotationDoc(content);

        AnnotationSet as = doc.getAnnotations(phiAnnotationName);

        Assert.assertTrue(AnnotationUtils.hasOverlappingAnnotations(as));
        AnnotationUtils.removeRedundantAnnotations(as);
        Assert.assertFalse(AnnotationUtils.hasOverlappingAnnotations(as));
    }

    @ParameterizedTest
    @MethodSource("testSplitAnnotationsAcrossMarkupBoundariesTestCases")
    public void testSplitAnnotationsAcrossMarkupBoundaries(Document doc) {
        AnnotationSet as = doc.getAnnotations(phiAnnotationName);
        AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        List<Annotation> origAnnots = as.stream().collect(Collectors.toList());

        DeidentificationSubstitution.splitAnnotationsAcrossMarkupBoundaries(as, markups);

        Assert.assertTrue(AnnotationUtils.checkAnnotationCoverage(origAnnots, as.stream().collect(Collectors.toList()), markups));
    }

    private static Stream<Arguments> testSplitAnnotationsAcrossMarkupBoundariesTestCases() {
        try {
            Stream<Arguments> args = Stream.of(
                    prepareAnnotationDoc("<doc><mydoc><Name><Firstname>Peter</Firstname><Lastname>Meier</Lastname></Name></mydoc></doc>"),
                    prepareAnnotationDoc("<doc><mydoc><Name><Firstname><prefix></prefix><prefix></prefix><el>Peter</el></Firstname></Name></mydoc></doc>"),
                    prepareAnnotationDoc("<doc><mydoc><Name><prefix></prefix><prefix></prefix><Firstname>Peter</Firstname></Name></mydoc></doc>"),
                    dummyDocumentWithAnnotation(ImmutableList.of(
                            AnnotTuple.of(0, 10, "Markup1"),
                            AnnotTuple.of(10, 11, "Markup2"),
                            AnnotTuple.of(11, 13, "Markup3"),
                            AnnotTuple.of(0, 10, "Name"),
                            AnnotTuple.of(5, 13, "Name")
                    ))
            ).map(f -> Arguments.of(f));


            return args;
        } catch (Exception e) {
            Assert.fail(e.getMessage());
            return Stream.empty();
        }
    }

    @ParameterizedTest
    @MethodSource("testSplitOverlappingAnnotationsTestCases")
    public void testSplitOverlappingAnnotations(Document doc) {
        AnnotationSet as = doc.getAnnotations(phiAnnotationName);

        List<Annotation> origAnnotations = as.stream().collect(Collectors.toList());

        DeidentificationSubstitution.splitOverlappingAnnotations(as);

        Assert.assertFalse(AnnotationUtils.hasOverlappingAnnotations(as));
        Assert.assertTrue(AnnotationUtils.checkAnnotationCoverage(origAnnotations,
                as.stream().collect(Collectors.toList()),
                doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME)));
    }

    private static Stream<Arguments> testSplitOverlappingAnnotationsTestCases() {
        Stream<Arguments> args = Stream.of(
                dummyDocumentWithAnnotation(ImmutableList.of(
                        AnnotTuple.of(2, 5, "Name"),
                        AnnotTuple.of(3, 7, "Name")
                )),
                dummyDocumentWithAnnotation(ImmutableList.of(
                        AnnotTuple.of(2, 5, "Name"),
                        AnnotTuple.of(4, 9, "Name"),
                        AnnotTuple.of(8, 10, "Name"))),
                // same but closer interval.
                dummyDocumentWithAnnotation(ImmutableList.of(
                        AnnotTuple.of(2, 5, "Name"),
                        AnnotTuple.of(4, 7, "Name"),
                        AnnotTuple.of(5, 10, "Name"))),
                // more complicated case
                dummyDocumentWithAnnotation(ImmutableList.of(
                        AnnotTuple.of(2, 6, "Name"),
                        AnnotTuple.of(3, 7, "Name"),
                        AnnotTuple.of(5, 10, "Name"),
                        AnnotTuple.of(8, 11, "Name"))),
                // no overlap
                dummyDocumentWithAnnotation(ImmutableList.of(
                        AnnotTuple.of(2, 5, "Name"),
                        AnnotTuple.of(6, 10, "Name")
                ))
        ).map(f -> Arguments.of(f));

        return args;
    }

    private static Document prepareAnnotationDoc(String content) {
        try {
            Document doc = simpleDocument(content);

            AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            List<Annotation> phiAnnotationAnnots = markups.stream().filter(a -> pipelineAnnotationTags.contains(a.getType())).collect(Collectors.toList());

            markups.removeAll(phiAnnotationAnnots);
            doc.getAnnotations(phiAnnotationName).addAll(phiAnnotationAnnots);

            return doc;
        } catch (GateException e) {
            Assert.fail(e.getMessage());
        }
        return null;
    }

    private static Document simpleDocument(String content) throws GateException {
        String docStr = String.format("<%s>%s</%s>", DUMMY_TAG, content, DUMMY_TAG);

        return GateTools.documentFromXmlString(docStr);
    }

    private static Document dummyDocumentWithAnnotation(Iterable<AnnotTuple> annotRanges) {
        try {
            String dummyString = IntStream.range(0, 26).mapToObj(c -> (char) ((int) 'a' + c)).collect(Collector.of(
                StringBuilder::new,
                StringBuilder::append,
                StringBuilder::append,
                StringBuilder::toString));

            Document doc = simpleDocument(dummyString);

            AnnotationSet phiTags = doc.getAnnotations(phiAnnotationName);
            AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            for (AnnotTuple r : annotRanges) {
                AnnotationSet as = pipelineAnnotationTags.contains(r.getTag()) ? phiTags : markups;
                as.add((long) r.getStart(), (long) r.getEnd(), r.getTag(), Factory.newFeatureMap());
            }

            return doc;
        } catch (GateException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }
}
