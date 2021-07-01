package org.ratschlab.deidentifier;


import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.SerialAnalyserController;
import gate.util.Benchmark;
import gate.util.GateException;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.sources.ImportCmd;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.deidentifier.workflows.*;
import org.ratschlab.gate.GateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(description = "Annotate Corpus", name = "annotate")
public class AnnotationCmd extends DbCommands implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(AnnotationCmd.class);

    @CommandLine.Option(names = {"-i"}, description = "Input corpus dir")
    private String corpusInputDirPath = null;

    @CommandLine.Option(names = {"--xml-input"}, description = "Assumes input dir consists of xml files, one per report (testing purposes)")
    private boolean xmlInput = false;

    @CommandLine.Option(names = {"-o"}, description = "Output corpus dir", required = true)
    private File corpusOutputDir = null;

    @CommandLine.Option(names = {"-c"}, description = "Config file", required = true)
    private File propertiesFile = null;

    @CommandLine.Option(names = {"-m"}, description = "Marked Corpus Dir")
    private String markedCorpusDirPath = null;

    @CommandLine.Option(names = {"--diagnostics-dir"}, description = "Marked Corpus Dir")
    private String diagnosticsDirPath = null;

    @CommandLine.Option(names = {"--fields-blacklist-eval"}, description = "Path to files giving field blacklist used during evaluation")
    private File fieldsBlacklistPath = null;

    @CommandLine.Option(names = {"-t"}, description = "Number of threads")
    private int threads = -1;

    public static final String PHI_ANNOTATION_NAME = "phi-annotations";

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        System.exit(CommandLine.call(new AnnotationCmd(), args));
    }

    @Override
    public Integer call() {
        if(corpusInputDirPath == null && databaseConfigPath == null) {
            System.err.println("Need at least -i or -d");
            return 1;
        }

        try {
            Gate.init();

            Benchmark.setBenchmarkingEnabled(true);
            log.info(String.format("Loading config from %s", propertiesFile.getAbsolutePath()));
            Config conf = ConfigUtils.loadConfig(propertiesFile);
            SerialAnalyserController myController = PipelineFactory.getRuleBasedPipeline(conf);

            Optional<File> diagnosticsDir = Optional.ofNullable(diagnosticsDirPath).map(p -> new File(p));
            diagnosticsDir.ifPresent(f -> {
                log.info(String.format("Writing diagnostics to %s", f.getAbsolutePath()));
                f.mkdirs();
            });

            if(threads == -1) {
                threads = Runtime.getRuntime().availableProcessors();
            }
            log.info(String.format("Using %d threads", threads));

            List<WorkflowConcern> concerns = new ArrayList<>();

            Optional<File> markedCorpusDir = Optional.ofNullable(markedCorpusDirPath).map(p -> {
                File f = new File(p);
                log.info(String.format("Loading marked corpus dir from %s", f.getAbsolutePath()));
                return f;
            });

            List<PathConstraint> evaluationFieldsBlacklist = Optional.ofNullable(fieldsBlacklistPath).map(f -> {
                try {
                    return PathConstraint.loadFieldBlacklistPath(f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return Collections.<PathConstraint>emptyList();
            }).orElse(Collections.emptyList());

            markedCorpusDir.map(path -> new JoinMarkedCorpus(path, PHI_ANNOTATION_NAME, evaluationFieldsBlacklist, threads)).
                    ifPresent(w -> concerns.add(w));


            diagnosticsDir.ifPresent(d -> {
                    concerns.add(new MlFeatureExtraction(new File(d, "ml-features.json"), PipelineFactory.finalASName,
                            markedCorpusDir.isPresent() ? Optional.of("phi-annotations-manual") : Optional.of(""),
                            2000000));

            });

            concerns.add(new WriteToSerializedCorpus(corpusOutputDir, threads, Optional.of(s -> ImportCmd.fcodeGrouping(s))));

            if(markedCorpusDirPath != null) {
                File reportOutput = diagnosticsDir.map(f -> {
                    File fe = new File(f, "evaluation");
                    fe.mkdirs();
                    return fe;
                }).orElseThrow(() -> new IllegalArgumentException(("Need diagnostics dir set when using marked corpus.")));

                concerns.add(new EvaluateCorpus(String.format("%s-manual", PipelineFactory.finalASName), PipelineFactory.finalASName, PipelineFactory.annotationTypes, corpusOutputDir, reportOutput));
            }

            if(corpusInputDirPath != null && !xmlInput) {
                PipelineWorkflow<Optional<Document>> workflow = new PipelineWorkflow<>(
                        GateTools.readDocsInCorpus(new File(corpusInputDirPath)),
                        d -> d,
                        myController,
                        threads,
                        concerns);

                workflow.run();
            }
            else if(corpusInputDirPath != null) {
                List<File> inputFiles = Lists.newArrayList(new File(corpusInputDirPath).listFiles());

                if(maxDocs > 0 && inputFiles.size() > maxDocs) {
                    inputFiles = inputFiles.subList(0, maxDocs);
                }

                PipelineWorkflow<File> workflow = new PipelineWorkflow<>(
                        inputFiles.stream(),
                        org.ratschlab.util.Utils.exceptionWrapper(f -> Optional.of(GateTools.readDocumentFromFile(f))),
                        myController,
                        threads,
                        concerns);

                workflow.run();
            } else {
                KisimSource ks = new KisimSource(new File(databaseConfigPath));
                Stream<Map<String, Object>> records = docsLimiting(ImportCmd.documentRecordsStream(ks, Optional.ofNullable(docTypeFilterPath),
                        Optional.ofNullable(docIdFilterPath)));

                PipelineWorkflow<Map<String, Object>> workflow = new PipelineWorkflow<>(
                        records,
                        org.ratschlab.util.Utils.exceptionWrapper(p -> Optional.of(ImportCmd.kisimDocConversion(p, ks))),
                        myController,
                        threads,
                        concerns);

                workflow.run();
            }
        } catch (GateException e) {
            e.printStackTrace();
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

}
