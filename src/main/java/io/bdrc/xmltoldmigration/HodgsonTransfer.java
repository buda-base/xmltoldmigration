package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getEvent;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
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

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class HodgsonTransfer {
    public static final Map<String,List<String>> langScriptMap = new HashMap<>();
    static {
        langScriptMap.put("Bhujimol",  Arrays.asList("SaNepaleseHooked"));
        langScriptMap.put("Pracalit", Arrays.asList("SaNewa"));
    }
    
    public static final void transfer() {
        System.out.println("Transfering Hodgson collection from Internet Archive works");
        SymetricNormalization.reinit();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("hodgson.csv");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        final CSVReader reader;
        final CSVParser parser = new CSVParserBuilder().build();
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        String[] line;
        try {
            line = reader.readNext();
            // ignoring first line
            line = reader.readNext();
            while (line != null) {
                List<Resource> resources = getResourcesFromLine(line);
                writeHodgsonFiles(resources);
                line = reader.readNext();
            }
            MigrationApp.insertMissingSymetricTriples("work");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final void writeHodgsonFiles(List<Resource> resources) {
        final Resource work = resources.get(0);
        final String workOutfileName = MigrationApp.getDstFileName("instance", work.getLocalName());
        MigrationHelpers.outputOneModel(work.getModel(), work.getLocalName(), workOutfileName, "instance");
        
        final Resource item = resources.get(1);
        final String itemOutfileName = MigrationApp.getDstFileName("iinstance", item.getLocalName());
        MigrationHelpers.outputOneModel(item.getModel(), item.getLocalName(), itemOutfileName, "iinstance");
        
        if (resources.size() > 2) {
            final Resource workA = resources.get(2);
            final String workAOutfileName = MigrationApp.getDstFileName("work", workA.getLocalName());
            MigrationHelpers.outputOneModel(workA.getModel(), workA.getLocalName(), workAOutfileName, "work");
        }
    }

    public static final List<Resource> getResourcesFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        setPrefixes(workModel);
        final String baseRID = line[0].trim();
        Resource work = createRoot(workModel, BDR+"MW"+baseRID, BDO+"Instance");
        res.add(work);
        
        
        String abstractWorkRID = EAPTransfer.rKTsToBDR(line[5], true);

        Model mA = null;
        Resource workA = null;
        Resource admWorkA = null;
        if (abstractWorkRID ==  null) {
            abstractWorkRID = "WA"+baseRID;
            mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            workA = createRoot(mA, BDR+abstractWorkRID, BDO+"Work");
            admWorkA = createAdminRoot(workA);
            res.add(workA);
            work.addProperty(workModel.createProperty(BDO, "instanceOf"), workA);
            workA.addProperty(workModel.createProperty(BDO, "workHasInstance"), work);
            addReleased(mA, admWorkA);
            mA.add(admWorkA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_IA_Metadata")); // ?
        } else {
            SymetricNormalization.addSymetricProperty(workModel, "instanceOf", "MW"+baseRID, abstractWorkRID, null);
        }

        // adm:AdminData
        Resource admWork = createAdminRoot(work);
        addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_IA_Metadata")); // ?

        // title
        String title = line[2].trim();
        Resource titleType = workModel.createResource(BDO+"Title");
        Resource titleR = getFacetNode(FacetType.TITLE, work, titleType);
        work.addProperty(workModel.createProperty(BDO, "hasTitle"), titleR);
        titleR.addProperty(RDFS.label, workModel.createLiteral(title, "sa-x-iast"));
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, "sa-x-iast"));
        if (workA != null) {
            workA.addLiteral(SKOS.prefLabel, mA.createLiteral(title, "sa-x-iast"));
        }
        
        if (!line[3].trim().isEmpty()) {
            title = line[3].trim();
            titleR = getFacetNode(FacetType.TITLE, work, titleType);
            work.addProperty(workModel.createProperty(BDO, "hasTitle"), titleR);
            titleR.addProperty(RDFS.label, workModel.createLiteral(title, "sa-x-iast"));
            work.addLiteral(SKOS.altLabel, workModel.createLiteral(title, "sa-x-iast"));
            if (workA != null) {
                workA.addLiteral(SKOS.altLabel, mA.createLiteral(title, "sa-x-iast"));
            }
        }
        
        CommonMigration.addNote(work, "From the Hodgson Collection", "en", null, null);
        
        if (line.length > 9 && !line[10].trim().isEmpty()) {
            CommonMigration.addNote(work, line[10], "en", null, null);
        }
        
        if (line.length > 8 && !line[9].trim().isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "colophon"), workModel.createLiteral(line[9], "sa-x-iast"));
        }
        if (line.length > 7 && !line[8].trim().isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "incipit"), workModel.createLiteral(line[8], "sa-x-iast"));
        }
        
        workModel.add(work, workModel.createProperty(BDO, "printMethod"), workModel.createResource(BDR+"PrintMethod_Manuscript"));
        work.addProperty(workModel.createProperty(BDO, "material"), workModel.createResource(BDR+"MaterialPalmLeaf"));
        work.addProperty(workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral("https://archive.org/details/"+line[1].trim(), XSDDatatype.XSDanyURI));
        
        // Topics and Genres, they should go with the abstract text
        if (!line[6].isEmpty() && workA != null) {
            workA.addProperty(mA.createProperty(BDO, "workIsAbout"), mA.createResource(BDR+line[6].trim()));
        }
        if (!line[7].isEmpty() && workA != null) {
            work.addProperty(mA.createProperty(BDO, "workGenre"), mA.createResource(BDR+line[7].trim()));
        }

        
        // bdo:Item for current bdo:Work
        final Model itemModel = ModelFactory.createDefaultModel();
        setPrefixes(itemModel);
        final String itemRID = "W"+baseRID;
        
        // Item for Work
        Resource item = createRoot(itemModel, BDR+itemRID, BDO+"ImageInstance");
        res.add(item);
        Resource product = itemModel.createResource(BDR+"PR0IA_HOD01");
        item.addProperty(itemModel.createProperty(BDO, "inCollection"), product);
        if (WorkMigration.addWorkHasItem) {
            workModel.add(work, workModel.createProperty(BDO, "instanceHasReproduction"), item);
        }

        // Item adm:AdminData
        Resource admItem = createAdminRoot(item);
        addStatus(itemModel, admItem, "released");
        itemModel.add(admItem, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
        itemModel.addLiteral(admItem, itemModel.getProperty(ADM, "restrictedInChina"), false);
        itemModel.add(admItem, itemModel.createProperty(ADM, "contentLegal"), itemModel.createResource(BDA + "LD_IA_PD"));
        itemModel.add(admItem, itemModel.createProperty(ADM, "metadataLegal"), itemModel.createResource(BDA + "LD_IA_Metadata"));

        // Volume for Item
        final String volumeRID = 'I'+baseRID;
        Resource volume = itemModel.createResource(BDR+volumeRID);
        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"ImageGroup"));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "instanceHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "hasIIIFManifest"), itemModel.createResource("https://iiif.archivelab.org/iiif/"+line[1].trim()+"/manifest.json"));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        if (WorkMigration.addItemForWork) {
            itemModel.add(item, itemModel.createProperty(BDO, "instanceReproductionOf"), itemModel.createResource(BDR+"MW"+baseRID));
            if (workA != null) {
                workA.addProperty(workModel.createProperty(BDO, "workHasInstance"), item);
                item.addProperty(itemModel.createProperty(BDO, "instanceOf"), workA);
            } else {
                SymetricNormalization.addSymetricProperty(itemModel, "instanceOf", itemRID, abstractWorkRID, null);
            }
        }


        return res;
    }
}
