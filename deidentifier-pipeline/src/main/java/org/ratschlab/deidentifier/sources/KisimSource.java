package org.ratschlab.deidentifier.sources;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger log = LoggerFactory.getLogger(KisimSource.class);
    private Connection conn;

    private String readSqlQuery;

    private String contentFieldName;
    private String reportIdFieldName;
    private String reportTypeIdName;

    private String destTable;
    private Set<String> destColumns;

    private static final String JDBC_URL_KEY = "jdbc_url";
    private static final String JSON_FIELD_NAME_KEY = "json_field_name";
    public static final String REPORT_TYPE_ID_NAME_KEY = "report_type_id_name";
    public static final String REPORTID_FIELD_NAME_KEY = "reportid_field_name";
    public static final String QUERY_KEY = "query";

    private static final Set<String> mandatoryFields = new HashSet<>(Arrays.asList(JDBC_URL_KEY, JSON_FIELD_NAME_KEY,
        REPORT_TYPE_ID_NAME_KEY, REPORTID_FIELD_NAME_KEY, QUERY_KEY));

    public KisimSource(File propertiesFile) throws IOException, SQLException {
        Properties props = new Properties();

        log.info("Reading database config from " + propertiesFile.getAbsolutePath());
        props.load(new FileInputStream(propertiesFile));

        for(String field : mandatoryFields) {
            if(props.getProperty(field) == null) {
                throw new IllegalArgumentException(String.format("Field %s is missing in file %s", field, propertiesFile.getAbsolutePath()));
            }
        }

        conn = DriverManager.getConnection(props.getProperty(JDBC_URL_KEY), props);

        contentFieldName = props.getProperty(JSON_FIELD_NAME_KEY).toUpperCase();
        reportIdFieldName = props.getProperty(REPORTID_FIELD_NAME_KEY).toUpperCase();
        reportTypeIdName = props.getProperty(REPORT_TYPE_ID_NAME_KEY).toUpperCase();

        destTable = props.getProperty("dest_table", "");

        destColumns = Arrays.stream(props.getProperty("dest_columns", "")
            .split(","))
            .map(s -> s.trim().toUpperCase())
            .collect(Collectors.toSet());

        readSqlQuery = props.getProperty(QUERY_KEY).toUpperCase();
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
            colVals.add(String.format("'%s'", content.replace("'", "''")));

            additionalCols.forEach((k, v) -> {
                if (destColumns.contains(k.toString().toUpperCase()) && !colNameList.contains(k.toString().toUpperCase())) {
                    colNameList.add(k.toString().toUpperCase());

                    String colVal = v != null ? String.format("'%s'", v.toString().replace("'", "''")) : "NULL";
                    colVals.add(colVal);
                }
            });

            Set<String> emptyCols = new HashSet(destColumns);
            emptyCols.removeAll(colNameList);

            emptyCols.forEach(s -> {
                colNameList.add(s);
                colVals.add("''");
            });

            String query = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    destTable,
                    colNameList.stream().collect(Collectors.joining(",")),
                    colVals.stream().collect(Collectors.joining(","))
            );

            st.addBatch(query);
        }

        log.info(String.format("Writing %d records", data.size()));
        int[] rets = st.executeBatch();

        log.info(String.format("All queries successful %b", Arrays.stream(rets).allMatch(r -> r == 1)));
    }
}
