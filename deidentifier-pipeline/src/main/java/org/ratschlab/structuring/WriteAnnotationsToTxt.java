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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WriteAnnotationsToTxt extends WorkflowConcernWithQueue<String> {
    private Function<Document, String> getDocId;

    private String colSep = ";";

    private PrintStream outStream;

    public WriteAnnotationsToTxt(File outputFile, Function<Document, String> getDocId) throws IOException {
        this.getDocId = getDocId;

        this.outStream = new PrintStream(new FileOutputStream(outputFile));
    }

    @Override
    public Document postProcessDoc(Document doc) {
        List<DiagnosisAnnotationRecord> records = (List<DiagnosisAnnotationRecord>) doc.getFeatures().get(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY);

        if(records != null) {
            String docId = getDocId.apply(doc);

            records.stream().map(r -> {
                List<String> fields = new ArrayList<>(Arrays.asList(docId,
                        r.getAnnotationText().replaceAll(colSep, "SEP"),
                        r.getCode(),
                        r.getReliability()
                ));

                return fields.stream().collect(Collectors.joining(colSep));
            }).forEach(s -> this.addToQueue(s));
        }
        return doc;
    }

    @Override
    protected void queueFlushAction(List<String> batch, boolean firstFlush) {
        if(firstFlush) {
            String header = Arrays.asList("docId", "annotationText", "code", "reliability").stream().collect(Collectors.joining(colSep));
            outStream.println(header);
        }

        batch.forEach(s -> outStream.println(s));
    }
}
