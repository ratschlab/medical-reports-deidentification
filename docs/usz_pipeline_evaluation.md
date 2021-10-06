## Pipeline Evaluation

### Method and Corpus

400 reports from the University Hospital Zurich were picked at random across around 30 document types (mainly discharge reports) giving rise to around 2.7 Mio tokens. These reports then got annotated using
a prelimninary version of the pipeline. A medical student then went through these annotations
in GATE Developer and complemented/corrected the existing ones.
In a last step, the annotations got checked by a member of the USZ staff.
Then, the annotations of the mature version of the pipeline was compared to the verified annotations ("goldstandard") and evaluated.

The corpus was split into two parts with 200 reports each. The first part was heavily used for development and tuning ("training set") whereas the second
was only used for evaluation ("validation set"). No evaluation on a strictly unseen test set was performed.

A bit more details can be found in the [Evaluation Guide](evaluation.md).

### Results

The following tables contain Precision/Recall by annotation types comparing the pipeline output with the annotations in the goldstandard.
The `Recall` and `Precision` columns refer to exact matches of annotations, the columns `Recall Lenient` and `Precision Lenient` also include partial
matches, for instance, for the name `Hanna Meier Huber`, the pipeline only annotates `Hanna Meier`.

#### Entire Goldstandard Corpus (Parts I + II):

|Type|Matches|Recall|Recall Lenient|Precision|Precision Lenient|
|-------|----|---------|------------|----|---------|
|Age|716|94.584|94.584|95.086|95.086|
|Contact|4805|99.154|99.505|99.154|99.505|
|Date|47223|99.145|99.397|98.574|98.825|
|ID|187|79.915|85.897|3.166|3.403|
|Location|31427|85.300|91.415|84.309|90.353|
|Name|17422|98.787|99.484|94.561|95.229|
|Occupation|290|55.238|63.619|60.291|69.439|

#### Goldstandard Part I

|Type|Matches|Recall|Recall Lenient|Precision|Precision Lenient|
|-------|----|---------|------------|----|---------|
|Age|361|96.524|96.524|93.282|93.282|
|Contact|2384|99.375|99.625|98.962|99.211|
|Date|24686|99.252|99.481|98.673|98.901|
|ID|82|80.392|87.255|2.495|2.708|
|Location|16220|87.576|93.391|84.020|89.599|
|Name|9195|98.712|99.377|93.445|94.075|
|Occupation|157|60.153|68.966|62.800|72.000|

#### Goldstandard Part II

|Type|Matches|Recall|Recall Lenient|Precision|Precision Lenient|
|-------|----|---------|------------|----|---------|
|Age|355|92.689|92.689|96.995|96.995|
|Contact|2421|98.937|99.387|99.343|99.795|
|Date|22537|99.029|99.306|98.466|98.742|
|ID|105|79.545|84.848|4.009|4.276|
|Location|15207|82.999|89.417|84.620|91.164|
|Name|8227|98.870|99.603|95.841|96.552|
|Occupation|133|50.379|58.333|57.576|66.667|


### Annotation Issues Observed

#### Age

Issues with more complex sentence structures, such as

* `Brüder verstorben mit 77, 71 und 78 Jahren`
* `Sie sei eigentlich 49 und nicht 42 Jahre alt`

#### Contact

Some hospital internal phone numbers were not recognized.

#### Date

Some scores are erroneously recognized as dates.

* `Nutritional Assessment: 7/15, Faszikulationen an 10/10 Stellen`
* `Beginn: 1.2, Albuminquotient 13.4`

#### ID

The extremely low precision comes from the fact, that what is considered as IDs was changed. That is, some entities now
annotated by the pipeline are not annotated in the goldstandard.
For the recall, some model numbers were erroneously annotated in the gold standard

#### Location

The somewhat medium performance in the location category originates in the broad definition including place names and terms referring
to names of organisations or organisational units.
It is also not always clear whether terms like `Physiotherapie`, `Unfallchirurgie`, `Innere Medizin` and the like are part of an organization name or
are just generic medical terms not carrying any identifying meaning. This sort of ambiguous case make up the largest part

A more detailed look reveals, that the pipeline missed very few patient related location information, that is a 7 locations
outside Switzerland in Goldstandard Part I. Some 10 organisations were missed.

#### Name

Great care was taken to not miss names of patients or staff, i.e. the pipeline is tuned for a high recall. The comparatively low
precision is due to the fact, that some staff abbreviations were not annotated in the goldstandard corpus. Another problem are
that medical terms like `M. Scheuermann`, `Spina`, `Carina`, `Karina`, `B. Fieber` get annotated as names.

#### Occupation

This is a difficult category to annotate, since the way professions or occupations can be expressed are quite variable.
They typically occur in certain fields related to anamnesis. These can be excluded
using the `--fields-blacklist` option in the [substitute command](overview.md#substitution-policies) if the risk is too high.
