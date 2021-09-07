package org.ratschlab.deidentifier.utils;

import picocli.CommandLine;

import java.util.stream.Stream;

public class DbCommands {
    @CommandLine.Option(names = {"-d"}, description = "DB config path")
    protected String databaseConfigPath = "";

    @CommandLine.Option(names = {"--doc-type-filter"}, description = "Path to file type list to consider")
    protected String docTypeFilterPath = null;

    @CommandLine.Option(names = {"--doc-id-filter"}, description = "Path to file id list to consider")
    protected String docIdFilterPath = null;

    @CommandLine.Option(names = {"--max-docs"}, description = "Maximum number of docs to process")
    protected int maxDocs = -1;

    @CommandLine.Option(names = {"--skip-docs"}, description = "Skipping number of docs (useful to just work on a slice of the corpus)")
    protected int skipDocs = 0;


    public <T> Stream<T> docsLimiting(Stream<T> st) {
        Stream<T> skipped = st.skip(skipDocs);

        if(maxDocs > 0) {
            skipped = skipped.limit(maxDocs);
        }

        return skipped;
    }

}
