package org.ratschlab.structuring;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import gate.*;
import gate.creole.SerialAnalyserController;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.sources.ImportCmd;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.ratschlab.deidentifier.workflows.DefaultWorkflowConcern;
import org.ratschlab.deidentifier.workflows.PipelineWorkflow;
import org.ratschlab.deidentifier.workflows.WorkflowConcern;
import org.ratschlab.deidentifier.workflows.WriteToSerializedCorpus;
import org.ratschlab.gate.GateTools;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Stream;

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


    private PipelineWorkflow<?> readFromFiles(List<WorkflowConcern> concerns, SerialAnalyserController controller) throws Exception {
        if (!xmlInput && !jsonInput) {
            Corpus inputCorpus = GateTools.openCorpus(corpusInputDir);

            return new PipelineWorkflow<>(
                    inputCorpus.stream(),
                    d -> {
                        Document copy = GateTools.copyDocument(d);
                        Factory.deleteResource(d);
                        return Optional.of(copy);
                    },
                    controller,
                    threads,
                    concerns);
        }

        if (xmlInput) {
            List<File> files = Lists.newArrayList(corpusInputDir.listFiles());

            return new PipelineWorkflow<>(
                    files.stream(),
                    f -> GateTools.readDocumentFromFile(f),
                    controller,
                    threads,
                    concerns);
        }


        List<File> files = Lists.newArrayList(corpusInputDir.listFiles());

        KisimFormat ksf = new KisimFormat();
        return new PipelineWorkflow<>(
                files.stream(),
                f -> {
                    try {
                        // TODO: add doc id
                        String jsonStr = new String(Files.readAllBytes(f.toPath()));
                        Document doc = ksf.jsonToDocument(jsonStr);
                        doc.setName(f.getName().replaceAll(".json", ""));
                        doc.getFeatures().put(DEFAULT_REPORTNR_KEY, f.getName().split("_")[1].replaceAll(".json", ""));
                        return Optional.of(doc);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return Optional.empty();
                },
                controller,
                threads,
                concerns);

    }

    public Integer call() {
        try {
            Gate.init();

            Config conf = ConfigUtils.loadConfig(pipelineConfigFile);
            SerialAnalyserController controller = KeywordBasedDiagnosisExtraction.getDiagnosisExtractionPipeline(conf);

            List<WorkflowConcern> concerns = new ArrayList<>();

            Function<Document, String> getDocId = d -> d.getFeatures().getOrDefault(DEFAULT_REPORTNR_KEY, "").toString();

            if (outputFile != null) {
                concerns.add(new WriteAnnotationsToTxt(outputFile, getDocId));
            }

            final KisimSource ks = (databaseConfigPath != null && databaseConfigPath != "") ? new KisimSource(new File(databaseConfigPath)) : null;

            if (databaseConfigPath != null && databaseConfigPath != "") {
                concerns.add(new WriteAnnotationsToDB(ks, d -> new KisimFormat().documentToJson(d)));
            }

            if (outputCorpusDir != null) {
                concerns.add(new DefaultWorkflowConcern() {
                    @Override
                    public Document postProcessDoc(Document doc) {
                        FeatureMap features = doc.getFeatures();
                        if (features.containsKey(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY)) {
                            features.remove(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY);
                        }
                        return doc;
                    }
                });

                WorkflowConcern writeToCorpus = new WriteToSerializedCorpus(outputCorpusDir, threads, Optional.empty());
                concerns.add(writeToCorpus);
            }

            PipelineWorkflow<?> workflow = null;

            if (corpusInputDir != null) {
                workflow = readFromFiles(concerns, controller);
            } else if (databaseConfigPath != null) {
                Stream<Map<String, Object>> records = docsLimiting(ImportCmd.documentRecordsStream(ks, Optional.ofNullable(docTypeFilterPath),
                        Optional.ofNullable(docIdFilterPath)));

                workflow = new PipelineWorkflow<>(
                        records,
                        p -> ImportCmd.kisimDocConversion(p, ks),
                        controller,
                        threads,
                        concerns);
            }

            workflow.run();
        } catch (Exception e) {
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
