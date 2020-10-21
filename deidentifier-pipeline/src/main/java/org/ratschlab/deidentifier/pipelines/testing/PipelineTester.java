package org.ratschlab.deidentifier.pipelines.testing;

import com.typesafe.config.Config;
import gate.*;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.junit.jupiter.api.function.Executable;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.pipelines.PipelineConfigKeys;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.jupiter.api.Assertions.assertAll;

public class PipelineTester {
    private static final Logger log = LoggerFactory.getLogger(PipelineTester.class);

    private SerialAnalyserController pipeline;
    private String finalOutputAs;

    public PipelineTester(File configFile) throws GateException {
        Config conf = ConfigUtils.loadConfig(configFile);
        this.pipeline = PipelineFactory.getRuleBasedPipeline(conf);
        this.finalOutputAs = conf.getString(PipelineConfigKeys.FINAL_ANNOTATION_SET_NAME);
    }

    public PipelineTester(SerialAnalyserController pipeline) {
        this.pipeline = pipeline;
    }

    private static boolean featuresAreSubset(FeatureMap subset, FeatureMap superset) {
        return subset.keySet().stream().
                allMatch(k -> superset.containsKey(k) && superset.get(k).equals(subset.get(k)));
    }

    private static List<Executable> checkIsSubset(Document doc, AnnotationSet subsetAs, AnnotationSet supersetAs, boolean checkFeatures, String msg) {
        List<Executable> fails = new ArrayList<>();

        for (Annotation a : subsetAs) {
            AnnotationSet s = supersetAs.getCovering(a.getType(), a.getStartNode().getOffset(), a.getEndNode().getOffset());

            List<Annotation> cands = s.stream().
                    filter(ca -> !checkFeatures || featuresAreSubset(a.getFeatures(), ca.getFeatures())).collect(Collectors.toList());

            int setSize = cands.size();

            String line = gate.Utils.stringFor(doc, doc.getAnnotations());

            String errorLine = line.substring(0, a.getStartNode().getOffset().intValue()) +
                        "~" + Utils.cleanStringFor(doc, a) + "~" +
                       line.substring(a.getEndNode().getOffset().intValue());


            String others = supersetAs.get(a.getType()).stream().
                    map(aa -> " " + aa.getStartNode().getOffset() + " " + aa.getEndNode().getOffset()).
                    reduce((x,y) -> x + y).
                    map(os -> String.format("(%s annotations at pos: %s)", a.getType(), os)).
                    orElse("");

            // TODO include others again?
            String m = String.format(msg, a.getType(), a.getFeatures().toString(), errorLine);

            fails.add(() -> {
                assertThat(m, setSize, greaterThan(0));
            });
        }

        return fails;
    }

    public void runSuite(PipelineTestSuite suite) {
        try {
            //ErrorCollector collector = new ErrorCollector();

            Corpus corpus = suite.getCorpus();

            log.info(String.format("Running test suite with %d test cases", corpus.size()));

            pipeline.setCorpus(corpus);

            pipeline.execute();

            pipeline.getCorpus().getDocumentNames().size();

            List<Executable> allFailures = new ArrayList<>();

            for (Document mydoc : corpus) {
                String xmlTxt = mydoc.toXml(mydoc.getAnnotations(finalOutputAs), true).replace("%", "%%");
                Integer linenr = Integer.parseInt(mydoc.getFeatures().get(PipelineTestSuite.LINE_NR_KEY).toString());

                AnnotationSet target = mydoc.getAnnotations(PipelineTestSuite.EXPECTED_AS_NAME);
                //target.removeIf(a -> a.getType().equals(PipelineTestSuite.DUMMY_TAG) || suite.getContextTags().contains(a.getType()));

                AnnotationSet annot = mydoc.getAnnotations(finalOutputAs); //compare sets
                annot.removeIf(a -> !suite.getTags().contains(a.getType())); // only focus on phi annotations we are interested in

                allFailures.addAll(checkIsSubset(mydoc, target, annot, true, String.format("Missing %%s (%%s) annotation in %s line %d: %%s \nOther annotations: %s", suite.getName(), linenr, xmlTxt)));
                allFailures.addAll(checkIsSubset(mydoc, annot, target, false, String.format("Didn't expect %%s (%%s) annotation in %s line nr %d: %%s \nOther annotations: %s", suite.getName(), linenr, xmlTxt)));
            }

            assertAll(allFailures);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void runTestcase(File f) throws IOException, ResourceInstantiationException {
        runSuite(PipelineTestSuite.createTestSuite(f.toPath()));
    }

    public void runAllTestcases(File dir) throws IOException, ResourceInstantiationException {
        File[] testFiles = dir.listFiles((d, n) -> n.endsWith(".txt"));

        for(File f : testFiles) {
            runTestcase(f);
        }
    }

    public static void main(String[] args) throws GateException, IOException {
        Gate.init();

        //new PipelineTester().runAllTestcases(new File("/Users/marc/git/deidentifier-poc/deidentifier-pipeline/src/test/resources/pipeline_testcases"));
        //new PipelineTester().runAllTestcases(new File("/Users/marc/git/deidentifier-poc/configs/kisim-usz/testcases/"));
    }
}
