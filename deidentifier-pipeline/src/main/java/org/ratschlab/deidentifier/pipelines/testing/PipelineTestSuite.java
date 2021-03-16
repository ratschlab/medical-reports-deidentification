package org.ratschlab.deidentifier.pipelines.testing;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class PipelineTestSuite {
    private static final Logger log = LoggerFactory.getLogger(PipelineTestSuite.class);

    public static final String LINE_NR_KEY = "linenr";
    public static final String DUMMY_TAG = "dummy";

    public static final String EXPECTED_ANNOTATION_SET_NAME = "myexpected";

    private Corpus corpus;
    private Set<String> tags;
    private Set<String> contextTags;
    private String name;

    public PipelineTestSuite(String name, Corpus corpus, Set<String> tags, Set<String> contextTags) {
        this.name = name;
        this.corpus = corpus;
        this.tags = tags;
        this.contextTags = contextTags;
    }

    public String getName() {
        return name;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Set<String> getContextTags() {
        return contextTags;
    }

    public static PipelineTestSuite createTestSuite(Path path) throws IOException, ResourceInstantiationException {
        log.info(String.format("Reading testcases from %s", path.toAbsolutePath().toString()));

        List<String> lines = Files.readAllLines(path);

        String header = lines.get(0);
        Pair<Set<String>, Set<String>> tagsPair = parseHeader(header);

        Set<String> tags = tagsPair.getLeft();
        Set<String> contextTags = tagsPair.getRight();

        List<Document> docs = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);

            line = removeComment(line);

            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            line = line.replace("\\n", "\n");

            try {
                FeatureMap params = Factory.newFeatureMap();

                String xmlDoc = String.format("<%s>%s</%s>", DUMMY_TAG, line, DUMMY_TAG);
                params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, xmlDoc);
                params.put(Document.DOCUMENT_MIME_TYPE_PARAMETER_NAME, "text/xml");
                Document doc = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
                doc.setSourceUrl(null);

                doc.setMarkupAware(true);
                doc.setPreserveOriginalContent(true);

                FeatureMap fm = Factory.newFeatureMap();
                fm.put(LINE_NR_KEY, String.valueOf(i + 1));
                doc.setFeatures(fm);

                AnnotationSet originalMarkups = doc.getAnnotations(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
                AnnotationSet expectedTags = doc.getAnnotations(EXPECTED_ANNOTATION_SET_NAME);

                for(String t : tags) {
                    for(Annotation a : originalMarkups.get(t)) {
                        expectedTags.add(a.getId(), a.getStartNode().getOffset(), a.getEndNode().getOffset(), a.getType(), a.getFeatures());
                        originalMarkups.remove(a);
                    }
                }

                docs.add(doc);
            } catch (ResourceInstantiationException e) {
                e.printStackTrace();
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }

        return createTestSuite(docs, tags, contextTags, path.getFileName().toString(), path.getFileName().toString());
    }

    public static PipelineTestSuite createTestSuite(List<Document> docs, Set<String> tags, Set<String> contextTags) throws ResourceInstantiationException {
        return createTestSuite(docs, tags, contextTags, "mysuite", "mytestcorpus");
    }

    public static PipelineTestSuite createTestSuite(List<Document> docs, Set<String> tags, Set<String> contextTags, String suiteName, String corpusName) throws ResourceInstantiationException {
        Corpus corpus = Factory.newCorpus(corpusName);

        for(int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            FeatureMap fm = d.getFeatures();
            fm.putIfAbsent(PipelineTestSuite.LINE_NR_KEY, i);
            corpus.add(d);
        }

        return new PipelineTestSuite(suiteName, corpus, tags, contextTags);
    }


    private static String removeComment(String line) {
        int pos = line.indexOf('#');

        while(pos >= 0) {
            if (pos == 0 || line.charAt(pos - 1) != '\\') {
                return line.substring(0, pos);
            } else {
                pos = line.indexOf('#', pos + 1);
            }
        }

        return line;
    }

    private static Pair<Set<String>, Set<String>> parseHeader(String headerLine) {
        String[] line = headerLine.split(";");

        Set<String> tags = Arrays.stream(line[0].split(",")).map(s -> s.trim()).collect(Collectors.toSet());

        Set<String> contextTags = Collections.emptySet();
        if(line.length > 1) {
           contextTags = Arrays.stream(line[1].split(",")).map(s -> s.trim()).collect(Collectors.toSet());
        }

        return Pair.of(tags, contextTags);
    }
}
