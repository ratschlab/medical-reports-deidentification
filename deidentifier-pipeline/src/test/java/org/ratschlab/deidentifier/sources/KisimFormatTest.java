package org.ratschlab.deidentifier.sources;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gate.Document;
import gate.Gate;
import gate.util.GateException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.ratschlab.deidentifier.sources.KisimFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class KisimFormatTest {

    @Test
    public void testEscapeFieldNames() {
        Assert.assertEquals("hello", KisimFormat.escapeFieldNames("hello"));
        Assert.assertEquals("NUMBER_123", KisimFormat.escapeFieldNames("123"));
        Assert.assertEquals("Hello___36---", KisimFormat.escapeFieldNames("Hello$"));
        Assert.assertEquals("Hello___32---World", KisimFormat.escapeFieldNames("Hello World"));
    }

    @Test
    public void testUnscapeFieldNames() {
        Assert.assertEquals("hello", KisimFormat.unescapeFieldNames("hello"));
        Assert.assertEquals("123", KisimFormat.unescapeFieldNames("NUMBER_123"));
        Assert.assertEquals("Hello$Bla", KisimFormat.unescapeFieldNames("Hello___36---Bla"));
        Assert.assertEquals("Hello$", KisimFormat.unescapeFieldNames("Hello___36---"));
        Assert.assertEquals("$", KisimFormat.unescapeFieldNames("___36---"));
        Assert.assertEquals("Hello beautiful world", KisimFormat.unescapeFieldNames("Hello___32---beautiful___32---world"));

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

            Assert.assertEquals(jsonStr, jsonStrBack);
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
