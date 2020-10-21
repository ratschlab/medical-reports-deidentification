package org.ratschlab.deidentifier;

import org.ratschlab.deidentifier.dev.DiagnosticsCmd;
import org.ratschlab.deidentifier.pipelines.testing.PipelineTesterCmd;
import org.ratschlab.deidentifier.sources.ImportCmd;
import org.ratschlab.deidentifier.substitution.SubstitutionCmd;
import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "Deid entry point", name = "Deid", version="deid dev",
        subcommands = {
                SubstitutionCmd.class,
                AnnotationCmd.class,
                ImportCmd.class,
                DiagnosticsCmd.class,
                PipelineTesterCmd.class
        }
)
public class DeidMain implements Runnable {
    @Override
    public void run() {
        // no command was given
        new CommandLine(new DeidMain()).usage(System.out);
    }

    public static void main(String[] args) {
        org.ratschlab.util.Utils.tieSystemOutAndErrToLog();
        CommandLine.run(new DeidMain(), args);
    }
}
