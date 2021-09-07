package org.ratschlab.deidentifier.pipelines;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import gate.*;
import gate.creole.Plugin;
import gate.creole.SerialAnalyserController;
import gate.qa.Measure;
import gate.util.GateException;
import org.ratschlab.deidentifier.ConfigUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PipelineFactory {

    public final static String finalASName = "phi-annotations";
    public final static Set<String> annotationTypes = ImmutableSet.of("Age", "Address", "Date", "Contact", "ID", "Location", "Name", "Occupation");

    public static SerialAnalyserController getRuleBasedPipeline(Config conf) throws GateException {

        Optional<String> posTaggerModel = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.POS_TAGGER_MODEL);
        Optional<String> annotationMappingUrl = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.ANNOTATION_MAPPING);
        Optional<String> structuredFieldsUrl = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.STRUCTURED_FIELD_MAPPING);
        Optional<String> specificTransducer = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.SPECIFIC_TRANSDUCER);
        Optional<String> gazetteerUrl = ConfigUtils.getOptionalString(conf, (PipelineConfigKeys.GAZETTEER));
        Optional<String> suffixGazeteerUrl = ConfigUtils.getOptionalString(conf, (PipelineConfigKeys.SUFFIX_GAZETTEER));
        Optional<String> schemaUrl = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.SCHEMA_DIR);
        Optional<String> annotationBlacklist = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.ANNOTATION_BLACKLIST);
        Optional<String> contextTriggersUrl = ConfigUtils.getOptionalString(conf, PipelineConfigKeys.CONTEXT_TRIGGERS);

        return getRuleBasedPipeline(
                conf.getString(PipelineConfigKeys.TOKENIZER_RULES),
                conf.getString(PipelineConfigKeys.SENTENCE_SPLITTER_GAZETTEER),
                conf.getString(PipelineConfigKeys.SENTENCE_SPLITTER_RULES),
                posTaggerModel,
                gazetteerUrl,
                suffixGazeteerUrl,
                contextTriggersUrl,
                conf.getString(PipelineConfigKeys.GENERIC_TRANSDUCER),
                specificTransducer,
                annotationMappingUrl,
                structuredFieldsUrl,
                annotationBlacklist,
                schemaUrl,
                new HashSet(conf.getStringList(PipelineConfigKeys.ANNOTATION_TYPES)),
                conf.getString(PipelineConfigKeys.FINAL_ANNOTATION_SET_NAME)
        );
    }

    public static SerialAnalyserController getRuleBasedPipeline(String tokenizerRulesUrl,
                                                                String sentenceSplitterGazetteerUrl,
                                                                String sentenceSplitterTransducerUrl,
                                                                Optional<String> posTaggerModelUrl,
                                                                Optional<String> gazetteerUrl,
                                                                Optional<String> suffixGazetteerUrl,
                                                                Optional<String> contextTriggersUrl,
                                                                String genericTransducerUrl,
                                                                Optional<String> sourceSpecificTransducerUrl,
                                                                Optional<String> annotationMappingUrl,
                                                                Optional<String> structuredFieldsUrl,
                                                                Optional<String> annotationBlacklistUrl,
                                                                Optional<String> schemaUrl,
                                                                Set<String> annotationTypes,
                                                                String finalAnnotationName
    ) throws GateException {
        String tmpAsName = "tmpAnnotations";
        String phiAnnotationsCleaned = "phi-annotations-cleaned";

        SerialAnalyserController controller = PipelineUtils.emptyPipeline("Rule based pipeline");

        controller.add(PipelineUtils.getTokenizer(tokenizerRulesUrl));

        controller.add(PipelineUtils.getSentenceSplitter(sentenceSplitterTransducerUrl, sentenceSplitterGazetteerUrl));

        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AddStructuredFieldInfo",
                ImmutableMap.of("inputASName", "")));

        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AdjustSentenceBoundaries",
                ImmutableMap.of("inputASName", "",
                        "outputASName", ""
                )));

        posTaggerModelUrl.ifPresent(posTaggerModel -> {
            try {
                controller.add(PipelineUtils.getPosTagger(posTaggerModel));
            } catch (GateException e) {
                e.printStackTrace();
            }
        });

        gazetteerUrl.ifPresent(url -> controller.add(PipelineUtils.getGazetteer(url, true, true)));

        suffixGazetteerUrl.ifPresent(url -> controller.add(PipelineUtils.getGazetteer(url, true, false)));

        String normalizingTmp = "normalizingTmp";

        controller.add(PipelineUtils.getAnnotationCopier(GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME, normalizingTmp, Optional.empty()));

        annotationMappingUrl.ifPresent(configFile -> {
                controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AnnotationNormalizer", ImmutableMap.of(
                        "inputASName", normalizingTmp,
                        "outputASName", normalizingTmp,
                        "configPath", configFile
                )));
        });

        // adding annotations of structured fields
        structuredFieldsUrl.ifPresent(url -> {
            controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AnnotationNormalizer", ImmutableMap.of(
                    "inputASName", normalizingTmp,
                    "outputASName", normalizingTmp,
                    "configPath", url)

            ));
        });

        controller.add(PipelineUtils.getAnnotationCopier(normalizingTmp, tmpAsName, Optional.empty()));
        controller.add(PipelineUtils.getAnnotationCopier("", tmpAsName, Optional.empty()));

        contextTriggersUrl.ifPresent(url -> {
            controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.TriggerBasedContextAnnotator", ImmutableMap.of(
                    "inputASName", tmpAsName,
                    "outputASName", tmpAsName,
                    "configPath", url)

            ));
        });

        controller.add(PipelineUtils.getTransducer(genericTransducerUrl, tmpAsName, tmpAsName));

        sourceSpecificTransducerUrl.ifPresent(url ->
                controller.add(PipelineUtils.getTransducer(url, tmpAsName, tmpAsName)));


        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.MetadataAnnotator", ImmutableMap.of("inputASName", tmpAsName,
                "outputASName", tmpAsName)));

        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.HighConfidenceStringAnnotator", ImmutableMap.of("inputASName", tmpAsName,
                "outputASName", tmpAsName)));

        annotationBlacklistUrl.ifPresent(url -> {
            controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AnnotationBlacklist",
                    ImmutableMap.of("inputASName", tmpAsName, "configPath", url)));
        });

        //controller.add(PipelineUtils.getAnnotationCopier(tmpAsName, phiAnnotationsCleaned, Optional.empty()));
        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.AnnotationCleanup", ImmutableMap.of(
                "annotationTypes", annotationTypes,
                "inputASName", tmpAsName,
                "outputASName", phiAnnotationsCleaned)));

        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.RemoveOverlappingAnnotations", ImmutableMap.of(
                "inputASName", phiAnnotationsCleaned)));

        schemaUrl.ifPresent(url -> controller.add(PipelineUtils.getSchemaEnforcer(url, annotationTypes, phiAnnotationsCleaned, finalAnnotationName)));

        //controller.add(PipelineUtils.getAnnotationCopier(phiAnnotationsCleaned, finalAnnotationName, Optional.empty()));

        if(!schemaUrl.isPresent()) {
            controller.add(PipelineUtils.getAnnotationCopier(phiAnnotationsCleaned, finalAnnotationName, Optional.of(annotationTypes)));
        }

        controller.add(PipelineUtils.getPr("gate.creole.annotdelete.AnnotationDeletePR", ImmutableMap.of("setsToRemove",
                ImmutableList.of(normalizingTmp, phiAnnotationsCleaned))));

        return controller;
    }

    public static SerialAnalyserController NoOpController() throws GateException {
        return
                (SerialAnalyserController) Factory.createResource(
                        "gate.creole.SerialAnalyserController",
                        Factory.newFeatureMap(),
                        Factory.newFeatureMap(), "No Op Controller");

    }

    public static SerialAnalyserController setupEvaluationPipeline(String key, String response, Set<String> annotationTypes, File reportOutputDir) throws GateException {
        List<Plugin> plugins = ImmutableList.of(
                new Plugin.Maven("uk.ac.gate.plugins", "tools", "8.6-SNAPSHOT")
        );

        for (Plugin p : plugins) {
            Gate.getCreoleRegister().registerPlugin(p);
        }

        SerialAnalyserController myController =
                (SerialAnalyserController) Factory.createResource(
                        "gate.creole.SerialAnalyserController",
                        Factory.newFeatureMap(),
                        Factory.newFeatureMap(), "Evaluation");

        FeatureMap qualityAssuranceFeatures = Factory.newFeatureMap();
        qualityAssuranceFeatures.put("keyASName", key);
        qualityAssuranceFeatures.put("responseASName", response);
        qualityAssuranceFeatures.put("annotationTypes", annotationTypes);
        qualityAssuranceFeatures.put("measure", Measure.F1_STRICT);
        //qualityAssuranceFeatures.put("featureNames", ImmutableList.of("type"));

        try {
            reportOutputDir.mkdirs();
            qualityAssuranceFeatures.put("outputFolderUrl", reportOutputDir.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        myController.add((ProcessingResource) Factory.createResource("gate.qa.QualityAssurancePR", qualityAssuranceFeatures));

        return myController;
    }
}
