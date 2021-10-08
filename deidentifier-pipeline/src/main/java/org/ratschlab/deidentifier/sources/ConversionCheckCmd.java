package org.ratschlab.deidentifier.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Document;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@CommandLine.Command(mixinStandardHelpOptions = true, description = "Roundtrip test between JSON <--> GATE format", name = "conversioncheck")
public class ConversionCheckCmd extends DbCommands {

    private static final Logger log = LoggerFactory.getLogger(ConversionCheckCmd.class);

    @CommandLine.Option(names = {"-o"}, description = "Writing out error cases", required = true)
    private File outfile = null;

    @Override
    public Integer call() {
        super.call();

        try {
            Gate.init();

            KisimSource ks = new KisimSource(new File(databaseConfigPath));

            Stream<Pair<String, String>> records = docsLimiting(ks.readJsonStringsWithReportId());

            PrintStream out = new PrintStream(new FileOutputStream(outfile));

            AtomicInteger cnt = new AtomicInteger(0);
            long errorCnt = records.filter(p -> {
                try {
                    cnt.addAndGet(1);
                    if(cnt.get() % 100 == 0) {
                        log.info(String.format("Processed %d documents", cnt.get()));
                    }

                    return !checkConversion(p.getLeft(), p.getRight(), out);
                } catch (IOException|ResourceInstantiationException e) {
                    throw new RuntimeException(e);
                }
            }).count();

            log.info(String.format("Checked %d documents, found %d errors (see details in %s)", cnt.get(), errorCnt, outfile.getAbsolutePath()));
        } catch(GateException | IOException | SQLException ex) {
            log.error("Exception thrown", ex);
            return 1;
        }

        return 0;
    }

    public static boolean checkConversion(String docId, String kisimJson, PrintStream out) throws IOException, ResourceInstantiationException {
        ObjectMapper om = new ObjectMapper();
        // parse and emit string again to not have to deal with formatting issues during assert
        String jsonStr = om.writeValueAsString(om.reader().readTree(kisimJson));

        KisimFormat kf = new KisimFormat();

        Document doc = kf.jsonToDocument(jsonStr);

        String jsonStrBack = kf.documentToJson(doc);

        if(!jsonStr.equals(jsonStrBack)) {
            out.println(String.format("Error in document %s", docId));
            out.println(jsonStr);
            out.println(jsonStrBack);

            int delta = 15;
            for(int i = 0; i < Math.min(jsonStr.length(), jsonStrBack.length()); i++) {
                if(jsonStr.charAt(i) != jsonStrBack.charAt(i)) {
                    out.println(String.format("difference at %d %s | %s", i, jsonStr.substring(i-delta, i+delta),  jsonStrBack.substring(i-delta, i+delta)));
                    break;
                }
            }

            return false;
        }

        return true;
    }

    public static void main(String[] args) {
        System.exit(CommandLine.call(new ConversionCheckCmd(), args));
    }
}
