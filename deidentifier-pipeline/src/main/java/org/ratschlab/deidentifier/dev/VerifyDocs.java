package org.ratschlab.deidentifier.dev;

import gate.Document;
import org.ratschlab.deidentifier.sources.KisimFormat;
import org.ratschlab.deidentifier.workflows.DefaultWorkflowConcern;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class VerifyDocs extends DefaultWorkflowConcern {

    @Override
    public Document postProcessDoc(Document doc) {
        String directory = "/home/marczim/data/deid_poc/sets/kisim/kisim_json";

        String docString = new KisimFormat().documentToJson(doc);

        String id = doc.getName().split("-")[1];

        try {

            Path origPath = new File(directory, id + ".json").toPath();

            String origJson = new String(Files.readAllBytes(origPath), StandardCharsets.UTF_8);

            if(!origJson.equals(docString)) {
                System.out.println(doc.getName());
                System.out.println(origJson);
                System.out.println(docString);

                int delta = 15;
                for(int i = 0; i < Math.min(origJson.length(), docString.length()); i++) {
                    if(origJson.charAt(i) != docString.charAt(i)) {
                        System.out.println(String.format("difference at %d %s | %s", i,
                                origJson.substring(Math.max(i-delta, 0), Math.min(i+delta, origJson.length())),
                                docString.substring(Math.max(i-delta, 0), Math.min(i+delta, docString.length()))));
                        break;
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch(RuntimeException ex) {
            ex.printStackTrace();
        }

        return doc;
    }
}
