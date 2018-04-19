package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class EAPTransfer {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    
    public static final Map<String,String> rKTsRIDMap = getrKTsRIDMap();
    
    public static final Map<String,String> getrKTsRIDMap() {
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        final Map<String,String> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("abstract-rkts.csv");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        String[] line = null;
        try {
            line = reader.readNext();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        while (line != null) {
            if (line.length > 1 && !line[1].contains("?"))
                res.put(line[1], line[0]);
            try {
                line = reader.readNext();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        return res;
    }
    
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
    
    public static final String rKTsToBDR(String rKTs) {
        if (rKTs == null || rKTs.isEmpty() || rKTs.contains("?") || rKTs.contains(" "))
            return null;
        if (rKTsRIDMap.containsKey(rKTs)) {
            return rKTsRIDMap.get(rKTs);
        }
        if (rKTs.startsWith("K")) {
            return "W0RKA"+rKTs.substring(1);
        }
        return "W0RTA"+rKTs.substring(1);
    }
    
    
    public static final List<Model> getModelsFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Model> res = new ArrayList<>();
        res.add(workModel);
        CommonMigration.setPrefixes(workModel);
        String RID = line[2].replace('/', '_');
        Resource work = workModel.createResource(BDR+RID);
        workModel.add(work, RDF.type, workModel.createResource(BDO+"Work"));
        String title = line[12];
        String titleLang = "sa-x-iast";
        if (title.endsWith("@en")) {
            title = title.substring(0, title.length()-3);
            titleLang = "en";
        } else {
            Resource titleR = workModel.createResource();
            workModel.add(work, workModel.createProperty(BDO, "workTitle"), titleR);
            workModel.add(titleR, RDF.type, workModel.createResource(BDO+"WorkBibliographicalTitle")); // ?
            workModel.add(titleR, RDFS.label, workModel.createLiteral(title, titleLang)); // ?
        }
        int linelen = line.length; 
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, titleLang));
        if (!line[3].isEmpty()) {
            int startDate = Integer.parseInt(line[3]);
            int endDate = Integer.parseInt(line[4]);
            Resource copyEventR = workModel.createResource();
            workModel.add(work, workModel.createProperty(BDO, "workEvent"), copyEventR);
            workModel.add(copyEventR, RDF.type, workModel.createResource(BDO+"CopyEvent"));
            if (startDate == endDate) {
                copyEventR.addLiteral(workModel.createProperty(BDO, "onYear"), workModel.createTypedLiteral(startDate, XSDDatatype.XSDinteger));
            } else {
                copyEventR.addLiteral(workModel.createProperty(BDO, "notBefore"), workModel.createTypedLiteral(startDate, XSDDatatype.XSDinteger));
                copyEventR.addLiteral(workModel.createProperty(BDO, "notAfter"), workModel.createTypedLiteral(endDate, XSDDatatype.XSDinteger));
            }
        }
        final StringBuilder note = new StringBuilder();
        note.append(line[8]);
        if (!line[13].isEmpty()) {
            note.append(", date: "+line[13]);
        }
        note.append(", recordID: "+line[0]);
        note.append(", MDARK: "+line[7]);
        Resource noteR = workModel.createResource();
        //workModel.add(noteR, RDF.type, workModel.createResource(BDO+"Note"));
        noteR.addLiteral(workModel.createProperty(BDO, "noteText"), note.toString());
        workModel.add(work, workModel.createProperty(BDO, "note"), noteR);
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
        // Topics and Genres, they should go with the abstract text
        if (linelen > 16 && !line[16].isEmpty()) {
            String[] topics = line[16].split(",");
            for (int i = 0; i < topics.length; i++)
            {
                work.addProperty(workModel.createProperty(BDO, "workIsAbout"), workModel.createResource(BDR+topics[i]));
            }
        }
        if (linelen > 17 && !line[17].isEmpty()) {
            String[] genres = line[17].split(",");
            for (int i = 0; i < genres.length; i++)
            {
                work.addProperty(workModel.createProperty(BDO, "workGenre"), workModel.createResource(BDR+genres[i]));
            }
        }
        
        String abstractWorkRID = rKTsToBDR(line[15]);
        if (abstractWorkRID != null) {
            //workModel.add(work, workModel.createProperty(BDO, "workExpressionOf"), workModel.createResource(BDR+abstractWorkRID));
            SymetricNormalization.addSymetricProperty(workModel, "workExpressionOf", RID, abstractWorkRID, null);
        }
        
        return res;
    }
    
}
