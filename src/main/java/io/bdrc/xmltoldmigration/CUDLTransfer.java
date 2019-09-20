package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addReleased;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
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
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class CUDLTransfer {

    public static  List<String[]> lines;

    public static final HashMap<String,String> scripts = getScripts();
    public static final String ORIG_URL_BASE = "https://cudl.lib.cam.ac.uk/view/";

    public static void CUDLDoTransfer() throws IOException {
        CSVReader reader;
        CSVParser parser = new CSVParserBuilder().build();
        InputStream inputStream = CUDLTransfer.class.getClassLoader().getResourceAsStream("cudl.csv");
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        lines=reader.readAll();
        for(int x=1;x<lines.size();x++) {
            writeCUDLFiles(getResourcesFromLine(lines.get(x)));
        }
    }

    public static final HashMap<String,String> getScripts() {

        final HashMap<String,String> res = new HashMap<>();
        res.put("nepālākṣarā","SaNepaleseHooked");
        res.put("pāla","SaRanj");
        res.put("sinhala","SaSinh");
        res.put("devanāgarī","SaDeva");
        res.put("rañjanā","SaRanj");
        res.put("bengali","SaBeng");
        res.put("naipālanāgarī","SaDeva");
        return res;
    }

    public static final void addMaterial(final Resource r, final String matStr) {
        final Model m = r.getModel();
        final HashMap<String,String> res = new HashMap<>();
        res.put("palm_leaf","MaterialPalmyraPalmLeaf");
        res.put("paper","MaterialPaper");
        res.put("corypha_palm_leaf","MaterialCoryphaPalmLeaf");
        res.put("mixed","MaterialMixed");
        switch(matStr) {
        case "palm_leaf":
        case "paper":
        case "corypha_palm_leaf":
        case "mixed":
            r.addProperty(m.createProperty(BDO, "workMaterial"), m.createResource(BDR+res.get(matStr)));
            break;
        case "nep_multi_layered_paper":
            r.addProperty(m.createProperty(BDO, "workMaterial"), m.createResource(BDR+"MaterialPaper"));
            r.addProperty(m.createProperty(BDO, "appliedMaterial"), m.createResource(BDR+"AppliedMaterial_Poison"));
            break;
        case "black_paper":
            r.addProperty(m.createProperty(BDO, "workMaterial"), m.createResource(BDR+"MaterialPaper"));
            r.addProperty(m.createProperty(BDO, "appliedMaterial"), m.createResource(BDR+"AppliedMaterial_IndigoDye"));
            break;
        }
    }

    public static final List<Resource> getResourcesFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        setPrefixes(workModel);
        String rid=line[0];
        
        // Work model
        Resource work = createRoot(workModel, BDR+"W0CDL0"+rid, BDO+"Work");
        Resource admWork = createAdminRoot(work);
        res.add(work);

        // Work adm:AdminData
        addReleased(workModel, admWork);
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), workModel.createResource(BDA + "LD_CUDL_metadata")); // ?
        final String origUrl = ORIG_URL_BASE+line[0];
        workModel.add(admWork, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));        

        // bdo:Work
        workModel.add(work,workModel.createProperty(BDO,"workCatalogInfo"),workModel.createLiteral(line[1], "en"));
        workModel.add(work,workModel.createProperty(BDO,"workBiblioNote"),workModel.createLiteral(line[2], "en"));
        String mainTitle=line[6];
        String title=line[3];
        Literal lit=null;
        if(title.endsWith("@en")) {
            lit=workModel.createLiteral(title);
        }else {
            lit=workModel.createLiteral(title,"sa-x-iast");
        }
        workModel.add(work, SKOS.prefLabel, lit);
        
        Resource titleType = workModel.createResource(BDO+"WorkBibliographicalTitle");
        Resource titleR = getFacetNode(FacetType.TITLE, work, titleType);
        work.addProperty(workModel.createProperty(BDO, "workTitle"), titleR);
        titleR.addProperty(RDFS.label, (mainTitle.equals("") ? lit : workModel.createLiteral(mainTitle, "sa-x-iast")));

        String altTitle=line[7];
        if(!altTitle.equals("")) {
            workModel.add(work, SKOS.altLabel, workModel.createLiteral(altTitle, "sa-x-iast")); // DO WE REALLY NEED THIS??
            titleType = workModel.createResource(BDO+"WorkOtherTitle");
            titleR = getFacetNode(FacetType.TITLE, work, titleType);
            work.addProperty(workModel.createProperty(BDO, "workTitle"), titleR);
            titleR.addProperty(RDFS.label, altTitle, "sa-x-iast");
        }
        
        final String abstractWorkRID = EAPTransfer.rKTsToBDR(line[4]);
        if (abstractWorkRID != null) {
            SymetricNormalization.addSymetricProperty(workModel, "workExpressionOf", "W0CDL0"+rid, abstractWorkRID, null);
        }
        if(!line[5].equals("")) {
            workModel.add(work, workModel.createProperty(BDO, "workIsAbout"), workModel.createResource(BDR+line[5]));
        }
        workModel.add(work, workModel.createProperty(BDO, "printMethod"), workModel.createResource(BDR+"PrintMethod_Manuscript"));
        addMaterial(work, line[9]);
        if(!line[14].equals("")) {
            workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+scripts.get(line[14].toLowerCase())));
        }
        if (!line[19].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimWidth"), line[19].replace(',','.').trim(), XSDDatatype.XSDdecimal);
        }
        if (!line[18].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimHeight"), line[18].replace(',','.').trim(), XSDDatatype.XSDdecimal);
        }
        if(!line[10].equals("") && !line[11].equals("")) {
            Resource event = getEvent(work, "PublishedEvent", "workEvent");
            workModel.add(event, workModel.createProperty(BDO, "notAfter"), workModel.createTypedLiteral(line[11], XSDDatatype.XSDinteger));
            workModel.add(event, workModel.createProperty(BDO, "notBefore"), workModel.createTypedLiteral(line[10], XSDDatatype.XSDinteger));
        }
        
        
        // Item model
        final Model itemModel = ModelFactory.createDefaultModel();
        setPrefixes(itemModel);
        final String itemRID = "I0CDL0"+rid;
        Resource item = createRoot(itemModel, BDR+itemRID, BDO+"ItemImageAsset");
        Resource itemAdm = createAdminRoot(item);
        res.add(item);
        
        if (WorkMigration.addWorkHasItem) {
            workModel.add(work, workModel.createProperty(BDO, "workHasItem"), workModel.createResource(BDR+itemRID));
        }
        
        // Item adm:AdminData
        addReleased(itemModel, itemAdm);
        itemModel.add(itemAdm, itemModel.createProperty(ADM, "contentLegal"), itemModel.createResource(BDA + "LD_CUDL_content"));
        itemModel.add(itemAdm, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
        itemModel.addLiteral(itemAdm, itemModel.getProperty(ADM, "restrictedInChina"), false);
               
        // bdo:ItemImageAsset
        final String volumeRID = "V0CDL0"+rid;
        
        // Volume of Item
        Resource volume = itemModel.createResource(BDR+volumeRID);

        // There doesn't appear to be any original url info to include
//        // Volume adm:AdminData
//        Resource admVol = itemModel.createResource(BDA+volumeRID);
//        itemModel.add(admVol, RDF.type, itemModel.createResource(ADM+"AdminData"));
//        origUrl = ManifestPREF+ref;
//        itemModel.add(admVol, itemModel.createProperty(ADM, "originalRecord"), itemModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));                

        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"VolumeImageAsset"));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "hasIIIFManifest"), itemModel.createResource(line[8]));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        
        if (WorkMigration.addItemForWork) {
            itemModel.add(item, itemModel.createProperty(BDO, "itemForWork"), itemModel.createResource(BDR+"W0CDL0"+rid));
        }

        return res;
    }

    public static void writeCUDLFiles(List<Resource> resources) {
        for(Resource r: resources) {
            String uri=r.getProperty(RDF.type).getObject().asResource().getURI();
            switch(uri) {
                case "http://purl.bdrc.io/ontology/core/Work":
                    final String workOutfileName = MigrationApp.getDstFileName("work", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), workOutfileName, "work");
                    break;
                case "http://purl.bdrc.io/ontology/core/ItemImageAsset":
                    final String itemOutfileName = MigrationApp.getDstFileName("item", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), itemOutfileName, "item");
                    break;
            }
        }
    }
}
