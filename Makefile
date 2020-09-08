PIPELINE_MAIN_CLASS="org.ratschlab.deidentifier.AnnotationCmd"

PIPELINE_COMMON_ARGS=-Dfile.encoding=UTF-8 -Dexec.cleanupDaemonThreads=false -Dexec.mainClass=$(PIPELINE_MAIN_CLASS) 
#-Dlog4j.configuration=/home/marczim/git/deidentifier-poc/log4j.properties

CONFIG_PATH="../configs/kisim-usz/kisim_usz.conf"

clean:
	cd deidentifier-pipeline; mvn clean

compile:
	cd deidentifier-pipeline; mvn compile

test:
	cd deidentifier-pipeline; mvn test
	echo "Test specific parts"
	cd deidentifier-pipeline; mvn -e exec:java -Dexec.mainClass="org.ratschlab.deidentifier.pipelines.testing.PipelineTesterCmd" -Dexec.args="../configs/kisim-usz/kisim_usz.conf ../configs/kisim-usz/testcases"

jar:
	cd deidentifier-pipeline; mvn package	

DE_EXEC_ARGS := -i ../toy-corpora/toy_de/clean -o ../toy-corpora/toy_de/processed/ --xml-input -c ../configs/kisim-usz/kisim_usz.conf -t 1

run-de:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(DE_EXEC_ARGS)"

run-de-jar:
	cd deidentifier-pipeline; java -jar  target/deidentifier-0.1-SNAPSHOT.jar annotate $(DE_EXEC_ARGS)

run-de-train:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(DE_EXEC_ARGS) --train --ml-model /tmp/de-model"

run-de-ml:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(DE_EXEC_ARGS) --ml-model /tmp/de-model"


run-reports-small:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c ../de_pipeline.conf -i /home/marczim/data/deid_poc/sets/set0/clean -o /home/marczim/data/deid_poc/sets/set0/processed --annotation-mapping ../configs/annotation_mapping_first_batch.txt"


KISIM_EXEC_ARGS := -c $(CONFIG_PATH) -d /home/marczim/postgres_pipeline_props_query.txt
KISIM_SMALL_EXEC_ARGS := $(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_small

import-test:
	cd deidentifier-pipeline; mvn -e exec:java -Dfile.encoding=UTF-8 -Dexec.cleanupDaemonThreads=false -Dexec.mainClass=org.ratschlab.deidentifier.DeidMain -Dexec.args="import -d /home/marczim/postgres_pipeline_props_query.txt -o /home/marczim/data/deid_poc/import_test --skip-docs 50 --max-docs 2000000"


conversion-test:
	cd deidentifier-pipeline; mvn -e exec:java -Dfile.encoding=UTF-8 -Dexec.cleanupDaemonThreads=false -Dexec.mainClass=org.ratschlab.deidentifier.DeidMain -Dexec.args="diagnostics conversioncheck -d /home/marczim/postgres_pipeline_props_query.txt -o /home/marczim/data/deid_poc/conversion_test_out.txt"


run-reports-kisim-small:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_SMALL_EXEC_ARGS) --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_small --max-docs 1000 -t 20"

run-reports-kisim-batch1:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_batch1 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_batch1_rerun -m /data/share/random-reports-batch1_1 --skip-docs 50 --max-docs 100 -t 20"
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_batch1_rerun/evaluation/corpus-stats.html	
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_batch1_rerun/ml-features.json

run-reports-kisim-proj-batch1:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_proj_batch1 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch1_rerun -m  /data/share/ade-midata-reports-batch1/ --skip-docs 50 --max-docs 100 -t 20 --doc-type-filter ../configs/project_specific_fcodes.txt"
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch1_rerun/evaluation/corpus-stats.html	
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_proj_batch1_rerun/ml-features.json

run-reports-kisim-proj-batch2:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_proj_batch2 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch2_rerun -m  /data/share/ade-midata-reports-batch2/ --skip-docs 150 --max-docs 100 -t 20 --doc-type-filter ../configs/project_specific_fcodes.txt"
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch2_rerun/evaluation/corpus-stats.html	
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_proj_batch2_rerun/ml-features.json

run-reports-kisim-proj-batch3:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_proj_batch3 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch3_rerun -m  /data/share/ade-midata-reports-batch3/ --skip-docs 250 --max-docs 100 -t 20 --doc-type-filter ../configs/project_specific_fcodes.txt"
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_proj_batch3_rerun/evaluation/corpus-stats.html	
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_proj_batch3_rerun/ml-features.json

run-reports-kisim-proj: run-reports-kisim-proj-batch1 run-reports-kisim-proj-batch2 run-reports-kisim-proj-batch3


# ade13 wrt to annotation of annotator I
run-reports-kisim-ade13:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -d /home/marczim/postgres_ade_delivery_201908.txt -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_ade13 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_rerun -m  /data/share/ade-manual-batch13 --max-docs 200 -t 20 --doc-id-filter ../configs/ade_manual_batch.txt"
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_rerun/evaluation/corpus-stats.html	
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_ade13_rerun/ml-features.json


# ade13 wrt to gold standard (annotator I and II)
run-reports-kisim-ade13-gold:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -d /home/marczim/postgres_ade_delivery_201908.txt -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_ade13_gold --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_gold_rerun -m /data/share/gold_standard_annotation_partA --max-docs 200 -t 20 --doc-id-filter ../configs/ade_manual_batch.txt"
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_ade13_gold_rerun/ml-features.json
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_gold_rerun/evaluation/corpus-stats.html	


# excluding addressfields to see change in location performance 
run-reports-kisim-ade13-gold-no-addr:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -d /home/marczim/postgres_ade_delivery_201908.txt -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_ade13_gold_no_addr --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_gold_no_addr -m /data/share/gold_standard_annotation_partA --max-docs 200 -t 20 --doc-id-filter ../configs/ade_manual_batch.txt --fields-blacklist-eval ../configs/addr_field_blacklist.txt"
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_ade13_gold_no_addr/ml-features.json
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_ade13_gold_no_addr/evaluation/corpus-stats.html


# ade14 wrt to annotation of annotator I
run-reports-kisim-ade14:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -d /home/marczim/postgres_ade_delivery_201908.txt -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_ade14 --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_ade14_rerun -m /data/share/ade-manual-batch14 --skip-docs 200 --max-docs 200 -t 20 --doc-id-filter ../configs/ade_manual_batch.txt"
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_ade14_rerun/ml-features.json
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_ade14_rerun/evaluation/corpus-stats.html


# ade14 wrt to gold standard (annotator I and II)
run-reports-kisim-ade14-gold:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -d /home/marczim/postgres_ade_delivery_201908.txt -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim_ade14_gold --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics_ade14_gold_rerun -m /data/share/gold_standard_annotation_partB --skip-docs 200 --max-docs 200 -t 20 --doc-id-filter ../configs/ade_manual_batch.txt"
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics_ade14_gold_rerun/ml-features.json
	firefox file:///home/marczim/data/deid_poc/sets/kisim/diagnostics_ade14_gold_rerun/evaluation/corpus-stats.html


run-reports-kisim-small-train:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_SMALL_EXEC_ARGS) --max-docs 100 --train --ml-model /tmp/kisim-small-model"

run-reports-kisim-small-ml:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_SMALL_EXEC_ARGS) --max-docs 100 --ml-model /tmp/kisim-small-model -t 4"

run-reports-kisim:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="$(KISIM_EXEC_ARGS) -o /home/marczim/data/deid_poc/sets/kisim/processed_kisim --diagnostics-dir /home/marczim/data/deid_poc/sets/kisim/diagnostics/full -t 25 --max-docs 100000 "
	python scripts/convert_ml_features_to_parquet.py ~/data/deid_poc/sets/kisim/diagnostics/full/ml-features.json


run-reports-kisim-1000:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c ../de_pipeline.conf --max-docs 10000 -d /home/marczim/postgres_pipeline_props_1000.txt -o /home/marczim/data/deid_poc/sets/kisim_1000/processed_kisim_1000 --annotation-mapping ../configs/annotation_mapping_kisim_usz.txt --structured-fields-config ../configs/structured_annotations_kisim_usz.txt -t 20"

run-reports-train:
	cd deidentifier-pipeline; mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c ../de_pipeline.conf -i /home/marczim/data/deid_poc/sets/train100/clean -o /home/marczim/data/deid_poc/sets/train100/processed -t 16 --annotation-mapping ../configs/annotation_mapping_first_batch.txt"

run-reports-train-full:
	cd deidentifier-pipeline; MAVEN_OPTS="-Xmx32G" mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c ../de_pipeline.conf -i /home/marczim/data/deid_poc/sets/train/clean -o /home/marczim/data/deid_poc/sets/train/processed -t 20 --annotation-mapping ../configs/annotation_mapping_first_batch.txt"

SEP=','
run-full-eval:
	cd deidentifier-pipeline; \
    cat ../eval/corpora_desc.csv | sed 1d | grep -v manual | while read l; do \
    batch=$$(echo "$${l}" | cut -d $(SEP) -f 2); \
    options=$$(echo "$${l}" | cut -d $(SEP) -f 3); \
    echo "Running $${batch} ....."; \
    mvn -e exec:java $(PIPELINE_COMMON_ARGS) -Dexec.args="-c $(CONFIG_PATH) -o /home/marczim/data/deid_poc/eval_runs/corpora/$${batch} --diagnostics-dir /home/marczim/data/deid_poc/eval_runs/diagnostics/$${batch} -m /data/share/$${batch} -t 25 $${options}" ; \
    python ../scripts/convert_ml_features_to_parquet.py /home/marczim/data/deid_poc/eval_runs/diagnostics/$${batch}/ml-features.json ; \
    done

UF=deployment/usage.txt.tmp
usage-readme:
	test ! -e $(UF) || rm $(UF)
	for c in annotate substitute import 'diagnostics conversioncheck' test; do java -jar ./deidentifier-pipeline/target/deidentifier-0.1-SNAPSHOT.jar $${c} -h >> $(UF) 2>&1 && echo -e "\n\n\n" >> $(UF); done
	grep -v Missing $(UF) >> deployment/$$(basename $(UF) .tmp)
	rm $(UF)

ZIP_FILE="snapshots/deid_snapshot_$$(date +%Y%m%d%H%M).zip"
STAGING=deid_usz
zip-distribution:
	test ! -e $(STAGING) || rm -r $(STAGING)

	[ -d $(STAGING) ] || mkdir -p $(STAGING)

#	mkdir -p "$(STAGING)/configs/kisim-usz"
	mkdir -p "$(STAGING)/lib"
	cp -r configs/kisim-usz $(STAGING)/
	cp -r deployment/* $(STAGING)

	unix2dos -n scripts/example_pipeline.ps1 $(STAGING)/example_pipeline.ps1
	cp deidentifier-pipeline/target/deidentifier-0.1-SNAPSHOT.jar $(STAGING)/lib

	unix2dos $(STAGING)/*.txt $(STAGING)/*.bat

	find ${STAGING} -name '*~' -exec rm {} \;
	zip -r $(ZIP_FILE) $(STAGING)
#	cp $(ZIP_FILE) $TODO copy to share and link with latest?

# lates tlink
ZIP_LATEST="snapshots/deid_snapshot_latest.zip"
update-latest:
	test ! -e $(ZIP_LATEST) || rm $(ZIP_LATEST)
	ln -s $$(pwd)/$$(ls -t snapshots/*.zip | head -n 1) $$(pwd)/$(ZIP_LATEST)

deployment: clean compile test jar run-de-jar zip-distribution update-latest

run-tutorial:
	java -jar deidentifier-pipeline/target/deidentifier-0.1-SNAPSHOT.jar test configs/tutorial/tutorial.conf configs/tutorial/testcases
