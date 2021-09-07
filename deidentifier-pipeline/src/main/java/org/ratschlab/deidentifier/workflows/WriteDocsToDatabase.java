package org.ratschlab.deidentifier.workflows;

import gate.Document;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.sources.KisimSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class WriteDocsToDatabase extends DefaultWorkflowConcern {
    private int batchSize = 100;
    private Function<Document, String> converter;

    private KisimSource ks;

    // content and
    private Queue<Pair<String, Map<Object, Object>>> writeQueue = new ConcurrentLinkedQueue<>();

    public WriteDocsToDatabase(KisimSource ks, Function<Document, String> converter) {
        this.ks = ks;
        this.converter = converter;
    }

    @Override
    public Document postProcessDoc(Document doc) {
        String docString = converter.apply(doc);
        writeQueue.add(Pair.of(docString, doc.getFeatures()));

        if (writeQueue.size() >= batchSize) {
            try {
                flushQueue();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return doc;
    }

    @Override
    public void doneHook() {
        try {
            flushQueue();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void flushQueue() throws SQLException {
        List<Pair<String, Map<Object, Object>>> lst = new ArrayList<>();

        while (!writeQueue.isEmpty()) {
            lst.add(writeQueue.poll());
        }

        ks.writeData(lst);
    }
}
