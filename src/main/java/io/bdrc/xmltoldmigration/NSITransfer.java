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
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.yearLit;

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
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class NSITransfer {


    public static final Map<String,List<String>> langMap = new HashMap<>();
    public static final Map<String,List<String>> scriptMap = new HashMap<>();
    static {
        langMap.put("Sanskrit;Pracalita",  Arrays.asList("Sa"));
        langMap.put("Nepal Bhasa;Devanagari", Arrays.asList("New"));
        langMap.put("Sanskrit;Devanagari", Arrays.asList("Sa"));
        langMap.put("Nepal Bhasa;Pracalita", Arrays.asList("New"));
        langMap.put("Nepali;Devanagari", Arrays.asList("Ne"));
        langMap.put("Sanskrit;Bhujimol", Arrays.asList("Sa"));
        langMap.put("Sanskrit/Nepal Bhasa;Pracalita", Arrays.asList("Sa", "New"));
        langMap.put("Sanskrit;Nagari", Arrays.asList("Sa"));
        langMap.put("Sanskrit/Nepal Bhasa;Devanagari", Arrays.asList("Sa", "New"));
        langMap.put("Sanskrit;Ranjana/Pracalita", Arrays.asList("Sa"));
        langMap.put("Sanskrit;Ranjana", Arrays.asList("Ranj"));
        scriptMap.put("Sanskrit;Pracalita",  Arrays.asList("Newa"));
        scriptMap.put("Nepal Bhasa;Devanagari", Arrays.asList("Deva"));
        scriptMap.put("Sanskrit;Devanagari", Arrays.asList("Deva"));
        scriptMap.put("Nepal Bhasa;Pracalita", Arrays.asList("Newa"));
        scriptMap.put("Nepali;Devanagari", Arrays.asList("Deva"));
        scriptMap.put("Sanskrit;Bhujimol", Arrays.asList("NepaleseHooked"));
        scriptMap.put("Sanskrit/Nepal Bhasa;Pracalita", Arrays.asList("Newa"));
        scriptMap.put("Sanskrit;Nagari", Arrays.asList("Nagari"));
        scriptMap.put("Sanskrit/Nepal Bhasa;Devanagari", Arrays.asList("Deva"));
        scriptMap.put("Sanskrit;Ranjana/Pracalita", Arrays.asList("Ranj", "Newa"));
        scriptMap.put("Sanskrit;Ranjana", Arrays.asList("Ranj"));
    }
    
    public static final void transferNIS() {
        System.out.println("Transfering Nepal Scanning Initiative works");
        SymetricNormalization.reinit();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("NSI.csv");
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
                writeNSIFiles(resources);
                line = reader.readNext();
            }
            MigrationApp.insertMissingSymetricTriples("work");
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public static final void writeNSIFiles(List<Resource> resources) {
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
        MigrationHelpers.setPrefixes(workModel);
        final String WRID = line[1].trim();
        Resource work = createRoot(workModel, BDR+'M'+WRID, BDO+"Instance");
        res.add(work);

        // adm:AdminData
        Resource admWork = createAdminRoot(work);
        addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_BDRC_CC0"));

        String abstractWorkRID = EAPTransfer.rKTsToBDR(line[19], true);
        Model mA = null;
        Resource workA = null;
        Resource admWorkA = null;
        if (abstractWorkRID == null) {
            abstractWorkRID = "WA"+WRID.substring(1);
            mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            workA = createRoot(mA, BDR+abstractWorkRID, BDO+"Work");
            admWorkA = createAdminRoot(workA);
            work.addProperty(workModel.createProperty(BDO, "instanceOf"), workA);
            addReleased(mA, admWorkA);
            mA.add(admWorkA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_BDRC_CC0"));
        } else {
            SymetricNormalization.addSymetricProperty(workModel, "instanceOf", 'M'+WRID, abstractWorkRID, null);
        }
        
        // title
        String title = line[4].trim();
        String titleLang = "sa-x-iast";
        if ("Unidentified".equals(title)) {
            titleLang = "en";
        }
        Resource titleType = workModel.createResource(BDO+"Title");
        Resource titleR = getFacetNode(FacetType.TITLE, work, titleType);
        work.addProperty(workModel.createProperty(BDO, "hasTitle"), titleR);
        titleR.addProperty(RDFS.label, workModel.createLiteral(title, titleLang));
        int linelen = line.length;
        work.addLiteral(SKOS.prefLabel, workModel.createLiteral(title, titleLang));
        if (workA != null) {
            workA.addLiteral(SKOS.prefLabel, mA.createLiteral(title, titleLang));
        }
        
        // event
        if (line[14].endsWith(" CE")) {
            String dateStr = line[14].substring(0, line[14].length()-3);
            Resource copyEventR = getEvent(work, "CopyEvent", "instanceEvent");
            copyEventR.addLiteral(workModel.createProperty(BDO, "eventWhen"), workModel.createTypedLiteral(dateStr, CommonMigration.EDTFDT));
        }
        
        // notes
        final StringBuilder noteText = new StringBuilder();
        noteText.append(line[6]);
        if (!line[13].isEmpty()) {
            noteText.append(", indicated date: "+line[13]);
        }
        noteText.append(", from the collection of  "+line[8]);
        CommonMigration.addNote(work, noteText.toString(), "en", null, null);
        
        workModel.add(work, workModel.createProperty(BDO, "printMethod"), workModel.createResource(BDR+"PrintMethod_Manuscript"));
        
        // other metadata
        final String langScript = line[9].trim()+";"+line[10].trim();
        if (mA != null) {
            List<String> langs = langMap.get(langScript);
            for (String ls : langs) {
                mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"Lang"+ls));
            }
        }
        List<String> scripts = scriptMap.get(langScript);
        for (String ls : scripts) {
            workModel.add(work, workModel.createProperty(BDO, "script"), workModel.createResource(BDR+"Script"+ls));
        }
        switch(line[17].trim()) {
        case "Yellow Paper":
            work.addProperty(workModel.createProperty(BDO, "material"), workModel.createResource(BDR+"MaterialPaper"));   
            work.addProperty(workModel.createProperty(BDO, "appliedMaterial"), workModel.createResource(BDR+"AppliedMaterial_Poison"));
            workModel.add(work, workModel.createProperty(BDO, "binding"), workModel.createResource(BDR+"Binding_LooseLeaf"));
            break;
        case "Bound Mss.":
            workModel.add(work, workModel.createProperty(BDO, "printMethod"), workModel.createResource(BDR+"PrintMethod_Manuscript"));
            workModel.add(work, workModel.createProperty(BDO, "binding"), workModel.createResource(BDR+"Binding_Codex_Sewn"));
            // not sure what that means...
            break;
        case "Thyāsaphū":
            workModel.add(work, workModel.createProperty(BDO, "binding"), workModel.createResource(BDR+"Binding_Continuous_Leporello"));
            break;
        }
        if (line[15].length() > 3 && line[16].length() > 3) {
            float dim1 = Float.parseFloat(line[15].substring(0, line[15].length()-3).trim());
            float dim2 = Float.parseFloat(line[16].substring(0, line[16].length()-3).trim());
            work.addProperty(workModel.createProperty(BDO, "dimWidth"), String.valueOf(Math.max(dim1, dim2)), XSDDatatype.XSDdecimal);
            work.addProperty(workModel.createProperty(BDO, "dimHeight"), String.valueOf(Math.min(dim1, dim2)), XSDDatatype.XSDdecimal);
        }
        // Topics and Genres, they should go with the abstract text
        if (mA != null && linelen > 16 && !line[16].isEmpty()) {
            final String[] topics = line[7].split("&");
            for (int i = 0; i < topics.length; i++)
            {
                workA.addProperty(mA.createProperty(BDO, "workIsAbout"), mA.createResource(BDR+topics[i].trim()));
            }
        }
        
        // bdo:Item for current bdo:Work
        final Model itemModel = ModelFactory.createDefaultModel();
        setPrefixes(itemModel);
        final String itemRID = line[1].trim();
        
        // Item for Work
        Resource item = createRoot(itemModel, BDR+itemRID, BDO+"ImageInstance");
        itemModel.add(item, itemModel.createProperty(BDO, "numberOfVolumes"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        res.add(item);
        Resource product = itemModel.createResource(BDR+"PR1NEPAL00");
        item.addProperty(itemModel.createProperty(BDO, "inCollection"), product);
        itemModel.add(item, itemModel.createProperty(BDO, "scanInfo"), itemModel.createLiteral("Digitized as part of the Nepalese Buddhist Sanskrit Manuscript Scanning Initiative, a collaboration with the Nagarjuna Institute of Buddhist Studies in Kathmandu, Nepal, and with funding from University of the West and Internet Archive.", "en"));

        workModel.add(work, workModel.createProperty(BDO, "instanceHasReproduction"), item);

        // Item adm:AdminData
        Resource admItem = createAdminRoot(item);
        addStatus(itemModel, admItem, "released");
        itemModel.add(admItem, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
        itemModel.addLiteral(admItem, itemModel.getProperty(ADM, "restrictedInChina"), false);

        // Volume for Item
        final String volumeRID = line[2].trim();
        Resource volume = itemModel.createResource(BDR+volumeRID);
        Resource volumeA = itemModel.createResource(BDA+volumeRID);
        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"ImageGroup"));
        itemModel.add(volumeA, RDF.type, itemModel.createResource(ADM+"AdminData"));
        itemModel.add(volumeA, itemModel.createProperty(ADM, "adminAbout"), volume);
        itemModel.add(volumeA, itemModel.createProperty(ADM, "legacyImageGroupRID"), itemModel.createLiteral(volumeRID));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "instanceHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumePagesTbrcIntro"), itemModel.createTypedLiteral(0, XSDDatatype.XSDinteger));

        SymetricNormalization.addSymetricProperty(itemModel, "instanceReproductionOf", itemRID, "M"+WRID, null);
        
        // the rest is just so that it looks normal so that it's fetched by requests in a normal way
        itemModel.add(volume, itemModel.createProperty(BDO, "imageList"), itemModel.createLiteral(""));
        itemModel.add(volume, itemModel.createProperty(BDO, "imageCount"), itemModel.createTypedLiteral(0, XSDDatatype.XSDinteger));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumePagesTotal"), itemModel.createTypedLiteral(0, XSDDatatype.XSDinteger));

        if (workA != null)
            res.add(workA);
        
        return res;
    }
    
}
