## Installation

### Get Software and Lexica 

Download the release zip from some appropriate place.

### Adapt Database Configuration

If reports are being processed from a database a configuration file needs to be created specifying host, username, password table etc.
You can find more details for in the `DB Configuration` section in `components.md`.

In the following we'll assume the file is called `db_conf.txt`.

Here an example, how the file could look like:

```
jdbc_url=jdbc:sqlserver://myhost:2345;databaseName=MyDB;
user=deid_poc
password=1asdffea
query=SELECT DAT,FALLNR,CONTENT,FCODE,REPORTNR FROM MyDB.KISIM_KIS_T_REPORT_JSON.KIS_T_REPORT_JSON
json_field_name=CONTENT
reportid_field_name=REPORTNR
report_type_id_name=FCODE
date_field_name=DAT

# for writing back
dest_table=subst_test
dest_columns=CONTENT,REPORTNR,FCODE,DAT,FALLNR
```

Reports are read from the `KISIM_KIS_T_REPORT_JSON` table of the `MyDB` mssql database. Substituted reports are written back into the table `subst_test` into the column `CONTENT` and the columns `REPORTNR,FCODE,DAT,FALLNR` are just copied over as is.

For a postgres DB the JDBC URL would start with `jdbc:postgresql://...`. Other databases are supported in principle, but the corresponding JDBC driver needs to be made available (see `Adapt Pipeline Script` section).


### Adapt Pipeline Script

There is an example script for a deidentification pipeline under `scripts/example_pipeline.ps1`.

There are a few variables at the beginning of the file (mostly paths),  which need to be tweaked.

The script imports reports from a database,  annotates them and substitutes the reports and writes them back into an appropriate database table.
Note, that intermediate data is stored in the base directory, s.t. it can be inspected using the `GATE Developer` GUI tool.


### Run

The above script can simply be run in a PowerShell. Note, that depending on the JDBC driver (mssql being one example) it is important that the driver is compatible with the Java version.
