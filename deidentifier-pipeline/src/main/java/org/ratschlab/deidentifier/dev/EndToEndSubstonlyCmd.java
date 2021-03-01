package org.ratschlab.deidentifier.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import gate.Document;
import gate.Gate;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.ConfigUtils;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.substitution.DeidentificationSubstitution;
import org.ratschlab.deidentifier.substitution.IdentitySubstitution;
import org.ratschlab.deidentifier.substitution.ReplacementTagsSubstitution;
import org.ratschlab.deidentifier.workflows.PipelineWorkflow;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

public class EndToEndSubstonlyCmd {
    static {
        boolean assertsEnabled = false;
        assert assertsEnabled = true; // Intentional side effect!!!
        if (!assertsEnabled)
            throw new RuntimeException("Asserts must be enabled (-ea)!");
    }

    // TODO: generalize, make tool
    public static void main(String[] args) throws IOException, SQLException, GateException {
        Gate.init();

        int threads = 1;
        String configPath = "/home/marczim/postgres_pipeline_props_query.txt";
        int maxDocs = 1000000000;

        KisimSource ks = new KisimSource(new File(configPath));

        Stream<Pair<String, String>> records = ks.readJsonStringsWithReportId().limit(maxDocs);
        Config pipelineConfig = ConfigUtils.loadConfig(new File("/home/marczim/git/deidentifier-poc/configs/kisim-usz/kisim_usz.conf"));
        final SerialAnalyserController myController = PipelineFactory.getRuleBasedPipeline(pipelineConfig);

        PipelineWorkflow<Pair<String, String>> workflow = new PipelineWorkflow<>(
                records,
                p -> {
                    try {
                        ObjectMapper om = new ObjectMapper();
                        // parse and emit string again to not have to deal with formatting issues during assert
                        String jsonStr = om.writeValueAsString(om.reader().readTree(p.getRight()));

                        //System.out.println(p.getLeft());
                        Files.write(new File(String.format("/home/marczim/data/deid_poc/sets/kisim/kisim_json/%s.json", p.getLeft())).toPath(), jsonStr.getBytes(StandardCharsets.UTF_8));

                        return Optional.of(checkConversion(p.getRight(), myController));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return Optional.empty();
                },
                PipelineFactory.NoOpController(),
                threads,
                new ArrayList<>());

        workflow.run();

    }

    public static Document checkConversion(String kisimJson, SerialAnalyserController myController) throws IOException {
        ObjectMapper om = new ObjectMapper();
        // parse and emit string again to not have to deal with formatting issues during assert
        String jsonStr = om.writeValueAsString(om.reader().readTree(kisimJson));

        KisimFormat kf = new KisimFormat();

        Document doc = kf.jsonToDocument(jsonStr);

        doc = GateTools.processDoc(doc, myController);

        //Document substDoc = new DeidentificationSubstitution("phi-annotations", d -> new ReplacementTagsSubstitution(), true, Collections.emptyList()).substitute(doc);

        Document substDoc = new DeidentificationSubstitution("phi-annotations", d -> new IdentitySubstitution(), true, Collections.emptyList()).substitute(doc);


        String jsonStrBack = kf.documentToJson(substDoc);

        if(!jsonStr.equals(jsonStrBack)) {
            System.out.println(jsonStr);
            System.out.println(jsonStrBack);

            int delta = 15;
            for(int i = 0; i < Math.min(jsonStr.length(), jsonStrBack.length()); i++) {
                if(jsonStr.charAt(i) != jsonStrBack.charAt(i)) {
                    System.out.println(String.format("difference at %d %s | %s", i, jsonStr.substring(i-delta, i+delta),  jsonStrBack.substring(i-delta, i+delta)));
                    break;
                }
            }

        }

        return doc;
    }


}
