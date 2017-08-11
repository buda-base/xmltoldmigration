package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
    public static String OUTPUT_DIR = "tbrc-ttl/";

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
    
    private static Map<String,Boolean> workCreatedByOutline = new HashMap<>();
    
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
        String outfileName = baseName+".ttl";
        outfileName = OUTPUT_DIR+type+"s/"+outfileName;
        Resource item;
        Model itemModel;
        switch(type) {
        case OUTLINE:
            Document outd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            if (!MigrationHelpers.mustBeMigrated(outd.getDocumentElement(), "outline"))
                return;
            String outWorkId = OutlineMigration.getWorkId(outd);
            if (outWorkId == null || outWorkId.isEmpty()) {
                //ExceptionHelper.logException(ExceptionHelper.ET_GEN, baseName, baseName, "outlineFor", "outline does not reference its main work");
                return;
            }
            Model workModel = null;
            // if order is the same (outlines before works), then no work is migrated yet
//            if (new File(OUTPUT_DIR+"works/"+outWorkId+".ttl").exists()) {
//                workModel = MigrationHelpers.modelFromFileName(OUTPUT_DIR+"works/"+outWorkId+".ttl");
//            }
            Resource work = null;
            if (workModel == null) {
                workModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(workModel);
                work = workModel.createResource(BDR+outWorkId);
            } else {
                work = workModel.getResource(BDR+outWorkId);
            }
            Model outlineModel = OutlineMigration.MigrateOutline(outd, workModel, work);
            if (OutlineMigration.splitOutlines) {
                String outlineFileName = OUTPUT_DIR+"works/"+outWorkId+"_O01.ttl";
                MigrationHelpers.outputOneModel(outlineModel, outWorkId+"_O01", outlineFileName, "work");                
            } else {
                String workFileName = OUTPUT_DIR+"works/"+outWorkId+".ttl";
                MigrationHelpers.outputOneModel(workModel, outWorkId, workFileName, "work");
                workCreatedByOutline.put(outWorkId, true);
            }
            break;
        case SCANREQUEST:
            Document srd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            String workId = ScanrequestMigration.getWork(srd);
            if (workId == null || workId.isEmpty()) 
                return;
            String itemFileName = OUTPUT_DIR+ITEMS+"/I"+workId.substring(1)+".ttl";
            itemModel = MigrationHelpers.modelFromFileName(itemFileName);
            if (itemModel == null)
                return;
            item = itemModel.getResource(BDR+"I"+workId.substring(1)+"_001");
            itemModel = ScanrequestMigration.MigrateScanrequest(srd, itemModel, item);
            MigrationHelpers.modelToFileName(itemModel, itemFileName, ITEMS, MigrationHelpers.OUTPUT_STTL);
            break;
        case WORK:
            Document d = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            Element root = d.getDocumentElement();
            if (!MigrationHelpers.mustBeMigrated(root, "work"))
                return;
            Model m = null;
            if (workCreatedByOutline.containsKey(baseName))
                m = MigrationHelpers.modelFromFileName(OUTPUT_DIR+"works/"+baseName+".ttl");
            if (m == null) {
                m = ModelFactory.createDefaultModel();
            }
            CommonMigration.setPrefixes(m);
            m = WorkMigration.MigrateWork(d, m);
            
            // migrate items
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
                String volOutfileName = OUTPUT_DIR+ITEMS+"/"+itemName+".ttl";
                //MigrationHelpers.modelToFileName(itemModel, volOutfileName, "item", MigrationHelpers.OUTPUT_STTL);
                MigrationHelpers.outputOneModel(itemModel, itemName, volOutfileName, "item");
            }
            
            // migrate pubinfo
            String pubinfoFileName = DATA_DIR+"tbrc-pubinfos/MW"+fileName.substring(1);
            File pubinfoFile = new File(pubinfoFileName);
            if (!pubinfoFile.exists()) {
                MigrationHelpers.writeLog("missing "+pubinfoFileName);
                //MigrationHelpers.modelToFileName(m, outfileName, type, MigrationHelpers.OUTPUT_STTL);
                MigrationHelpers.outputOneModel(m, baseName, outfileName, "work");
                return;
            }
            d = MigrationHelpers.documentFromFileName(pubinfoFileName);
            
            final Map<String, Model> itemModels = new HashMap<>();
            m = PubinfoMigration.MigratePubinfo(d, m, m.getResource(BDR+baseName), itemModels);
            //MigrationHelpers.modelToFileName(m, outfileName, type, MigrationHelpers.OUTPUT_STTL);
            for (Entry<String,Model> e : itemModels.entrySet()){
                //iterate over the pairs
                MigrationHelpers.outputOneModel(e.getValue(), e.getKey(), OUTPUT_DIR+ITEMS+"/"+e.getKey()+".ttl", "item");
            }
            MigrationHelpers.outputOneModel(m, baseName, outfileName, "work");
            break;
        default:
            MigrationHelpers.convertOneFile(file.getAbsolutePath(), baseName, outfileName, type, MigrationHelpers.OUTPUT_STTL, fileName);
            break;
        }
    }
    
    public static void migrateType(String type, String mustStartWith) {
        createDirIfNotExists(OUTPUT_DIR+type+"s");
        if (type.equals(WORK)) createDirIfNotExists(OUTPUT_DIR+ITEMS);
        if (type.equals(OUTLINE)) createDirIfNotExists(OUTPUT_DIR+WORK+"s");
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
        //Stream.of(files).parallel().forEach(file -> migrateOneFile(file, type, mustStartWith));
        Stream.of(files).forEach(file -> migrateOneFile(file, type, mustStartWith));
        pw.close();
    }
    
    public static void main( String[] args )
    {
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.equals("-useCouchdb")) {
				MigrationHelpers.usecouchdb = true;
				MigrationHelpers.writefiles = true;
			}
		    if (arg.equals("-datadir")) {
                DATA_DIR = args[i+1];
            }
		    if (arg.equals("-outdir")) {
                OUTPUT_DIR = args[i+1];
            }
		    if (arg.equals("-writefiles")) {
		        MigrationHelpers.writefiles = true;
		    }
		}
		
		if (MigrationHelpers.usecouchdb)
		    System.out.println("sending JSON documents to CouchDB");
		if (MigrationHelpers.usecouchdb)
            System.out.println("writing files in "+OUTPUT_DIR);
		if (!MigrationHelpers.usecouchdb && !MigrationHelpers.writefiles) {
		    System.err.println("nothing to do, please pass -useCouchdb or -writefiles arguments");
	        CommonMigration.speller.close();
	        ExceptionHelper.closeAll();
		    return;
		}
		    
		
        createDirIfNotExists(OUTPUT_DIR);
        long startTime = System.currentTimeMillis();
        //MigrationHelpers.usecouchdb = true;
//        migrateOneFile(new File(DATA_DIR+"tbrc-persons/P1KG16739.xml"), "person", "P");
        // migrate outlines first to have the oldOutlineId -> newOutlineId correspondance, for externals
        migrateType(OUTLINE, "O");
        migrateType(PERSON, "P");
    	migrateType(PLACE, "G");
    	migrateType(OFFICE, "R");
        migrateType(CORPORATION, "C");
        migrateType(LINEAGE, "L");
        migrateType(TOPIC, "T");
//        migrateOneFile(new File(DATA_DIR+"tbrc-works/W2046.xml"), "work", "W");
//        migrateOneFile(new File(DATA_DIR+"tbrc-outlines/O00EGS103132.xml"), "outline", "O");
//        //migrateOneFile(new File(DATA_DIR+"tbrc-scanrequests/SR1KG10424.xml"), "scanrequest", "SR");
        migrateType(WORK, "W"); // also does pubinfos and imagegroups
        migrateType(SCANREQUEST, "SR"); // requires works to be finished
        CommonMigration.speller.close();
        ExceptionHelper.closeAll();
    	long estimatedTime = System.currentTimeMillis() - startTime;
    	System.out.println("done in "+estimatedTime+" ms");
    }
}
