package org.ratschlab.deidentifier.utils;

import org.apache.log4j.Logger;

import java.io.PrintStream;

// following https://stackoverflow.com/questions/1200175/log4j-redirect-stdout-to-dailyrollingfileappender
public class StdOutErrLog {
    private static final Logger logger = Logger.getLogger(StdOutErrLog.class);

    public static void tieSystemOutAndErrToLog() {
        System.setOut(createLoggingProxy(System.out));
        System.setErr(createLoggingProxy(System.err));
    }

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.warn(string);
            }
        };
    }
}