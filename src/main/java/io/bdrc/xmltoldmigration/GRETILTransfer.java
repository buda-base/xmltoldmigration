package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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

public class GRETILTransfer {

    private static final String BDO = CommonMigration.ONTOLOGY_NS;
    private static final String BDR = CommonMigration.RESOURCE_NS;
    private static final String ADM = CommonMigration.ADMIN_NS;
    private static final String BDA = CommonMigration.ADMIN_DATA_NS;
    private static final String BDG = CommonMigration.GRAPH_NS;

    public static final Map<String,String> rKTsRIDMap = EAPTransfer.getrKTsRIDMap();
    public static final String ORIG_URL_BASE = "gretil.sub.uni-goettingen.de/";

    public static final void transferGRETIL() {
        System.out.println("Transfering GRETIL works");
        SymetricNormalization.reinit();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("gretil.csv");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        ArrayList<String> processed=new ArrayList<>();
        try {
            String[] line= reader.readNext();// skip first two lines
            line= reader.readNext();
            line= reader.readNext();
            while (line != null) {
                //avoiding identical originalRecords
                if(line[8]!=null && !processed.contains(line[8])) {
                    List<Resource> resources = getResourcesFromLine(line);
                    writeGRETILFiles(resources);
                    processed.add(line[8]);
                }
                line = reader.readNext();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        MigrationApp.insertMissingSymetricTriples("work");
    }

    public static final void writeGRETILFiles(List<Resource> resources) {
        final Resource work = resources.get(0);
        final String workOutfileName = MigrationApp.getDstFileName("work", work.getLocalName());
        MigrationHelpers.outputOneModel(work.getModel(), work.getLocalName(), workOutfileName, "work");
    }
    
    public static final List<Resource> getResourcesFromLine(String[] line) {
        
        // Work model
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        CommonMigration.setPrefixes(workModel);
        Resource work = workModel.createResource(BDR+line[0]);
        Resource admWork = MigrationHelpers.getAdmResource(work);
        res.add(work);

        // Work AdminData
        CommonMigration.addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_GRETIL")); // ?
        if (line[8] != null && !"".equals(line[8])) {
            final String origUrl = ORIG_URL_BASE+line[8].replace('/', '-');
            workModel.add(admWork, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));
        }
        
        // bdo:Work
        workModel.add(work, RDF.type, workModel.createResource(BDO+"UnicodeWork"));
        workModel.add(work, SKOS.prefLabel, workModel.createLiteral(line[1], "en"));
        workModel.add(work, SKOS.prefLabel, workModel.createLiteral(line[3], "sa-x-iast"));
//        workModel.add(work, workModel.createProperty(BDO, "workType"), workModel.createResource(BDR+"WorkTypeUnicodeText"));
        Resource titleR = workModel.createResource();
        workModel.add(work, workModel.createProperty(BDO, "workTitle"), titleR);
        workModel.add(titleR, RDF.type, workModel.createResource(BDO+"WorkBibliographicalTitle")); // ?
        workModel.add(titleR, RDFS.label, workModel.createLiteral(line[3], "sa-x-iast"));

        String rkts=line[2];
        if(rkts!=null) {
            if(rkts.contains(",")) {
                rkts=rkts.substring(0,rkts.indexOf(","));
            }
            final String abstractWorkRID = EAPTransfer.rKTsToBDR(line[2]);
            if (abstractWorkRID != null) {
                SymetricNormalization.addSymetricProperty(workModel, "workExpressionOf", line[0], abstractWorkRID, null);
            }
        }
        String author=line[5];
        if(author!=null && !"".equals(author)) {
            workModel.add(work, workModel.createProperty(BDO, "creatorMainAuthor"), workModel.createResource(BDR+author));
        }
        String topic=line[6];
        if(topic!=null && !"".equals(topic)) {
            // Basic cchecking but some validation of the topic should occur here
            //We might need to query ldspdi for the list of all topics
            if(topic.startsWith("T")) {
                workModel.add(work, workModel.createProperty(BDO, "workIsAbout"), workModel.createResource(BDR+topic));
            }
        }
        String note=line[9];
        if(note!=null && !"".equals(note)) {
            workModel.add(work, workModel.createProperty(BDO, "note"), "Input by "+note);
        }
        note=line[10];
        if(note!=null && !"".equals(note)) {
            workModel.add(work, workModel.createProperty(BDO, "note"), "Based on "+note);
        }
        return res;
    }

}
