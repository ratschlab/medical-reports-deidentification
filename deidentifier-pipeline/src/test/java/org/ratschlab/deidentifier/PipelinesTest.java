package org.ratschlab.deidentifier;

import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.ratschlab.deidentifier.pipelines.testing.PipelineTester;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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

    private static Stream<Arguments> createTestCases() {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();

        File[] fileList = new File(classloader.getResource("pipeline_testcases").getPath()).
                listFiles((d, n) -> n.endsWith(".txt"));

        return Arrays.stream(fileList).map(f -> Arguments.of(f));
    }
}