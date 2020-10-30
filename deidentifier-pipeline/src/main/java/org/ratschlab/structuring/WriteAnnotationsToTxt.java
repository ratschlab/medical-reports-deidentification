package org.ratschlab.structuring;

import gate.Document;
import gate.FeatureMap;
import org.ratschlab.deidentifier.workflows.WorkflowConcernWithQueue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WriteAnnotationsToTxt extends WorkflowConcernWithQueue<String> {
    private Function<Document, String> getDocId;

    private String annotationSetName;

    private String colSep = ";";

    private PrintStream outStream;

    public WriteAnnotationsToTxt(File outputFile, Function<Document, String> getDocId, String annotationSetName) throws IOException {
        this.getDocId = getDocId;
        this.annotationSetName = annotationSetName;

        this.outStream = new PrintStream(new FileOutputStream(outputFile));
    }

    @Override
    public Document postProcessMergedDoc(Document doc) {
        doc.getAnnotations(annotationSetName).get("Diagnosis").stream().
            map(a -> {
                FeatureMap m = a.getFeatures();

                String docId = getDocId.apply(doc);
                String annotText = gate.Utils.cleanStringFor(doc, a).replaceAll(colSep, "SEP");

                List<String> fields = new ArrayList<>(Arrays.asList(
                    docId,
                    annotText));

                List<String> fieldNames = Arrays.asList("code", "rank");

                fields.addAll(fieldNames.stream().map(k -> m.getOrDefault(k, "").toString()).collect(Collectors.toList()));

                return fields.stream().collect(Collectors.joining(colSep));
            }).forEach(s -> this.addToQueue(s));

        return doc;
    }

    @Override
    protected void queueFlushAction(List<String> batch, boolean firstFlush) {
        if(firstFlush) {
            String header = Arrays.asList("docId", "annotationText", "code", "rank").stream().collect(Collectors.joining(colSep));
            outStream.println(header);
        }

        batch.forEach(s -> outStream.println(s));
    }
}
