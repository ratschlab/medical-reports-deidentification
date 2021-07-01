package org.ratschlab.deidentifier.sources;

import gate.*;
import gate.corpora.DocumentJsonUtils;
import gate.util.GateException;
import org.apache.commons.lang.NotImplementedException;
import org.ratschlab.gate.GateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CommandLine.Command(description = "Convert reports into different formats", name = "convert")
public class ConversionCmd implements Callable<Integer> {
    private static final Logger log = LoggerFactory.getLogger(ConversionCmd.class);

    @CommandLine.Option(names = {"-i"}, description = "Input dir", required = true)
    private File inputDir = null;

    @CommandLine.Option(names = {"-o"}, description = "Output dir", required = true)
    private File outputDir = null;

    @CommandLine.Option(names = {"--direction"}, description = "Available Conversions: ${COMPLETION-CANDIDATES}", required = true)
    private Conversions conversion = null;

    public enum Conversions {
        KisimJson2Gate, Gate2GateJson
    }

    protected static void convertKisimJsonsToGateCorpus(File inputDir, File output) throws GateException, IOException {
        Corpus corpus = GateTools.getOutputCorpus(output);

        KisimFormat kf = new KisimFormat();

        Stream<File> input = inputDir.isFile() ? Stream.of(inputDir) : Arrays.stream(inputDir.listFiles());

        input.map(org.ratschlab.util.Utils.exceptionWrapper(f -> {
            log.info(String.format("Converting %s", f.getAbsolutePath()));

            Document doc = kf.jsonToDocument(f);
            doc.setName(f.getName().replaceAll(".json$", ""));
            return doc;
        })).forEach(doc -> {
                corpus.add(doc);
                corpus.unloadDocument(doc);
                Factory.deleteResource(doc);
        });

        corpus.sync();
        corpus.getDataStore().close();
    }

    private static String getAnnotationSetPrefix(String annotationName) {
        if (annotationName.isEmpty()) {
            return "default-annotations_";
        } else {
            return String.format("%s_", annotationName);
        }
    }

    protected static void convertGateCorpusToGateJson(File inputDir, File outputDir) throws GateException, IOException {
        Corpus documents = GateTools.openCorpus(inputDir);

        int nrDocs = 0;
        for (Document doc : documents) {
            // TODO: get all anntations and prefix them accordingly.

            List<String> allAnnotations = new ArrayList<>(doc.getAnnotationSetNames());
            allAnnotations.add("");

            final Map<String, Collection<Annotation>> merged = allAnnotations.stream().flatMap(annotationName -> {
                String annotationLabelPrefix = getAnnotationSetPrefix(annotationName);

                new ArrayList(doc.getAnnotations(annotationName));


                return doc.getAnnotations(annotationName).stream().
                    collect(
                        Collectors.groupingBy(a -> annotationLabelPrefix + a.getType(),
                        Collectors.toCollection(ArrayList::new))).entrySet().stream();
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            DocumentJsonUtils.writeDocument(doc, merged, new File(outputDir, doc.getName() + ".json"));
            nrDocs++;
        }

        log.info(String.format("Converted %d documents.", nrDocs));
    }

    @Override
    public Integer call() {
        try {
            Gate.init();

            switch(conversion) {
                case KisimJson2Gate:
                    convertKisimJsonsToGateCorpus(inputDir, outputDir);
                    break;
                case Gate2GateJson:
                    convertGateCorpusToGateJson(inputDir, outputDir);
                    break;
                default:
                    throw new NotImplementedException(String.format("Conversion %s not implemented", conversion));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }

        return 0;
    }
}
