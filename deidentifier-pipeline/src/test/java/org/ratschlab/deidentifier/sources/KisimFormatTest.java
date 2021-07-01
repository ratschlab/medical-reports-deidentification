package org.ratschlab.deidentifier.sources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Document;
import gate.Gate;
import gate.util.GateException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KisimFormatTest {

    @Test
    public void testEscapeFieldNames() {
        assertEquals("hello", KisimFormat.escapeFieldNames("hello"));
        assertEquals("NUMBER_123", KisimFormat.escapeFieldNames("123"));
        assertEquals("Hello___36---", KisimFormat.escapeFieldNames("Hello$"));
        assertEquals("Hello___32---World", KisimFormat.escapeFieldNames("Hello World"));
    }

    @Test
    public void testUnscapeFieldNames() {
        assertEquals("hello", KisimFormat.unescapeFieldNames("hello"));
        assertEquals("123", KisimFormat.unescapeFieldNames("NUMBER_123"));
        assertEquals("Hello$Bla", KisimFormat.unescapeFieldNames("Hello___36---Bla"));
        assertEquals("Hello$", KisimFormat.unescapeFieldNames("Hello___36---"));
        assertEquals("$", KisimFormat.unescapeFieldNames("___36---"));
        assertEquals("Hello beautiful world", KisimFormat.unescapeFieldNames("Hello___32---beautiful___32---world"));

    }

    @Test
    public void testKisimFormatReverse() {
        try {
            Gate.init();

            File jsonFile = new File(Thread.currentThread().getContextClassLoader().getResource("kisim_simple_example.json").getFile());

            ObjectMapper om = new ObjectMapper();
            // parse and emit string again to not have to deal with formatting issues during assert
            String jsonStr = om.writeValueAsString(om.reader().readTree(new FileInputStream(jsonFile)));

            KisimFormat kf = new KisimFormat();

            Document doc = kf.jsonToDocument(jsonStr);
            String jsonStrBack = kf.documentToJson(doc);

            assertEquals(jsonStr, jsonStrBack);
        } catch (GateException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
