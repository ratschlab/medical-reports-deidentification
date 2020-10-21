package org.ratschlab.util;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;

import java.io.PrintStream;
import java.util.Optional;

public class Utils {
    public static Optional<Integer> maybeParseInt(String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static Optional<Double> maybeParseDouble(String str) {
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static void tieSystemOutAndErrToLog() {
        PrintStream logger = IoBuilder.forLogger("System.out").setLevel(Level.DEBUG).buildPrintStream();
        PrintStream errorLogger = IoBuilder.forLogger("System.err").setLevel(Level.ERROR).buildPrintStream();
        System.setOut(logger);
        System.setErr(errorLogger);
    }
}
