package org.ratschlab.structuring;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.typesafe.config.Config;
import gate.GateConstants;
import gate.ProcessingResource;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.Gazetteer;
import gate.creole.gazetteer.Lookup;
import gate.util.GateException;
import org.ratschlab.deidentifier.pipelines.PipelineUtils;
import scala.concurrent.java8.FuturesConvertersImpl;

import java.util.HashMap;
import java.util.Optional;

import static org.ratschlab.deidentifier.pipelines.PipelineUtils.DEFAULT_ENCODING;
import static org.ratschlab.deidentifier.pipelines.PipelineUtils.getAnnotationCopier;

public class KeywordBasedDiagnosisExtraction {

//    private static Gazetteer getGazetteer() {
//        Gazetteer gaz = new gate.creole.gazetteer.DefaultGazetteer();
//
//        gaz.setCaseSensitive(true);
//
//        try {
//            gaz.init();
//        } catch (ResourceInstantiationException e) {
//            e.printStackTrace();
//        }
//
//        //String theList, String major, String minor, String theLanguages, String annotationType
//        gaz.add("Multiple Sklerose", new Lookup("list", "diagnosis", "ICD10-G35", "en"));
//
//        return gaz;
//    }

    // TODO: rename?
    public static SerialAnalyserController getExtractionPipeline(Config conf) throws GateException {
        return getExtractionPipeline(
                conf.getString(StructuringConfigKeys.TOKENIZER_RULES),
                conf.getString(StructuringConfigKeys.KEYWORDS_CONFIG),
                conf.getString(StructuringConfigKeys.RELIABILITY_CONTEXT_CONFIG),
                conf.getString(StructuringConfigKeys.JAPE_RULES));
    }

    public static SerialAnalyserController getExtractionPipeline(String tokenizerRulesUrl, String keywordsUrl, String reliabilityContextsUrl, String japeRulesUrl)
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

        //        controller.add(PipelineUtils.getAnnotationCopier(normalizingTmp, tmpAsName, Optional.empty()));
//        controller.add(PipelineUtils.getAnnotationCopier("", tmpAsName, Optional.empty()));

        controller.add(PipelineUtils.getTransducer(japeRulesUrl, tmpAsName, tmpAsName));

        controller.add(getAnnotationCopier(tmpAsName, finalAnnotationSetName, Optional.of(ImmutableSet.of("Diagnosis"))));

        controller.add(PipelineUtils.getPr("org.ratschlab.structuring.AnnotationConsolidation", ImmutableMap.of("inputASName", finalAnnotationSetName)));

        return controller;
    }


}
