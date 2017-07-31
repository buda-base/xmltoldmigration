# Migrate from TBRC XML files to BDRC RDF LD

This repository contains code that will be used to migrate from TBRC XML files to the new Linked Data.

## Compiling and running

Simple run:

```
mvn compile exec:java -Dexec.args=-useCouchdb
```

Compiling and generating jar file:

```
mvn clean package
```

If the above fails try:

```
git submodule update --init
```

Running the jar file:

```
java -jar target/xmltoldmigration-0.2.0.jar [-useCouchdb] [-datadir <dir path>] [-outdir <dir path>]
```

All args are optional. By default the jsonld files are written to `./tbrc-jsonld/`. If the `-outdir` is present it must end with `'/'`. Similarly, by default the data is read from `./tbrc/`, and if the `-datadir` is present it must be terminated with `'/'`. Finally, by default the files are not loaded into couchdb so if one wants to upload to couchdb then `-useCouhdb` needs to be supplied.

## TODO

- Outline nodes labels are based on names, but when absent, it may need to be based on titles?
- make sure the vol info is present in outline locations when a work has more than 1 volume. Maybe consider that default volume value is 1? In outlines, remove locationWork (always the same, obviously)?
- link the volumes to the work?
- add name in the volumes? (extracted from outlines)
- check that children cannot contain cycles, Children must be born after the parent, deathDate must be after birthDate ([source](https://www.w3.org/TR/shacl-ucr/#dfn-uc23))
- pubinfo properties' lang/encoding tags have a difficult history and should be fixed. Some euristics should be found to apply them correctly
- transfer places marked as provisional, like G9GBX33010...
- ignore `work/info@parent=*LEGACY` like in W2DB4598
- check if description@type=content and description@type=summary appear in the same node
- validate RIDs before making URLs
- add place and code for FPL library (G1TLMFPL000001)
- add code to sort log entries, lineageHolders, labels?


## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
