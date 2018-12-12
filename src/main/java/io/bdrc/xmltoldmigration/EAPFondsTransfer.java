package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class EAPFondsTransfer {

    public static HashMap<String,HashMap<String,String[]>> seriesByCollections;
    public static  List<String[]> lines;
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    private static final String SKOS = CommonMigration.SKOS_PREFIX;
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
        HashMap<String,HashMap<String,String[]>> map=new HashMap<>();
        for(String[] line:lines) {
            if(line[1].equals("Fonds")) {
                map.put(line[0], new HashMap<>());
            }
        }
        Set<String> set=map.keySet();
        for(String s:set) {
            HashMap<String,String[]> mp=map.get(s);
            for(String[] line:lines) {
                if(line[3].equals(s)) {
                    mp.put(line[0], line);
                }
            }
            map.put(s,mp);
        }
        seriesByCollections=map;
        set=seriesByCollections.keySet();
        map=new HashMap<>();
        for(String s:set) {
            HashMap<String,String[]> mp1=seriesByCollections.get(s);
            Set<String> seriesKeys=mp1.keySet();
            for(String key:seriesKeys) {
                System.out.println(key);
            }
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
            String uri=r.getProperty(RDF.type).getObject().asResource().getURI();
            System.out.println(r.getProperty(RDF.type).getObject().asResource().getURI());
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

    public List<Resource> getResources(){
        Set<String> keys=seriesByCollections.keySet();
        List<Resource> res = new ArrayList<>();
        for(String key:keys) {
            HashMap<String,String[]> map=seriesByCollections.get(key);
            Set<String> seriesKeys=map.keySet();
            for(String serie:seriesKeys) {
                Model itemModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(itemModel);
                Resource item = itemModel.createResource(BDR+"I"+serie);
                itemModel.add(item, itemModel.createProperty(BDO, "itemImageAssetForWork"), itemModel.createResource(BDR+"W"+serie));
                itemModel.add(item, RDF.type, itemModel.createResource(BDO+"ItemImageAsset"));
                Model workModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(workModel);
                Resource work = workModel.createResource(BDR+"W"+serie);
                workModel.add(work, RDF.type, workModel.createResource(BDO+"Work"));
                workModel.add(work, workModel.createProperty(BDO,"workHasItemImageAsset"), item);
                workModel.add(work, workModel.createProperty(ADM, "license"), workModel.createResource(BDR+"PublicDomain")); // ?
                workModel.add(work, workModel.getProperty(ADM+"status"), workModel.createResource(BDR+"StatusReleased"));
                workModel.add(work, workModel.createProperty(ADM, "access"), workModel.createResource(BDR+"AccessOpen"));
                workModel.add(work, workModel.createProperty(BDO, "workType"), workModel.createResource(BDR+"WorkTypePublishedWork"));
                Resource noteR = workModel.createResource();
                noteR.addLiteral(workModel.createProperty(BDO, "noteText"), workModel.createLiteral(seriesByCollections.get(key).get(serie)[36],"en"));
                workModel.add(work, workModel.createProperty(BDO, "note"), noteR);
                workModel.add(work, workModel.createProperty(SKOS, "prefLabel"), workModel.createLiteral(seriesByCollections.get(key).get(serie)[39],"en"));
                HashMap<String, ArrayList<String[]>> vols=getVolumes(serie);
                Set<String> serVol=vols.keySet();
                int numVol=0;
                for(String ser:serVol) {
                    ArrayList<String[]> volume=vols.get(ser);
                    for(int x=0;x<volume.size();x++) {
                        Model volModel = ModelFactory.createDefaultModel();
                        CommonMigration.setPrefixes(volModel);
                        Resource vol = volModel.createResource(BDR+"V"+volume.get(x)[0]);
                        itemModel.add(item, itemModel.createProperty(BDO, "itemHasVolume"), vol);
                        volModel.add(vol, RDF.type, volModel.createResource(BDO+"VolumeImageAsset"));
                        String tmp=volume.get(x)[18];
                        String name=volume.get(x)[39];
                        String ref=volume.get(x)[4].replace('/', '-');
                        tmp=tmp.substring(tmp.indexOf("containing")).split(" ")[1];
                        volModel.add(vol, volModel.createProperty(BDO,"imageCount"),volModel.createTypedLiteral(Integer.parseInt(tmp)));
                        volModel.add(vol, volModel.createProperty(BDO,"hasIIIFManifest"),volModel.createResource(ManifestPREF+ref+"/manifest"));
                        volModel.add(vol, volModel.createProperty(BDO,"volumeName"),volModel.createLiteral(name));
                        volModel.add(vol, volModel.createProperty(BDO,"volumeNumber"),volModel.createTypedLiteral(Integer.parseInt(volume.get(x)[37])));
                        volModel.add(vol, volModel.createProperty(BDO,"volumeOf"),volModel.createResource(BDR+"I"+ser));
                        res.add(vol);
                        numVol++;
                    }
                }
                itemModel.add(item, itemModel.createProperty(BDO, "itemVolumes"), itemModel.createTypedLiteral(numVol));
                workModel.add(work, workModel.createProperty(BDO, "workNumberOfVolumes"), workModel.createTypedLiteral(numVol));
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
