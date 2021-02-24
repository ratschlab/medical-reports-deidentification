package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.pipelines.PipelineConfigKeys;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SubstitutionTest {

    static SerialAnalyserController pipeline = null;

    static String phiAnnotationName = null;

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
        Properties prop = new Properties();
        try {
            Gate.init();

            File configFile = new File(Thread.currentThread().getContextClassLoader().getResource("generic_test_pipeline.conf").getFile());

            Config cfg = ConfigUtils.loadConfig(configFile);
            pipeline = PipelineFactory.getRuleBasedPipeline(cfg);
            phiAnnotationName = cfg.getString(PipelineConfigKeys.FINAL_ANNOTATION_SET_NAME);
        } catch (GateException e) {
            e.printStackTrace();
        }

    }

    private static String DUMMY_TAG = "dummy";

    @Test
    public void testSimpleSubstitution() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "This is a date <TheDate>21.10.2017</TheDate> and <TheDate>24.10.2017</TheDate> should be replaced", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);
            GateTools.processDoc(doc, pipeline);

            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
            Document substDoc = subst.substitute(doc);

            AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("TheDate");

            Assert.assertEquals(2, dateAnnots.size());

            for(Annotation an : dateAnnots) {
                Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, an));
            }
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOverlapSubstitution() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "This is a date <TheDate>21.10.2017</TheDate> and <TheDate>24.10.2017</TheDate> should be replaced", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);
            GateTools.processDoc(doc, pipeline);

            // Adding rogue annotations on the existing ones which are a bit shorter
            AnnotationSet annotated = doc.getAnnotations(phiAnnotationName);
            List<Pair<Long, Long>> newAnnotCoords = annotated.stream().map(a -> Pair.of(a.getStartNode().getOffset() + 2, a.getEndNode().getOffset() - 1)).collect(Collectors.toList());
            newAnnotCoords.forEach(p -> {
                try {
                    annotated.add(p.getLeft(), p.getRight(),"Contact", Factory.newFeatureMap());
                } catch(Exception e) {Assert.fail();}
            });

            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
            Document substDoc = subst.substitute(doc);

            AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("TheDate");

            Assert.assertEquals(2, dateAnnots.size());

            for(Annotation an : dateAnnots) {
                Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, an));
            }
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testOverlapSubstitutionComplex() {
        try {
            String location = "Xwil";
            String content = "Klinik Xwil 20.01.2010";
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, content, DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);

            long locationStart = (long) content.indexOf(location);
            long dateStart = (long) content.indexOf("20.");

            // Adding rogue annotations on the existing ones which are a bit shorter
            AnnotationSet annotated = doc.getAnnotations(phiAnnotationName);
            annotated.add(0L, dateStart, "Location", Factory.newFeatureMap());
            annotated.add(locationStart, dateStart + 2, "Location", Factory.newFeatureMap());
            annotated.add(dateStart, dateStart + 10, "Date", Factory.newFeatureMap());

            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new IdentitySubstitution(), false, Collections.emptyList());
            Document substDoc = subst.substitute(doc);

            //Assert.assertTrue(ReplacementTagsSubstitution.documentValid(substDoc.getContent().toString()));
            Assert.assertEquals(content, substDoc.getContent().toString());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCrossingOrigMarkupSubstitution() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "<Markup1>some text</Markup1><Markup2>some_text</Markup2>", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);

            AnnotationSet origMarkups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            AnnotationSet annotated = doc.getAnnotations(phiAnnotationName);

            // adding annotation which spans Markup1 and Markup2 elements
            annotated.add(origMarkups.get("Markup1").iterator().next().getStartNode().getOffset(),
                    origMarkups.get("Markup2").iterator().next().getEndNode().getOffset(),
                    "Date", Factory.newFeatureMap());

            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, Collections.emptyList());
            Document substDoc = subst.substitute(doc);

            AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup1").iterator().next()));
            Assert.assertEquals("DATE", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("Markup2").iterator().next()));
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAddressSubstitution() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, " <PostalAddress> Unispital, Musterstrasse </PostalAddress>    ", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);

            AnnotationSet origMarkups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            AnnotationSet annotated = doc.getAnnotations(phiAnnotationName);

            annotated.add(origMarkups.get("PostalAddress").iterator().next().getStartNode().getOffset(),
                    origMarkups.get("PostalAddress").iterator().next().getEndNode().getOffset() - 1,
                    "Location", Factory.newFeatureMap());

            annotated.add(origMarkups.get("PostalAddress").iterator().next().getStartNode().getOffset(),
                    origMarkups.get("PostalAddress").iterator().next().getEndNode().getOffset(),
                    "Address", Factory.newFeatureMap());

            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), true, Collections.emptyList());
            Document substDoc = subst.substitute(doc);

            AnnotationSet origMarkupsSubst = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

            Assert.assertEquals("ADDRESS", gate.Utils.stringFor(substDoc, origMarkupsSubst.get("PostalAddress").iterator().next()));
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSimpleFilter() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "<text>This is some address:</text><Address>secret reoad</Address> <Address>another secret road</Address><moretext>should be removed</moretext>", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);
            GateTools.processDoc(doc, pipeline);


            List<PathConstraint> filters = ImmutableList.of(new PathConstraint("Address", Collections.emptyList()));
            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, filters);
            Document substDoc = subst.substitute(doc);

            Assert.assertFalse(substDoc.getContent().toString().contains("secret"));

            AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("Address");
            Assert.assertEquals(0, dateAnnots.size());
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNestedFilter() {
        try {
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "<text>20.01.2019 This is some address </text><MyParent><Address>secret address 20.01.2019 </Address></MyParent><Address>public address</Address><text>okay</text>", DUMMY_TAG);

            Document doc = GateTools.documentFromXmlString(xmlDoc);
            GateTools.processDoc(doc, pipeline);

            // should remove 'Address' fields only if they are somewhere below a 'MyParent' field.
            List<PathConstraint> filters = ImmutableList.of(new PathConstraint("Address", ImmutableList.of("", "MyParent", "Address")));
            DeidentificationSubstitution subst = new DeidentificationSubstitution(phiAnnotationName, d -> new ScrubberSubstitution(), false, filters);
            Document substDoc = subst.substitute(doc);

            Assert.assertFalse(substDoc.getContent().toString().contains("secret"));

            AnnotationSet dateAnnots = substDoc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME).get("Address");
            Assert.assertEquals(1, dateAnnots.size());
        } catch (InvalidOffsetException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    private static Document prepareAnnotationDoc(String content) throws GateException {
        String docStr = String.format("<%s>%s</%s>", DUMMY_TAG, content, DUMMY_TAG);

        Document doc = GateTools.documentFromXmlString(docStr);
        AnnotationSet markups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);

        Set<String> pipelineAnnotationTags = ImmutableSet.of("Name", "Location", "Date");

        List<Annotation> phiAnnotationAnnots = markups.stream().filter(a -> pipelineAnnotationTags.contains(a.getType())).collect(Collectors.toList());

        markups.removeAll(phiAnnotationAnnots);
        doc.getAnnotations(phiAnnotationName).addAll(phiAnnotationAnnots);

        return doc;
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
            DeidentificationSubstitution.removeOverlappingAnnotations(as);
            Assert.assertFalse(org.ratschlab.deidentifier.annotation.Utils.hasOverlappingAnnotations(as));

        } catch(Exception e) {
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

        } catch(Exception e) {
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
}

