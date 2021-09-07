package org.ratschlab.deidentifier.sources;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KisimSource {
    private static final Logger log = Logger.getLogger(KisimSource.class);

    private Connection conn;

    private String readSqlQuery;

    private String contentFieldName;
    private String reportIdFieldName;
    private String reportTypeIdName;

    private String destTable;
    private Set<String> destColumns;

    public KisimSource(File propertiesFile) throws IOException, SQLException {
        Properties props = new Properties();

        log.info("Reading database config from " + propertiesFile.getAbsolutePath());
        props.load(new FileInputStream(propertiesFile));

        conn = DriverManager.getConnection(props.getProperty("jdbc_url"), props);

        contentFieldName = props.getProperty("json_field_name");
        reportIdFieldName = props.getProperty("reportid_field_name");
        reportTypeIdName = props.getProperty("report_type_id_name");

        destTable = props.getProperty("dest_table", "");
        destColumns = new HashSet(Arrays.asList(props.getProperty("dest_columns", "").split(",")));

        readSqlQuery = props.getProperty("query");
    }

    public Stream<Map<String, Object>> readRecords() {
        try {
            return new QueryRunner()
                    .query(conn, readSqlQuery, new MapListHandler())
                    .stream();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Stream.empty();
    }

    public Stream<Pair<String, String>> readJsonStringsWithReportId() {
        return readRecords().map(m -> Pair.of(m.get(reportIdFieldName).toString(), m.get(contentFieldName).toString()));
    }

    public String getContentFieldName() {
        return contentFieldName;
    }

    public String getReportIdFieldName() {
        return reportIdFieldName;
    }

    public String getReportTypeIdName() {
        return reportTypeIdName;
    }

    public void writeData(Collection<Pair<String, Map<Object, Object>>> data) throws SQLException {
        if(data.isEmpty()) {
            return;
        }

        Statement st = conn.createStatement();

        for(Pair<String, Map<Object, Object>> d : data){
            String content = d.getLeft();
            Map<Object, Object> additionalCols = d.getRight();

            List<String> colNameList = new ArrayList<>();
            colNameList.add(contentFieldName);

            List<String> colVals = new ArrayList<>();
            colVals.add(content.replace("'", "''"));

            additionalCols.forEach((k, v) -> {
                if (destColumns.contains(k.toString())) {
                    colNameList.add(k.toString());
                    colVals.add(v.toString().replace("'", "''"));
                }
            });

            Set<String> emptyCols = new HashSet(destColumns);
            emptyCols.removeAll(colNameList);

            emptyCols.forEach(s -> {
                colNameList.add(s);
                colVals.add("");
            });

            String query = String.format("INSERT INTO %s (%s) VALUES ('%s')",
                    destTable,
                    colNameList.stream().collect(Collectors.joining(",")),
                    colVals.stream().collect(Collectors.joining("','"))
            );

            st.addBatch(query);
        }

        log.info(String.format("Writing %d records", data.size()));
        int[] rets = st.executeBatch();

        log.info(String.format("All queries successful %b", Arrays.stream(rets).allMatch(r -> r == 1)));
    }
}
