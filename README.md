# Migrate from TBRC XML files to BDRC RDF LD

This repository contains code that will be used to migrate from TBRC XML files to the new Linked Data.

## Compiling and running

Compiling and generating jar file:

```
mvn compile assembly:single
```

Simple run:

```
mvn exec:java -Dexec.mainClass=io.bdrc.xmltoldmigration.MigrationApp
```

Running the jar file:

```
java -jar target/xmltoldmigration-0.1-jar-with-dependencies.jar
```

## TODO

- map all nodes of O5TAX003 to topics, to map place/event/affiliation/@rid="lineage:*"
- build some euristics to guess if an ascii string is Tibetan or English (for pubinfo properties mostly)
- add licenses as individuals?
- handle BDRC staff by some individuals of a new class?
- check if the "onDisk" description type appear outside imagegroup
- migrate some GIS id (WB_area_sq_km, etc.) to descriptions
- migrate most description to more specific properties (Kangyur references, etc.)

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
