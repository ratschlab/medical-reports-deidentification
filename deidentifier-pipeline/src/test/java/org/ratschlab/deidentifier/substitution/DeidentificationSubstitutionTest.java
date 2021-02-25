package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.util.GateException;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.ratschlab.deidentifier.annotation.AnnotTuple;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.GateTools;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class DeidentificationSubstitutionTest {
    static String phiAnnotationName = "phiannotations";

    private static Set<String> pipelineAnnotationTags = ImmutableSet.of("Name", "Location", "Date", "Address");

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
        try {
            Gate.init();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    private static String DUMMY_TAG = "dummy";

    @Test
    public void testSimpleSubstitution() throws GateException {

        Document doc = prepareAnnotationDoc("This is a date <Date><TheDate>21.10.2017</TheDate></Date> and <TheDate><Date>24.10.2017</Date></TheDate> should be replaced");

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("TheDate");

        Assert.assertEquals(2, dateAnnots.size());

        for (Annotation an : dateAnnots) {
            Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, an));
        }
    }

    @Test
    public void testContainedSubstitution() throws GateException {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 5, "TheDate"),
            AnnotTuple.of(2, 5, "Date"),
            AnnotTuple.of(3, 4, "Name"), // shorter than previous annotation, should be removed
            // second block
            AnnotTuple.of(10, 15, "TheDate"),
            AnnotTuple.of(10, 15, "Date"),
            AnnotTuple.of(11, 15, "Name")
        ));


        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("TheDate");

        Assert.assertEquals(2, dateAnnots.size());

        for (Annotation an : dateAnnots) {
            Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, an));
        }
    }

    @Test
    public void testOverlapSubstitutionComplex() throws GateException {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 5, "Location"),
            AnnotTuple.of(3, 7, "Location"),
            AnnotTuple.of(6, 10, "Date")
        ));

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new IdentitySubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        String substCont = substDoc.getContent().toString();

        Assert.assertEquals(doc.getContent().toString(), substCont);
        //Assert.assertTrue(ReplacementTagsSubstitution.documentValid(substCont));
    }

    @Test
    public void testCrossingOrigMarkupSubstitution() throws GateException {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 5, "Markup1"),
            AnnotTuple.of(7, 10, "Markup2"),
            AnnotTuple.of(2, 10, "Location") // adding annotation which spans Markup1 and Markup2 elements
        ));


        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        Assert.assertEquals("LOCATION", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup1").iterator().next()));
        Assert.assertEquals("LOCATION", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup2").iterator().next()));
    }

    @Test
    public void testAddressSubstitution() throws GateException {
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 20, "PostalAddress"),
            AnnotTuple.of(3, 20, "Location"),
            AnnotTuple.of(2, 20, "Address")
        ));

        DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), true, Collections.emptyList());
        Document substDoc = subst.substitute(doc);

        AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        // the PostalAddress markup tag should be replaced with "ADDRESS" string
        Assert.assertEquals("ADDRESS", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("PostalAddress").iterator().next()));
    }

    @Test
    public void testSimpleFilter() throws GateException {
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
    public void testNestedFilter() throws GateException {
        String tag = "SecretTag";
        Document doc = dummyDocumentWithAnnotation(ImmutableList.of(
            AnnotTuple.of(2, 25, "ParentTag"),
            AnnotTuple.of(3, 22, tag),
            AnnotTuple.of(5, 10, "Name"),
            AnnotTuple.of(30, 35, tag),
            AnnotTuple.of(32, 34, "Location")
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
    @CsvSource({
        "This is some ovelrap <Location><Name myfeautre='bla'>text</Name></Location>",
        "This is some ovelrap <Name><Location><Name myfeautre='bla'>text</Name></Location></Name> ab",
        "hello <Name> world <Location>sth</Location> <Name><Location> hello </Location></Name></Name>"
    })
    public void testNested(String content) {
        try {
            Document doc = prepareAnnotationDoc(content);

            AnnotationSet as = doc.getAnnotations(phiAnnotationName);

            Assert.assertTrue(org.ratschlab.deidentifier.annotation.Utils.hasOverlappingAnnotations(as));
            DeidentificationSubstitution.removeRedundantAnnotations(as);
            Assert.assertFalse(org.ratschlab.deidentifier.annotation.Utils.hasOverlappingAnnotations(as));

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    @ParameterizedTest
    @MethodSource("testSplitAnnotationsAcrossMarkupBoundariesTestCases")
    public void testSplitAnnotationsAcrossMarkupBoundaries(Document doc) {
        try {
            AnnotationSet as = doc.getAnnotations(phiAnnotationName);
            AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            DeidentificationSubstitution.splitAnnotationsAcrossMarkupBoundaries(as, markups);

            Assert.assertFalse(org.ratschlab.deidentifier.annotation.Utils.hasOverlappingAnnotations(as));

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    private static Stream<Arguments> testSplitAnnotationsAcrossMarkupBoundariesTestCases() {
        try {
            Stream<Arguments> args = Stream.of(
                prepareAnnotationDoc("<doc>  <Name><Firstname>Peter</Firstname><Lastname>Meier</Lastname></Name></doc>")
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

        DeidentificationSubstitution.splitOverlappingAnnotations(as);

        Assert.assertFalse(org.ratschlab.deidentifier.annotation.Utils.hasOverlappingAnnotations(as));
    }

    private static Stream<Arguments> testSplitOverlappingAnnotationsTestCases() throws GateException {
        Stream<Arguments> args = Stream.of(
            dummyDocumentWithAnnotation(ImmutableList.of(
                AnnotTuple.of(2, 5, "Name"),
                AnnotTuple.of(4, 7, "Name"),
                AnnotTuple.of(6, 10, "Name")))
        ).map(f -> Arguments.of(f));

        return args;
    }

    private static Document prepareAnnotationDoc(String content) throws GateException {
        Document doc = simpleDocument(content);

        AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        List<Annotation> phiAnnotationAnnots = markups.stream().filter(a -> pipelineAnnotationTags.contains(a.getType())).collect(Collectors.toList());

        markups.removeAll(phiAnnotationAnnots);
        doc.getAnnotations(phiAnnotationName).addAll(phiAnnotationAnnots);

        return doc;
    }

    private static Document simpleDocument(String content) throws GateException {
        String docStr = String.format("<%s>%s</%s>", DUMMY_TAG, content, DUMMY_TAG);

        return GateTools.documentFromXmlString(docStr);
    }

    private static Document dummyDocumentWithAnnotation(Iterable<AnnotTuple> annotRanges) throws GateException {
        Document doc = simpleDocument(RandomStringUtils.randomAlphanumeric(100)); // TODO: something better

        AnnotationSet phiTags = doc.getAnnotations(phiAnnotationName);
        AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        for (AnnotTuple r : annotRanges) {
            AnnotationSet as = pipelineAnnotationTags.contains(r.getTag()) ? phiTags : markups;
            as.add((long) r.getStart(), (long) r.getEnd(), r.getTag(), Factory.newFeatureMap());
        }

        return doc;
    }

}
