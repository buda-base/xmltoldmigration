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
- handle '.' in ewts converter (in g.yag for instance)

changed descriptions:
- outline: libraryOfCongress, extent, chapters, incipit, colophon
- outline/number: bon_bka_gyur_number, catalogue_number, gser_bris_number, lhasa_number, otani, otani_beijing, rKTsReference, sde_dge_number, shey_number, snar_thang_number, stog_number, toh, urga_number, vinayottaragrantha
- place: nameLex, nameKR, gbdist, town_syl, town_py, town_ch, prov_py, gonpaPerEcumen, gonpaPer1000, dist_py

changes types:
- all wor:pubinfoProperty now range xsd:string, except authorship statement, biblioNote, editionStatement, printery, publisherLocation, publisherName, seriesName, sourceNote
- all wor:locationDataProperty and vol:number passed to (xsd:string or xsd:positiveInteger)

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).
