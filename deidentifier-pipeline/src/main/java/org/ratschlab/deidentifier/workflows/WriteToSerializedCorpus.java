package org.ratschlab.deidentifier.workflows;

import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.corpora.SerialCorpusImpl;
import gate.persist.PersistenceException;
import gate.util.GateException;
import org.apache.commons.io.FileUtils;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WriteToSerializedCorpus extends DefaultWorkflowConcern {

    public WriteToSerializedCorpus(File corpusOutputDir, int threads, Optional<Function<String, String>> groupingFct) throws IOException {
        this.corpusOutputDir = corpusOutputDir;
        this.threads = threads;
        this.groupingFct = groupingFct;

        // TODO: really keep that way?
        if (corpusOutputDir.exists()) {
            FileUtils.deleteDirectory(corpusOutputDir);
        }

        workerTmpDir = new File(corpusOutputDir.getParentFile() + File.separator +  corpusOutputDir.getName() + "_tmp");

        // every parallel instance of the GATE pipeline has its own output corpus (for performance reasons)
        corpusDirs = IntStream.range(0, threads).
                mapToObj(i -> new File(workerTmpDir,"worker-" + i)).
                collect(Collectors.toList());

        corpusDirs.forEach(f -> f.mkdirs());

        workerCorpora = corpusDirs.stream().map(f -> {
            try {
                // TODO: better way with try/catch?
                Corpus c =  GateTools.getOutputCorpus(f);
                // remove listener, as otherwise we run into concurrency issues, where at the call of Factory.deleteResource
                // documents are attempted to be removed from corpora they are not in.
                // In the code below in postProcessDoc, we make sure, only one thread is accessing the corpus the document is actually in.
                gate.Gate.getCreoleRegister().removeCreoleListener((SerialCorpusImpl) c);
                return c;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GateException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

    }


    private File corpusOutputDir;
    private File workerTmpDir;
    private int threads;
    private List<File> corpusDirs;
    private List<Corpus> workerCorpora;
    private Optional<Function<String, String>> groupingFct;

    @Override
    public Document postProcessDoc(Document doc) {
        int workerId = (Integer) doc.getFeatures().get(PipelineWorkflow.WORKFLOW_INDEX);

        Corpus corpus = workerCorpora.get(workerId);

        doc.getFeatures().remove(PipelineWorkflow.WORKFLOW_INDEX);

        if(doc.getFeatures().containsKey("content")) {
            // remove unnecessary original content field.
            doc.getFeatures().remove("content");
        }

        // make copy of document and write it into the corpus
        // this is done to avoid concurrency issues when later calling Factory.deleteResource(doc) and doc is still associated with some corpus
        Document toCorpus = GateTools.copyDocument(doc);

        synchronized (corpus.getDataStore()) {
            corpus.add(toCorpus);
            corpus.unloadDocument(toCorpus); // also writes document to disk
            Factory.deleteResource(toCorpus);
        }

        return doc;
    }

    @Override
    public void doneHook() {
        workerCorpora.forEach(c -> {
            try {
                c.sync();
                c.getDataStore().close();
            } catch (PersistenceException e) {
                e.printStackTrace();
            }
        });

        GateTools.mergeStores(corpusDirs, corpusOutputDir, false, groupingFct);

        try {
            FileUtils.deleteDirectory(workerTmpDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
