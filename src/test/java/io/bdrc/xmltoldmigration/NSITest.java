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

public class NSITest {
    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testNSI() throws IOException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/nsitest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        line = reader.readNext();
        List<Resource> resources = NSITransfer.getResourcesFromLine(line);
        // work
        Model workModel = resources.get(0).getModel();
        //MigrationHelpers.modelToOutputStream(workModel, System.out, "work", MigrationHelpers.OUTPUT_STTL, null);
        Model correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/nsitest.ttl");
        
        assertTrue( MigrationHelpers.isSimilarTo(workModel, correctModel) );
        //assertTrue( CommonMigration.rdfOkInOntology(workModel, MigrationHelpers.getOntologyModel()) );
        //
        Model itemModel = resources.get(1).getModel();
        //MigrationHelpers.modelToOutputStream(itemModel, System.out, "item", MigrationHelpers.OUTPUT_STTL, null);
        correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/nsitest-item.ttl");  
        assertTrue( MigrationHelpers.isSimilarTo(itemModel, correctModel) );
        //assertTrue( CommonMigration.rdfOkInOntology(itemModel, MigrationHelpers.getOntologyModel()) );
    }
}
