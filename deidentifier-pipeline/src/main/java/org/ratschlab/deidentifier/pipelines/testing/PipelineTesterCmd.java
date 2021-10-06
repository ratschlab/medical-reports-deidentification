package org.ratschlab.deidentifier.pipelines.testing;

import gate.Gate;
import gate.util.GateException;
import org.ratschlab.DeidCmd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

@CommandLine.Command(description = "Tests a pipeline", name = "test")
public class PipelineTesterCmd extends DeidCmd {
    private static final Logger log = LoggerFactory.getLogger(PipelineTesterCmd.class);

    @CommandLine.Parameters(index = "0", description = "Pipeline Configuration File")
    private File pipelineConfigFile;

    @CommandLine.Parameters(index = "1", description = "Testcases Directory")
    private File testCasesDirectory;

    @CommandLine.Option(names = {"--pipeline"}, description = "Pipeline: ${COMPLETION-CANDIDATES}", defaultValue = "Deidentification")
    private PipelineTester.PipelineType pipelineType = null;

    public static void main(String[] args) {
        System.exit(CommandLine.call(new PipelineTesterCmd(), args));
    }

    @Override
    public Integer call() {
        super.call();

        try {
            Gate.init();

            log.info(String.format("Testing with configuration %s and testcases in %s",
                    pipelineConfigFile.getAbsolutePath(),
                    testCasesDirectory.getAbsolutePath()));

            PipelineTester ts = new PipelineTester(pipelineConfigFile, pipelineType);

            if(testCasesDirectory.isDirectory()) {
                ts.runAllTestcases(testCasesDirectory);
            }
            else {
                ts.runTestcase(testCasesDirectory);
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
}
