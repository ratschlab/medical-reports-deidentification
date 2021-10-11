A pipeline to deidentify clinical texts/reports in German.

The pipeline code was developed for the needs of the [University Hospital Zurich](https://www.usz.ch/fachbereich/clinical-trials-center/angebote/research-data-service-center/)
can however be adapted fairly easily for other hospitals contexts. It takes reports in [JSON format](overview.md#document-import) and
in a first step annotates identifying information such as names, locations, age, dates, organisations and occupations.
In a second step, the annotated text parts are substituted with
some other text, where different [strategies](docs/overview.md#subst_policies) can be applied and reexported to JSON.

The pipeline to recognize identifying information is "rule" based, that is [various lexica](docs/lexica.md) and
[deterministic rules](docs/overview.md#jape_example) are used. It is based on the [GATE framework](https://gate.ac.uk/).

Some important features:
 * parallel execution to scale to large corpora of reports (>100'000 reports)
 * test suite to test annotation pipeline, also accessible to non-software developers tuning rules and lexica
 * large parts of pipeline tuning can be done without writing code
 * annotations contain information to trace back which pipeline step or rule generated the annotation


## Installation

* [Installation and running instructions](docs/installation.md)

## Pipeline Details

* [Pipeline overview](docs/overview.md)
* [About lexica](docs/lexica.md)
* [Pipeline Evaluation Results](docs/usz_pipeline_evaluation.md)


## Adapting the Pipeline

The tool is laid out such that it can be relatively easily adapted to another hospital or another context.
Adaptation to another language is possible in principle, may incur more work though.

* More details about [Pipeline components and their configuration](docs/components.md)
* [Description of the rules and how to tune them](docs/rules_and_tuning.md)
* [Tuning tutorial](docs/tuning_tutorial.md) with a heavily simplified pipeline


## Other Functionalities

For simplicity, some more functionality has been included into this code base which is not related to deidentification per se, but
still to working with medical reports.

### Diagnosis Extraction Pipeline

* [Diagnosis Extraction Pipeline](docs/structuring.md)


## Development

* [Notes on Development](docs/development.md)
* [Code overview](docs/code_overview.md)
