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
        List<Resource> resources = HodgsonTransfer.getResourcesFromLine(line);
        System.out.println("RES 0 >>> "+resources);
        // work
        Model workModel = resources.get(0).getModel();
        //workModel.write(System.out, "TTL");
        Model correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/hodgsontest.ttl");
 
        assertTrue( MigrationHelpers.isSimilarTo(workModel, correctModel) );
        //assertTrue( CommonMigration.rdfOkInOntology(workModel, MigrationHelpers.getOntologyModel()) );
        //
        Model itemModel = resources.get(1).getModel();
        //itemModel.write(System.out, "TTL");
        correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/hodgsontest-item.ttl");

        assertTrue( MigrationHelpers.isSimilarTo(itemModel, correctModel) );
        //assertTrue( CommonMigration.rdfOkInOntology(itemModel, MigrationHelpers.getOntologyModel()) );
    }
}
