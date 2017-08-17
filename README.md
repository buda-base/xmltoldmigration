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
java -jar target/xmltoldmigration-0.2.0.jar
```

#### Arguments

```
-outdir OUTDIR              (defaults to ./tbrc-jsonld/, must end with /)
-datadir DATADIR            (defaults to ./tbrc/, must end with /)
-useCouchdb                 (use couchDB, by default it doesn't)
-onlyOneSymetricDirection=X (where X is 0 or 1, defaults to 0, use one direction in symetric statements)
-preferManyOverOne=X        (where X is 0 or 1, defaults to 0, when only one symetric direction is taken, prefer things like workHasPart instead of workPartOf)
```

## TODO

- make sure the vol info is present in outline locations when a work has more than 1 volume. Maybe consider that default volume value is 1? In outlines, remove locationWork (always the same, obviously)?
- link the volumes to the work?
- add name in the volumes? (extracted from outlines)
- check that children cannot contain cycles, Children must be born after the parent, deathDate must be after birthDate ([source](https://www.w3.org/TR/shacl-ucr/#dfn-uc23))
- pubinfo properties' lang/encoding tags have a difficult history and should be fixed. Some euristics should be found to apply them correctly
- transfer places marked as provisional, like G9GBX33010...
- check if description@type=content and description@type=summary appear in the same node
- validate RIDs before making URLs


## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
