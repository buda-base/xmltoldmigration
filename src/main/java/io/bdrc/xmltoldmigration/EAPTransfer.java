package io.bdrc.xmltoldmigration;

import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.ADM;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDA;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDO;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDR;

import java.io.BufferedReader;
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
import io.bdrc.xmltoldmigration.xml2files.CommonMigration.FacetType;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class EAPTransfer {

    public static final Map<String,String> rKTsRIDMap = getrKTsRIDMap();
    public static final String ORIG_URL_BASE = "https://eap.bl.uk/archive-file/";

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

    public static final void transferEAP() {
        System.out.println("Transfering EAP works");
        SymetricNormalization.reinit();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("eap.csv");
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
                writeEAPFiles(resources);
                line = reader.readNext();
            }
            MigrationApp.insertMissingSymetricTriples("work");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final void writeEAPFiles(List<Resource> resources) {
        final Resource work = resources.get(0);
        final String workOutfileName = MigrationApp.getDstFileName("work", work.getLocalName());
        MigrationHelpers.outputOneModel(work.getModel(), work.getLocalName(), workOutfileName, "work");
        
        final Resource item = resources.get(1);
        final String itemOutfileName = MigrationApp.getDstFileName("item", item.getLocalName());
        MigrationHelpers.outputOneModel(item.getModel(), item.getLocalName(), itemOutfileName, "item");
    }

    public static final String rKTsToBDR(String rKTs) {
        if (rKTs == null || rKTs.isEmpty() || rKTs.contains("?") || rKTs.contains("&"))
            return null;
        rKTs = rKTs.trim();
        if (rKTsRIDMap.containsKey(rKTs)) {
            return rKTsRIDMap.get(rKTs);
        }
        String rktsid = null;
        try {
            rktsid = String.format("%04d", Integer.parseInt(rKTs.substring(1)));
        } catch (Exception e) {
            return null;
        }
        if (rKTs.startsWith("K")) {
            return "W0RKA"+rktsid;
        }
        return "W0RTA"+rktsid;
    }


    public static final List<Resource> getResourcesFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        CommonMigration.setPrefixes(workModel);
        final String baseRid = line[2].replace('/', '-');
        final String RID = 'W'+baseRid;
        Resource work = CommonMigration.createRoot(workModel, BDR+RID);
        res.add(work);
        workModel.add(work, RDF.type, workModel.createResource(BDO+"Work"));

        // adm:AdminData
        Resource admWork = CommonMigration.createAdminRoot(work);
        CommonMigration.addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_EAP")); // ?
        final String origUrl = ORIG_URL_BASE+line[2].replace('/', '-');
        workModel.add(admWork, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));

        // title
        String title = line[12];
        String titleLang = "sa-x-iast";
        if (title.endsWith("@en")) {
            title = title.substring(0, title.length()-3);
            titleLang = "en";
        } else {
            Resource titleType = workModel.createResource(BDO+"WorkBibliographicalTitle");
            Resource titleR = CommonMigration.getFacetNode(FacetType.TITLE, work, "MigrationApp", titleType);
            work.addProperty(workModel.createProperty(BDO, "workTitle"), titleR);
            titleR.addProperty(RDFS.label, workModel.createLiteral(title, titleLang));
        }
        int linelen = line.length;
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, titleLang));
        
        // event
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
        
        // notes
        final StringBuilder noteText = new StringBuilder();
        noteText.append(line[8]);
        if (!line[13].isEmpty()) {
            noteText.append(", date: "+line[13]);
        }
        noteText.append(", recordID: "+line[0]);
        noteText.append(", MDARK: "+line[7]);
        CommonMigration.addNote(work, noteText.toString(), "en", null, null);
        
        // other metadata
        final String langCode = line[5];
        final String scriptCode = line[6];
        switch(langCode) {
        case "san":
            if (scriptCode.equals("Newa")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaNewa"));
            } else if (scriptCode.equals("Ranj")) {
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"SaRanj"));
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
            final String[] topics = line[16].split(",");
            for (int i = 0; i < topics.length; i++)
            {
                work.addProperty(workModel.createProperty(BDO, "workIsAbout"), workModel.createResource(BDR+topics[i]));
            }
        }
        if (linelen > 17 && !line[17].isEmpty()) {
            final String[] genres = line[17].split(",");
            for (int i = 0; i < genres.length; i++)
            {
                work.addProperty(workModel.createProperty(BDO, "workGenre"), workModel.createResource(BDR+genres[i]));
            }
        }
        workModel.add(work, workModel.createProperty(BDO, "workMaterial"), workModel.createResource(BDR+"MaterialPaper"));
        workModel.add(work, workModel.createProperty(BDO, "workObjectType"), workModel.createResource(BDR+"ObjectTypeManuscript"));
        final String abstractWorkRID = rKTsToBDR(line[15]);
        if (abstractWorkRID != null) {
            SymetricNormalization.addSymetricProperty(workModel, "workExpressionOf", RID, abstractWorkRID, null);
        }
        final String iiifManifestUrl = origUrl+"/manifest";
        
        
        // bdo:Item for current bdo:Work
        final Model itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel);
        final String itemRID = 'I'+baseRid;
        
        // Item for Work
        Resource item = CommonMigration.createRoot(itemModel, BDR+itemRID);
        res.add(item);
        itemModel.add(item, RDF.type, itemModel.createResource(BDO+"ItemImageAsset"));

        if (WorkMigration.addWorkHasItem) {
            workModel.add(work, workModel.createProperty(BDO, "workHasItem"), item);
        }

        // Item adm:AdminData
        Resource admItem = CommonMigration.createAdminRoot(item);
        CommonMigration.addStatus(itemModel, admItem, "released");
        itemModel.add(admItem, itemModel.createProperty(ADM, "contentLegal"), itemModel.createResource(BDA + "LD_EAP")); // ?
        itemModel.add(admItem, itemModel.createProperty(ADM, "metadataLegal"), itemModel.createResource(BDA + "LD_EAP")); // ?

        // Volume for Item
        final String volumeRID = 'V'+itemRID.substring(1);
        Resource volume = itemModel.createResource(BDR+volumeRID);
        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"VolumeImageAsset"));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "hasIIIFManifest"), itemModel.createResource(iiifManifestUrl));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        if (WorkMigration.addItemForWork) {
            itemModel.add(item, itemModel.createProperty(BDO, "itemForWork"), itemModel.createResource(BDR+RID));
        }
        
        // there doesn't appear to be an original url for the volume to record in the Volume AdminData
//        // Volume adm:AdminData
//        Resource admVol = CommonMigration.getAdmResource(itemModel, volumeRID);
//        origUrl = ManifestPREF+ref;
//        itemModel.add(admVol, itemModel.createProperty(ADM, "originalRecord"), itemModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));                
        
        return res;
    }

}
