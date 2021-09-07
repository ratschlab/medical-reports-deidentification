package org.ratschlab.deidentifier.workflows;

import gate.Document;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class WriteDocsToFiles extends DefaultWorkflowConcern {

    private File directory;

    private Function<Document, String> converter;

    private Function<Document, String> fileName;

    public WriteDocsToFiles(File directory, Function<Document, String> converter, Function<Document, String> fileName) {
        this.directory = directory;
        this.converter = converter;
        this.fileName = fileName;

        try {
            // TODO: really keep that way?
            if (directory.exists()) {
                FileUtils.deleteDirectory(directory);
            }
            directory.mkdirs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Document postProcessDoc(Document doc) {
        String docString = converter.apply(doc);

        try {
            Path outputPath = new File(directory, this.fileName.apply(doc)).toPath();
            Files.write(outputPath, docString.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return doc;
    }
}
