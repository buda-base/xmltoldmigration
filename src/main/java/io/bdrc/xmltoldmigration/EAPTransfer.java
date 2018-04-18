package io.bdrc.xmltoldmigration;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class EAPTransfer {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    
    public static final void transferCsvFile(String filename) throws IOException {
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        try {
            reader = new CSVReaderBuilder(new FileReader(filename))
                    .withCSVParser(parser)
                    .build();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        String[] line = reader.readNext();
        // ignoring first line
        line = reader.readNext();
        while (line != null) {
            List<Model> models = getModelsFromLine(line);
            line = reader.readNext();
        }
    }
    
    public static final List<Model> getModelsFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Model> res = new ArrayList<>();
        res.add(workModel);
        CommonMigration.setPrefixes(workModel);
        String RID = line[1].replace('/', '_');
        Resource work = workModel.createResource(BDR+RID);
        String title = line[12];
        String titleLang = "sa-x-iast";
        if (title.endsWith("@en")) {
            title = title.substring(0, title.length()-3);
            titleLang = "en";
        }
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, titleLang));
        // not sure what to do with dates, see https://github.com/BuddhistDigitalResourceCenter/owl-schema/issues/76
//        int startDate = Integer.parseInt(line[3]);
//        int endDate = Integer.parseInt(line[4]);
//        if (startDate == endDate) {
//            work.addLiteral(workModel.createProperty(BDO, "onYear"), startDate);
//        } else {
//            work.addLiteral(workModel.createProperty(BDO, "notBefore"), startDate);
//            work.addLiteral(workModel.createProperty(BDO, "notAfter"), endDate);
//        }
        final String langCode = line[5];
        final String scriptCode = line[6];
        switch(langCode) {
        case "san":
            if (scriptCode.equals("Newa")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaNewa"));
            } else if (scriptCode.equals("Beng")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaBeng"));
            } else {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaDeva"));
            }
            break;
        case "new":
            if (scriptCode.equals("Newa")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"NewNewa"));
            } else {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"NewDeva"));
            }
            break;
        case "san;new":
            if (scriptCode.equals("Newa")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaNewa"));
                workModel.add(work, workModel.createProperty(BDO, "workOtherLangScript"), workModel.createResource(BDR+"NewNewa"));
            } else {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaDeva"));
                workModel.add(work, workModel.createProperty(BDO, "workOtherLangScript"), workModel.createResource(BDR+"NewDeva"));
            }
            break;
        case "tib":
            workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"BoDeva"));
            break;
        }
        if (!line[9].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimWidth"), line[9], XSDDatatype.XSDdecimal);
        }
        if (!line[10].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimHeight"), line[10], XSDDatatype.XSDdecimal);
        }
        return res;
    }
    
}
