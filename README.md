# Migrate from TBRC XML files to BDRC RDF LD

This repository contains code that will be used to migrate from TBRC XML files to the new Linked Data.

## Compiling and running

Compiling and generating jar file:

```
mvn compile assembly:single
```

Simple run:

```
mvn exec:java -Dexec.mainClass=io.bdrc.xmltoldmigration.App
```

Running the jar file:

```
java -jar target/xmltoldmigration-0.1-jar-with-dependencies.jar
```

## License

The code is Copyright 2017 Buddhist Digital Resource Center, and is provided under [Apache License 2.0](LICENSE).