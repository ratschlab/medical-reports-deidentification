# This script is a template for a deidentification pipeline importing reports from a database, annotating them and substituting and writes back into an designated database table.

$ErrorActionPreference = "Stop"

### Variables needing adaptation

# path to deid jar file (in release zip)
$jar_path="deid_usz/lib/deidentifier-0.1-SNAPSHOT.jar"

# deid jar as well as JDBC driver jars (here with mssql for Java 8)
# download driver jar e.g. from https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/7.4.1.jre8/mssql-jdbc-7.4.1.jre8.jar
$classpath = "${jar_path};deid_usz/lib/mssql-jdbc-7.4.1.jre8.jar"

# database configuration (create manually, see doc for an example)
$query_config_path="path to db_conf.txt"

# pipeline configuration (in release zip)
$pipeline_config_path="deid_usz/kisim-usz/kisim_usz.conf"

# working directory
$base_dir="path to any existing directory"

$proxy_settings="-Djava.net.useSystemProxies=true -Dhttp.proxyHost=proxydirect.usz.ch -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxydirect.usz.ch -Dhttps.proxyPort=8080 "

### Generally not necessary to touch

# derived vars
$corpus_dir="$base_dir/unannotated"
$annot_dir="$base_dir/annotated"

$base_args = "-Dfile.encoding=UTF-8 -Dexec.cleanupDaemonThreads=false -Ddeid.log.path=pipeline.log ${proxy_settings}"

## Import reports
& java "-Xmx4g $base_args -cp ${classpath} import -d $query_config_path -o $corpus_dir --max-docs 50".Split(" ")

## Annotate reports
& java "-Xmx4g $base_args -cp ${classpath} annot -i $corpus_dir -o $annot_dir -t 4 -c $pipeline_config_path".Split(" ")

## Substitute
& java "-Xmx4g $base_args -cp ${classpath} subst $annot_dir --db-config $query_config_path --method ReplacementTags".Split(" ")
