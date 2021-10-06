package org.ratschlab.deidentifier.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Corpus;
import gate.Gate;
import gate.util.GateException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ratschlab.gate.GateTools;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ConversionCmdTest {

    @BeforeAll
    public static void initialize() throws GateException {
        Gate.init();
    }

    private File exampleJson = new File(this.getClass().getClassLoader().getResource("kisim_simple_example.json").getFile());

    @Test
    public void testConvertKisimJsonsToGateCorpus(@TempDir File tmpDir) {
        File output = new File(tmpDir, "mycorpus");

        try {
            ConversionCmd.convertKisimJsonsToGateCorpus(exampleJson, output);

            Corpus c = GateTools.openCorpus(output);
            assertEquals(1, c.stream().count());
        } catch (GateException|IOException e) {
            fail(e);
        }
    }

    @Test
    public void testConvertGateCorpusToGateJson(@TempDir File tmpDir) {
        File output = new File(tmpDir, "mycorpus");

        File jsonOutputDir = new File(tmpDir, "json_output");
        jsonOutputDir.mkdir();

        try {
            ConversionCmd.convertKisimJsonsToGateCorpus(exampleJson, output);

            ConversionCmd.convertGateCorpusToGateJson(output, jsonOutputDir);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(jsonOutputDir, exampleJson.getName()));
            assertEquals(2, rootNode.size());
            assertEquals(65, rootNode.get("entities").size());

        } catch (GateException|IOException e) {
            fail(e);
        }
    }
}
