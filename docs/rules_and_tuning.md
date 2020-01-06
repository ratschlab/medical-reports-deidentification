## Rules Guide and Tuning

This section describes how the rules are set up as well as their current
limitations. Also, some typical tuning
scenarios are described. It is assumed the reader is familiar with the
`components.md` section.


### General Considerations

In general, we need to balance between annotating as many relevant tokens as
possible and but not annotating "too much". That is, we'd like to have a high
recall (few false negatives), but still maintain a reasonable precision (few
false positives), otherwise the data quality of
downstream applications may suffer.

Typically, annotations carry attributes to be able to trace back to the rule
generating it. This is very useful for debugging.

There are also a few "negativ rule", i.e. rules which "consume" tokens but don't
trigger annotations. This is typically to avoid false positives. An example are
citations like `Meier et al.`, where `Meier` should not be annotated as surname
to be deidentified.

### Date Annotation

Dates are a fairly closed category, that is, as long as most patterns are
captured in the rules how dates can be written, the annotation works well.

There is one a bit more challenging pattern where the year as well as the last
`.` is missing like in `10.1` meaning 10th of January. Some care needs to be
taken to correctly distinguish such dates from decimal numbers (which are e.g.
followed by some unit).

Remaining issues are mainly due to misspellings of dates such as `16.11.2918` or
`21.111.2018` or `20.102015`. Although to a human reader it is clear what date
is actually meant, it is not straightforward to capture these in patterns. This
could be addressed in future extensions.

### Name Annotation

Name annotations are driven by triggers such as titles like `Dr.` or names from
lexika. That is, tokens like `Dr.`, `Frau`, `Prof` etc are most of the time
followed by a name. Some rules exploit this fact.

Names not preceded by such trigger tokens are recognized using lexica. Note,
that blindly annotating tokens appearing in name lexica will lead to many false
positives, e.g. `Iris` may be annotated as name although the part of the eye is
meant. Hence, some more "evidence" is needed, that a name candidate is indeed a
name. This includes:
 * token is followed/preceded by another token appearing in a name lexicon
 * token is a frequent name and the token doesn't appear in any medical or general
   lexica
   
This approach requires lexica of good quality, i.e. they should be reasonably
complete and not contain tokens which are not actually names (may be problematic
if lexica directly compiled from a hospital database system)

Special care is also taken to not annotate citations such as `Meier et al.`


#### Shorthands/Abbreviations

Shorthands or abbreviations of hospital staff also fall in the name category. At
USZ staff shorthands are typically 5 characters long (although the length ranges
from 3 to 8 characters) and most of the time spelled in upper case, such as
`ABCDE`.

Since shorter abbreviations of 3 or 4 characters may also simply be a (medical)
acronym, the annotation of such strings is only triggered in certain fields.
Some report specific rules were needed for some report types where the
shorthands are spelled in small case.


### Location Annotation

This is a fairly broad category encompassing physical locations such as places
and countries (by extension also languages) as well as organizations such as
hospitals, departments within hospitals, medical practices etc.

`Location` annotations heavily depend on good lexica. There are a few rules to
recognize street names and zip codes. Also many health care organizations can be
recognized by certain triggers like `Spital`, `Altersheim` etc. These triggers
are also important to recognize organizations which may be in some lexica, but
are referred to differently by the medical staff (e.g. `Altersheim XY` instead
of the official `Pflegezentrum XY`).

Challenges arise with ambiguous locations such as `Wangen` (place and "cheeks")
as well as with misspellings `Grüningen` vs `Grünigen`, `**thal` vs `*tal`.

### Occupation Annotation

Both patterns and lexica are important to recognize occupations. Similarly
with names, we cannot blindly annotate tokens which appear in a occupation
dictionary, since the token may designate for example a surname or the
profession or role of somebody involved with the patient, but not the patient
him/herself (e.g. `beim Augenarzt`, `Polizist`).


### Tuning Guidelines

In case a token should have been annotated by the pipeline and it wasn't, the
following steps could be taken:
 * first (!) add a test case to the test suite. Perhaps don't take the original data for
    privacy reasons but slightly alter it to reflect the situation. Verify the
    test case fails (`make test`).
 * if appropriate, add more tokens to corresponding lexicon
 * if appropriate, tweak existing rule or add a new rule
 * verify the test(s) passes now.


The same procedure can roughly be followed if a token was wrongly annotated.
In this case, perhaps entries need to be removed from a lexicon.


TODO elaborate more?
