## Tuning Tutorial

Here few hands-on "exercises" on how to tune the pipeline on a heavily
simplified version of the USZ deidentification pipeline.
The goal is to get familiar with the various components in a simplified setting
without being overwhelmed by all the details of a full-fledged pipeline.

### Setup


To test whether modification of the pipeline lead to the desired behavior, we
are going to use the testing framework included in the deidentification tool.
Tests for many of the exercises below are already prepared in the
`configs/tutorial/testcases`, they just need to be uncommented (i.e. removing
the `#`)

The test suite can be run using the following command:
```
java -jar [path to jar file] test [pipeline config file] [test cases directory]

```

where `[path to jar file]` should point to a current `jar` file of the pipeline
(typically `deidentifier-*.jar`), `[pipeline config file]` to some path ending
with `configs/tutorial/tutorial.conf` and `[test cases directory]` a path ending on
`configs/tutorial/testcases`.
Tip: put the resulting long command into a `.bat` or
`.sh` file which you then execute.

When running the test suite, if everything goes well, you should see lines
containing `Reading testcases from` at the end. If a testcase should fail, a
clear error message is displayed with some more details what went wrong.

### Exercices

#### Add test case for dates

In the file `date.txt` there is already one test case defined to check whether a
date is indeed recognized. Uncomment that line and run the test suite. You
should see something like

```
2019-12-06 13:27:40.891 INFO  org.ratschlab.deidentifier.pipelines.testing.PipelineTestSuite - Reading testcases from configs/tutorial/testcases/date.txt
2019-12-06 13:27:40.895 INFO  org.ratschlab.deidentifier.pipelines.testing.PipelineTester - Running test suite with 1 test cases
```

On a new line add another date without the `<Date>` tags. Run the test suite
again and see how it fails. Add the tags, run again and this time the suite
should pass.


#### Add missing location

The location `Oberikon` is not recognized in a document. Add a test case for
it in the `locations.txt` file and run the test suite. It should fail on that test.
Then, add the place to some appropriate lexikon (e.g. in the already existing
`locations/additional_locations.lst`). After that, the test suite


#### Internal phone number format

Assume internal phone numbers consist of two blocks of 3 digits, e.g.:
`123 456`.
Write a JAPE rule which recognizes these numbers.
There is already some test case in `contact.txt`

Hints:
* add the rule in `specific-rules/contacts.jape`. There is already some rule
  recognizing some Swiss phone numbers (copied from the generic JAPE rule set).
* See https://gate.ac.uk/sale/thakker-jape-tutorial/GATE%20JAPE%20manual.pdf if
  you'd like to know more about how JAPE rules work.
* In practice, you would probably add a "trigger" on the left side, i.e. fire
  the rule only if the two blocks are preceded by a "Tel" token.
