package org.ratschlab.util;

import gate.util.Files;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.io.IoBuilder;

import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.Function;

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

    public static File createFileFromUrlOrPath(String urlOrPath) {
        String url = urlOrPath;
        if(!url.startsWith("file")) {
            url = "file:///" + urlOrPath;
        }

        try {
            return Files.fileFromURL(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return new File(urlOrPath); //fallback
    }

    // taken from https://www.oreilly.com/content/handling-checked-exceptions-in-java-streams/
    public static <T, R, E extends Exception>
    Function<T, R> exceptionWrapper(FunctionWithException<T, R, E> fe) {
        return arg -> {
            try {
                return fe.apply(arg);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
