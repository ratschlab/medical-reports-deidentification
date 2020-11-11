package org.ratschlab.structuring;

import gate.Document;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.workflows.WorkflowConcernWithQueue;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class WriteAnnotationsToDB extends WorkflowConcernWithQueue<Pair<String, Map<Object, Object>>> {

    private KisimSource ks;

    private Function<Document, String> conventConverter;

    public WriteAnnotationsToDB(KisimSource ks, Function<Document, String> conventConverter) {
        this.ks = ks;
        this.conventConverter = conventConverter;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        List<DiagnosisAnnotationRecord> records = (List<DiagnosisAnnotationRecord>) doc.getFeatures().get(AnnotationConsolidation.ANNOTATIONS_OUTPUT_FIELD_KEY);

        if(records != null) {
            String content = conventConverter.apply(doc);

            Map<Object, Object> features = doc.getFeatures();

            records.stream().map(r -> {
                Map<Object, Object> featuresRecord = new HashMap<>(features);

                featuresRecord.put(ks.getAnnotationTextFieldName().orElseThrow(() -> new IllegalArgumentException("Require annotationtext_field_name set in db config")),
                        r.getAnnotationText());
                featuresRecord.put(ks.getCodeFieldName().orElseThrow(() -> new IllegalArgumentException("Require code_field_name set in db config")), r.getCode());
                featuresRecord.put(ks.getReliabilityFieldName().orElseThrow(() -> new IllegalArgumentException("Require reliability_field_name set in db config")), r.getReliability());

                return Pair.of(content, featuresRecord);
            }).forEach(r -> this.addToQueue(r));
        }

        return doc;
    }


    @Override
    protected void queueFlushAction(List<Pair<String, Map<Object, Object>>> items, boolean firstFlush) {
        try {
            ks.writeData(items);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot write to database.", e);
        }
    }
}
