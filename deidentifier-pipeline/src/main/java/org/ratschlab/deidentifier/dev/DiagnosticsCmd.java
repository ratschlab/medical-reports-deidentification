package org.ratschlab.deidentifier.dev;

import org.ratschlab.deidentifier.sources.ConversionCheckCmd;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "Diagnostics entry point", name = "diagnostics",
        subcommands = {
                ConversionCheckCmd.class
        })
public class DiagnosticsCmd implements Callable<Integer> {
    @Override
    public Integer call() {
        new CommandLine(new DiagnosticsCmd()).usage(System.out);
        return 2;
    }

    public static void main(String[] args) {
        System.exit(CommandLine.call(new DiagnosticsCmd(), args));
    }
}
