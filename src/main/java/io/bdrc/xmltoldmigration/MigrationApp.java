package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Hello world!
 *
 */
public class MigrationApp 
{
    
    // extract tbrc/ folder of exist-db backup here:
    public static String DATA_DIR = "tbrc/";
    public static String OUTPUT_DIR = "tbrc-jsonld/";

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    
    public static final String CORPORATION = MigrationHelpers.CORPORATION;
    public static final String LINEAGE = MigrationHelpers.LINEAGE;
    public static final String OFFICE = MigrationHelpers.OFFICE;
    public static final String OUTLINE = MigrationHelpers.OUTLINE;
    public static final String PERSON = MigrationHelpers.PERSON;
    public static final String PLACE = MigrationHelpers.PLACE;
    public static final String SCANREQUEST = MigrationHelpers.SCANREQUEST;
    public static final String TOPIC = MigrationHelpers.TOPIC;
    public static final String VOLUMES = MigrationHelpers.VOLUMES;
    public static final String ITEMS = MigrationHelpers.ITEMS;
    public static final String WORK = MigrationHelpers.WORK;
    
    public static OntModel ontology = null;
    
    
    public static void init() {
        ontology = MigrationHelpers.getOntologyModel();
    }
    
    public static void createDirIfNotExists(String dir) {
        File theDir = new File(dir);
        if (!theDir.exists()) {
            System.out.println("creating directory: " + dir);
            try{
                theDir.mkdir();
            } 
            catch(SecurityException se){
                System.err.println("could not create directory, please fasten your seat belt");
            }        
        }
    }

    public static void migrateOneFile(File file, String type, String mustStartWith) {
        if (file.isDirectory()) return;
        String fileName = file.getName();
        if (!fileName.startsWith(mustStartWith)) return;
        if (!fileName.endsWith(".xml")) return;
        String baseName = fileName.substring(0, fileName.length()-4);
        String outfileName = baseName+".jsonld";
        outfileName = OUTPUT_DIR+type+"s/"+outfileName;
        Resource item;
        Model itemModel;
        switch(type) {
        case SCANREQUEST:
            Document srd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            String workId = ScanrequestMigration.getWork(srd);
            if (workId.isEmpty()) return;
            String volumesFileName = OUTPUT_DIR+ITEMS+"/I"+workId.substring(1)+".jsonld";
            File workFile = new File(volumesFileName);
            if (!workFile.exists()) {
                //System.err.println("ignoring scan request for unreleased "+workId);
                return;
            }
            itemModel = MigrationHelpers.modelFromFileName(volumesFileName);
            if (itemModel == null) return;
            item = itemModel.getResource(BDR+"I"+workId.substring(1)+"_001");
            itemModel = ScanrequestMigration.MigrateScanrequest(srd, itemModel, item);
            MigrationHelpers.modelToFileName(itemModel, volumesFileName, ITEMS, true);
            break;
        case WORK:
            Document d = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            Element root = d.getDocumentElement();
            if (!MigrationHelpers.mustBeMigrated(root, "work")) return;
            Model m = null;
            try {
                m = MigrationHelpers.xmlToRdf(d, type);
            } catch (IllegalArgumentException e) {
                System.err.println("error in "+fileName+" "+e.getMessage());
                return;
            }
            
            // migrate volumes
            Map<String,String> vols = WorkMigration.getImageGroupList(d);
            if (vols.size() > 0) {
                itemModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(itemModel);
                String itemName = "I"+baseName.substring(1)+"_001";
                item = itemModel.createResource(BDR+itemName);
                itemModel.add(item, RDF.type, itemModel.createResource(BDO + "ItemImageAsset"));
                for (Map.Entry<String,String> vol : vols.entrySet()) {
                    String imagegroup = vol.getKey();
                    String imagegroupFileName = DATA_DIR+"tbrc-imagegroups/"+imagegroup+".xml";
                    File imagegroupFile = new File(imagegroupFileName);
                    if (!imagegroupFile.exists()) {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "imagegroup", "image group `"+imagegroupFileName+"` referenced but absent from database");
                        continue;
                    }
                    d = MigrationHelpers.documentFromFileName(imagegroupFileName);
                    ImagegroupMigration.MigrateImagegroup(d, itemModel, item, imagegroup, vol.getValue(), itemName);
                }
                String volOutfileName = OUTPUT_DIR+ITEMS+"/"+itemName+".jsonld";
                MigrationHelpers.modelToFileName(itemModel, volOutfileName, "item", true);
            }
            
            // migrate pubinfo
            String pubinfoFileName = DATA_DIR+"tbrc-pubinfos/MW"+fileName.substring(1);
            File pubinfoFile = new File(pubinfoFileName);
            if (!pubinfoFile.exists()) {
                MigrationHelpers.writeLog("missing "+pubinfoFileName);
                MigrationHelpers.modelToFileName(m, outfileName, type, true);
                return;
            }
            d = MigrationHelpers.documentFromFileName(pubinfoFileName);
            m = PubinfoMigration.MigratePubinfo(d, m, m.getResource(BDR+baseName));
            MigrationHelpers.modelToFileName(m, outfileName, type, true);
            break;
        default:
            MigrationHelpers.convertOneFile(file.getAbsolutePath(), outfileName, type, true, fileName);
            break;
        }
    }
    
    public static void migrateType(String type, String mustStartWith) {
        createDirIfNotExists(OUTPUT_DIR+type+"s");
        if (type.equals(WORK)) createDirIfNotExists(OUTPUT_DIR+ITEMS);
        File logfile = new File(OUTPUT_DIR+type+"s-migration.log");
        PrintWriter pw;
        try {
            pw = new PrintWriter(logfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        MigrationHelpers.writeLogsTo(pw);
        String dirName = DATA_DIR+"tbrc-"+type+"s";
        File[] files = new File(dirName).listFiles();
        System.out.println("converting "+files.length+" "+type+" files");
        Stream.of(files).parallel().forEach(file -> migrateOneFile(file, type, mustStartWith));
        //Stream.of(files).forEach(file -> migrateOneFile(file, type, mustStartWith));
        pw.close();
    }
    
    public static void main( String[] args )
    {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-useCouchdb")) {
				MigrationHelpers.usecouchdb = true;
			} else if (arg.equals("-datadir")) {
                DATA_DIR = args[i+1];
            } else if (arg.equals("-outdir")) {
                OUTPUT_DIR = args[i+1];
            }
		}
		
        createDirIfNotExists(OUTPUT_DIR);
        long startTime = System.currentTimeMillis();
//        migrateOneFile(new File(DATA_DIR+"tbrc-persons/P1KG16739.xml"), "person", "P");
        // migrate outlines first to have the oldOutlineId -> newOutlineId correspondance, for externals
        migrateType(OUTLINE, "O");
//        migrateType(PERSON, "P");
//    	migrateType(PLACE, "G");
//    	migrateType(OFFICE, "R");
//        migrateType(CORPORATION, "C");
//        migrateType(LINEAGE, "L");
//        migrateType(TOPIC, "T");
//////        migrateOneFile(new File(DATA_DIR+"tbrc-works/W1KG10421.xml"), "work", "W");
////        //migrateOneFile(new File(DATA_DIR+"tbrc-scanrequests/SR1KG10424.xml"), "scanrequest", "SR");
//        migrateType(WORK, "W"); // also does pubinfos and imagegroups
//        migrateType(SCANREQUEST, "SR"); // requires works to be finished
        CommonMigration.speller.close();
        ExceptionHelper.closeAll();
    	long estimatedTime = System.currentTimeMillis() - startTime;
    	System.out.println("done in "+estimatedTime+" ms");
    }
}
