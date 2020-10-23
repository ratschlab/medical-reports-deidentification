package org.ratschlab.deidentifier.pipelines.testing;

import gate.Gate;
import gate.util.GateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

@CommandLine.Command(description = "Tests a pipeline", name = "test")
public class PipelineTesterCmd implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(PipelineTesterCmd.class);

    @CommandLine.Parameters(index = "0", description = "Pipeline Configuration File")
    private File pipelineConfigFile;

    @CommandLine.Parameters(index = "1", description = "Testcases Directory")
    private File testCasesDirectory;

    public static void main(String[] args) {
        System.exit(CommandLine.call(new PipelineTesterCmd(), args));
    }

    @Override
    public Integer call() {
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
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}
