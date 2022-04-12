package io.bdrc.xmltoldmigration;

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.BeforeClass;
import org.junit.Test;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;

public class GretilTest {

    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testGretil() throws IOException, CsvValidationException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/gretiltest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        Model workModel = MigrationTest.mergeResources(GRETILTransfer.getWorkFromLine(line));
        reader.close();
        //workModel.write(System.out, "TTL");
        Model correctModel = ModelFactory.createDefaultModel();
        correctModel.read(new FileInputStream("src/test/ttl/gretiltest.ttl"), null,"TTL");
        assertTrue( workModel.isIsomorphicWith(correctModel) );
    }

}
