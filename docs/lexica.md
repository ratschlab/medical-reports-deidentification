
## Lexica

In GATE, a lexicon (or gazetteer) consists of a text file with one term per
line. A term may contain spaces. The terms are treated as case-sensitive.

The lexica compiled for the USZ pipeline have many different sources.
Below tables describing the different lexica along with their provenance.
Note, that some lexica cannot be published, as they contain hospital internal data.
Note, that `ANNIE` refers to a GATE plugin, `GeoNames` to the geographical
database which can be found here `https://www.geonames.org/`.


### General


|File Name                             |Source                                                      |Description                                                 |
|--------------------------------------|------------------------------------------------------------|------------------------------------------------------------|
|abbreviations_stop.lst                | ANNIE German                                               | Abbreviations like `z.B`                                   |
|general_wordlist_with_uppercased.lst  | Aspell dictionary                                          | General wordlist                                           |
|stop.lst                              | ANNIE German                                               | Stopwords                                                  |



### Locations

#### Geographical

| File Name                       | Source                           | Description                                                                                                                                    |
|---------------------------------|----------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|
| additional_locations.lst        | Manually edited                  |                                                                                                                                                |
| canton_names.lst                | GeoNames and manually added      | "Uri"                                                                                                                                          |
| cantons_abbrevs.lst             | GeoNames and manually added      | "ZH", "BE"                                                                                                                                     |
| citizenships.lst                | Wikipedia and manually added     | "Schweizer", "Deutsche"                                                                                                                        |
| city.lst                        | ANNIE                            | various cities (worldwide)                                                                                                                     |
| city_ambiguous_manual.lst       | Manually edited                  | Cities which typically have another meaning in the context of medical reports, e.g. `Wangen`, `Füssen`. No Location annotations are performed. |
| city_derived.lst                | ANNIE                            |                                                                                                                                                |
| city_german.lst                 | ANNIE                            |                                                                                                                                                |
| city_switzerland.lst            | Swisstopo Ortschaftenverzeichnis |                                                                                                                                                |
| country.lst                     | ANNIE                            |                                                                                                                                                |
| country_adjectives_german.lst   | Wikipedia and manually added     | "Italienisch", "Italienischer"                                                                                                                 |
| country_german.lst              | ANNIE                            |                                                                                                                                                |
| country_german_wiki.lst         | Wikipedia                        | Country Names                                                                                                                                  |
| country_iso_codes.lst           | Manual                           |                                                                                                                                                |
| country_manual.lst              | Manual                           | Contains countries not existing anymore                                                                                                        |
| country_regions_german_wiki.lst | Wikipedia                        | "Norditalien"                                                                                                                                  |
| languages_manual.lst            | Manual                           |                                                                                                                                                |
| larger_cities.lst               | GeoNames                         | Larger international cities, "Tripoli", "Hannover"                                                                                             |
| location_false_positives.lst    | Manual                           | Typically medical terms containing location as a part (not annotated)                                                                          |
| province.lst                    | ANNIE                            |                                                                                                                                                |
| regions.lst                     | Manual                           |                                                                                                                                                |
| streetnames.lst                 | Manual                           | Streets or similar not matching a typical pattern                                                                                              |
| toponyms_switzerland.lst        | GeoNames                         | All sorts of topopynms. Extensive blacklist was needed for ambiguous locations                                                                 |
| toponyms_switzerland_manual.lst | Manual                           | More Swiss Toponyms                                                                                                                            |


#### Organisational


| File Name                     | Source      | Description                                 |
|-------------------------------|-------------|---------------------------------------------|
| buildings_usz.lst             | USZ (KISIM) | Building abbreviations at USZ               |
| hospitals.lst                 | Manual      | Hospital names                              |
| institutions.lst              | Manual      | Institutions related to USZ                 |
| organisational_units_usz.lst  | USZ (KISIM) | Abbreviations of organisational units       |
| related_organisations_usz.lst | USZ (KISIM) | Institutions related to USZ. Internal only. |


### Medical

Including information about medical terms into the pipeline is mainly to avoid
an annotation on it, typically for surnames.


| File Name                | Source                       | Description                                                                                 |
|--------------------------|------------------------------|---------------------------------------------------------------------------------------------|
| drugs_usz.lst            | USZ (KISIM)                  |                                                                                             |
| medical_mesh_terms.lst   | MESH 2019 German Translation | https://www.dimdi.de/dynamic/en/classifications/further-classifications-and-standards/mesh/ |
| medical_terms_de.lst     | Wikipedia                    |                                                                                             |
| medical_terms_manual.lst | Manual                       |                                                                                             |


### Occupations

Professions and companies a patient might work for.

| File Name               | Source    | Description                                                    |
|-------------------------|-----------|----------------------------------------------------------------|
| company_list_ch.lst     | Wikipedia |                                                                |
| generic_occupations.lst | Manual    | More for testing purposes                                      |
| occupations_usz.lst     | USZ Kisim | Processed list of professions entered in KISIM. Internal only. |


### Person Names

A few lexica are partitioned into two parts with "frequent" and "seldom" names.
The distinction is used by some rules as indication whether a token might likely
be a name or not.


| File Name                           | Source                                                | Description         |
|-------------------------------------|-------------------------------------------------------|---------------------|
| firstnames_switzerland_frequent.lst | Bundesamt für Statistik: Vornamen in der Schweiz 2017 |                     |
| firstnames_switzerland_seldom.lst   | Bundesamt für Statistik: Vornamen in der Schweiz 2017 |                     |
| firstnames_usz_frequent.lst         | USZ (KISIM)                                           |                     |
| name_false_positives.lst            | Manual                                                | Often medical terms |
| surnames_usz_frequent.lst           | USZ (KISIM)                                           |                     |
| firstnames_usz_seldom.lst           | USZ (KISIM)                                           | Internal only       |
| surnames_staff_usz.lst              | USZ (KISIM)                                           | Internal only       |
| surnames_usz_seldom.lst             | USZ (KISIM)                                           | Internal only       |


### Suffix Lists

Instead of using lexica annotating terms 1:1 in the text, a part of the pipeline
annotates tokens based on suffixes. This is useful for missing terms in the
other dictionaries. For example `Nasenerkrankung` would still be recognized as
medical term, even though it might not be in any medical dictionary.


| File Name            | Source | Description             |
|----------------------|--------|-------------------------|
| medical_suffixes.lst | Manual | `erkrankung`,`geräusch` |
| surname_suffixes.lst | Manual | `mann`,`oulos`          |
