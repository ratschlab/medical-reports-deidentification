package org.ratschlab.deidentifier.pipelines.testing;

import gate.Gate;
import gate.util.GateException;
import org.apache.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;

@CommandLine.Command(description = "Tests a pipeline", name = "test")
public class PipelineTesterCmd implements Runnable {
    private static final Logger log = Logger.getLogger(PipelineTesterCmd.class);

    @CommandLine.Parameters(index = "0", description = "Pipeline Configuration File")
    private File pipelineConfigFile;

    @CommandLine.Parameters(index = "1", description = "Testcases Directory")
    private File testCasesDirectory;

    public static void main(String[] args) {
        CommandLine.run(new PipelineTesterCmd(), args);
    }

    @Override
    public void run() {
        try {
            Gate.init();

            log.info(String.format("Testing with configuration %s and testcases in %s",
                    pipelineConfigFile.getAbsolutePath(),
                    testCasesDirectory.getAbsolutePath()));

            PipelineTester ts = new PipelineTester(pipelineConfigFile);

            if(testCasesDirectory.isDirectory()) {
                ts.runAllTestcases(testCasesDirectory);
            }
            else {
                ts.runTestcase(testCasesDirectory);
            }
        } catch (GateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
