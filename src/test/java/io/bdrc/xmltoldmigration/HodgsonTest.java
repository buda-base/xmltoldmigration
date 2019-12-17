package io.bdrc.xmltoldmigration;

import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.BeforeClass;
import org.junit.Test;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class HodgsonTest {
    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testHodgson() throws IOException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/hodgsontest.csv"))
                    .withCSVParser(parser)
                    .build();
        reader.readNext();
        String[] line = reader.readNext();
        Model workModel = MigrationTest.mergeResources(HodgsonTransfer.getResourcesFromLine(line));
        //workModel.write(System.out, "TTL");
        Model correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/hodgsontest.ttl");
 
        assertTrue( MigrationHelpers.isSimilarTo(workModel, correctModel) );
        //assertTrue( CommonMigration.rdfOkInOntology(workModel, MigrationHelpers.getOntologyModel()) );
    }
}
