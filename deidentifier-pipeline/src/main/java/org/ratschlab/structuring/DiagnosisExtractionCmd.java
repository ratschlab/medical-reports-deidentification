package org.ratschlab.structuring;

import com.google.common.collect.Lists;
import com.typesafe.config.Config;
import gate.Gate;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.ratschlab.deidentifier.workflows.PipelineWorkflow;
import org.ratschlab.deidentifier.workflows.WorkflowConcern;
import org.ratschlab.deidentifier.workflows.WriteToSerializedCorpus;
import org.ratschlab.gate.GateTools;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

public class DiagnosisExtractionCmd extends DbCommands implements Callable<Integer> {

    @CommandLine.Option(names = {"-i"}, description = "Input corpus dir")
    private File corpusInputDir = null;

    @CommandLine.Option(names = {"--xml-input"}, description = "Assumes input dir consists of xml files, one per report (testing purposes)")
    private boolean xmlInput = false;

    @CommandLine.Option(names = {"-c"}, description = "Config file", required = true)
    private File pipelineConfigFile = null;

    @CommandLine.Option(names = {"--output-corpus-dir"}, description = "Output GATE Corpus Dir")
    private File outputCorpusDir = null;

    @CommandLine.Option(names = {"-o"}, description = "Output Txt File")
    private File outputFile = null;

    @CommandLine.Option(names = {"-t"}, description = "Number of threads")
    private int threads = 1;

    public Integer call() {
        try {
            Gate.init();

            Config conf = ConfigUtils.loadConfig(pipelineConfigFile);
            SerialAnalyserController controller = KeywordBasedDiagnosisExtraction.getExtractionPipeline(conf);

            List<WorkflowConcern> concerns = new ArrayList<>();

            if (outputFile != null) {
                concerns.add(new WriteAnnotationsToTxt(outputFile,
                    d -> d.getFeatures().getOrDefault("TODO", "").toString(),
                    conf.getString(StructuringConfigKeys.FINAL_ANNOTATION_SET_NAME)));
            }

            if (outputCorpusDir != null) {
                WorkflowConcern writeToCorpus = new WriteToSerializedCorpus(outputCorpusDir, threads, Optional.empty());
                concerns.add(writeToCorpus);
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
