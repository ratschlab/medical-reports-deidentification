package org.ratschlab.deidentifier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ratschlab.deidentifier.annotation.TestUtils;
import org.ratschlab.deidentifier.pipelines.testing.PipelineTestSuite;
import org.ratschlab.deidentifier.pipelines.testing.PipelineTester;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

public class PipelinesTest {
    static PipelineTester tester = null;

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
        try {
            Gate.init();

            File config = new File(Thread.currentThread().getContextClassLoader().getResource("generic_test_pipeline.conf").getFile());
            tester = new PipelineTester(config);
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    @ParameterizedTest
    @MethodSource("createTestCases")
    public void testPipeline(File path) {
        try {
            tester.runTestcase(path);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testDateFormatNotIncludeDanglingSlash() throws GateException {
        // doing a specialised test here, since with '<Date>09.03.2017</Date>/Zürich' in a testfile, a whitespace before
        // the '/' is introduced during parsing, potentially leading to a different behaviour of the algorithm determining the date format
        Document doc = TestUtils.fromString("09.03.2017/Zürich");
        AnnotationSet expected = doc.getAnnotations(PipelineTestSuite.EXPECTED_ANNOTATION_SET_NAME);

        FeatureMap fm = Factory.newFeatureMap();
        fm.put("format", "dd.MM.yyyy");
        expected.add(0L, 10L, "Date", fm);

        runSingleTest(doc);
    }

    @Test
    public void testDateFormatNoDoubleDots() throws GateException {
        // doing a specialised test here, since with '<Date>09.03.2017</Date>/Zürich' in a testfile, a whitespace before
        // the '/' is introduced during parsing, potentially leading to a different behaviour of the algorithm determining the date format
        Document doc = TestUtils.fromString("am 24. November 2015....");
        AnnotationSet expected = doc.getAnnotations(PipelineTestSuite.EXPECTED_ANNOTATION_SET_NAME);

        FeatureMap fm = Factory.newFeatureMap();
        fm.put("format", "dd. MMMM yyyy");
        expected.add(3L, 20L, "Date", fm);

        runSingleTest(doc);
    }

    private static Stream<Arguments> createTestCases() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        File[] fileList = new File(classloader.getResource("pipeline_testcases").getPath()).
                listFiles((d, n) -> n.endsWith(".txt"));

        return Arrays.stream(fileList).map(f -> Arguments.of(f));
    }

    private static void runSingleTest(Document doc) {
        PipelineTestSuite suite = null;
        try {
            suite = PipelineTestSuite.createTestSuite(ImmutableList.of(doc), ImmutableSet.of("Date"), Collections.emptySet());
        } catch (ResourceInstantiationException e) {
            Assert.fail(e.getMessage());
        }

        tester.runSuite(suite);
    }
}
