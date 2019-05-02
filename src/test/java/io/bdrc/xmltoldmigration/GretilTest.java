package io.bdrc.xmltoldmigration;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.BeforeClass;
import org.junit.Test;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;

public class GretilTest {

    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testGretil() throws IOException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/gretiltest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        List<Resource> res= GRETILTransfer.getResourcesFromLine(line);
        reader.close();
        Model workModel = res.get(0).getModel();
        Model correctModel = ModelFactory.createDefaultModel();
        correctModel.read(new FileInputStream("src/test/ttl/gretiltest.ttl"), null,"TTL");
        
        // ==== TEMP DEBUG ====
        workModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/GRETIL_TEST-testGretil-workModel.ttl"), "TTL");
        correctModel.write(new FileWriter("/Users/chris/BUDA/NEW_MIGRATION_TESTING/GRETIL_TEST-testGretil-correctModel.ttl"), "TTL");
        
        assertTrue( workModel.isIsomorphicWith(correctModel) );
    }

}
