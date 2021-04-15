package org.ratschlab.deidentifier.sources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gate.Document;
import gate.Factory;
import gate.GateConstants;
import gate.creole.ResourceInstantiationException;
import org.apache.tools.ant.filters.StringInputStream;
import org.json.XML;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: extract abstract interface
public class KisimFormat {

    public static final String LIST_ELEMENT_NAME = "LISTELEMENT";
    public static final String NUMBER_MARKER = "NUMBER_";

    public static final String RAW_NULL_TOKEN = "___RAW_NULL___";

    // TODO: more structured input with metadat
    // TODO: perhaps json in
    // TODO: how to handle exceptions?
    public Document jsonToDocument(String jsonStr) {
        try {
            String xmlStr = convertToXmlString(jsonStr);

            // TODO: do it without temporary file
            //File xmlTmp = new File("/tmp/kisim_example.xml");
            //Files.write(xmlTmp.toPath(), xmlStr.getBytes());

            //Document doc = Factory.newDocument(xmlTmp.toURI().toURL(), "UTF-8");
            Document doc = Factory.newDocument(xmlStr);

            doc.setMarkupAware(true);
            doc.setPreserveOriginalContent(true);

            return doc;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ResourceInstantiationException e) {
            e.printStackTrace();
        }

        return null; // optional?
    }

    public String documentToJson(Document doc) {
        return documentToJson(doc, GateConstants.ORIGINAL_MARKUPS_ANNOT_SET_NAME);
    }

    public String documentToJson(Document doc, String annotationName) {
        String origXml = doc.toXml(doc.getAnnotations(annotationName));

        try {
            JsonNode root = convertXmlToJson(origXml);

            ObjectMapper mapper = new ObjectMapper();
            return mapper.writer().writeValueAsString(root);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return null; // TODO: optional?
    }

    private String convertToXmlString(String jsonStr) throws IOException {
        ByteArrayOutputStream sos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(sos);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonStr);

        out.print("<?xml version='1.0' encoding='UTF-8'?>");
        out.print("<report>");
        visitJsonNode(rootNode, out);
        out.print("</report>");

        return sos.toString("UTF-8");
    }

    private void visitJsonNode(JsonNode node, PrintStream out) {
        if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
            Iterator<Map.Entry<String, JsonNode>> children = node.fields();

            while (children.hasNext()) {
                Map.Entry<String, JsonNode> c = children.next();

                String tagName = escapeFieldNames(c.getKey());
                out.print("<" + tagName + ">");
                visitJsonNode(c.getValue(), out);
                out.println("</" + tagName + ">");
            }
        } else if (node.getNodeType().equals(JsonNodeType.ARRAY)) {
            Iterator<JsonNode> children = node.elements();

            while (children.hasNext()) {
                JsonNode n = children.next();

                out.print("<" + LIST_ELEMENT_NAME + ">");
                visitJsonNode(n, out);
                out.println("</" + LIST_ELEMENT_NAME + ">"); // TODO space matters?
            }

        } else if(node.isNull()) {
            out.print(RAW_NULL_TOKEN);
        }
        else {
            String jsonStr = node.toString().replace("\\n", "\n").
                    replace("\\t", "\t");

            String noQuotes = jsonStr.substring(1, jsonStr.length() - 1);
            if(noQuotes.equals(" ")) {
                noQuotes = "___SPACE___"; // TODO generalize
            }
            // if content should just be a new line ....
            if(noQuotes.equals("\n")) {
                noQuotes = "___NEWLINE___";
            }

            out.print(XML.escape(noQuotes)); // removing quotes
        }
    }

    // TODO:check for allowed spaces in xml tags and replace accordingly
    protected static String escapeFieldNames(String s) {
        if(s.length() == 0) {
            return "EMPTY";
        }
        else if (s.charAt(0) >= '0' && s.charAt(0)  <= '9') {
            return NUMBER_MARKER + s;
        } else {
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < s.length(); i++) {
                if(!Character.isAlphabetic(s.codePointAt(i)) && !Character.isDigit(s.codePointAt(i))) {
                    sb.append("___").append(((int) s.charAt(i))).append("---");
                } else {
                    sb.append(s.charAt(i));
                }
            }

            return sb.toString();
        }
    }

    protected static String unescapeFieldNames(String s) {
        if(s.equals("EMPTY")) {
            return "";
        }

        if (s.startsWith(NUMBER_MARKER)) {
            s = s.substring(NUMBER_MARKER.length());
        }


        int markerLength = 3;
        Pattern pat = Pattern.compile("___[0-9]+---");

        Matcher m = pat.matcher(s);

        //if(pat.matcher(s).matches()) {
            StringBuilder sb = new StringBuilder();

            int last = 0;
            while(m.find()) {
                sb.append(s.substring(last, m.start(0)));

                sb.append((char) Integer.parseInt(s.substring(m.start(0)+markerLength, m.end(0) - markerLength)));

                last = m.end(0);
            }

            sb.append(s.substring(last));
            s = sb.toString();
        //}


        /*
        String escMarker = "ESC";
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while(pos < s.length()) {
            if(s.startsWith(escMarker, pos)) {
                int escPos = pos + escMarker.length();
                StringBuilder numberBuffer = new StringBuilder();
                while(escPos < s.length()) {
                    if(Character.isDigit(s.charAt(escPos))) {
                        numberBuffer.append(s.charAt(escPos));
                        escPos++;
                    } else {
                        sb.append((char) Integer.parseInt(numberBuffer.toString()));
                        pos = escPos;
                        break;
                    }
                }
            } else {
                sb.append(s.charAt(pos));
                pos++;
            }
        }
        */

        return s;
    }

    private JsonNode convertXmlToJson(String xmlStr) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        DocumentBuilder builder = factory.newDocumentBuilder();

        org.w3c.dom.Document doc = builder.parse(new StringInputStream(xmlStr));

        Element root = doc.getDocumentElement();
        JsonNode node = visitXmlNode(root);

        return node.get("report");
    }


    private String unescapeFields(String s) {
        // TODO: use XML in a better way?
        return XML.unescape(s).replace("\\n", "\n").
                replace("\\t", "\t").
                replace("\\\"", "\"").
                replace("\\r", "\r").
                replace("\\\\", "\\").
                replace("___SPACE___", " ").
                replace("___NEWLINE___", "\n");

    }

    private JsonNode visitXmlNode(org.w3c.dom.Node node) {
        NodeList nl = node.getChildNodes();
        
        if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
            if(node.getTextContent().equals("\n") || node.getTextContent().matches(" ")) {
                return null;
            }

            if(node.getTextContent().equals(RAW_NULL_TOKEN)) {
                return JsonNodeFactory.instance.nullNode();
            }

            return JsonNodeFactory.instance.textNode(unescapeFields(node.getTextContent()).
                    replace("\\n", "\n")); // TODO: handle numerical values specially??
        } else if (nl.getLength() > 0 && nl.item(0).getNodeName().equals(LIST_ELEMENT_NAME)) {
            // TODO: check more thoroughly. here assuming that first element is marker for the reminaing elements.

            ArrayNode an = JsonNodeFactory.instance.arrayNode(nl.getLength());

            for (int i = 0; i < nl.getLength(); i++) {
                JsonNode n = visitXmlNode(nl.item(i));
                if (n != null) {
                    // representing empty objects as empty strings
                    // TODO: same handling has below. reduce duplication
                    if (n instanceof ObjectNode && n.size() == 0) {
                        n = JsonNodeFactory.instance.textNode("");
                    }
                    an.add(n);
                }
            }

            return an;
        } else {
            ObjectNode on = JsonNodeFactory.instance.objectNode();

            for (int i = 0; i < nl.getLength(); i++) {
                JsonNode n = visitXmlNode(nl.item(i));

                if (n == null) {
                    continue;
                }

                if (nl.item(i).getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
                    if(nl.item(i).getTextContent().matches(" ")) {
                        continue;
                    }
                    return n; // TODO; make nicer?  are we forgetting something?
                } else if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    String fieldName = unescapeFieldNames(nl.item(i).getNodeName());

                    // representing empty objects as empty strings
                    if (n instanceof ObjectNode && n.size() == 0) {
                        n = JsonNodeFactory.instance.textNode("");
                    }

                    on.set(fieldName, n);
                }
            }

            return on;
        }
    }
}
