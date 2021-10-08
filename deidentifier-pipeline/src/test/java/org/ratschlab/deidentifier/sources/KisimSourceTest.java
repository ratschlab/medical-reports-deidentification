package org.ratschlab.deidentifier.sources;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.dbunit.Assertion;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.operation.DatabaseOperation;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;

class KisimSourceTest{
    @BeforeEach
    void initializeDB() throws Exception {
        DataSource source = getDataSource();
        IDataSet data = getDataSet();

        IDatabaseConnection connection = new H2Connection(source.getConnection(), "public");

        try {
            DatabaseOperation.CLEAN_INSERT.execute(connection, data);
        } finally {
            connection.close();
        }
    }

    @Test
    void writeData() {
        try {
            KisimSource ks = new KisimSource(new File(this.getClass().getClassLoader().getResource("kisim-tests/database_cfg.props").getFile()));

            Stream<Map<String, Object>> origData = ks.readRecords();
            Stream<Map<Object, Object>> origDataCasted = origData.map(m -> m.entrySet().stream().collect(HashMap::new, (nm,e)-> nm.put(e.getKey(), e.getValue()), HashMap::putAll));

            Stream<Pair<String, Map<Object, Object>>> data = origDataCasted.map(d -> {
                String content = d.get(ks.getContentFieldName()).toString();
                d.remove(ks.getContentFieldName());
                return Pair.of(content, d);
            });

            List<Pair<String, Map<Object, Object>>> dataToWrite =  data.collect(Collectors.toList());

            ks.writeData(dataToWrite);

            ITable expectedData = getDataSet().getTable("sample_reports");

            DataSource source = getDataSource();
            IDatabaseConnection connection = new H2Connection(source.getConnection(), "public");

            ITable processedData = connection.createDataSet().getTable("processed_reports");

            Stream<Map<String, Object>> data_written = new QueryRunner()
                .query(source.getConnection(), "SELECT * FROM processed_reports", new MapListHandler())
                .stream();

            List<Map<String, Object>> collect = data_written.collect(Collectors.toList());

            Assertion.assertEquals(expectedData, processedData);


//            System.out.println(collect.size());


        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    //@Override
    private DataSource getDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(
            "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;init=runscript from 'classpath:kisim-tests/table-schema.sql'");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }

    //@Override
    private IDataSet getDataSet() throws Exception {
        return new FlatXmlDataSetBuilder().build(getClass().getClassLoader()
            .getResourceAsStream("kisim-tests/example_data.xml"));
    }


}
