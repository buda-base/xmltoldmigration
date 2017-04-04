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
java -jar target/xmltoldmigration-0.2.0.jar -useCouchdb
```

## TODO

- map all nodes of O5TAX003 to topics, to map place/event/affiliation/@rid="lineage:*"
- build some euristics to guess if an ascii string is Tibetan or English (for pubinfo properties mostly)
- add licenses as individuals?
- handle BDRC staff by some individuals of a new class?
- check if the "onDisk" description type appear outside imagegroup
- migrate some GIS id (WB_area_sq_km, etc.) to descriptions
- migrate most description to more specific properties (first batch done, more to be done later)
- some properties have a useless lang tag: gbdist, gonpaPer1000, etc.
- https://www.tbrc.org/xmldoc?rid=O5TAX003 has two "TaklungKagyu" entries, does it matter?


## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
