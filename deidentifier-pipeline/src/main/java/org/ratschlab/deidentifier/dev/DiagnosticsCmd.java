package org.ratschlab.deidentifier.dev;

import org.ratschlab.deidentifier.sources.ConversionCheckCmd;
import picocli.CommandLine;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "Diagnostics entry point", name = "diagnostics",
        subcommands = {
                ConversionCheckCmd.class
        })
public class DiagnosticsCmd implements Runnable  {
    @Override
    public void run() {
        new CommandLine(new DiagnosticsCmd()).usage(System.out);
    }

    public static void main(String[] args) {
        CommandLine.run(new DiagnosticsCmd(), args);
    }
}
