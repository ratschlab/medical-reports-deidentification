package org.ratschlab.deidentifier.pipelines;

public class PipelineConfigKeys {
    public static final String TOKENIZER_RULES = "pipeline.tokenizerRules";
    public static final String GATE_VERSION = "pipeline.gate_version";
    public static final String RESOURCE_ROOTS ="pipeline.resource_roots";
    public static final String FINAL_ANNOTATION_SET_NAME = "pipeline.finalAnnotationSetName";
    public static final String ANNOTATION_TYPES = "pipeline.annotationTypes";
    public static final String SENTENCE_SPLITTER_GAZETTEER = "pipeline.sentenceSplitterGazetteer";
    public static final String SENTENCE_SPLITTER_RULES = "pipeline.sentenceSplitterRules";
    public static final String GAZETTEER = "pipeline.gazetteer";
    public static final String SUFFIX_GAZETTEER = "pipeline.suffixGazetteer";
    public static final String GENERIC_TRANSDUCER = "pipeline.genericTransducer";
    public static final String SPECIFIC_TRANSDUCER = "pipeline.specificTransducer";
    public static final String ANNOTATION_MAPPING ="pipeline.annotationMapping";
    public static final String ANNOTATION_BLACKLIST = "pipeline.annotationBlacklist";
    public static final String CONTEXT_TRIGGERS = "pipeline.contextTriggerConfig";
    public static final String STRUCTURED_FIELD_MAPPING ="pipeline.structuredFieldMapping";
    public static final String SCHEMA_DIR = "pipeline.schemaDir";
}
