package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class EAPFondsTransfer {

    public static HashMap<String,HashMap<String,String[]>> seriesByCollections;
    public static HashMap<String,String[]> seriesLines;
    public static  List<String[]> lines;
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    private static final String ManifestPREF="https://eap.bl.uk/archive-file/";

    public EAPFondsTransfer(String filename) throws IOException {
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
        for(String[] line:lines) {
            if(line[1].equals("Fonds")) {
                seriesByCollections.put(line[0], new HashMap<>());
            }
        }
        for(String s : seriesByCollections.keySet()) {
            HashMap<String,String[]> mp = seriesByCollections.get(s);
            for(String[] line:lines) {
                if(line[3].equals(s)) {
                    mp.put(line[0], line);
                }
            }
            seriesByCollections.put(s,mp);
        }
    }
    public HashMap<String, ArrayList<String[]>> getVolumes(String serie){
        HashMap<String, ArrayList<String[]>> vols=new HashMap<>();
        ArrayList<String[]> ll=new ArrayList<>();
        String key="";
        for(String[] line:lines) {
            key=line[3];
            if(line[3].equals(serie)) {
                ll.add(line);
            }
        }
        vols.put(key, ll);
        return vols;
    }

    public final void writeEAPFiles(List<Resource> resources) {
        for(Resource r: resources) {
            String uri=r.getProperty(RDF.type).getObject().asResource().getLocalName();
            //r.getModel().write(System.out, "TURTLE");
            switch(uri) {
                case "Work":
                    final String workOutfileName = MigrationApp.getDstFileName("work", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), workOutfileName, "work");
                    break;
                case "ItemImageAsset":
                case "Item":
                    final String itemOutfileName = MigrationApp.getDstFileName("item", r.getLocalName());
                    MigrationHelpers.outputOneModel(r.getModel(), r.getLocalName(), itemOutfileName, "item");
                    break;
            }
        }
    }

    public List<Resource> getResources(){
        Set<String> keys=seriesByCollections.keySet();
        List<Resource> res = new ArrayList<>();
        for(String key:keys) {
            HashMap<String,String[]> map=seriesByCollections.get(key);
            Set<String> seriesKeys=map.keySet();
            for(String serie:seriesKeys) {
                Model itemModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(itemModel);
                String[] serieLine = seriesByCollections.get(key).get(serie);
                //System.out.println(Arrays.toString(serieLine));
                String serieID = serieLine[4].replace('/', '-');
                Resource item = itemModel.createResource(BDR+"I"+serieID);
                itemModel.add(item, itemModel.createProperty(BDO, "itemImageAssetForWork"), itemModel.createResource(BDR+"W"+serieID));
                itemModel.add(item, RDF.type, itemModel.createResource(BDO+"ItemImageAsset"));
                Model workModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(workModel);
                Resource work = workModel.createResource(BDR+"W"+serie);
                workModel.add(work, RDF.type, workModel.createResource(BDO+"Work"));
                workModel.add(work, workModel.createProperty(BDO,"workHasItemImageAsset"), item);
                workModel.add(work, workModel.createProperty(BDO,"contentProvider"), workModel.createResource(BDR+"CPEAP"));
                workModel.add(work, workModel.createProperty(BDO, "originalRecord"), workModel.createTypedLiteral("https://eap.bl.uk/collection/"+serieID, XSDDatatype.XSDanyURI));
                workModel.add(work, workModel.createProperty(ADM, "license"), workModel.createResource(BDR+"PublicDomain")); // ?
                workModel.add(work, workModel.getProperty(ADM+"status"), workModel.createResource(BDR+"StatusReleased"));
                workModel.add(work, workModel.createProperty(ADM, "access"), workModel.createResource(BDR+"AccessOpen"));
                workModel.add(work, workModel.createProperty(BDO, "workType"), workModel.createResource(BDR+"WorkTypePublishedWork"));
                workModel.add(work, workModel.createProperty(BDO, "workLangScript"), workModel.createResource(BDR+"BoTibt"));
                Resource noteR = workModel.createResource();
                noteR.addLiteral(workModel.createProperty(BDO, "noteText"), workModel.createLiteral(serieLine[36],"en"));
                workModel.add(work, workModel.createProperty(BDO, "note"), noteR);
                workModel.add(work, SKOS.prefLabel, workModel.createLiteral(serieLine[39],"en"));
                HashMap<String, ArrayList<String[]>> vols=getVolumes(serie);
                Set<String> serVol=vols.keySet();
                int numVol=0;
                for(String ser:serVol) {
                    ArrayList<String[]> volumes=vols.get(ser);
                    for(int x=0;x<volumes.size();x++) {
                        final String[] volume = volumes.get(x);
                        String ref=volume[4].replace('/', '-');
                        //System.out.println(Arrays.toString(volume));
                        Resource vol = itemModel.createResource(BDR+"V"+ref);
                        itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), vol);
                        itemModel.add(vol, RDF.type, itemModel.createResource(BDO+"VolumeImageAsset"));
                        String tmp=volume[18];
                        String name=volume[39];
                        // TODO: properly tag lang
                        //tmp=tmp.substring(tmp.indexOf("containing")).split(" ")[1];
                        //itemModel.add(vol, itemModel.createProperty(BDO,"imageCount"),itemModel.createTypedLiteral(Integer.parseInt(tmp), XSDDatatype.XSDinteger));
                        itemModel.add(vol, workModel.createProperty(BDO,"originalRecord"), itemModel.createTypedLiteral(ManifestPREF+ref, XSDDatatype.XSDanyURI));
                        itemModel.add(vol, itemModel.createProperty(BDO,"hasIIIFManifest"),itemModel.createResource(ManifestPREF+ref+"/manifest"));
                        itemModel.add(vol, itemModel.createProperty(BDO,"volumeName"),itemModel.createLiteral(name, "en"));
                        itemModel.add(vol, itemModel.createProperty(BDO,"volumeNumber"),itemModel.createTypedLiteral(Integer.parseInt(volume[37]), XSDDatatype.XSDinteger));
                        itemModel.add(vol, itemModel.createProperty(BDO,"volumeOf"),item);
                        res.add(vol);
                        numVol++;
                    }
                }
                itemModel.add(item, itemModel.createProperty(BDO, "itemVolumes"), itemModel.createTypedLiteral(numVol));
                workModel.add(work, workModel.createProperty(BDO, "workNumberOfVolumes"), workModel.createTypedLiteral(numVol, XSDDatatype.XSDinteger));
                res.add(item);
                res.add(work);
            }
        }
        return res;
    }

    public static void EAPFondsDoTransfer() throws IOException {
        EAPFondsTransfer tr = new EAPFondsTransfer("EAP310.csv");
        tr.writeEAPFiles(tr.getResources());
        tr= new EAPFondsTransfer("EAP039.csv");
        tr.writeEAPFiles(tr.getResources());
    }

}
