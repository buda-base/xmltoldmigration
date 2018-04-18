package io.bdrc.xmltoldmigration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class EAPTest {

    @Test
    public void testEAP() throws IOException {
        // get the line
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(new FileReader("src/test/eaptest.csv"))
                    .withCSVParser(parser)
                    .build();
        String[] line = reader.readNext();
        List<Model> models = EAPTransfer.getModelsFromLine(line);
        MigrationHelpers.modelToOutputStream(models.get(0), System.out, "work", MigrationHelpers.OUTPUT_STTL, null);
    }
    
}
