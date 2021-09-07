
$ErrorActionPreference = "Stop"

$jar_path="/home/marczim/git/deidentifier-poc/deidentifier-pipeline/target/deidentifier-0.1-SNAPSHOT.jar"

$query_config_path="/home/marczim/postgres_pipeline_props_query.txt"
$pipeline_config_path="/home/marczim/git/deidentifier-poc/configs/kisim-usz/kisim_usz.conf"

$base_dir="/home/marczim/data/deid_poc/pipeline_test"

# derived vars
$corpus_dir="$base_dir/unannotated"
$annot_dir="$base_dir/annotated"

$base_args = "-Dfile.encoding=UTF-8 -Dexec.cleanupDaemonThreads=false -Ddeid.log.path=pipeline.log -Djava.net.useSystemProxies=true -Dhttp.proxyHost=proxydirect.usz.ch -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxydirect.usz.ch -Dhttps.proxyPort=8080 $jar_path"

## Import reports
& java "-Xmx4g -jar $base_args import -d $query_config_path -o $corpus_dir --skip-docs 50 --max-docs 50".Split(" ")

## Annotate reports
& java "-Xmx4g -jar $base_args annot -i $corpus_dir -o $annot_dir -t 4 -c $pipeline_config_path".Split(" ")

## Substitute
& java "-Xmx4g -jar $base_args subst $annot_dir --db-config $query_config_path --method ReplacementTags".Split(" ")
