package org.ratschlab.deidentifier.pipelines.testing;

import gate.*;
import gate.creole.ResourceInstantiationException;
import gate.util.InvalidOffsetException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineTestSuite {
    private static final Logger log = Logger.getLogger(PipelineTestSuite.class);

    public static final String LINE_NR_KEY = "linenr";
    public static final String DUMMY_TAG = "dummy";

    public static final String EXPECTED_AS_NAME = "myexpected";

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

        Corpus corpus = Factory.newCorpus(path.getFileName().toString());

        String header = lines.get(0);
        Pair<Set<String>, Set<String>> tagsPair = parseHeader(header);

        Set<String> tags = tagsPair.getLeft();
        Set<String> contextTags = tagsPair.getRight();

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
                AnnotationSet expectedTags = doc.getAnnotations(EXPECTED_AS_NAME);

                for(String t : tags) {
                    for(Annotation a : originalMarkups.get(t)) {
                        expectedTags.add(a.getId(), a.getStartNode().getOffset(), a.getEndNode().getOffset(), a.getType(), a.getFeatures());
                        originalMarkups.remove(a);
                    }
                }

                corpus.add(doc);
            } catch (ResourceInstantiationException e) {
                e.printStackTrace();
            } catch (InvalidOffsetException e) {
                e.printStackTrace();
            }
        }


        return new PipelineTestSuite(path.getFileName().toString(), corpus, tags, contextTags);
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