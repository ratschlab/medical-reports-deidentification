package org.ratschlab.deidentifier.dev;

import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Document;
import gate.Gate;
import gate.util.GateException;
import org.apache.commons.lang3.tuple.Pair;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.pipelines.PipelineFactory;
import org.ratschlab.deidentifier.sources.KisimSource;
import org.ratschlab.deidentifier.substitution.DeidentificationSubstitution;
import org.ratschlab.deidentifier.substitution.IdentitySubstitution;
import org.ratschlab.deidentifier.workflows.PipelineWorkflow;

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

    // TODO: generalize, make tool
    public static void main(String[] args) throws IOException, SQLException, GateException {
        Gate.init();

        int threads = 10;
        String configPath = "/home/marczim/postgres_pipeline_props_query.txt";
        int maxDocs = 1000000000;

        KisimSource ks = new KisimSource(new File(configPath));

        Stream<Pair<String, String>> records = ks.readJsonStringsWithReportId().limit(maxDocs);

        PipelineWorkflow<Pair<String, String>> workflow = new PipelineWorkflow<>(
                records,
                p -> {
                    try {
                        ObjectMapper om = new ObjectMapper();
                        // parse and emit string again to not have to deal with formatting issues during assert
                        String jsonStr = om.writeValueAsString(om.reader().readTree(p.getRight()));

                        //System.out.println(p.getLeft());
                        Files.write(new File(String.format("/home/marczim/data/deid_poc/sets/kisim/kisim_json/%s.json", p.getLeft())).toPath(), jsonStr.getBytes(StandardCharsets.UTF_8));

                        return Optional.of(checkConversion(p.getRight()));
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

    public static Document checkConversion(String kisimJson) throws IOException {
        ObjectMapper om = new ObjectMapper();
        // parse and emit string again to not have to deal with formatting issues during assert
        String jsonStr = om.writeValueAsString(om.reader().readTree(kisimJson));

        KisimFormat kf = new KisimFormat();

        Document doc = kf.jsonToDocument(jsonStr);

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
