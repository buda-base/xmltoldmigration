package io.bdrc.xmltoldmigration;

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

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class CUDLTransfer {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    public static  List<String[]> lines;

    public static final Map<String,String> rKTsRIDMap = getrKTsRIDMap();
    public static final HashMap<String,String> scripts = getScripts();
    public static final HashMap<String,String> materials = getMaterials();

    public static void CUDLDoTransfer() throws IOException {
        CSVReader reader;
        CSVParser parser = new CSVParserBuilder().build();
        InputStream inputStream = EAPFondsTransfer.class.getClassLoader().getResourceAsStream("cudl.csv");
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
        res.put("Hooked Nepālākṣarā (Bhujimol)","SaNepaleseHooked");
        res.put("bengali","SaBeng");
        return res;
    }

    public static final HashMap<String,String> getMaterials() {

        final HashMap<String,String> res = new HashMap<>();
        res.put("palm_leaf","MaterialPalmLeaf");
        res.put("paper","MaterialPaper");
        res.put("nep_multi_layered_paper","MaterialMultiLayerPaper");
        res.put("corypha_palm_leaf","MaterialCoryphaPalmLeaf");
        res.put("mixed","MaterialMixed");
        return res;
    }

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

    public static final String rKTsToBDR(String rKTs) {
        if (rKTs == null || rKTs.isEmpty() || rKTs.contains("?") || rKTs.contains("&"))
            return null;
        rKTs = rKTs.trim();
        if (rKTsRIDMap.containsKey(rKTs)) {
            return rKTsRIDMap.get(rKTs);
        }
        final String rktsid = String.format("%04d", Integer.parseInt(rKTs.substring(1)));
        if (rKTs.startsWith("K")) {
            return "W0RKA"+rktsid;
        }
        return "W0RTA"+rktsid;
    }

    public static final List<Resource> getResourcesFromLine(String[] line) {
        final Model workModel = ModelFactory.createDefaultModel();
        final List<Resource> res = new ArrayList<>();
        CommonMigration.setPrefixes(workModel);
        String rid="W0CDL"+line[0];
        Resource work = workModel.createResource(BDR+rid);
        res.add(work);
        workModel.add(work,workModel.createProperty(BDO,"workCatalogInfo"),workModel.createLiteral(line[1], "en"));
        workModel.add(work,workModel.createProperty(BDO,"workBiblioNote"),workModel.createLiteral(line[2], "en"));
        workModel.add(work, RDF.type, workModel.createResource(BDO+"Work"));
        String mainTitle=line[6];
        String title=line[3];
        Literal l=null;
        if(title.endsWith("@en")) {
            l=workModel.createLiteral(title);
        }else {
            l=workModel.createLiteral(title,"sa-x-iast");
        }
        String altTitle=line[7];
        Resource titleR = workModel.createResource();
        workModel.add(work, workModel.createProperty(BDO, "workTitle"), titleR);
        workModel.add(work,workModel.createProperty(CommonMigration.SKOS_PREFIX,"prefLabel"),l);
        if(!altTitle.equals("")) {
            workModel.add(work,workModel.createProperty(CommonMigration.SKOS_PREFIX,"altLabel"),workModel.createLiteral(altTitle, "sa-x-iast"));
        }
        if(mainTitle.equals("")) {
            workModel.add(titleR, workModel.createProperty(BDO, "WorkBibliographicalTitle"), l);
        }else {
            workModel.add(titleR, workModel.createProperty(BDO, "WorkBibliographicalTitle"), workModel.createLiteral(mainTitle, "sa-x-iast"));
        }
        final String abstractWorkRID = rKTsToBDR(line[4]);
        if (abstractWorkRID != null) {
            SymetricNormalization.addSymetricProperty(workModel, "workExpressionOf", rid, abstractWorkRID, null);
        }
        if(!line[5].equals("")) {
            workModel.add(work, workModel.createProperty(BDO, "workIsAbout"), workModel.createResource(CommonMigration.RESOURCE_PREFIX+line[5]));
        }
        workModel.add(work, workModel.createProperty(ADM, "license"), workModel.createResource(BDR+"LicenseCopyrighted")); // ?
        workModel.add(work, workModel.getProperty(ADM+"status"), workModel.createResource(BDR+"StatusReleased"));
        workModel.add(work, workModel.createProperty(ADM, "access"), workModel.createResource(BDR+"AccessOpen"));
        workModel.add(work, workModel.createProperty(BDO, "workMaterial"), workModel.createResource(BDR+materials.get(line[9])));
        if(!line[14].equals("")) {
            workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+scripts.get(line[14])));
        }
        if (!line[19].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimWidth"), line[19].replace(',','.').trim(), XSDDatatype.XSDdecimal);
        }
        if (!line[18].isEmpty()) {
            work.addProperty(workModel.createProperty(BDO, "workDimHeight"), line[18].replace(',','.').trim(), XSDDatatype.XSDdecimal);
        }
        final Model itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel);
        final String itemRID = "I0CDL"+rid;
        if (WorkMigration.addWorkHasItem) {
            workModel.add(work, workModel.createProperty(BDO, "workHasItemImageAsset"), workModel.createResource(BDR+itemRID));
        }
        Resource item = itemModel.createResource(BDR+itemRID);
        res.add(item);
        itemModel.add(item, RDF.type, itemModel.createResource(BDO+"ItemImageAsset"));
        final String volumeRID = "V0CDL"+itemRID.substring(1);
        Resource volume = itemModel.createResource(BDR+volumeRID);
        itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"VolumeImageAsset"));
        if (ImagegroupMigration.addVolumeOf)
            itemModel.add(volume, itemModel.createProperty(BDO, "volumeOf"), item);
        if (ImagegroupMigration.addItemHasVolume)
            itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), volume);
        itemModel.add(volume, itemModel.createProperty(BDO, "hasIIIFManifest"), itemModel.createResource(line[8]));
        itemModel.add(volume, itemModel.createProperty(BDO, "volumeNumber"), itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        if (WorkMigration.addItemForWork) {
            itemModel.add(item, itemModel.createProperty(BDO, "itemImageAssetForWork"), itemModel.createResource(BDR+rid));
        }
        if(!line[10].equals("") && !line[11].equals("")) {
            Resource event = workModel.createResource();
            workModel.add(work, workModel.createProperty(BDO, "workEvent"), event);
            workModel.add(event, RDF.type, workModel.createResource(BDO+"PublishedEvent"));
            workModel.add(event, workModel.createProperty(BDO, "notAfter"), workModel.createTypedLiteral(line[11], XSDDatatype.XSDinteger));
            workModel.add(event, workModel.createProperty(BDO, "notBefore"), workModel.createTypedLiteral(line[10], XSDDatatype.XSDinteger));
        }
        workModel.write(System.out,"TURTLE");
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
                case "http://purl.bdrc.io/ontology/core/VolumeImageAsset":
                    final String volumeOutfileName = MigrationApp.getDstFileName("volume", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), volumeOutfileName, "volume");
                    break;
                case "http://purl.bdrc.io/ontology/core/ItemImageAsset":
                    final String itemOutfileName = MigrationApp.getDstFileName("item", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), itemOutfileName, "item");
                    break;
            }
        }
    }
}
