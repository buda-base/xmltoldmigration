package io.bdrc.xmltoldmigration;

import java.io.FileReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

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
        while(line!=null) {
            GRETILTransfer.getResourcesFromLine(line);
            line = reader.readNext();
        }
    }

}
