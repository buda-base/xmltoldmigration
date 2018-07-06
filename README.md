# Migrate from TBRC XML files to BDRC RDF LD

This repository contains code that will be used to migrate from TBRC XML files to the new Linked Data.

## Compiling and running

Simple run:

```
mvn compile exec:java
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
-onlyOneSymetricDirection=X (where X is 0 or 1, defaults to 0, use one direction in symetric statements)
-preferManyOverOne=X        (where X is 0 or 1, defaults to 0, when only one symetric direction is taken, prefer things like workHasPart instead of workPartOf)
```

## TODO

- add name in the volumes? (extracted from outlines)
- check that children cannot contain cycles, Children must be born after the parent, deathDate must be after birthDate ([source](https://www.w3.org/TR/shacl-ucr/#dfn-uc23))
- check if description@type=content and description@type=summary appear in the same node
- validate RIDs before making URLs

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
