# Usage

(currently semi-automatically generated)



```

Usage: Deid annot [--train] [--diagnostics-dir=<diagnosticsDirPath>]
                  [--doc-id-filter=<docIdFilterPath>]
                  [--doc-type-filter=<docTypeFilterPath>]
                  [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                  -c=<propertiesFile> [-d=<databaseConfigPath>]
                  [-i=<corpusInputDirPath>] [-m=<markedCorpusDirPath>]
                  -o=<corpusOutputDir> [-t=<threads>]
Annotate Corpus
      --diagnostics-dir=<diagnosticsDirPath>
                             Marked Corpus Dir
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
      --train                Train Model
  -c=<propertiesFile>        Config file
  -d=<databaseConfigPath>    DB config path
  -i=<corpusInputDirPath>    Input corpus dir
  -m=<markedCorpusDirPath>   Marked Corpus Dir
  -o=<corpusOutputDir>       Output corpus dir
  -t=<threads>               Number of threads




Usage: Deid subst [--keep-case-ids] [--keep-patient-ids]
                  [--annotation-name=<finalAnnotationName>]
                  [--doc-type-filter=<docTypeFilterPath>]
                  [--fields-blacklist=<fieldsBlacklistPath>]
                  [--max-days-shift=<maxDaysShift>] --method=<substMethod>
                  [--min-days-shift=<minDaysShift>] --rnd-seed=<rngSeed>
                  [-t=<threads>] <annotatedCorpusDir> <outputDir>
Apply Substitution to Annotated Corpus
      <annotatedCorpusDir>   Corpus Dir
      <outputDir>            Output Dir
      --annotation-name=<finalAnnotationName>
                             Annotation set name
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --fields-blacklist=<fieldsBlacklistPath>
                             Path to files giving field blacklist
      --keep-case-ids        don't substitute case IDs
      --keep-patient-ids     don't substitute patient IDs
      --max-days-shift=<maxDaysShift>
                             Maximum number of days to shift
      --method=<substMethod> Substitution Method
      --min-days-shift=<minDaysShift>
                             Minimum number of days to shift
      --rnd-seed=<rngSeed>   Random seed
  -t=<threads>               Number of threads




Usage: Deid import [--doc-id-filter=<docIdFilterPath>]
                   [--doc-type-filter=<docTypeFilterPath>]
                   [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                   [-d=<databaseConfigPath>] -o=<corpusOutputDir>
Import reports from database
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
  -d=<databaseConfigPath>    DB config path
  -o=<corpusOutputDir>       Output corpus dir




Usage: Deid annot [--train] [--diagnostics-dir=<diagnosticsDirPath>]
                  [--doc-id-filter=<docIdFilterPath>]
                  [--doc-type-filter=<docTypeFilterPath>]
                  [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                  -c=<propertiesFile> [-d=<databaseConfigPath>]
                  [-i=<corpusInputDirPath>] [-m=<markedCorpusDirPath>]
                  -o=<corpusOutputDir> [-t=<threads>]
Annotate Corpus
      --diagnostics-dir=<diagnosticsDirPath>
                             Marked Corpus Dir
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
      --train                Train Model
  -c=<propertiesFile>        Config file
  -d=<databaseConfigPath>    DB config path
  -i=<corpusInputDirPath>    Input corpus dir
  -m=<markedCorpusDirPath>   Marked Corpus Dir
  -o=<corpusOutputDir>       Output corpus dir
  -t=<threads>               Number of threads




Usage: Deid subst [--keep-case-ids] [--keep-patient-ids]
                  [--annotation-name=<finalAnnotationName>]
                  [--doc-type-filter=<docTypeFilterPath>]
                  [--fields-blacklist=<fieldsBlacklistPath>]
                  [--max-days-shift=<maxDaysShift>] --method=<substMethod>
                  [--min-days-shift=<minDaysShift>] --rnd-seed=<rngSeed>
                  [-t=<threads>] <annotatedCorpusDir> <outputDir>
Apply Substitution to Annotated Corpus
      <annotatedCorpusDir>   Corpus Dir
      <outputDir>            Output Dir
      --annotation-name=<finalAnnotationName>
                             Annotation set name
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --fields-blacklist=<fieldsBlacklistPath>
                             Path to files giving field blacklist
      --keep-case-ids        don't substitute case IDs
      --keep-patient-ids     don't substitute patient IDs
      --max-days-shift=<maxDaysShift>
                             Maximum number of days to shift
      --method=<substMethod> Substitution Method
      --min-days-shift=<minDaysShift>
                             Minimum number of days to shift
      --rnd-seed=<rngSeed>   Random seed
  -t=<threads>               Number of threads




Usage: Deid import [--doc-id-filter=<docIdFilterPath>]
                   [--doc-type-filter=<docTypeFilterPath>]
                   [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                   [-d=<databaseConfigPath>] -o=<corpusOutputDir>
Import reports from database
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
  -d=<databaseConfigPath>    DB config path
  -o=<corpusOutputDir>       Output corpus dir




Usage: Deid annotate [--xml-input] [--diagnostics-dir=<diagnosticsDirPath>]
                     [--doc-id-filter=<docIdFilterPath>]
                     [--doc-type-filter=<docTypeFilterPath>]
                     [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                     -c=<propertiesFile> [-d=<databaseConfigPath>]
                     [-i=<corpusInputDirPath>] [-m=<markedCorpusDirPath>]
                     -o=<corpusOutputDir> [-t=<threads>]
Annotate Corpus
      --diagnostics-dir=<diagnosticsDirPath>
                             Marked Corpus Dir
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
      --xml-input            Assumes input dir consists of xml files, one per report
                               (testing purposes)
  -c=<propertiesFile>        Config file
  -d=<databaseConfigPath>    DB config path
  -i=<corpusInputDirPath>    Input corpus dir
  -m=<markedCorpusDirPath>   Marked Corpus Dir
  -o=<corpusOutputDir>       Output corpus dir
  -t=<threads>               Number of threads




Usage: Deid substitute [--keep-case-ids] [--keep-patient-ids]
                       [--annotation-name=<finalAnnotationName>]
                       [--db-config=<databaseConfigPath>]
                       [--doc-type-filter=<docTypeFilterPath>]
                       [--fields-blacklist=<fieldsBlacklistPath>]
                       [--max-days-shift=<maxDaysShift>] --method=<substMethod>
                       [--min-days-shift=<minDaysShift>] [--rnd-seed=<rngSeed>]
                       [-o=<outputDir>] [-t=<threads>] <annotatedCorpusDir>
Apply Substitution to Annotated Corpus
      <annotatedCorpusDir>   Corpus Dir
      --annotation-name=<finalAnnotationName>
                             Annotation set name
      --db-config=<databaseConfigPath>
                             DB config path
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --fields-blacklist=<fieldsBlacklistPath>
                             Path to files giving field blacklist
      --keep-case-ids        don't substitute case IDs
      --keep-patient-ids     don't substitute patient IDs
      --max-days-shift=<maxDaysShift>
                             Maximum number of days to shift
      --method=<substMethod> Substitution Methods: DateShift, ReplacementTags,
                               Scrubber, Identity
      --min-days-shift=<minDaysShift>
                             Minimum number of days to shift
      --rnd-seed=<rngSeed>   Random seed
  -o=<outputDir>             Output Dir
  -t=<threads>               Number of threads




Usage: Deid import [--doc-id-filter=<docIdFilterPath>]
                   [--doc-type-filter=<docTypeFilterPath>]
                   [--max-docs=<maxDocs>] [--skip-docs=<skipDocs>]
                   [-d=<databaseConfigPath>] -o=<corpusOutputDir>
Import reports from database
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
  -d=<databaseConfigPath>    DB config path
  -o=<corpusOutputDir>       Output corpus dir




Usage: Deid diagnostics conversioncheck [-hV]
                                        [--doc-id-filter=<docIdFilterPath>]
                                        [--doc-type-filter=<docTypeFilterPath>]
                                        [--max-docs=<maxDocs>]
                                        [--skip-docs=<skipDocs>]
                                        [-d=<databaseConfigPath>] -o=<outfile>
Roundtrip test between JSON <--> GATE format
      --doc-id-filter=<docIdFilterPath>
                             Path to file id list to consider
      --doc-type-filter=<docTypeFilterPath>
                             Path to file type list to consider
      --max-docs=<maxDocs>   Maximum number of docs to process
      --skip-docs=<skipDocs> Skipping number of docs (useful to just work on a slice
                               of the corpus)
  -d=<databaseConfigPath>    DB config path
  -h, --help                 Show this help message and exit.
  -o=<outfile>               Writing out error cases
  -V, --version              Print version information and exit.




Usage: Deid test <pipelineConfigFile> <testCasesDirectory>
Tests a pipeline
      <pipelineConfigFile>   Pipeline Configuration File
      <testCasesDirectory>   Testcases Directory

```


