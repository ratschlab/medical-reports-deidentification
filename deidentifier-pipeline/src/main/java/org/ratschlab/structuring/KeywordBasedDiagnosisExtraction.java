package org.ratschlab.structuring;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.deidentifier.pipelines.PipelineUtils;

import java.util.Optional;

import static org.ratschlab.deidentifier.pipelines.PipelineUtils.getAnnotationCopier;

public class KeywordBasedDiagnosisExtraction {
    public static SerialAnalyserController getDiagnosisExtractionPipeline(Config conf) throws GateException {
        return getDiagnosisExtractionPipeline(
                conf.getString(StructuringConfigKeys.TOKENIZER_RULES),
                conf.getString(StructuringConfigKeys.KEYWORDS_CONFIG),
                conf.getString(StructuringConfigKeys.RELIABILITY_CONTEXT_CONFIG),
                conf.getString(StructuringConfigKeys.JAPE_RULES));
    }

    public static SerialAnalyserController getDiagnosisExtractionPipeline(String tokenizerRulesUrl, String keywordsUrl, String reliabilityContextsUrl, String japeRulesUrl)
        throws GateException {

        String tmpAsName = "tmpAnnotations";
        String finalAnnotationSetName = "extracted-diagnosis";

        SerialAnalyserController controller = PipelineUtils.emptyPipeline("Rule based pipeline");

        controller.add(PipelineUtils.getTokenizer(tokenizerRulesUrl));

        controller.add(getAnnotationCopier("", tmpAsName, Optional.empty()));

        controller.add(PipelineUtils.getPr("org.ratschlab.structuring.KeywordAnnotator",
            ImmutableMap.of("configPath", keywordsUrl,
                    "inputASName", tmpAsName,
                "outputASName", tmpAsName)));

        controller.add(PipelineUtils.getPr("org.ratschlab.deidentifier.annotation.TriggerBasedContextAnnotator",
                ImmutableMap.of("configPath", reliabilityContextsUrl,
                        "inputASName", tmpAsName,
                        "outputASName", tmpAsName)));


        controller.add(PipelineUtils.getTransducer(japeRulesUrl, tmpAsName, tmpAsName));

        controller.add(getAnnotationCopier(tmpAsName, finalAnnotationSetName, Optional.of(ImmutableSet.of("Diagnosis"))));

        controller.add(PipelineUtils.getPr("org.ratschlab.structuring.AnnotationConsolidation", ImmutableMap.of("inputASName", finalAnnotationSetName)));

        return controller;
    }


}
