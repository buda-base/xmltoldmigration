package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

public class FEMCTransfer {

    public static void transferFEMCWorks() {
        System.out.println("transfer FEMC works");
        CSVReader reader;
        CSVParser parser = new CSVParserBuilder().build();
        InputStream inputStream = EAPFondsTransfer.class.getClassLoader().getResourceAsStream("femc-works.csv");
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        try {
            for (String[] line: reader.readAll()) {
                Model mA = ModelFactory.createDefaultModel();
                setPrefixes(mA);
                Resource mainA = createRoot(mA, BDR+line[0], BDO+"Work");
                Resource admMainA = createAdminRoot(mainA);
                addReleased(mA, admMainA);
                mA.add(admMainA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_BDRC_CC0"));
                Resource logEntry = getFacetNode(FacetType.LOG_ENTRY, BDA, admMainA);
                mA.add(logEntry, RDF.type, mA.createResource(ADM+"InitialDataCreation"));
                mA.add(logEntry, mA.createProperty(ADM+"logWho"), mA.createResource(CommonMigration.BDU+"U00092"));
                mA.add(admMainA, mA.getProperty(ADM, "logEntry"), logEntry);
                mainA.addProperty(SKOS.prefLabel, line[1], "km");
                //mainA.addProperty(SKOS.prefLabel, line[2], "km-x-twktt");
                mainA.addProperty(SKOS.prefLabel, line[2], "km-x-twktt");
                mainA.addProperty(mA.createProperty(BDO+"workIsAbout"), mA.createResource(BDR+line[5]));
                mainA.addProperty(mA.createProperty(BDO+"language"), mA.createResource(BDR+line[6]));
                String workOutfileName = MigrationApp.getDstFileName("work", mainA.getLocalName());
                MigrationHelpers.outputOneModel(mA, mainA.getLocalName(), workOutfileName, "work");
            }
        } catch (IOException | CsvException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
