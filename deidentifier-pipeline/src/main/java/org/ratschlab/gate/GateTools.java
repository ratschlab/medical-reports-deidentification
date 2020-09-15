package org.ratschlab.gate;

import gate.*;
import gate.corpora.DocumentContentImpl;
import gate.corpora.DocumentImpl;
import gate.corpora.SerialCorpusImpl;
import gate.creole.AnnotationSchema;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;
import gate.util.GateException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GateTools {
    private static final Logger log = Logger.getLogger(GateTools.class);

    public static Optional<Document> readDocumentFromFile(File f)  {
        try {
            Document doc = Factory.newDocument(f.toURI().toURL(), "UTF-8");

            doc.setMarkupAware(true);
            doc.setPreserveOriginalContent(true);

            return Optional.of(doc);
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch(RuntimeException re) {
            re.printStackTrace();
        }

        return Optional.empty();
    }

    public static Document documentFromXmlString(String s) throws GateException {
        FeatureMap params = Factory.newFeatureMap();

        params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, s);
        params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/xml");
        Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
        doc.setSourceUrl(null);

        doc.setMarkupAware(true);
        doc.setPreserveOriginalContent(true);

        return doc;
    }

    public static Document processDoc(Document doc, SerialAnalyserController controller) {
        try {
            Corpus dummy = Factory.newCorpus("Dummy corpus");
            dummy.add(doc);

            controller.setCorpus(dummy);
            controller.execute();

            Factory.deleteResource(dummy);
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch(RuntimeException re) {
            re.printStackTrace();
        }

        // TODO: should crash if it wasn't annotatee
        return doc;
    }

    public static Corpus getOutputCorpus(File dataStoreDir) throws IOException, GateException {
        if (dataStoreDir.exists()) {
            FileUtils.deleteDirectory(dataStoreDir);
        }

        SerialDataStore sds = (SerialDataStore) Factory.createDataStore("gate.persist.SerialDataStore",
                dataStoreDir.toURI().toURL().toString());

        return (Corpus) sds.adopt(Factory.newCorpus("Processed corpus"));
    }

    public static Corpus openCorpus(File dataStoreDir) throws MalformedURLException, ResourceInstantiationException, PersistenceException {
        DataStore ds = Factory.openDataStore("gate.persist.SerialDataStore",
                dataStoreDir.toURI().toURL().toString());

        String rsName = "gate.corpora.SerialCorpusImpl";

        if(ds.getLrIds(rsName).isEmpty()) {
            throw new IllegalArgumentException(dataStoreDir.getAbsolutePath() + " doesn't contain any corpus");
        }

        return (Corpus) Factory.createResource(rsName,
                Utils.featureMap(DataStore.DATASTORE_FEATURE_NAME, ds,
                        DataStore.LR_ID_FEATURE_NAME, ds.getLrIds(rsName).get(0)));

    }

    public static Stream<Optional<Document>> readDocsInCorpus(File dataStoreDir) throws MalformedURLException, ResourceInstantiationException, PersistenceException {
        DataStore ds = Factory.openDataStore("gate.persist.SerialDataStore",
                dataStoreDir.toURI().toURL().toString());

        String rsName = "gate.corpora.DocumentImpl";

        if(ds.getLrIds(rsName).isEmpty()) {
            throw new IllegalArgumentException(dataStoreDir.getAbsolutePath() + " doesn't contain any documents");
        }

        return ds.getLrIds(rsName).stream().map(s -> {
            try {
                return Optional.of((Document) Factory.createResource("gate.corpora.DocumentImpl",
                        Utils.featureMap(DataStore.DATASTORE_FEATURE_NAME, ds,
                                DataStore.LR_ID_FEATURE_NAME, s)));
            } catch (ResourceInstantiationException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        });
    }


    /**
     * Merges several SerialDataStores into one.
     *
     * Simply copies files. This may break in future GATE versions...
     *
     * @param stores directories of SerialDataStores
     * @param targetDir target directory
     * @param deleteAfterMerge whether or not to delete the original stores after merging
     */
    public static void mergeStores(Collection<File> stores, File targetDir, boolean deleteAfterMerge, Optional<Function<String, String>> groupingFct) {
        String docSubDirName = "gate.corpora.DocumentImpl";
        String versionFileName = "__GATE_SerialDataStore__";

        List<File> storesExisting = stores.stream().filter(f -> new File(f, docSubDirName).exists()).collect(Collectors.toList());

        if(storesExisting.isEmpty()) {
            throw new IllegalArgumentException("stores was empty. Expect at least one element");
        }

        int nrDocsOrig = storesExisting.stream().
                mapToInt(f -> new File(f, docSubDirName).list().length).
                sum();

        File templateStore = storesExisting.iterator().next();

        try {
            FileUtils.copyFile(new File(templateStore, versionFileName), new File(targetDir, versionFileName));

            File docDir = new File(targetDir, docSubDirName);
            docDir.mkdir();

            storesExisting.forEach(f -> {
                try {
                    FileUtils.copyDirectory(new File(f, docSubDirName), docDir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            int nrDocsCopied = docDir.list().length;
            if(nrDocsCopied != nrDocsOrig) {
                log.warn(String.format("Copied %d but should have %d docs.", nrDocsCopied, nrDocsOrig));
            } else {
                if(deleteAfterMerge) {
                    storesExisting.forEach(f -> FileUtils.deleteQuietly(f));
                }
            }

            createSerialCorpus(targetDir, "All_Reports", x -> true);

            groupingFct.ifPresent(grpFunc -> createGroupedCorpora(targetDir, grpFunc));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GateException e) {
            e.printStackTrace();
        }
    }

    public static void createSerialCorpus(File dataStoreDir, String corpusName, Predicate<File> cond) throws GateException, MalformedURLException {
        SerialDataStore sds = (SerialDataStore) Factory.openDataStore("gate.persist.SerialDataStore",
                dataStoreDir.toURI().toURL().toString());

        SerialCorpusImpl corpus = (SerialCorpusImpl) sds.adopt(Factory.newCorpus(corpusName));

        File docsDir = new File(dataStoreDir, "gate.corpora.DocumentImpl");

        File[] files = docsDir.listFiles();
        Arrays.sort(files);
        for(File f : files) {
            if(cond.test(f)) {
                Document fake = Factory.newDocument("");

                fake.setParameterValue(DataStore.LR_ID_FEATURE_NAME, f.getName());
                fake.setName(f.getName().split("__")[0]);
                corpus.add(fake);
                corpus.unloadDocument(fake, false);
            }
        }

        try {
            corpus.sync();
        } catch (PersistenceException e) {
            e.printStackTrace();
        }
    }

    public static void createGroupedCorpora(File targetDir, Function<String, String> grpFunc) {
        Set<String> groups =  Arrays.stream(new File(targetDir, "gate.corpora.DocumentImpl").listFiles()).
                map(f -> grpFunc.apply(f.getName())).
                collect(Collectors.toSet());

        groups.stream().sorted().forEach(g -> {
            try {
                createSerialCorpus(targetDir, g, f -> {
                    String docName = f.getName();
                    int index = f.getName().indexOf("___");
                    if(index >= 0) {
                        docName = f.getName().substring(0, index);
                    }
                    return grpFunc.apply(docName).equals(g);
                });
            } catch (GateException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        });
    }

    public static AnnotationSchema loadSchema(URL masterFileUrl) throws ResourceInstantiationException, IOException {
        FeatureMap features = Factory.newFeatureMap();
        features.put("xmlFileUrl", masterFileUrl);

        return (AnnotationSchema) Factory.createResource("gate.creole.AnnotationSchema", features);
    }

    public static ProcessingResource getAnnotationCopier(String in, String out, Optional<Set<String>> types) throws ResourceInstantiationException {
        FeatureMap annotTransferFeatures = Factory.newFeatureMap();
        annotTransferFeatures.put("inputASName", in);
        annotTransferFeatures.put("outputASName", out);
        annotTransferFeatures.put("copyAnnotations", true);

        types.ifPresent(t -> annotTransferFeatures.put("annotationTypes", t));

        return (ProcessingResource) Factory.createResource("gate.creole.annotransfer.AnnotationSetTransfer", annotTransferFeatures);
    }

    public static Document copyDocument(Document origDoc) {
        try {
            Document doc = Factory.newDocument("");

            doc.setFeatures(origDoc.getFeatures());
            doc.setContent(new DocumentContentImpl(origDoc.getContent().toString()));
            doc.setPreserveOriginalContent(origDoc.getPreserveOriginalContent());
            doc.setMarkupAware(origDoc.getMarkupAware());

            doc.setName(origDoc.getName());

            for (String an : origDoc.getAnnotationSetNames()) {
                AnnotationSet as = origDoc.getAnnotations(an);
                doc.getAnnotations(an).addAll(as);
            }

            doc.getAnnotations().addAll(origDoc.getAnnotations());

            return doc;
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }

        return null;
    }
}
