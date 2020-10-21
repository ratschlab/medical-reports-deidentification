package org.ratschlab.deidentifier.pipelines;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.AnnotationSchema;
import gate.creole.Plugin;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.ratschlab.gate.GateTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PipelineUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineUtils.class.getName());

    // make class instance
    public static String DEFAULT_ENCODING = "UTF-8";


    public static void registerDeidComponents() {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Gate.getCreoleRegister().registerPlugin(new Plugin.Directory(cl.getResource("deid-creole")));
        } catch(GateException e) {
            e.printStackTrace();
        }
    }

    public static SerialAnalyserController emptyPipeline(String name) throws GateException {
        String gateVersion = "8.6-SNAPSHOT";

        // TODO parametrize versions.
        List<Plugin> basePlugins = ImmutableList.of(
                new Plugin.Maven("uk.ac.gate.plugins", "annie", gateVersion),
                new Plugin.Maven("uk.ac.gate.plugins", "lang-german", gateVersion),
                new Plugin.Maven("uk.ac.gate.plugins", "jape-plus", gateVersion),
                new Plugin.Maven("uk.ac.gate.plugins", "tools", "8.6-SNAPSHOT"));

        for(Plugin p : basePlugins) {
            Gate.getCreoleRegister().registerPlugin(p);
        }

        registerDeidComponents();

        SerialAnalyserController myController =
                (SerialAnalyserController) Factory.createResource(
                        "gate.creole.SerialAnalyserController",
                        Factory.newFeatureMap(),
                        Factory.newFeatureMap(), name);

        // RESET
        //myController.add((ProcessingResource) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR", Factory.newFeatureMap()));

        return myController;
    }

    public static ProcessingResource getTokenizer(String rulesUrl) throws ResourceInstantiationException {
        return getTokenizer(rulesUrl, DEFAULT_ENCODING);
    }

    public static ProcessingResource getTokenizer(String rulesUrl, String encoding) throws ResourceInstantiationException {
        FeatureMap tokenizerFeature = Factory.newFeatureMap();
        tokenizerFeature.put("encoding", encoding);
        tokenizerFeature.put("rulesURL", rulesUrl);

        return (ProcessingResource) Factory.createResource("gate.creole.tokeniser.SimpleTokeniser", tokenizerFeature);
    }

    public static ProcessingResource getSentenceSplitter(String transducerUrl, String gazetteerUrl) throws ResourceInstantiationException {
        Map<String, Object> options = ImmutableMap.of("encoding", DEFAULT_ENCODING,
                            "gazetteerListsURL", gazetteerUrl,
                            "transducerURL", transducerUrl);

        return getPr("gate.creole.splitter.SentenceSplitter", options);
    }


    public static ProcessingResource getGazetteer(String gazetteerUrl, boolean caseSensitive, boolean wholeWordsOnly) {
        LOGGER.warn(String.format("Attempt to load Gazetteer from %s", gazetteerUrl));
        Map<String, Object> options = ImmutableMap.of("encoding", DEFAULT_ENCODING,
                "caseSensitive", caseSensitive,
                "wholeWordsOnly", wholeWordsOnly,
                "gazetteerFeatureSeparator", "\t",
                "listsURL", gazetteerUrl);

        return getPr("gate.creole.gazetteer.DefaultGazetteer", options);
    }

    public static ProcessingResource getPosTagger(String modelFile) throws GateException {
        Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins", "stanford-corenlp", "8.5.1")); // TODO parametrize version

        return getPr("gate.stanford.Tagger",
                ImmutableMap.of("modelFile", modelFile));

    }

    public static ProcessingResource getTransducer(String japeRulesUrl, String inputAsName, String outputAsName) {
        japeRulesUrl = resolvePath(japeRulesUrl);

        return getPr("gate.jape.plus.Transducer",
                ImmutableMap.of("encoding", DEFAULT_ENCODING,
                        "grammarURL", japeRulesUrl,
                        "inputASName", inputAsName,
                        "outputASName", outputAsName));
    }

    public static ProcessingResource getAnnotationCopier(String in, String out, Optional<Set<String>> types) throws ResourceInstantiationException {
        FeatureMap annotTransferFeatures = Factory.newFeatureMap();
        annotTransferFeatures.put("inputASName", in);
        annotTransferFeatures.put("outputASName", out);
        annotTransferFeatures.put("copyAnnotations", true);

        types.ifPresent(t -> annotTransferFeatures.put("annotationTypes", Lists.newArrayList(t)));

        return (ProcessingResource) Factory.createResource("gate.creole.annotransfer.AnnotationSetTransfer", annotTransferFeatures);
    }

    public static ProcessingResource getSchemaEnforcer(String schemaUrlDir, Set<String> annotationTypes, String inputAs, String outputAs)  {
        try {
            Gate.getCreoleRegister().registerPlugin(new Plugin.Maven("uk.ac.gate.plugins", "schema-tools", "8.5"));
        } catch(GateException e) {
            e.printStackTrace();
        }

        String schemaUrlDirResolved = resolvePath(schemaUrlDir);
        List<AnnotationSchema> schemas = annotationTypes.stream().map(s -> {
            try {
                return GateTools.loadSchema(new URL(schemaUrlDirResolved + File.separator + s.toLowerCase() + ".xml"));
            } catch (IOException | ResourceInstantiationException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        return getPr("gate.creole.schema.SchemaEnforcer", ImmutableMap.of(
                "inputASName", inputAs,
                "outputASName", outputAs,
                "schemas", schemas));
    }


    public static ProcessingResource getPr(String prName, Map<String, Object> options) {
        try {
            FeatureMap features = Factory.newFeatureMap();
            features.putAll(options);
            return (ProcessingResource) Factory.createResource(prName, features);
        } catch (ResourceInstantiationException e) {
            System.err.println(options);
            System.err.println(e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    private static String resolvePath(String p) {
        if(p.startsWith("creole://") || p.startsWith("file:/")) {
            return p;
        }

        if(!new File(p).isAbsolute()) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();

            try {
                LOGGER.info("Getting resource " + p + "  --> " + cl.getResource(p));
                return cl.getResource(p).toURI().toString();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        return p;
    }
}
