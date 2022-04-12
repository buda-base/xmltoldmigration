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
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;

public class NSITest {
    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testNSI() throws IOException, CsvValidationException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/nsitest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        line = reader.readNext();
        Model workModel = MigrationTest.mergeResources(NSITransfer.getResourcesFromLine(line));
        //workModel.write(System.out, "TTL");
        Model correctModel = MigrationHelpers.modelFromFileName("src/test/ttl/nsitest.ttl");
        
        assertTrue(MigrationHelpers.isSimilarTo(workModel, correctModel) );
    }
}
