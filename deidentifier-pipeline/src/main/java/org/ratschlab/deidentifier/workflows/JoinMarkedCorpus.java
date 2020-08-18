package org.ratschlab.deidentifier.workflows;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;
import org.ratschlab.deidentifier.utils.paths.PathConstraint;
import org.ratschlab.gate.FilterDocuments;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JoinMarkedCorpus extends DefaultWorkflowConcern {
    public JoinMarkedCorpus(File corpusPath, String markedAnnotationName, List<PathConstraint> evaluationFieldsBlacklist, int threads) {
        this.markedAnnotationName = markedAnnotationName;
        this.evaluationBlacklist = evaluationFieldsBlacklist;

        markedCorpusDir = IntStream.range(0, threads).
                mapToObj(x ->
                {
                    try {
                        return (SerialDataStore) Factory.openDataStore("gate.persist.SerialDataStore",
                                corpusPath.toURI().toURL().toString());
                    } catch (PersistenceException e) {
                        e.printStackTrace();
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
    }

    private List<DataStore> markedCorpusDir;
    private String markedAnnotationName;
    private List<PathConstraint> evaluationBlacklist;

    @Override
    public Document preProcessDoc(Document doc) {
        return doc;
    }

    @Override
    public Document postProcessDoc(Document docOrig) {
        Document doc = evaluationBlacklist.isEmpty() ? docOrig : FilterDocuments.filterDocument(docOrig, evaluationBlacklist);

        int workerId = (Integer) doc.getFeatures().get(PipelineWorkflow.WORKFLOW_INDEX);

        DataStore markedCorpusDs = markedCorpusDir.get(workerId);

        try {
            List<String> ids = markedCorpusDs.getLrIds("gate.corpora.DocumentImpl");

            // TODO: find better way. GATE adds some "random" suffixes to the actual names.
            String name = doc.getName();
            int underscoreIndex = name.lastIndexOf('_');
            if(underscoreIndex < 0) {
                underscoreIndex = name.length();
            }

            String nameNoSuffix = name.substring(0, underscoreIndex);
            Optional<String> docId = ids.stream().filter(id -> id.startsWith(nameNoSuffix)).findFirst();

            docId.ifPresent(id -> {
                Document markedDoc = null;
                try {
                    markedDoc = (Document) Factory.createResource("gate.corpora.DocumentImpl",
                            Utils.featureMap(DataStore.DATASTORE_FEATURE_NAME, markedCorpusDs,
                                    DataStore.LR_ID_FEATURE_NAME, id));

                    if(!evaluationBlacklist.isEmpty()) {
                        markedDoc = FilterDocuments.filterDocument(markedDoc, evaluationBlacklist);
                    }
                } catch (ResourceInstantiationException e) {
                    e.printStackTrace();
                }

                AnnotationSet markedAnnotations = markedDoc.getAnnotations(markedAnnotationName);

                // early annotated corprora contained empty annotations due to a bug in the annotation pipeline.
                org.ratschlab.deidentifier.annotation.Utils.removeEmptyAnnotations(markedAnnotations);

                doc.getAnnotations(String.format("%s-%s", markedAnnotationName, "manual")).addAll(markedAnnotations);
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        return doc;
    }

    @Override
    public void doneHook() {
        markedCorpusDir.forEach(ds -> {
            try {
                ds.close();
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        });
    }
}
