package org.ratschlab.deidentifier.sources;

import com.google.common.collect.ImmutableList;
import gate.Corpus;
import gate.Document;
import gate.Gate;
import gate.persist.PersistenceException;
import gate.util.GateException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.ratschlab.deidentifier.utils.DbCommands;
import org.ratschlab.deidentifier.utils.StdOutErrLog;
import org.ratschlab.gate.GateTools;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(description = "Import reports from database", name = "import")
public class ImportCmd extends DbCommands implements Runnable {

    private static final Logger log = Logger.getLogger(ImportCmd.class);

    @CommandLine.Option(names = {"-o"}, description = "Output corpus dir", required = true)
    private File corpusOutputDir = null;

    public static void main(String[] args) {
        StdOutErrLog.tieSystemOutAndErrToLog();
        CommandLine.run(new ImportCmd(), args);
    }

    @Override
    public void run() {

        try {
            Gate.init();

            KisimSource ks = new KisimSource(new File(databaseConfigPath));
            Stream<Map<String, Object>> records = docsLimiting(documentRecordsStream(ks, Optional.ofNullable(docTypeFilterPath),
                    Optional.ofNullable(docIdFilterPath)));

            Stream<Document> docs = records.map(m -> kisimDocConversion(m, ks)).filter(o -> o.isPresent()).map(o -> o.get());

            if (corpusOutputDir.exists()) {
                FileUtils.deleteDirectory(corpusOutputDir);
            }
            corpusOutputDir.mkdirs();

            Corpus corpus = GateTools.getOutputCorpus(corpusOutputDir);
            corpus.setName("All_reports");

            docs.forEach(d -> {
                corpus.add(d);
                corpus.unloadDocument(d);
            });

            try {
                corpus.sync();
            } catch (PersistenceException e) {
                e.printStackTrace();
            }

            corpus.getDataStore().close();

            GateTools.createGroupedCorpora(corpusOutputDir, s -> fcodeGrouping(s));

        } catch (IOException | GateException | SQLException e) {
            e.printStackTrace();
        }

    }

    public static Stream<Map<String, Object>> documentRecordsStream(KisimSource ks, Optional<String> docTypeFilterPath, Optional<String> docIdFilterPath) {
        final Predicate<String> docTypeFilter = docTypeFilterPath.map(p -> getDocFilter(new File(p))).orElse(s -> true);
        final Predicate<String> docIdFilter = docIdFilterPath.map(p -> getIdDocFilter(new File(p))).orElse(s -> true);

        return ks.readRecords().
                filter(p -> docTypeFilter.test(p.get(ks.getReportTypeIdName()).toString())).
                filter(p -> docIdFilter.test(p.get(ks.getReportIdFieldName()).toString()));
    }

    private static Predicate<String> getDocFilter(File path) {
        try {
            List<Pattern> patterns = Files.readAllLines(path.toPath()).stream().map(s -> Pattern.compile(s.split(",")[0])).collect(Collectors.toList());
            return s -> patterns.stream().anyMatch(p -> p.matcher(s).matches());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Predicate<String> getIdDocFilter(File path) {
        try {
            Set<String> ids = Files.readAllLines(path.toPath()).stream().map(s -> s.split(",")[0].trim()).collect(Collectors.toSet());
            return s -> ids.contains(s);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Optional<Document> kisimDocConversion(Map<String, Object> p, KisimSource ks) {
        Document doc = new KisimFormat().jsonToDocument(p.get(ks.getContentFieldName()).toString());
        String reportType = p.get(ks.getReportTypeIdName()).toString();
        String reportId = p.get(ks.getReportIdFieldName()).toString();
        String reportIdLargeRadix = "";

        try {
            reportIdLargeRadix = Integer.toString(Integer.parseInt(reportId), Character.MAX_RADIX).toUpperCase();
        } catch (NumberFormatException ex) {
        }

        if (reportIdLargeRadix.isEmpty()) {
            doc.setName(String.join("-", ImmutableList.of(reportType, reportId)));
        } else {
            doc.setName(String.join("-", ImmutableList.of(reportType, reportId, reportIdLargeRadix)));
        }

        // keeping columns
        p.entrySet().stream().
                filter(e -> !e.getKey().equals(ks.getContentFieldName())).
                forEach(e -> doc.getFeatures().put(e.getKey(), e.getValue()));

        return Optional.of(doc);
    }

    public static String fcodeGrouping(String fileName) {
            String[] arr = fileName.split("-");
            if(arr.length > 0) {
                return arr[0];
            }
            return fileName;
    }
}
