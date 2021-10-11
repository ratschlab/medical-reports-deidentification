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
import org.ratschlab.deidentifier.sources.KisimFormat;
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
public class AnnotationCmd extends DbCommands {
    private static final Logger log = LoggerFactory.getLogger(AnnotationCmd.class);

    @CommandLine.Option(names = {"-i"}, description = "Input corpus dir")
    private String corpusInputDirPath = null;

    @CommandLine.Option(names = {"--json-input"}, description = "Assumes input dir consists of json files, one per report")
    private boolean jsonInput = false;

    @CommandLine.Option(names = {"--xml-input"}, description = "Assumes input dir consists of xml files, one per report (testing purposes)")
    private boolean xmlInput = false;

    @CommandLine.Option(names = {"-o"}, description = "Output corpus dir", required = true)
    private File corpusOutputDir = null;

    @CommandLine.Option(names = {"-c"}, description = "Pipeline config file", required = true)
    private File propertiesFile = null;

    @CommandLine.Option(names = {"-m"}, description = "Path to marked/hand annotated corpus dir. " +
        "Used to measure annotation accuracy of the current pipeline compared to the marked corpus (e.g. gold standard)")
    private String markedCorpusDirPath = null;

    @CommandLine.Option(names = {"--diagnostics-dir"}, description = "Path to diagnostic output (only in conjunction with the -m option)")
    private String diagnosticsDirPath = null;

    @CommandLine.Option(names = {"--fields-blacklist-eval"}, description = "Path to files giving field blacklist used during evaluation")
    private File fieldsBlacklistPath = null;

    @CommandLine.Option(names = {"-t"}, description = "Number of parallel pipeline annotations (default: number of CPUs available)")
    private int threads = -1;

    public static final String PHI_ANNOTATION_NAME = "phi-annotations";

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        System.exit(CommandLine.call(new AnnotationCmd(), args));
    }

    private PipelineWorkflow<?> readFromFiles(List<WorkflowConcern> concerns, SerialAnalyserController controller) throws Exception {
        if(!xmlInput && !jsonInput) {
            return new PipelineWorkflow<>(
                GateTools.readDocsInCorpus(new File(corpusInputDirPath)),
                d -> d,
                controller,
                threads,
                concerns);
        }

        if(xmlInput) {
            List<File> inputFiles = Lists.newArrayList(new File(corpusInputDirPath).listFiles());

            return new PipelineWorkflow<>(
                docsLimiting(inputFiles.stream()),
                org.ratschlab.util.Utils.exceptionWrapper(f -> Optional.of(GateTools.readDocumentFromFile(f))),
                controller,
                threads,
                concerns);
        }

        List<File> files = Lists.newArrayList(new File(corpusInputDirPath).listFiles());

        KisimFormat ksf = new KisimFormat();
        return new PipelineWorkflow<>(
            docsLimiting(files.stream()),
            org.ratschlab.util.Utils.exceptionWrapper(f -> {
                Document doc = ksf.jsonToDocument(f);

                doc.setName(f.getName().replaceAll(".json", ""));
                doc.getFeatures().put("reportnr", f.getName().replaceAll(".json", ""));
                return Optional.of(doc);
            }),
            controller,
            threads,
            concerns);
    }

    @Override
    public Integer call() {
        super.call();

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

            PipelineWorkflow<?> workflow;
            if (corpusInputDirPath != null) {
                workflow = readFromFiles(concerns, myController);
            } else {
                KisimSource ks = new KisimSource(new File(databaseConfigPath));
                Stream<Map<String, Object>> records = docsLimiting(ImportCmd.documentRecordsStream(ks, Optional.ofNullable(docTypeFilterPath),
                        Optional.ofNullable(docIdFilterPath)));

                workflow = new PipelineWorkflow<>(
                        records,
                        org.ratschlab.util.Utils.exceptionWrapper(p -> Optional.of(ImportCmd.kisimDocConversion(p, ks))),
                        myController,
                        threads,
                        concerns);
            }

            workflow.run();

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
