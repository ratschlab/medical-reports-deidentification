# Code Overview

A brief overview over the software architecture and design of the tool, s.t. it
becomes easier to navigate the code base and understand how the different parts
work together.

The tool is a command line application written in Java 8. Its most important
dependency is the GATE NLP framework.

## Package Overview

Largest part of the Java code are in the package `org.ratschlab.deidentifier`:
 * `annotation`: Language analyzers (steps in the annnotation pipeline)
 * `dev`: Small tools for sanity checks during development. Not used in production
 * `pipelines`: Constructing and testing GATE pipelines for deidentifiation
 * `sources`: Handling sources of reports. Currently only reports from KISIM in JSON format
 * `substitution`: implementation of different strategies to substitute
   annotated tokens
 * `utils`: common utility functions
 * `workflows`: 
 
## NLP Pipeline Implementation

Annotating tokens to be deidentified happens over several steps forming a
pipeline. A GATE annotation pipeline (`SerialAnalyserController`) consists of
sequence of `LanguageAnalyser`s. A document is passed along every analyser and
modified accordingly (e.g. by adding certain annotations).
Note, that many concepts in GATE are represented by annotations, including
tokens and sentences.

The pipelines are constructed in a `PipelineFactory` in the
`org.ratschlab.deidentifier.pipelines` package. It adds analyser steps based on
the pipeline configuration. Many analysers could be reused from GATE or GATE
plugins, notably tokenization, sentence splitting and lexica (gazeteer)
annotations and JAPE rule engine.


## Aspects

### Configuration

Pipeline configurations are managed using a configuration file in [HOCON
format](https://github.com/lightbend/config).
A pipeline configuration file include paths to relevant files containing lexica,
specific JAPE rules, field lists for structured annotation etc


### Parallelization

Documents can be processed in parallel by having several instances of the
annotation (or substitution) pipeline running in parallel.

This is managed by an Akka Stream compute flow/graph, which consist of the following parts:
 * reading a document from a source (file or DB) and convert it to a GATE
   document structure
 * distributing the document to one of the annotation pipeline instance and
   annotate it
 * post process single doc: e.g. write to database or file
 * post process corpus: e.g. write doc stats to a single file, evaluate corpus

All these steps happen in a concurrent and asynchronic manner.
 

### Testing

Code is tested using tests written in JUnit. Additionally, there is a testing
framework to test annotations. See `Testcases` section in the components.md.

