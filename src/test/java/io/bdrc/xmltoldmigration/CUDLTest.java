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

public class CUDLTest {

    @BeforeClass
    public static void init() throws NoSuchAlgorithmException {
        SymetricNormalization.normalizeOneDirection(false, false);
    }

    @Test
    public void testCUDL() throws IOException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/cudltest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        List<Resource> res= CUDLTransfer.getResourcesFromLine(line);
        reader.close();
        Model workModel = res.get(0).getModel();
        Model model = ModelFactory.createDefaultModel();
        model.read(new FileInputStream("src/test/ttl/cudltest.ttl"), null,"TTL");
        
        workModel.write(new FileWriter("/Users/chris/CUDLTest-workModel.ttl"), "TTL");
        model.write(new FileWriter("/Users/chris/CUDLTest-model.ttl"), "TTL");

        assertTrue( workModel.isIsomorphicWith(model) );
    }

}
