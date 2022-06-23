package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.BDG;
import static io.bdrc.libraries.Models.VCARD;
import static io.bdrc.libraries.Models.FacetType;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getEvent;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

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
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
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
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
            return null;
        }
        while (line != null) {
            if (line.length > 1 && !line[1].contains("?"))
                res.put(line[1], line[0]);
            try {
                line = reader.readNext();
            } catch (IOException | CsvValidationException e) {
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
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public static final void writeEAPFiles(List<Resource> resources) {
        Resource r = resources.get(0);
        String rOutfileName = MigrationApp.getDstFileName("instance", r.getLocalName());
        MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), rOutfileName, "instance");
        
        r = resources.get(1);
        if (r != null) {
            rOutfileName = MigrationApp.getDstFileName("work", r.getLocalName());
            MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), rOutfileName, "work");
        }
        
        r = resources.get(2);
        rOutfileName = MigrationApp.getDstFileName("iinstance", r.getLocalName());
        MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), rOutfileName, "iinstance");
    }

    public static final String rKTsToBDR(String rKTs, boolean indicversion) {
        if (rKTs == null || rKTs.isEmpty() || rKTs.contains("?") || rKTs.contains("&") || rKTs.contains("/"))
            return null;
        rKTs = rKTs.trim();
        if (indicversion && rKTsRIDMap.containsKey(rKTs)) {
            return rKTsRIDMap.get(rKTs);
        }
        String rktsid = null;
        try {
            rktsid = String.format("%04d", Integer.parseInt(rKTs.substring(1)));
        } catch (Exception e) {
            return null;
        }
        if (rKTs.startsWith("K")) {
            return "WA0RK"+(indicversion?"I":"")+rktsid;
        }
        return "WA0RT"+(indicversion?"I":"")+rktsid;
    }


    public static final List<Resource> getResourcesFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        MigrationHelpers.setPrefixes(workModel);
        final String baseRid = line[2].replace('/', '-');
        final String RID = 'W'+baseRid;
        Resource work = createRoot(workModel, BDR+'M'+RID, BDO+"Instance");
        res.add(work);

        String abstractWorkRID = rKTsToBDR(line[15], true);
        Model mA = null;
        Resource workA = null;
        Resource admWorkA = null;
        if (abstractWorkRID == null) {
            abstractWorkRID = "WA"+baseRid;
            mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            workA = createRoot(mA, BDR+abstractWorkRID, BDO+"Work");
            admWorkA = createAdminRoot(workA);
            res.add(workA);
            work.addProperty(workModel.createProperty(BDO, "instanceOf"), workA);
            addReleased(mA, admWorkA);
            mA.add(admWorkA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_EAP_metadata")); // ?
        } else {
            SymetricNormalization.addSymetricProperty(workModel, "instanceOf", 'M'+RID, abstractWorkRID, null);
            res.add(null);
        }
        
        // adm:AdminData
        Resource admWork = createAdminRoot(work);
        addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_EAP_metadata")); // ?
        final String origUrl = ORIG_URL_BASE+line[2].replace('/', '-');
        workModel.add(admWork, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));

        // title
        String title = line[12];
        String titleLang = "sa-x-iast";
        if (title.endsWith("@en")) {
            title = title.substring(0, title.length()-3);
            titleLang = "en";
        } else {
            Resource titleType = workModel.createResource(BDO+"Title");
            Resource titleR = getFacetNode(FacetType.TITLE, work, titleType);
            work.addProperty(workModel.createProperty(BDO, "hasTitle"), titleR);
            titleR.addProperty(RDFS.label, workModel.createLiteral(title, titleLang));
        }
        int linelen = line.length;
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, titleLang));
        if (workA != null) {
            workA.addLiteral(SKOS.prefLabel, mA.createLiteral(title, titleLang));
        }
        
        // event
        if (!line[3].isEmpty()) {
            int startDate = Integer.parseInt(line[3]);
            int endDate = Integer.parseInt(line[4]);
            Resource copyEventR = getEvent(work, "CopyEvent", "instanceEvent");
            if (startDate == endDate) {
                copyEventR.addLiteral(workModel.createProperty(BDO, "eventWhen"), workModel.createTypedLiteral(startDate, CommonMigration.EDTFDT));
            } else {
                copyEventR.addLiteral(workModel.createProperty(BDO, "eventWhen"), workModel.createTypedLiteral(startDate+"/"+endDate, CommonMigration.EDTFDT));
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
        workModel.add(work, workModel.createProperty(BDO, "printMethod"), workModel.createResource(BDR+"PrintMethod_Manuscript"));
        
        // other metadata
        final String langCode = line[5];
        final String scriptCode = line[6];
        if (scriptCode.equals("Newa")) {
            workModel.add(work, workModel.createProperty(BDO, "script"), workModel.createResource(BDR+"ScriptNewa"));
        } else if (scriptCode.equals("Ranj")) {
            workModel.add(work, workModel.createProperty(BDO, "script"), workModel.createResource(BDR+"ScriptRanj"));
        } else if (scriptCode.equals("Beng")) {
            workModel.add(work, workModel.createProperty(BDO, "script"), workModel.createResource(BDR+"ScriptBeng"));
        } else {
            workModel.add(work, workModel.createProperty(BDO, "script"), workModel.createResource(BDR+"ScriptDeva"));
        }
        switch(langCode) {
        case "san":
            if (workA != null) 
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangSa"));
            workModel.add(work, workModel.createProperty(BDO, "language"), workModel.createResource(BDR+"LangSa"));
            break;
        case "new":
            if (workA != null) 
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangNew"));
            workModel.add(work, workModel.createProperty(BDO, "language"), workModel.createResource(BDR+"LangNew"));
            break;
        case "san;new":
            if (workA != null) {
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangNew"));
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangSa"));
            }
            workModel.add(work, workModel.createProperty(BDO, "language"), workModel.createResource(BDR+"LangNew"));
            workModel.add(work, workModel.createProperty(BDO, "language"), workModel.createResource(BDR+"LangSa"));
            break;
        case "tib":
            if (workA != null) 
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangBo"));
            break;
        }
        if (!line[9].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "dimWidth"), line[9], XSDDatatype.XSDdecimal);
        }
        if (!line[10].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "dimHeight"), line[10], XSDDatatype.XSDdecimal);
        }
        if (workA != null) {
            if (linelen > 16 && !line[16].isEmpty()) {
                final String[] topics = line[16].split(",");
                for (int i = 0; i < topics.length; i++)
                {
                    workA.addProperty(mA.createProperty(BDO, "workIsAbout"), mA.createResource(BDR+topics[i]));
                }
            }
            if (linelen > 17 && !line[17].isEmpty()) {
                final String[] genres = line[17].split(",");
                for (int i = 0; i < genres.length; i++)
                {
                    workA.addProperty(mA.createProperty(BDO, "workGenre"), mA.createResource(BDR+genres[i]));
                }
            }
        }
        workModel.add(work, workModel.createProperty(BDO, "material"), workModel.createResource(BDR+"MaterialPaper"));

        final String iiifManifestUrl = origUrl+"/manifest";
        
        
        // bdo:Item for current bdo:Work
        final Model itemModel = ModelFactory.createDefaultModel();
        setPrefixes(itemModel);
        final String itemRID = 'W'+baseRid;
        
        // Item for Work
        Resource item = createRoot(itemModel, BDR+itemRID, BDO+"ImageInstance");
        res.add(item);
        
        Resource product = itemModel.createResource(BDR+"PR0EAP676");
        item.addProperty(itemModel.createProperty(BDO, "inCollection"), product);

        workModel.add(work, workModel.createProperty(BDO, "instanceHasReproduction"), item);

        // Item adm:AdminData
        Resource admItem = createAdminRoot(item);
        addStatus(itemModel, admItem, "released");
        itemModel.add(admItem, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
        itemModel.addLiteral(admItem, itemModel.getProperty(ADM, "restrictedInChina"), false);
        itemModel.add(admItem, itemModel.createProperty(ADM, "contentLegal"), itemModel.createResource(BDA + "LD_EAP_content")); // ?
        itemModel.add(admItem, itemModel.createProperty(ADM, "metadataLegal"), itemModel.createResource(BDA + "LD_EAP_metadata")); // ?

        // Volume for Item
        final String volumeRID = "I0"+itemRID.substring(1);
        Resource volume = itemModel.createResource(BDR+volumeRID);
        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"ImageGroup"));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "instanceHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "hasIIIFManifest"), itemModel.createResource(iiifManifestUrl));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        SymetricNormalization.addSymetricProperty(itemModel, "instanceReproductionOf", itemRID, "M"+RID, null);
        
        // there doesn't appear to be an original url for the volume to record in the Volume AdminData
//        // Volume adm:AdminData
//        Resource admVol = CommonMigration.getAdmResource(itemModel, volumeRID);
//        origUrl = ManifestPREF+ref;
//        itemModel.add(admVol, itemModel.createProperty(ADM, "originalRecord"), itemModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));                
        
        return res;
    }

}
