package org.ratschlab.deidentifier.workflows;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.FeatureMap;
import org.apache.commons.io.FileUtils;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MlFeatureExtraction extends DefaultWorkflowConcern {

    private JsonGenerator jgen;

    private String annotationLabels;
    private Optional<String> manualAnnotationLabels;

    private int recordsPerPart;
    private File outputDir;

    public MlFeatureExtraction(File outputDir, String annotationLabels, Optional<String> manualAnnotationLabels, int recordsPerPart) {
        this.outputDir = outputDir;

        try {
            if (outputDir.exists()) {
                if(outputDir.isDirectory()) {
                    FileUtils.deleteDirectory(outputDir);
                } else {
                    outputDir.delete();
                }
            }
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
        } catch(IOException e) {
            e.printStackTrace();
        }

        this.annotationLabels = annotationLabels;
        this.manualAnnotationLabels = manualAnnotationLabels;
        this.recordsPerPart = recordsPerPart;
    }


    private int sentenceNr = 0;
    private int partNr = 0;
    private int tokenInPartNr = 0;

    private static String generateLabelString(List<Annotation> ans) {
        String labels = ans.stream().map(a -> a.getType()).collect(Collectors.joining(","));
        if(labels.isEmpty()) {
            labels = "O";
        }

        return labels;
    }

    private static String generateExtendedLabelString(List<Annotation> ans) {
        String labels = ans.stream().map(a -> a.getType() + "-" + a.getFeatures().getOrDefault("type", "other")).
                collect(Collectors.joining(","));

        if(labels.isEmpty()) {
            labels = "O";
        }

        return labels;
    }

    private MlFeatureEntry getFeatureEntry(Document doc, Annotation tok, AnnotationSet labels,
                                           Optional<AnnotationSet> manualLabels, AnnotationSet lookups, int tokenNr, boolean isFirstToken) {
        String txt = gate.Utils.stringFor(doc, tok);

        String docType = doc.getName().split("-")[0];
        String docId = doc.getName();

        FeatureMap features = tok.getFeatures();

        //labels
        List<Annotation> labelAns = gate.Utils.getOverlappingAnnotations(labels, tok).stream().sorted((a1, a2) ->
                (int) (a2.getEndNode().getOffset() - a2.getStartNode().getOffset()) - (int) (a1.getEndNode().getOffset() - a1.getStartNode().getOffset())).collect(Collectors.toList());

        Optional<List<Annotation>> manualLabelAn = manualLabels.map(lbs -> gate.Utils.getOverlappingAnnotations(lbs, tok).stream().sorted((a1, a2) ->
                (int) (a2.getEndNode().getOffset() - a2.getStartNode().getOffset()) - (int) (a1.getEndNode().getOffset() - a1.getStartNode().getOffset())).
                collect(Collectors.toList())
        );

        String lookupsLst = gate.Utils.getOverlappingAnnotations(lookups, tok).stream().map(a1 ->
                a1.getFeatures().getOrDefault("majorType", "") + "-" + a1.getFeatures().getOrDefault("minorType", "")
        ).collect(Collectors.joining(";"));

        String label = generateLabelString(labelAns);

        String labelExtended = generateExtendedLabelString(labelAns);

        String manualLabel = manualLabelAn.map(x -> generateLabelString(x)).orElse("");
        String manualLabelExtended = manualLabelAn.map(x -> generateExtendedLabelString(x)).orElse("");

        String rule = labelAns.stream().map(a -> a.getFeatures().getOrDefault("rule", "").toString()).collect(Collectors.joining(";"));

        return new MlFeatureEntry(docId, sentenceNr, tokenNr, txt,
                    features.getOrDefault("category", "").toString(),
                    features.getOrDefault("kind", "").toString(),
                    features.getOrDefault("fieldName", "").toString(),
                    features.getOrDefault("fieldPath", "").toString(),
                    docType,
                    label,
                    labelExtended, lookupsLst, isFirstToken, rule, manualLabel, manualLabelExtended
        );
    }

    @Override
    public Document postProcessMergedDoc(Document doc) {
        AnnotationSet tokens = doc.getAnnotations().get("Token");
        AnnotationSet adjSentences = doc.getAnnotations().get("SentenceAdj");
        AnnotationSet labels = doc.getAnnotations(annotationLabels);
        Optional<AnnotationSet> manualLabels = manualAnnotationLabels.map(s -> doc.getAnnotations(s));
        AnnotationSet lookups = doc.getAnnotations().get("Lookup");

        ObjectMapper objectMapper = new ObjectMapper();

        for(Annotation sentence : adjSentences) {
            AnnotationSet tokensInSentence = gate.Utils.getContainedAnnotations(tokens, sentence);

            boolean isFirstToken = true;
            int tokenNr = 0;
            for(Annotation tok : tokensInSentence.inDocumentOrder()) {
                MlFeatureEntry entry = this.getFeatureEntry(doc, tok, labels, manualLabels, lookups, tokenNr, isFirstToken);

                try {
                    if(tokenInPartNr % recordsPerPart == 0) {
                        if(jgen != null) {
                           closeFile();
                        }

                        partNr++;
                        tokenInPartNr = 0;
                        File output = new File(this.outputDir, String.format("part-%d.json", partNr));
                        jgen = new JsonFactory().createGenerator(output, JsonEncoding.UTF8);
                        jgen.writeStartArray(); // [
                    }

                    objectMapper.writeValue(jgen, entry);
                } catch(IOException e) {
                    e.printStackTrace();
                }

                tokenInPartNr++;
                isFirstToken = false;
                tokenNr++;
            }

            sentenceNr++;
        }


        return super.postProcessMergedDoc(doc);
    }


    private void closeFile() {
        try {
            jgen.writeEndArray();
            jgen.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doneHook() {
        closeFile();
    }

    public class MlFeatureEntry {
        private String docId;
        private String tokenText;
        private String posTag;
        private String tokenKind;
        private String fieldName;
        private String fieldPath;
        private String docType;
        private String label;
        private String labelExtended;
        private String lookupTags;
        private int sentenceNr;
        private int tokenNr;
        private boolean isSentenceBeginning;
        private String triggeredRule;
        private String manualLabel;
        private String manualLabelExtended;

        public MlFeatureEntry(String docId, int sentenceNr, int tokenNr, String tokenText, String posTag, String tokenKind, String fieldName,
                              String fieldPath, String docType, String label, String labelExtended,
                              String lookupTags, boolean isSentenceBeginning, String triggeredRule, String manualLabel, String manualLabelExtended) {
            this.docId = docId;
            this.sentenceNr = sentenceNr;
            this.tokenNr = tokenNr;
            this.tokenText = tokenText;
            this.posTag = posTag;
            this.tokenKind = tokenKind;
            this.fieldName = fieldName;
            this.fieldPath = fieldPath;
            this.label = label;
            this.labelExtended = labelExtended;
            this.docType = docType;
            this.lookupTags = lookupTags;
            this.isSentenceBeginning = isSentenceBeginning;
            this.triggeredRule = triggeredRule;
            this.manualLabel = manualLabel;
            this.manualLabelExtended = manualLabelExtended;
        }

        public String getTokenText() {
            return tokenText;
        }

        public String getPosTag() {
            return posTag;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldPath() {
            return fieldPath;
        }

        public String getDocType() {
            return docType;
        }

        public String getLabel() {
            return label;
        }

        public String getLabelExtended() {
            return labelExtended;
        }

        public int getSentenceNr() {
            return sentenceNr;
        }

        public int getTokenNr() {
            return tokenNr;
        }

        public String getTokenKind() {
            return tokenKind;
        }

        public String getLookupTags() {
            return lookupTags;
        }

        public boolean isSentenceBeginning() {
            return isSentenceBeginning;
        }

        public String getDocId() {
            return docId;
        }

        public String getTriggeredRule() {
            return triggeredRule;
        }

        public String getManualLabel() {
            return manualLabel;
        }

        public String getManualLabelExtended() {
            return manualLabelExtended;
        }
    }
}
