package org.ratschlab.deidentifier;

import org.ratschlab.deidentifier.dev.DiagnosticsCmd;
import org.ratschlab.deidentifier.pipelines.testing.PipelineTesterCmd;
import org.ratschlab.deidentifier.sources.ImportCmd;
import org.ratschlab.deidentifier.substitution.SubstitutionCmd;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "Deid entry point", name = "Deid", version="deid dev",
        subcommands = {
                SubstitutionCmd.class,
                AnnotationCmd.class,
                ImportCmd.class,
                DiagnosticsCmd.class,
                PipelineTesterCmd.class
        }
)
public class DeidMain implements Callable<Integer> {
    @Override
    public Integer call() {
        new CommandLine(new DeidMain()).usage(System.out);
        return 2;
    }

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        int exitCode = CommandLine.call(new DeidMain(), args);
        System.exit(exitCode);
    }
}
