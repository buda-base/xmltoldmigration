package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.FacetType;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.yearLit;
import org.apache.jena.rdf.model.Literal;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

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

public class EAPFondsTransfer {

    public HashMap<String,HashMap<String,String[]>> seriesByCollections;
    public HashMap<String,String[]> seriesLines;
    public List<String[]> lines;
    public boolean simplified; // for the eap2.csv file that contains less columns
    
    private static final String ManifestPREFIX = "https://eap.bl.uk/archive-file/";
    public static final String ORIG_URL_BASE = "https://eap.bl.uk/collection/";

    public EAPFondsTransfer(String filename, boolean simplified) throws IOException {
        this.simplified = simplified;
        CSVReader reader;
        CSVParser parser = new CSVParserBuilder().build();
        InputStream inputStream = EAPFondsTransfer.class.getClassLoader().getResourceAsStream(filename);
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        reader = new CSVReaderBuilder(in)
                .withCSVParser(parser)
                .build();
        lines=reader.readAll();
        getSeriesByFonds();
    }

    public void getSeriesByFonds() throws IOException{
        seriesByCollections = new HashMap<>();
        for (String[] line:lines) {
            String type = this.simplified ? line[0] : line[1];
            if (type.toLowerCase().equals("fonds")) {
                final HashMap<String,String[]> res = new HashMap<>();
                res.put("fondsline", line);
                seriesByCollections.put(simplified ? line[1] : line[0], res);
            }
        }
        for(String s : seriesByCollections.keySet()) {
            HashMap<String,String[]> mp = seriesByCollections.get(s);
            for(String[] line:lines) {
                if ((!simplified && line[3].equals(s)) || (simplified && line[0].toLowerCase().startsWith("serie") && line[1].startsWith(s+"/"))) {
                    mp.put(simplified ? line[1] : line[0], line);
                }
            }
            seriesByCollections.put(s,mp);
        }
    }

    public List<String[]> getVolumes(String serie){
        List<String[]> volumes = new ArrayList<>();
        for(String[] line:lines) {
            if ((!simplified && line[3].equals(serie)) || simplified && line[0].toLowerCase().startsWith("file") && line[1].startsWith(serie+"/")) {
                volumes.add(line);
            }
        }
        return volumes;
    }
    
    public final void writeEAPFiles(List<Resource> resources) {
        int nbresources = 0;
        for(Resource r: resources) {
            String uri=r.getProperty(RDF.type).getObject().asResource().getLocalName();
            //r.getModel().write(System.out, "TURTLE");
            nbresources += 1;
            switch(uri) {
                case "Work":
                    final String workOutfileName = MigrationApp.getDstFileName("work", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), workOutfileName, "work");
                    break;
                case "Instance":
                case "PhysicalInstance":
                    final String instanceOutfileName = MigrationApp.getDstFileName("instance", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), instanceOutfileName, "instance");
                    break;
                case "ImageInstance":
                    final String iInstanceOutfileName = MigrationApp.getDstFileName("iinstance", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), iInstanceOutfileName, "iinstance");
                    break;
                case "Collection":
                    final String colOutFileName = MigrationApp.getDstFileName("product", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), colOutFileName, "product");
                    break;
            }
        }
        System.out.println("wrote "+nbresources+" files of eap funds");
    }

    public Literal getLiteral(String title, Model m) {
        int firstChar = title.codePointAt(0);
        String lang = "bo-x-ewts";
        if (firstChar > 3840 && firstChar < 4095) {
            lang = "bo";
        }
        if (title.endsWith("@en")) {
            title = title.substring(0, title.length()-3);
            lang = "en";
        }
        return m.createLiteral(title, lang);
    }
    
    public Integer getVolNum(String[] line) {
        if (simplified) {
            String id = line[1];
            int lastSlashIdx = id.lastIndexOf('/');
            return Integer.parseInt(id.substring(lastSlashIdx+1));
        } else {
            return Integer.parseInt(line[37]);
        }
    }
    
    public void addEvent(String[] line, Model workModel, Resource work) {
        String notBefore = simplified ? line[3] : line[38];
        String notAfter = simplified ? line[4] : line[17];
        if (!notBefore.isEmpty() && !notAfter.isEmpty()) {
            Resource event = workModel.createResource(BDR+"E"+work.getLocalName()+"_01");
            workModel.add(work, workModel.createProperty(BDO, "workEvent"), event);
            workModel.add(event, RDF.type, workModel.createResource(BDO+"CopyEvent"));
            if (simplified && !line[13].isEmpty()) {
                workModel.add(event, workModel.createProperty(BDO, "eventWhere"), workModel.createResource(BDR+line[13]));
            }
            // TODO: add locations in other eap sheets
            if (notBefore.equals(notAfter)) {
                workModel.add(event, workModel.createProperty(BDO, "onYear"), yearLit(workModel, notBefore));    
            } else {
                workModel.add(event, workModel.createProperty(BDO, "notBefore"), yearLit(workModel, notBefore));    
                workModel.add(event, workModel.createProperty(BDO, "notAfter"), yearLit(workModel, notAfter));    
            }
        }
    }
    
    public void addNote(String[] line, Model workModel, Resource work) {
        String noteText;
        if (simplified) {
            noteText = line[10]+line[11]+line[12];
        } else {
            noteText = line[36];
        }
        if (!noteText.isEmpty()) {
            Resource noteR = getFacetNode(FacetType.NOTE,  work);
            noteR.addLiteral(workModel.createProperty(BDO, "noteText"), workModel.createLiteral(noteText,"en"));
            workModel.add(work, workModel.createProperty(BDO, "note"), noteR);
        }
    }
    
    public void addSeriesC(String[] serieLine, List<Resource> res, String serie, String prrid) {
        String serieID = (simplified ? serieLine[1] : serieLine[4]).replace('/', '-');
        // Work model
        Model workModel = ModelFactory.createDefaultModel();
        setPrefixes(workModel);
        Resource work = createRoot(workModel, BDR+"MW"+serieID, BDO+"Instance");
        Resource admWork = createAdminRoot(work);
        res.add(work);

        String abstractWorkRID = "WA"+serieID;
        Model mA = ModelFactory.createDefaultModel();
        setPrefixes(mA);
        Resource workA = createRoot(mA, BDR+abstractWorkRID, BDO+"Work");
        Resource admWorkA = createAdminRoot(work);
        res.add(workA);
        work.addProperty(workModel.createProperty(BDO, "instanceOf"), workA);
        workA.addProperty(workModel.createProperty(BDO, "workHasInstance"), work);
        // bdo:Work
        mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangBo"));
        addReleased(mA, admWorkA);
        mA.add(admWorkA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_EAP_metadata")); // ?
        
        // Work adm:AdminData
        Resource ldEAPc = workModel.createResource(BDA+"LD_EAP_content");
        Resource ldEAPm = workModel.createResource(BDA+"LD_EAP_metadata");
        workModel.add(admWork, RDF.type, workModel.createResource(ADM+"AdminData"));
        workModel.add(admWork, workModel.getProperty(ADM+"status"), workModel.createResource(BDR+"StatusReleased"));
        workModel.add(admWork, workModel.createProperty(ADM, "metadataLegal"), ldEAPm); // ?
        String origUrl = ORIG_URL_BASE+serieID;
        workModel.add(admWork, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));                
        
        addNote(serieLine, workModel, work);
        String name = simplified ? serieLine[9] : serieLine[39];
        if (name.startsWith("bka' 'gyur")) {
            workModel.add(work, workModel.createProperty(BDO, "workGenre"), workModel.createResource(BDR+"T2423"));
        }
        workModel.add(work, SKOS.prefLabel, getLiteral(name, workModel));
        addEvent(serieLine, workModel, work);
        
        // Item model
        Model itemModel = ModelFactory.createDefaultModel();
        setPrefixes(itemModel);
        Resource item = createRoot(itemModel, BDR+"W"+serieID, BDO+"ImageInstance");
        Resource admItem = createAdminRoot(item);
        res.add(item);

        workModel.add(work, workModel.createProperty(BDO,"instanceHasReproduction"), item);

        // Item adm:AdminData
        itemModel.add(admItem, RDF.type, itemModel.createResource(ADM+"AdminData"));
        itemModel.add(admItem, itemModel.getProperty(ADM+"status"), itemModel.createResource(BDR+"StatusReleased"));
        itemModel.add(admItem, itemModel.createProperty(ADM, "contentLegal"), ldEAPc);
        itemModel.add(admItem, itemModel.createProperty(ADM, "metadataLegal"), ldEAPm);
        itemModel.addLiteral(admItem, itemModel.getProperty(ADM+"restrictedInChina"), false);
        itemModel.add(admItem, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
        
        itemModel.add(item, itemModel.createProperty(BDO, "instanceOf"), itemModel.createResource(BDR+abstractWorkRID));
        
        itemModel.add(item, itemModel.createProperty(BDO, "inCollection"), itemModel.createResource(BDR+prrid));
        
        List<String[]> volumes = getVolumes(serie);
        int numVol=0;
        for(int x=0;x<volumes.size();x++) {
            final String[] volume = volumes.get(x);
            String ref=(simplified ? volume[1] : volume[4]).replace('/', '-');
            Resource vol = itemModel.createResource(BDR+"I0"+ref);
            itemModel.add(item, itemModel.createProperty(BDO, "instanceHasVolume"), vol);
            itemModel.add(vol, RDF.type, itemModel.createResource(BDO+"ImageGroup"));
            String volName = simplified ? volume[9] : volume[39];
            itemModel.add(vol, itemModel.createProperty(BDO,"hasIIIFManifest"),itemModel.createResource(ManifestPREFIX+ref+"/manifest"));
            //itemModel.add(vol, itemModel.createProperty(BDO,"volumeName"),getLiteral(name, workModel));
            itemModel.add(vol, SKOS.prefLabel,getLiteral(volName, workModel));
            itemModel.add(vol, itemModel.createProperty(BDO,"volumeNumber"),itemModel.createTypedLiteral(getVolNum(volume), XSDDatatype.XSDinteger));
            itemModel.add(vol, itemModel.createProperty(BDO,"volumeOf"),item);
            res.add(vol);
            
            // Volume adm:AdminData
            Resource admVol = getAdminData(vol);
            itemModel.add(admVol, RDF.type, itemModel.createResource(ADM+"AdminData"));
            numVol++;
        }
        itemModel.add(item, itemModel.createProperty(BDO, "itemVolumes"), itemModel.createTypedLiteral(numVol));
        workModel.add(work, workModel.createProperty(BDO, "numberOfVolumes"), workModel.createTypedLiteral(numVol, XSDDatatype.XSDinteger));
    }

    public void addSeries(String[] serieLine, List<Resource> res, String serie, String prrid) {
        List<String[]> works = getVolumes(serie);
        for(int x=0;x<works.size();x++) {
            final String[] workLine = works.get(x);
            String ref=(simplified ? workLine[1] : workLine[4]).replace('/', '-');
            Model workModel = ModelFactory.createDefaultModel();
            setPrefixes(workModel);
            Model itemModel = ModelFactory.createDefaultModel();
            setPrefixes(itemModel);
            Resource ldEAPc = workModel.createResource(BDA+"LD_EAP_content");
            Resource ldEAPm = workModel.createResource(BDA+"LD_EAP_metadata");
            Resource work = createRoot(workModel, BDR+"MW"+ref, BDO+"Instance");
            Resource workAdm = createAdminRoot(work);
            
            String abstractWorkRID = "WA"+ref;
            Model mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            Resource workA = createRoot(mA, BDR+abstractWorkRID, BDO+"Work");
            Resource admWorkA = createAdminRoot(work);
            res.add(workA);
            work.addProperty(workModel.createProperty(BDO, "instanceOf"), workA);
            workA.addProperty(workModel.createProperty(BDO, "workHasInstance"), work);
            // bdo:Work
            mA.add(workA, mA.createProperty(BDO, "language"), workModel.createResource(BDR+"LangBo"));
            addReleased(mA, admWorkA);
            mA.add(admWorkA, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_EAP_metadata")); // ?
            
            workModel.add(workAdm, RDF.type, workModel.createResource(ADM+"AdminData"));
            workModel.add(workAdm, workModel.getProperty(ADM+"status"), workModel.createResource(BDR+"StatusReleased"));
            workModel.add(workAdm, workModel.createProperty(ADM, "metadataLegal"), ldEAPm); // ?
            String origUrl = ManifestPREFIX+ref; // for files, not collections
            workModel.add(workAdm, workModel.createProperty(ADM, "originalRecord"), workModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));                
            //workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"BoTibt"));
            addNote(serieLine, workModel, work);
            //workModel.add(work, SKOS.prefLabel, getLiteral(simplified ? serieLine[9] : serieLine[39], workModel));
            addEvent(serieLine, workModel, work);
            Resource item = createRoot(itemModel, BDR+"W"+ref, BDO+"ImageInstance");
            Resource itemAdm = createAdminRoot(item);
            
            itemModel.add(item, itemModel.createProperty(BDO, "inCollection"), itemModel.createResource(BDR+prrid));
            
            itemModel.add(item, itemModel.createProperty(BDO, "instanceReproductionOf"), itemModel.createResource(BDR+"MW"+ref));
            itemModel.add(itemAdm, RDF.type, itemModel.createResource(ADM+"AdminData"));
            itemModel.add(itemAdm, itemModel.getProperty(ADM+"status"), itemModel.createResource(BDR+"StatusReleased"));
            itemModel.addLiteral(itemAdm, itemModel.getProperty(ADM+"restrictedInChina"), false);
            itemModel.add(itemAdm, itemModel.createProperty(ADM, "access"), itemModel.createResource(BDA + "AccessOpen"));
            itemModel.add(itemAdm, itemModel.createProperty(ADM, "contentLegal"), ldEAPc);
            itemModel.add(itemAdm, itemModel.createProperty(ADM, "metadataLegal"), ldEAPm);
            Resource volume = itemModel.createResource(BDR+"I"+ref);
            itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), volume);
            itemModel.add(volume, RDF.type, itemModel.createResource(BDO+"ImageGroup"));
            String name = simplified ? workLine[9] : workLine[39];
            workModel.add(work, SKOS.prefLabel,getLiteral(name, workModel));
            itemModel.add(volume, itemModel.createProperty(BDO,"hasIIIFManifest"),itemModel.createResource(ManifestPREFIX+ref+"/manifest"));
            itemModel.add(volume, itemModel.createProperty(BDO,"volumeNumber"),itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
            itemModel.add(volume, itemModel.createProperty(BDO,"volumeOf"),item);
            res.add(item);
            res.add(work);
            
            // Volume adm:AdminData
            Resource admVol = getAdminData(volume);
            itemModel.add(admVol, RDF.type, itemModel.createResource(ADM+"AdminData"));
            origUrl = ManifestPREFIX+ref;
            itemModel.add(admVol, itemModel.createProperty(ADM, "originalRecord"), itemModel.createTypedLiteral(origUrl, XSDDatatype.XSDanyURI));
            //res.add(work);
            //res.add(item);
        }
    }
    
    public Resource writeProduct(String[] line) {
        Model mA = ModelFactory.createDefaultModel();
        setPrefixes(mA);
        String serieID = (simplified ? line[1] : line[4]).replace('/', '-');
        String prrid="PREAP"+serieID;
        Resource p = createRoot(mA, BDR+prrid, BDO+"Collection");
        Resource padm = createAdminRoot(p);
        mA.add(padm, mA.createProperty(ADM, "metadataLegal"), mA.createResource(BDA + "LD_EAP_metadata"));
        String name = simplified ? line[9] : line[39];
        if (name.endsWith("@en")) {
            name = name.substring(0, name.length()-3);
        }
        mA.add(p, SKOS.prefLabel, mA.createLiteral(name, "en"));
        mA.add(p, mA.createProperty(ADM, "originalRecord"), mA.createTypedLiteral(ORIG_URL_BASE+serieID, XSDDatatype.XSDanyURI));
        return p;
    }
    
    public List<Resource> getResources(){
        Set<String> keys=seriesByCollections.keySet();
        List<Resource> res = new ArrayList<>();
        for(String key:keys) {
            HashMap<String,String[]> map=seriesByCollections.get(key);
            String[] fondsline = map.get("fondsline");
            String prrid="PREAP"+(simplified ? fondsline[1] : fondsline[4]).replace('/', '-');
            Resource p = writeProduct(fondsline);
            res.add(p);
            Set<String> seriesKeys=map.keySet();
            for(String serie:seriesKeys) {
                String[] serieLine = seriesByCollections.get(key).get(serie);
                String type = this.simplified ? serieLine[0] : serieLine[1];
                if (type.toLowerCase().startsWith("seriesc")) {
                    addSeriesC(serieLine, res, serie, prrid);
                } else {
                    addSeries(serieLine, res, serie, prrid);
                }

            }
        }
        return res;
    }

    public static void EAPFondsDoTransfer() throws IOException {
        EAPFondsTransfer tr = new EAPFondsTransfer("EAP310.csv", false);
        tr.writeEAPFiles(tr.getResources());
        tr = new EAPFondsTransfer("EAP039.csv", false);
        tr.writeEAPFiles(tr.getResources());
        tr = new EAPFondsTransfer("eap2.csv", true);
        tr.writeEAPFiles(tr.getResources());
    }

}
