package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.GitHelpers.commitChanges;
import static io.bdrc.libraries.GitHelpers.ensureGitRepo;
import static io.bdrc.libraries.GitHelpers.getChanges;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getMd5;
import static io.bdrc.libraries.Models.setPrefixes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;
import io.bdrc.xmltoldmigration.xml2files.PersonMigration;
import io.bdrc.xmltoldmigration.xml2files.PubinfoMigration;
import io.bdrc.xmltoldmigration.xml2files.ScanrequestMigration;
import io.bdrc.xmltoldmigration.xml2files.TaxonomyMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.ImageGroupInfo;

/**
 * Hello world!
 *
 */
public class MigrationApp
{

    // extract tbrc/ folder of exist-db backup here:
    public static String DATA_DIR = "../../data/db/";
    public static String RKTS_DIR = null;
    public static String ETEXT_DIR = DATA_DIR+"eTextsChunked/";
    public static String XML_DIR = DATA_DIR+"tbrc/";
    public static String OUTPUT_DIR = "tbrc-ttl/";
    public static String commitMessage = "xmltold automatic migration";
    public static boolean firstMigration = false;
    public static boolean noXmlMigration = false;
    public static boolean useHash = true;

    public static final String CORPORATION = MigrationHelpers.CORPORATION;
    public static final String LINEAGE = MigrationHelpers.LINEAGE;
    public static final String OFFICE = MigrationHelpers.OFFICE;
    public static final String OUTLINE = MigrationHelpers.OUTLINE;
    public static final String PERSON = MigrationHelpers.PERSON;
    public static final String PLACE = MigrationHelpers.PLACE;
    public static final String SCANREQUEST = MigrationHelpers.SCANREQUEST;
    public static final String TOPIC = MigrationHelpers.TOPIC;
    public static final String PRODUCT = MigrationHelpers.PRODUCT;
    public static final String VOLUMES = MigrationHelpers.VOLUMES;
    public static final String ITEMS = MigrationHelpers.ITEMS;
    public static final String WORK = MigrationHelpers.WORK;

    public static final Map<String,String> imageGroupWork = new HashMap<>();

    private static Map<String,Boolean> workCreatedByOutline = new HashMap<>();

//    public static OntModel ontology = null;
    static {
        init();
    }


    public static void init() {
//        ontology = MigrationHelpers.getOntologyModel();
    }

    public static void createDirIfNotExists(String dir) {
        File theDir = new File(dir);
        if (!theDir.exists()) {
            //System.out.println("creating directory: " + dir);
            try{
                theDir.mkdir();
            }
            catch(SecurityException se){
                System.err.println("could not create directory, please fasten your seat belt");
            }
        }
    }

    public static String getDstFileName(String type, String baseName) {
        return getDstFileName(type, baseName, "");
    }

    public static String getDstFileName(String type, String baseName, String extension) {
        final boolean needsHash = useHash && !type.equals("office") && !type.equals("corporation") && !type.equals("product");
        String res = OUTPUT_DIR+type+"s/";
        if (needsHash) {
            String hashtext = getMd5(baseName);
            res = res+hashtext.toString()+"/";
            createDirIfNotExists(res);
        }
        res = res + baseName + extension;
        return res;
    }

    // the WorkMigration gets the access and legal info from the work xml doc, adds access and legal data
    // resources to HashMaps so they can be added to the :workHasItem :Item if there is one - 
    // no access => no Item.
    public static void moveAdminInfo(Model itemM, Resource work, Resource admItem) {
        Resource access = WorkMigration.getAccess(itemM, work);
        Resource legal = WorkMigration.getLegal(itemM, work);
        boolean ric = WorkMigration.isRestrictedInChina(itemM, work);
        
        if (access != null) {
            admItem.addProperty(itemM.getProperty(ADM, "access"), access);
            itemM.addLiteral(admItem, itemM.getProperty(ADM, "restrictedInChina"), ric);
        }
        if (legal != null) {
            admItem.addProperty(itemM.getProperty(ADM, "contentLegal"), legal);
        }
    }

    // checking RID discrepancies (seem to only happen in outlines)
    public static boolean checkRID(String baseName, Element root) {
        String rid = root.getAttribute("RID");
        if (!rid.equals(baseName)) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, baseName, baseName, "`"+baseName+".xml` has RID `"+rid+"`");
            return false;
        }
        return true;
    }

    public static void migrateOneFile(File file, String type, String mustStartWith) {
        if (file.isDirectory()) return;
        String fileName = file.getName();
        if (!fileName.startsWith(mustStartWith)) return;
        if (!fileName.endsWith(".xml")) return;
        String baseName = fileName.substring(0, fileName.length()-4);
        Resource item;
        Resource admItem;
        Model itemModel;
        switch(type) {
        case OUTLINE:
            Document outd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            if (!checkRID(baseName, outd.getDocumentElement())) {
                return;
            }
            Element outroot = outd.getDocumentElement();
            MigrationHelpers.resourceHasStatus(outroot.getAttribute("RID"), outroot.getAttribute("status"));
            if (!MigrationHelpers.mustBeMigrated(outd.getDocumentElement(), "outline", outroot.getAttribute("status"))){
                return;
            }
            String outWorkId = OutlineMigration.getWorkId(outd);
            if (outWorkId == null || outWorkId.isEmpty()) {
                //ExceptionHelper.logException(ExceptionHelper.ET_GEN, baseName, baseName, "outlineFor", "outline does not reference its main work");
                return;
            }

            Model workModel = ModelFactory.createDefaultModel();
            setPrefixes(workModel, "work");
            Resource work = workModel.createResource(BDR+outWorkId);

            Model outlineModel = OutlineMigration.MigrateOutline(outd, workModel, work);
            if (OutlineMigration.splitOutlines) {
                String outlineFileName = getDstFileName("work", outWorkId+"_001");
                MigrationHelpers.outputOneModel(outlineModel, outWorkId+"_O01", outlineFileName, "work");
            } else {
                String workFileName = getDstFileName("work", outWorkId);
                MigrationHelpers.outputOneModel(workModel, outWorkId, workFileName, "work");
                workCreatedByOutline.put(outWorkId, true);
            }
            break;
        case SCANREQUEST:
            Document srd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            String workId = ScanrequestMigration.getWork(srd);
            if (workId == null || workId.isEmpty())
                return;
            String srItemName = "I"+workId.substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX;
            String itemFileName = getDstFileName("item", srItemName, ".trig");
            itemModel = MigrationHelpers.modelFromFileName(itemFileName);
            if (itemModel == null)
                return;
            item = itemModel.getResource(BDR+srItemName);
            itemModel = ScanrequestMigration.MigrateScanrequest(srd, itemModel, item);
            MigrationHelpers.outputOneModel(itemModel, srItemName, itemFileName, "item");
            break;
        case WORK:
            Document d = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            Element root = d.getDocumentElement();
            MigrationHelpers.resourceHasStatus(root.getAttribute("RID"), root.getAttribute("status"));
            final String workOutFileName = getDstFileName("work", baseName);
            if (!MigrationHelpers.mustBeMigrated(root, "work", root.getAttribute("status"))) {
                // case of released outlines of withdrawn works (ex: O1GS129876 / W18311)
                File outWorkFile = new File(workOutFileName);
                if (outWorkFile.exists())
                    outWorkFile.delete();
                return;
            }
            Model m = null;
            if (workCreatedByOutline.containsKey(baseName)) {
                m = MigrationHelpers.modelFromFileName(getDstFileName("work", baseName, ".trig"));
            }
            if (m == null) {
                m = ModelFactory.createDefaultModel();
            }
            setPrefixes(m);
            final Map<String, Model> itemModels = new HashMap<>();
            m = WorkMigration.MigrateWork(d, m, itemModels);
            
            Resource workR = m.getResource(BDR+baseName);

            int nbVolsTotal = 0;
            Statement s = workR.getProperty(m.getProperty(BDO, "workNumberOfVolumes"));
            if (s != null)
                nbVolsTotal = s.getObject().asLiteral().getInt();

            // migrate items
            ImageGroupInfo imageGroups = WorkMigration.getImageGroupList(d, nbVolsTotal);
            Map<Integer,String> vols = imageGroups.imageGroupList;
            if (vols.size() > 0) {
                // replace workNumberOfVolumes by the corrected value
                if (imageGroups.totalVolumes > nbVolsTotal) {
                    workR.removeAll(m.getProperty(BDO, "workNumberOfVolumes"));
                    workR.addProperty(m.getProperty(BDO, "workNumberOfVolumes"), m.createTypedLiteral(imageGroups.totalVolumes, XSDDatatype.XSDinteger));
                }
                String itemName = "I"+baseName.substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX;
                if (WorkMigration.addWorkHasItem) {
                    m.add(workR, m.getProperty(BDO, "workHasItem"), m.createResource(BDR+itemName));
                }
                itemModel = ModelFactory.createDefaultModel();
                setPrefixes(itemModel);
                item = createRoot(itemModel, BDR+itemName, BDO+"ItemImageAsset");
                
                admItem = createAdminRoot(item);
                addStatus(itemModel, admItem, root.getAttribute("status")); // same status as work
                admItem.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
                moveAdminInfo(itemModel, workR, admItem);

                itemModel.add(item, itemModel.getProperty(BDO, "itemVolumes"), itemModel.createTypedLiteral(vols.size(), XSDDatatype.XSDinteger));
                if (imageGroups.missingVolumes != null && !imageGroups.missingVolumes.isEmpty())
                    item.addProperty(itemModel.getProperty(BDO, "itemMissingVolumes"), imageGroups.missingVolumes);
                if (WorkMigration.addItemForWork) {
                    itemModel.add(item, itemModel.getProperty(BDO, "itemForWork"), itemModel.createResource(BDR + baseName));
                }
                // workHasItem already added in WorkMigration
                for (Map.Entry<Integer,String> vol : vols.entrySet()) {
                    String imagegroup = vol.getValue();
                    String imagegroupFileName = XML_DIR+"tbrc-imagegroups/"+imagegroup+".xml";
                    File imagegroupFile = new File(imagegroupFileName);
                    if (!imagegroupFile.exists()) {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "imagegroup", "image group `"+imagegroupFileName+"` referenced but absent from database");
                        continue;
                    }
                    d = MigrationHelpers.documentFromFileName(imagegroupFileName);
                    root = d.getDocumentElement(); // necessary?
                    MigrationHelpers.resourceHasStatus(root.getAttribute("RID"), root.getAttribute("status"));
                    if (imageGroupWork.containsKey(imagegroup)) {
                        final String oldvalue = imageGroupWork.get(imagegroup);
                        final String indicatedWork = ImagegroupMigration.getVolumeOf(d);
                        //final boolean hasOnDisk = ImagegroupMigration.getOnDisk(d);
                        final String exceptionMessage = "is referrenced in both ["+oldvalue+"](https://www.tbrc.org/#!rid="+oldvalue+") and ["+baseName+"](https://www.tbrc.org/#!rid="+baseName+") (indicates "+indicatedWork+")";
                        //System.out.println(imagegroup+","+oldvalue+","+baseName+","+indicatedWork+","+(hasOnDisk?"true":"false"));
                        ExceptionHelper.logException(ExceptionHelper.ET_IMAGEGROUP, imagegroup, imagegroup, exceptionMessage);
                    } else {
                        imageGroupWork.put(imagegroup, baseName);
                    }
                    ImagegroupMigration.MigrateImagegroup(d, itemModel, item, imagegroup, vol.getKey(), itemName);
                }
                String itemOutfileName = getDstFileName("item", itemName);
                MigrationHelpers.outputOneModel(itemModel, itemName, itemOutfileName, "item");
            }
            if (!WorkMigration.isAbstract(m, baseName)) {
                // migrate pubinfo
                String pubinfoFileName = XML_DIR+"tbrc-pubinfos/MW"+fileName.substring(1);
                File pubinfoFile = new File(pubinfoFileName);
                if (pubinfoFile.exists()) {
                    d = MigrationHelpers.documentFromFileName(pubinfoFileName);
                    m = PubinfoMigration.MigratePubinfo(d, m, workR, itemModels);
                } else {
                    MigrationHelpers.writeLog("missing "+pubinfoFileName);
                }
                for (Entry<String,Model> e : itemModels.entrySet()){
                    //iterate over the pairs
                    MigrationHelpers.outputOneModel(e.getValue(), e.getKey(), getDstFileName("item", e.getKey()), "item");
                }
            } else if (!itemModels.isEmpty()) {
                System.out.println("abstract work "+baseName+" has items!");
            }
            MigrationHelpers.outputOneModel(m, baseName, workOutFileName, "work");
            break;
        default:
            String outfileName = getDstFileName(type, baseName);
            Model defaultM = MigrationHelpers.getModelFromFile(file.getAbsolutePath(), type, fileName);
            MigrationHelpers.outputOneModel(defaultM, baseName, outfileName, type);
            break;
        }
    }

    public static void migrateTaxonomies() {
        String fileName = XML_DIR+"tbrc-outlines/O9TAXTBRC201605.xml";
        String baseName = "O9TAXTBRC201605";
        Document d = MigrationHelpers.documentFromFileName(fileName);
        Model m = TaxonomyMigration.MigrateTaxonomy(d);
        String outfileName = OUTPUT_DIR+baseName+".trig";
        MigrationHelpers.outputOneModel(m, baseName, outfileName, "taxonomy");
        System.out.println("created taxonomy on "+outfileName);
    }

    public static void insertMissingSymetricTriples(String type) {
        if (!SymetricNormalization.triplesToAdd.isEmpty()) {
            System.out.println("adding missing symetric triples in "+SymetricNormalization.triplesToAdd.size()+" files");
            for (String s : SymetricNormalization.triplesToAdd.keySet()) {
                // System.out.println("adding triples in "+s);
                // System.out.println(SymetricNormalization.triplesToAdd.get(s));
                String inFileName = getDstFileName(type, s, ".trig");
                Model m = MigrationHelpers.modelFromFileName(inFileName);
                if (m == null) {
                    System.out.println("cannot open "+inFileName);
                    continue;
                }
                SymetricNormalization.insertMissingTriplesInModel(m, s, false);
                MigrationHelpers.outputOneModel(m, s, inFileName, type);
            }
        }
    }

    public static void migrateType(String type, String mustStartWith) {
        switch (type) {
        case "outline":
        case "scanrequest":
            ensureGitRepo("work", OUTPUT_DIR);
            break;
        case "work":
            ensureGitRepo("work", OUTPUT_DIR);
            ensureGitRepo("item", OUTPUT_DIR);
            break;
        default:
            ensureGitRepo(type, OUTPUT_DIR);
            break;
        }
        SymetricNormalization.reinit();
        File logfile = new File(OUTPUT_DIR+type+"s-migration.log");
        PrintWriter pw;
        try {
            pw = new PrintWriter(logfile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }
        MigrationHelpers.writeLogsTo(pw);
        String dirName = XML_DIR+"tbrc-"+type+"s";
        File[] files = new File(dirName).listFiles();
        System.out.println("converting "+files.length+" "+type+" files");
        //Stream.of(files).parallel().forEach(file -> migrateOneFile(file, type, mustStartWith));
        Stream.of(files).forEach(file -> migrateOneFile(file, type, mustStartWith));
        pw.close();
        insertMissingSymetricTriples(type);
        if (type.equals("person"))
            System.out.println("recorded "+PersonMigration.placeEvents.size()+" events to migrate to places");
    }

    public static void sendChangesToCouch(Set<String> modifiedFiles, String type) {
        System.out.println("sending to CouchDB");
        for (String modifiedFile : modifiedFiles) {
            String mainId = modifiedFile;
            int slashIdx = mainId.lastIndexOf('/');
            if (slashIdx != -1)
                mainId = mainId.substring(slashIdx+1, mainId.length());
            mainId = mainId.substring(0, mainId.length()-4);
            modifiedFile = OUTPUT_DIR+type+"s/"+modifiedFile;
        }
    }

    public static void finishType(String type) {
        Set<String> modifiedFiles = getChanges(type);
        if (modifiedFiles == null)
            return;
        System.out.println(modifiedFiles.size()+" "+type+"s changed");
        commitChanges(type, commitMessage);
    }

    public static void finishTypes() {
        System.out.println("committing modifications");
        List<String> types = Arrays.asList("work", "item", "place", "person", "product", "corporation", "office", "lineage", "topic", "etext", "etextcontent");
        for (String type : types) {
            finishType(type);
        }
    }

    public static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
      }

    public static void main( String[] args ) throws NoSuchAlgorithmException, IllegalArgumentException, IOException
    {
        boolean oneDirection = false;
        boolean manyOverOne = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
            if (arg.equals("-noXmlMigration")) {
                noXmlMigration = true;
            }
            if (arg.equals("-rKTsDir")) {
                RKTS_DIR = args[i+1];
            }
            if (arg.equals("-preferManyOverOne=1")) {
                manyOverOne = true;
            }
            if (arg.equals("-preferManyOverOne=0")) {
                manyOverOne = false;
            }
            if (arg.equals("-onlyOneSymetricDirection=1")) {
                oneDirection = true;
            }
            if (arg.equals("-onlyOneSymetricDirection=0")) {
                oneDirection = false;
            }
            SymetricNormalization.normalizeOneDirection(oneDirection, manyOverOne);
		    if (arg.equals("-datadir")) {
                DATA_DIR = args[i+1];
                if (!DATA_DIR.endsWith("/")) {
                    DATA_DIR = DATA_DIR+'/';
                }
                ETEXT_DIR = DATA_DIR+"eTextsChunked/";
                XML_DIR = DATA_DIR+"tbrc/";
            }
		    if (arg.equals("-outdir")) {
                OUTPUT_DIR = args[i+1];
                if (!OUTPUT_DIR.endsWith("/")) {
                    OUTPUT_DIR = OUTPUT_DIR+'/';
                }
            }
            if (arg.equals("-commitMessage")) {
                commitMessage = args[i+1];
            }
		}

        File theDir = new File(OUTPUT_DIR);
        if (!theDir.exists()) {
            System.out.println("considering that this is the first migration");
            firstMigration = true;
        }
        createDirIfNotExists(OUTPUT_DIR);
        long startTime = System.currentTimeMillis();
        // migrate outlines first to have the oldOutlineId -> newOutlineId correspondence, for externals
        if (!noXmlMigration) {
            migrateType(OUTLINE, "O");
//            migrateType(PERSON, "P");
//            migrateType(PLACE, "G");
//            migrateType(OFFICE, "R");
//            migrateType(CORPORATION, "C");
//            migrateType(LINEAGE, "L");
//            migrateType(TOPIC, "T");
            migrateType(WORK, "W"); // also does pubinfos and imagegroups
            //migrateType(SCANREQUEST, "SR"); // requires works to be finished
            //migrateType(PRODUCT, "PR");
            //EtextMigration.EtextInfos ei = EtextMigration.migrateOneEtext(ETEXT_DIR+"UCB-OCR/UT16936/UT16936-4905/UT16936-4905-0000.xml", true, new NullOutputStream(), true, ModelFactory.createDefaultModel(), true);
            //MigrationHelpers.modelToOutputStream(ei.etextModel, new FileOutputStream(new File("/tmp/mod.txt")), "etext", MigrationHelpers.OUTPUT_STTL, ei.etextId);
            //EtextMigration.migrateEtexts();
        }
//        if (RKTS_DIR != null) {
//            rKTsTransfer.doTransfer();
//        }
//        EAPTransfer.transferEAP();
//        GRETILTransfer.transferGRETIL();
//        EAPFondsTransfer.EAPFondsDoTransfer();
//        CUDLTransfer.CUDLDoTransfer();
//        NSITransfer.transferNIS();
        migrateTaxonomies();
        CommonMigration.speller.close();
        MigrationHelpers.reportMissing();
        ExceptionHelper.closeAll();
        finishTypes();
        long fileMigrationEndTime = System.currentTimeMillis();
    	long estimatedTime = fileMigrationEndTime - startTime;
    	System.out.println("symetry triple changes: +"+SymetricNormalization.addedTriples+"/-"+SymetricNormalization.removedTriples);
    	System.out.println("done in "+estimatedTime+" ms");
    }
}
