## Installation

### Prerequisites

* at least Java 8
* appropriate JDBC driver, if reports are loaded from database (PostgreSQL drivers are already included)

### Getting the Software

Download the latest release from the [releases page](https://github.com/ratschlab/medical-reports-deidentification/releases), that is,
the jar file in the `Assets` section.

Alternatively (more advanced), you can download a recent zip archive generated everytime the github action workflows
are triggered. You can look for [workflow runs of the branch "main"](https://github.com/ratschlab/medical-reports-deidentification/actions)
and look for the "Artifacts" section of a specific run.

#### Building Yourself

You need'll need [Maven](https://maven.apache.org/install.html) to build from source code. Once `maven` is set up, you
can run the following in the root directory of the repository

```
mvn package --file deidentifier-pipeline/pom.xml
```

You'll then find the `jar` file containing the pipeline code as well as its dependencies in `deidentifier-pipeline/target`


### Basic Usage Example

In the following, a small example to deidentify a few JSON files.

First, create a directory `orig_reports` and populate it with [an example file](/deidentifier-pipeline/src/test/resources/kisim_simple_example.json) and/or create your own JSON files to put there.

The basic invocation of the deidentification tool is
```
java -jar [path to deidentifier-VERSION.jar]
```

In the following we abbreviate this by `DEID_CMD` (on a shell you could e.g. run `DEID_CMD="java -jar deidentifier-1.0.jar"`).

To annotate terms which need to be deidentified in the documents, run

```
$DEID_CMD annotate -i orig_reports --json-input -o annotated_reports -c configs/kisim-usz/kisim_usz.conf
```
You may have to adapt the path to the [kisim_usz.conf](/configs/kisim-usz/kisim_usz.conf) file.
The output in `annotated_reports` can be opened and inspected using [GATE Developer](https://gate.ac.uk/download/), the graphical user interface of the GATE framework.

The annotations can now be replaced and written out to disk again.
```
$DEID_CMD substitute -o substituted_reports --method Scrubber annotated_reps
```
The substituted reports can now be found in `substituted_reports`, where annotated terms are replaced by a fixed string
(more about this in the [substitution policy](overview.md#substitution-policies) section).


#### Adapt Database Configuration

In case reports should be read from a database instead of a directory, a configuration file
needs to be created specifying host, username, password table etc.
You can find more details in the [DB Configuration](components.md#db_config) section.

In the following we'll assume the file is called `db_conf.txt`.

Here an example, how the file could look like:

<a id="db_config_example"/>

```
jdbc_url=jdbc:sqlserver://myhost:2345;databaseName=MyDB;
user=deid_poc
password=1asdffea
query=SELECT DAT,FALLNR,CONTENT,FCODE,REPORTNR FROM MyDB.KISIM_KIS_T_REPORT_JSON.KIS_T_REPORT_JSON
json_field_name=CONTENT
reportid_field_name=REPORTNR
report_type_id_name=FCODE
date_field_name=DAT

# for writing back
dest_table=subst_test
dest_columns=CONTENT,REPORTNR,FCODE,DAT,FALLNR
```

Reports are read from the `KISIM_KIS_T_REPORT_JSON` table of the `MyDB` mssql database.
Substituted reports are written back into the table `subst_test` into the column `CONTENT` and the columns `REPORTNR,FCODE,DAT,FALLNR` are just copied over as is.

For a postgres DB the JDBC URL would start with `jdbc:postgresql://...`.
Other databases are supported in principle, but the corresponding JDBC driver needs to be made available on the java classpath.

To annotate reports from a database table, the command would become:

```
$DEID_CMD annotate -d db_conf.txt -o annotated_reports -c configs/kisim-usz/kisim_usz.conf
```

The `annotate` command provides some basic mean to select appropriate reports via the options
`--max-docs`, `--skip-docs`, `--doc-id-filter` and `--doc-type-filter` (see `$DEID_CMD annotate --help` for more details)
More complex filtering could be done by tweaking the `query` field in `db_conf.txt`.

To substitute the annotated reports and write them back into another database table, the command would be:
```
$DEID_CMD substitute -d db_conf.txt --method Scrubber annotated_reps
```

### Further Tips for Running the Deidentifier Tool

#### File Encoding

If you run into encoding related issues, try adding `-Dfile.encoding=UTF-8` into your `DEID_CMD`, e.g

```
java -Dfile.encoding=UTF-8 -jar deidentifier-*.jar
```

#### Increasing Memory

If the tool crashes with e.g. an `OutOfMemoryError` or the processing is very slow, try increasing the memory the Java
virtual machine (jvm) is allowed to use using the `-Xmx` option, e.g.

```
java -Xmx4g -jar deidentifier-*.jar
```

Here, in total at most 4g of RAM would be used.

#### Customize Logging

We use `log4j2` to manage logs. You can provide a custom `log4j` config by passing `-Dlog4j.configurationFile=[path to log config]` to the java command.
The default logging configuration can be found [here](/deidentifier-pipeline/src/main/resources/log4j2.xml).
See the [log4j documentation](https://logging.apache.org/log4j/2.x/manual/configuration.html) for more infos.

To debug the logging setup, you can add the `-Dlog4j.debug` flag to the java command.

#### Adding JDBC Drivers

To connect to a database other than Postgres, you need to download an appropriate JDBC driver
(make sure it is compatible with both the java and database version you are using).

The command line invocation (the `DEID_CMD`) becomes then
```
java -cp "deidentifier-pipeline/target/deidentifier-0.99.jar;[path to jdbc jar]" org.ratschlab.deidentifier.DeidMain
```

### Running the Deidentifier Tool in Docker

1. First add a `db_conf.txt` with your credentials to the root folder of the repository.

2. Then build the container withe the command:

```bash
docker build --tag deidentifier:1.0.0 .
```

>
> Please read the instructions in the Dockerfile when using MSSQL. The JDBC jar is avaiable [here](https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-ver16)

3. To run the annotaion and substitution in a docker container use the following command (Linux):

```bash
docker run deidentifier:1.0.0 sh -c  "java -cp '/deidentifier-pipeline/target/deidentifier-1.0.0.jar:/mssql-jdbc-12.6.2.jre8.jar' org.ratschlab.deidentifier.DeidMain annotate -d db_conf.txt -o annotated_reports -c configs/kisim-usz/kisim_usz.conf && java -cp '/deidentifier-pipeline/target/deidentifier-1.0.0.jar:/mssql-jdbc-12.6.2.jre8.jar' org.ratschlab.deidentifier.DeidMain substitute --db-config db_conf.txt --method Scrubber annotated_reports"
```

When using a Windows command line use the following command:


```bash
docker run deidentifier:1.0.0 sh -c  "java -cp '/deidentifier-pipeline/target/deidentifier-1.0.0.jar;/mssql-jdbc-12.6.2.jre8.jar' org.ratschlab.deidentifier.DeidMain annotate -d db_conf.txt -o annotated_reports -c configs/kisim-usz/kisim_usz.conf && java -cp '/deidentifier-pipeline/target/deidentifier-1.0.0.jar;/mssql-jdbc-12.6.2.jre8.jar' org.ratschlab.deidentifier.DeidMain substitute --db-config db_conf.txt --method Scrubber annotated_reports"
```

