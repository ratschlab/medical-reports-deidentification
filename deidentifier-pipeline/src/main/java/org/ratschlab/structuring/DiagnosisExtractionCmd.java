package org.ratschlab.structuring;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import gate.*;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.ratschlab.deidentifier.workflows.DefaultWorkflowConcern;
import org.ratschlab.deidentifier.workflows.PipelineWorkflow;
import org.ratschlab.deidentifier.workflows.WorkflowConcern;
import org.ratschlab.deidentifier.workflows.WriteToSerializedCorpus;
import org.ratschlab.gate.GateTools;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class DiagnosisExtractionCmd extends DbCommands implements Callable<Integer> {

    @CommandLine.Option(names = {"-i"}, description = "Input corpus dir")
    private File corpusInputDir = null;

    @CommandLine.Option(names = {"--xml-input"}, description = "Assumes input dir consists of xml files, one per report (testing purposes)")
    private boolean xmlInput = false;

    @CommandLine.Option(names = {"--json-input"}, description = "Assumes input dir consists of json files, one per report (testing purposes)")
    private boolean jsonInput = false;

    @CommandLine.Option(names = {"-c"}, description = "Config file", required = true)
    private File pipelineConfigFile = null;

    @CommandLine.Option(names = {"--output-corpus-dir"}, description = "Output GATE Corpus Dir")
    private File outputCorpusDir = null;

    @CommandLine.Option(names = {"-o"}, description = "Output Txt File")
    private File outputFile = null;

    @CommandLine.Option(names = {"-t"}, description = "Number of threads")
    private int threads = 1;

    private static final String DEFAULT_REPORTNR_KEY = "reportnr";

    public Integer call() {
        try {
            Gate.init();

            Config conf = ConfigUtils.loadConfig(pipelineConfigFile);
            SerialAnalyserController controller = KeywordBasedDiagnosisExtraction.getExtractionPipeline(conf);

            List<WorkflowConcern> concerns = new ArrayList<>();

            if (outputFile != null) {
                concerns.add(new WriteAnnotationsToTxt(outputFile,
                    d -> {
                        return d.getFeatures().getOrDefault(DEFAULT_REPORTNR_KEY, "").toString();
                    },
                    conf.getString(StructuringConfigKeys.FINAL_ANNOTATION_SET_NAME)));
            }

            if (outputCorpusDir != null) {
                concerns.add(new DefaultWorkflowConcern() {
                    @Override
                    public Document postProcessDoc(Document doc) {
                        FeatureMap features = doc.getFeatures();
                        if(features.containsKey(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY)) {
                            features.remove(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY);
                        }
                        return doc;
                    }
                });

                WorkflowConcern writeToCorpus = new WriteToSerializedCorpus(outputCorpusDir, threads, Optional.empty());
                concerns.add(writeToCorpus);
            }

            if (corpusInputDir != null && !xmlInput && !jsonInput) {
                Corpus inputCorpus = GateTools.openCorpus(corpusInputDir);

                PipelineWorkflow<Document> workflow = new PipelineWorkflow<>(
                        inputCorpus.stream(),
                        d -> {
                            Document copy = GateTools.copyDocument(d);
                            Factory.deleteResource(d);
                            return Optional.of(copy);
                        },
                        controller,
                        threads,
                        concerns);

                workflow.run();
            }

            if (corpusInputDir != null && xmlInput) {
                List<File> files = Lists.newArrayList(corpusInputDir.listFiles());

                PipelineWorkflow<File> workflow = new PipelineWorkflow<>(
                    files.stream(),
                    f -> GateTools.readDocumentFromFile(f),
                    controller,
                    threads,
                    concerns);

                workflow.run();
            }

            if (corpusInputDir != null && jsonInput) {
                List<File> files = Lists.newArrayList(corpusInputDir.listFiles());

                KisimFormat ksf = new KisimFormat();
                PipelineWorkflow<File> workflow = new PipelineWorkflow<>(
                        files.stream(),
                        f -> {
                            try {
                                // TODO: add doc id
                                String jsonStr = new String(Files.readAllBytes(f.toPath()));
                                Document doc = ksf.jsonToDocument(jsonStr);
                                doc.setName(f.getName().replaceAll(".json", ""));
                                doc.getFeatures().put(DEFAULT_REPORTNR_KEY, f.getName().split("_")[1].replaceAll(".json", ""));
                                return Optional.of(doc);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                            return Optional.empty();
                        },
                        controller,
                        threads,
                        concerns);

                workflow.run();
            }

        } catch (GateException e) {
            e.printStackTrace();
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        System.exit(CommandLine.call(new DiagnosisExtractionCmd(), args));
    }
}
