package org.ratschlab.deidentifier.substitution;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.pipelines.PipelineConfigKeys;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


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
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "This is a date: <TheDate>21.10.2017</TheDate> and <TheDate>24.10.2017</TheDate> should be replaced", DUMMY_TAG);

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
            String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, "This is a date: <TheDate>21.10.2017</TheDate> and <TheDate>24.10.2017</TheDate> should be replaced", DUMMY_TAG);

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
}
