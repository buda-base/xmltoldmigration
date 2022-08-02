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
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import io.bdrc.libraries.GitHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.EtextMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;
import io.bdrc.xmltoldmigration.xml2files.PersonMigration;
import io.bdrc.xmltoldmigration.xml2files.ProductMigration;
import io.bdrc.xmltoldmigration.xml2files.PubinfoMigration;
import io.bdrc.xmltoldmigration.xml2files.ScanrequestMigration;
import io.bdrc.xmltoldmigration.xml2files.TaxonomyMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.ImageGroupInfo;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.Volinfo;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.WorkModelInfo;

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
    public static String OUTPUT_DIR = "../tbrc-ttl/";
    public static String commitMessage = "xmltold automatic migration";
    public static boolean firstMigration = false;
    public static boolean noXmlMigration = false;
    public static boolean useHash = true;
    public static boolean exportTitles = false;
    public static Writer titleswriter = null;
    public static CSVWriter csvWriter = null;

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
        final boolean needsHash = useHash && !type.equals("office") && !type.equals("corporation") && !type.equals("product")  && !type.equals("subscriber")  && !type.equals("collection");
        String res = type.equals("office") ? OUTPUT_DIR+"roles/" : OUTPUT_DIR+type+"s/";
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
    public static void moveAdminInfo(Model itemM, Resource work, Resource admItem, Resource item) {
        Resource access = WorkMigration.getAccess(itemM, work);
        boolean ric = WorkMigration.isRestrictedInChina(itemM, work);
        boolean lowQuality = WorkMigration.isLowQuality(itemM, work);
        
        if (access != null) {
            admItem.addProperty(itemM.getProperty(ADM, "access"), access);
            itemM.addLiteral(admItem, itemM.getProperty(ADM, "restrictedInChina"), ric);
        }
        if (lowQuality) {
            item.addLiteral(itemM.getProperty(BDO, "qualityGrade"), itemM.createTypedLiteral(0, XSDDatatype.XSDinteger));
        }
        if (MigrationHelpers.nokForLending.containsKey(work.getLocalName().substring(1))) {
            item.addLiteral(itemM.getProperty(BDO, "digitalLendingPossible"), itemM.createTypedLiteral(false, XSDDatatype.XSDboolean));
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
        //if (!fileName.startsWith("O1KG11703")) return;
        String baseName = fileName.substring(0, fileName.length()-4);
        Resource item = null;
        Resource admItem = null;
        Model itemModel = null;
        String itemName = null;
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
                System.out.println("outline with no work id: "+outroot.getAttribute("RID"));
                return;
            }

            Model workModel = ModelFactory.createDefaultModel();
            MigrationHelpers.setPrefixes(workModel, "work");
            Resource work = workModel.createResource(BDR+outWorkId);
            
            List<Element> ancestorCreators = OutlineMigration.getCreatorAncestorsForWork(outWorkId.substring(1));
            List<WorkModelInfo> wmiList = OutlineMigration.MigrateOutline(outd, workModel, work, ancestorCreators); 
            Model outlineModel = wmiList.get(0).m;
            if (OutlineMigration.splitOutlines) {
                String outlineFileName = getDstFileName("outline", outroot.getAttribute("RID"));
                MigrationHelpers.outputOneModel(outlineModel, outWorkId, outlineFileName, "outline");
            } else {
                String workFileName = getDstFileName("instance", outWorkId);
                MigrationHelpers.outputOneModel(workModel, outWorkId, workFileName, "instance");
                workCreatedByOutline.put(outWorkId, true);
            }
            // abstract works created in the outlines:
            for (int i = 1 ; i < wmiList.size() ; i++) {
                WorkModelInfo wmi = wmiList.get(i);
                WorkMigration.exportTitleInfo(wmi.m);
                String workFileName = getDstFileName("work", wmi.resourceName);
                MigrationHelpers.outputOneModel(wmi.m, wmi.resourceName, workFileName, "work");
            }
            break;
        case SCANREQUEST:
            Document srd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            String workId = ScanrequestMigration.getWork(srd);
            if (workId == null || workId.isEmpty())
                return;
            itemName = workId;
            String itemFileName = getDstFileName("iinstance", itemName, ".trig");
            itemModel = MigrationHelpers.modelFromFileName(itemFileName);
            if (itemModel == null)
                return;
            item = itemModel.getResource(BDR+itemName);
            //itemModel = ScanrequestMigration.MigrateScanrequest(srd, itemModel, item);
            MigrationHelpers.outputOneModel(itemModel, itemName, itemFileName, "iinstance");
            break;
        case WORK:
            //if (!file.getAbsolutePath().contains("W00KG02331")) return;
            Document d = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            Document workD = d;
            Element root = d.getDocumentElement();
            MigrationHelpers.resourceHasStatus(root.getAttribute("RID"), root.getAttribute("status"));
            String redirectionInstanceId = MigrationHelpers.instanceClusters.get(root.getAttribute("RID"));
            String workOutFileName = getDstFileName("instance", 'M'+baseName);
            if (!MigrationHelpers.mustBeMigrated(root, "instance", root.getAttribute("status"))) {
                // case of released outlines of withdrawn works (ex: O1GS129876 / W18311)
                File outWorkFile = new File(workOutFileName);
                if (outWorkFile.exists()) {
                    System.out.println("error: outline for withdrawn work "+root.getAttribute("RID"));
                    outWorkFile.delete();
                }
                //return;
            }
            Model m = null;
            Resource serialWork = null; // collects the SerialWork optionally created in MigratePubinfo
            if (workCreatedByOutline.containsKey('M'+baseName)) {
                m = MigrationHelpers.modelFromFileName(getDstFileName("instance", 'M'+baseName, ".trig"));
            }
            if (m == null) {
                m = ModelFactory.createDefaultModel();
            }
            setPrefixes(m);
            final Map<String, Model> itemModels = new HashMap<>();
            List<WorkModelInfo> models = WorkMigration.MigrateWork(d, m, itemModels);
            /*
             * models is a list where:
             *   - 0: MW
             *   - 1: WA
             *   - 2: WAS
             *   - 3: IE
             */
            if (models.size() != 0 && models.get(0) != null) {
                
                Resource workR = m.getResource(BDR+'M'+baseName);
                
                WorkModelInfo abstractMI = null;
                
                if (models.size() >1 && models.get(1) != null) {
                    abstractMI = models.get(1);
                }

                int nbVolsTotal = 0;
                Statement s = workR.getProperty(m.getProperty(BDO, "numberOfVolumes"));
                if (s != null)
                    nbVolsTotal = s.getObject().asLiteral().getInt();
    
                // migrate instance
                ImageGroupInfo imageGroups = WorkMigration.getImageGroupList(d, nbVolsTotal);
                List<Volinfo> vols = imageGroups.imageGroupList;
                if (vols.size() > 0) {
                    // replace numberOfVolumes by the corrected value
                    if (imageGroups.totalVolumes > nbVolsTotal) {
                        workR.removeAll(m.getProperty(BDO, "numberOfVolumes"));
                        workR.addProperty(m.getProperty(BDO, "numberOfVolumes"), m.createTypedLiteral(imageGroups.totalVolumes, XSDDatatype.XSDinteger));
                    }
                    if (!MigrationHelpers.removeW.containsKey(baseName)) {
                        itemName = baseName;
    
                        itemModel = ModelFactory.createDefaultModel();
                        setPrefixes(itemModel);
                        item = createRoot(itemModel, BDR+itemName, BDO+"ImageInstance");
                        
                        admItem = createAdminRoot(item);
                        addStatus(itemModel, admItem, root.getAttribute("status")); // same status as work
                        admItem.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
                        moveAdminInfo(itemModel, workR, admItem, item);
                        
                        // move sourceNote to image instance:
                        StmtIterator scanInfoSi = workR.listProperties(workR.getModel().getProperty(BDO, "sourceNote"));
                        while (scanInfoSi.hasNext()) {
                            Literal scanInfo = scanInfoSi.next().getLiteral();
                            item.addProperty(itemModel.getProperty(BDO, "sourceNote"), scanInfo);
                        }
                        workR.removeAll(workR.getModel().getProperty(BDO, "sourceNote"));
                        
                        final String oclc = MigrationHelpers.oclcW.get(item.getLocalName()); 
                        if (oclc != null) {
                            CommonMigration.addIdentifier(item, BDR+"OclcControlNumber", oclc);
                        }
                        
                        // move scaninfo to the image instance:
                        scanInfoSi = workR.listProperties(workR.getModel().getProperty(BDO, "scanInfo"));
                        while (scanInfoSi.hasNext()) {
                            Literal scanInfo = scanInfoSi.next().getLiteral();
                            item.addProperty(itemModel.getProperty(BDO, "scanInfo"), scanInfo);
                        }
                        
                        // move inCollection to the image instance
                        scanInfoSi = workR.listProperties(workR.getModel().getProperty(BDO, "inCollection"));
                        while (scanInfoSi.hasNext()) {
                            final Resource collection = scanInfoSi.next().getResource();
                            item.addProperty(itemModel.getProperty(BDO, "inCollection"), collection);
                        }
                    } else {
                        String itemOutfileName = getDstFileName("iinstance", baseName)+".trig";
                        try {
                            if (Files.deleteIfExists(Paths.get(itemOutfileName))) {
                                System.out.println("removing "+itemOutfileName);
                            }
                        } catch (IOException e2) {
                            System.err.println("couldn't remove file "+itemOutfileName);
                            e2.printStackTrace();
                        }
                    }
                    workR.removeAll(workR.getModel().getProperty(BDO, "scanInfo"));
                    workR.removeAll(workR.getModel().getProperty(BDO, "inCollection"));
                    
                    if (models.size() >1 && models.get(1) != null && admItem != null) {
                        abstractMI = models.get(1);
                        Resource mainAdm = abstractMI.m.getResource(BDA+abstractMI.resourceName);
                        // copy scanrequest logentry to the image instance:
                        StmtIterator srleSi = mainAdm.getModel().listStatements(null, RDF.type, m.createResource(ADM+"ScanRequestCreation"));
                        while (srleSi.hasNext()) {
                            Resource le = srleSi.next().getSubject();
                            StmtIterator srleSi2 = mainAdm.getModel().listStatements(le, null, (RDFNode) null);
                            while (srleSi2.hasNext()) {
                                Statement les = srleSi2.next();
                                itemModel.add(les);
                            }
                            admItem.addProperty(itemModel.getProperty(ADM, "logEntry"), le);
                        }
                    }
                    if (item != null) {
                        
                        itemModel.add(item, itemModel.getProperty(BDO, "numberOfVolumes"), itemModel.createTypedLiteral(vols.size(), XSDDatatype.XSDinteger));
                        if (imageGroups.missingVolumes != null && !imageGroups.missingVolumes.isEmpty())
                            item.addProperty(itemModel.getProperty(BDO, "missingVolumes"), imageGroups.missingVolumes);
                        
                        // workHasItem already added in WorkMigration
                        for (Volinfo vi : vols) {
                            String imagegroup = vi.imagegroup;
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
                            ImagegroupMigration.MigrateImagegroup(d, itemModel, item, imagegroup, vi.volnum, itemName, baseName);
                        }
                    }
                }
                
                // migrate pubinfo
                String pubinfoFileName = XML_DIR+"tbrc-pubinfos/MW"+fileName.substring(1);
                File pubinfoFile = new File(pubinfoFileName);
                if (!pubinfoFile.exists()) {
                    // see https://github.com/buda-base/xmltoldmigration/issues/159
                    String pirid = WorkMigration.getPubinfoRID(workD);
                    pubinfoFileName = XML_DIR+"tbrc-pubinfos/"+pirid+".xml";
                    pubinfoFile = new File(pubinfoFileName);
                }
                if (pubinfoFile.exists()) {
                    d = MigrationHelpers.documentFromFileName(pubinfoFileName);
                    List<Resource> resPubMigration = null; 
                    if (models.size() >1 && models.get(1) != null) {
                        abstractMI = models.get(1);
                        Resource mainA = abstractMI.m.getResource(BDR+abstractMI.resourceName);
                        resPubMigration = PubinfoMigration.MigratePubinfo(d, m, workR, itemModels, mainA, item);
                    } else {
                        resPubMigration = PubinfoMigration.MigratePubinfo(d, m, workR, itemModels, null, item);
                    }
                    if (resPubMigration.size() > 0) {
                        serialWork = resPubMigration.get(0);
                    }
                    if (resPubMigration.size() > 1) {
                        Resource serialMemberWork = resPubMigration.get(1);
                        workOutFileName = getDstFileName("work", serialMemberWork.getLocalName());
                        MigrationHelpers.outputOneModel(serialMemberWork.getModel(), serialMemberWork.getLocalName(), workOutFileName, "work");
                    }
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
            WorkModelInfo instanceMI = models.get(0);
            if (instanceMI != null) {
                if (instanceMI.resourceName.equals("MW3CN27014")) {
                    instanceMI.m.read(MigrationApp.class.getClassLoader().getResourceAsStream("MW3CN27014.ttl"), null, "TTL");
                }
                if (instanceMI.resourceName.startsWith("MW1FPL") || instanceMI.resourceName.startsWith("MW1EAP")) {
                    // don't write FPL instances to the git repo, we only want the image instances
                    //System.out.println("ignoring "+instanceMI.resourceName);
                } else {
                    workOutFileName = getDstFileName("instance", instanceMI.resourceName);
                    MigrationHelpers.outputOneModel(instanceMI.m, instanceMI.resourceName, workOutFileName, "instance");
                }
            }
            if (models.size() >1 && models.get(1) != null) {
                WorkModelInfo abstractMI = models.get(1);
                WorkMigration.exportTitleInfo(abstractMI.m);
                workOutFileName = getDstFileName("work", abstractMI.resourceName);
                MigrationHelpers.outputOneModel(abstractMI.m, abstractMI.resourceName, workOutFileName, "work");
            }
            if (models.size() >2 && models.get(2) != null) {
                WorkModelInfo serialMI = models.get(2);
                workOutFileName = getDstFileName("work", serialMI.resourceName);
                MigrationHelpers.outputOneModel(serialMI.m, serialMI.resourceName, workOutFileName, "work");
            } else if ( serialWork != null) {
                workOutFileName = getDstFileName("work", serialWork.getLocalName());
                MigrationHelpers.outputOneModel(serialWork.getModel(), serialWork.getLocalName(), workOutFileName, "work");
            }
            if (models.size() >3 && models.get(3) != null) {
                WorkModelInfo einstanceMI = models.get(3);
                workOutFileName = getDstFileName("einstance", einstanceMI.resourceName);
                MigrationHelpers.outputOneModel(einstanceMI.m, einstanceMI.resourceName, workOutFileName, "einstance");
            }
            break;
        case PRODUCT:
            // do not migrate these two
            if (file.getName().contains("PR99SUBSCRIBERS") || file.getName().contains("PR88CT000129") || file.getName().contains("PR01JW33589"))
                return;
            Document prd = MigrationHelpers.documentFromFileName(file.getAbsolutePath());
            String prType = ProductMigration.getType(prd);
            Model prM = null;
            if (prType.equals("collection")) {
                prM = ProductMigration.MigrateCollection(prd);
            } else {
                prM = ProductMigration.MigrateSubscriber(prd);
                baseName = "PRA"+baseName.substring(2);
            }
            String prOutfileName = getDstFileName(prType, baseName);
            MigrationHelpers.outputOneModel(prM, baseName, prOutfileName, prType);
            break;
        default:
            String outfileName = getDstFileName(type, baseName);
            Model defaultM = MigrationHelpers.getModelFromFile(file.getAbsolutePath(), type, fileName);
            MigrationHelpers.outputOneModel(defaultM, baseName, outfileName, type);
            break;
        }
        if (itemName != null) {
            String itemOutfileName = getDstFileName("iinstance", itemName);
            MigrationHelpers.outputOneModel(itemModel, itemName, itemOutfileName, "iinstance");
        }
    }
    
    static final List<String> taxonomies = new ArrayList<>();
    static {
        taxonomies.add("O9TAXTBRC201605");
        taxonomies.add("O3JW5309");
    }

    public static void migrateTaxonomies() {
        for (final String baseName : taxonomies) {
            String fileName = XML_DIR+"tbrc-outlines/"+baseName+".xml";
            Document d = MigrationHelpers.documentFromFileName(fileName);
            Model m = TaxonomyMigration.MigrateTaxonomy(d);
            String outfileName = OUTPUT_DIR+baseName+".trig";
            MigrationHelpers.outputOneModel(m, baseName, outfileName, "taxonomy");
            System.out.println("created taxonomy on "+outfileName);
        }
    }

    public static void insertMissingSymetricTriples(final String type) {
        if (!SymetricNormalization.triplesToAdd.isEmpty()) {
            System.out.println("adding missing symetric triples in "+SymetricNormalization.triplesToAdd.size()+" files");
            for (String s : SymetricNormalization.triplesToAdd.keySet()) {
                // System.out.println("adding triples in "+s);
                // System.out.println(SymetricNormalization.triplesToAdd.get(s));
                String thistype = type;
                if (s.startsWith("MW"))
                    thistype = "instance";
                String inFileName = getDstFileName(thistype, s, ".trig");
                Model m = MigrationHelpers.modelFromFileName(inFileName);
                if (m == null) {
                    Map<String,List<String>> o = SymetricNormalization.triplesToAdd.get(s);
                    System.out.println("cannot open "+inFileName+"to write: ");
                    System.out.println(o);
                    continue;
                }
                SymetricNormalization.insertMissingTriplesInModel(m, s, false);
                MigrationHelpers.outputOneModel(m, s, inFileName, thistype);
            }
        }
    }

    static Map<String,Map<String,List<String>>> outlineTriplesToAdd = null;
    
    public static void migrateType(String type, String mustStartWith) {
        switch (type) {
        case "office":
            ensureGitRepo("role", OUTPUT_DIR);
            break;
        case "product":
            ensureGitRepo("collection", OUTPUT_DIR);
            ensureGitRepo("subscriber", OUTPUT_DIR);
            break;
        case "outline":
            ensureGitRepo("outline", OUTPUT_DIR);
        case "scanrequest":
            ensureGitRepo("work", OUTPUT_DIR);
            ensureGitRepo("instance", OUTPUT_DIR);
            break;
        case "etext":
            ensureGitRepo("einstance", OUTPUT_DIR);
            ensureGitRepo("work", OUTPUT_DIR);
            ensureGitRepo("instance", OUTPUT_DIR);
        case "work":
            ensureGitRepo("work", OUTPUT_DIR);
            ensureGitRepo("item", OUTPUT_DIR);
            ensureGitRepo("iinstance", OUTPUT_DIR);
            ensureGitRepo("einstance", OUTPUT_DIR);
            ensureGitRepo("instance", OUTPUT_DIR);
            break;
        default:
            ensureGitRepo(type, OUTPUT_DIR);
            break;
        }
        SymetricNormalization.reinit();
        if (type.equals("work") && outlineTriplesToAdd != null) {
            SymetricNormalization.triplesToAdd = outlineTriplesToAdd;
        }
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
        System.out.println("listing files in "+dirName);
        File[] files = new File(dirName).listFiles();
        if (files == null) {
            System.out.println("couldn't find any file");
            return;
        }
        System.out.println("converting "+files.length+" "+type+" files");
        //Stream.of(files).parallel().forEach(file -> migrateOneFile(file, type, mustStartWith));
        //migrateOneFile(new File(XML_DIR+"tbrc-outlines/O21018.xml"), type, mustStartWith);
        //migrateOneFile(new File(XML_DIR+"tbrc-works/W12827.xml"), type, mustStartWith);
        Stream.of(files).forEach(file -> migrateOneFile(file, type, mustStartWith));
        pw.close();
        if (type.equals("outline")) {
            outlineTriplesToAdd = SymetricNormalization.triplesToAdd;
        } else {
            insertMissingSymetricTriples(type);
        }
        if (type.equals("product")) {
            ProductMigration.finishProductMigration();
        }
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

    public static Set<String> getChanges(String type) {
        Repository r = GitHelpers.typeRepo.get(type);
        if (r == null) {
            System.out.println("getChanges DID NOT FIND REPO FOR " + type);
            return null;
        }
        Git git = new Git(r);
        Status status;
        Set<String> res = new HashSet<>();
        try {
            status = git.status().call();
        } catch (NoWorkTreeException | GitAPIException e) {
            e.printStackTrace();
            git.close();
            return null;
        }
        res.addAll(status.getModified());
        res.addAll(status.getAdded());
        res.addAll(status.getRemoved());
        git.close();
        return res;
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
        List<String> types = Arrays.asList("outline", "collection", "subscriber", "work", "item", "instance", "place", "person", "corporation", "role", "lineage", "topic", "etext", "etextcontent", "iinstance", "einstance");
        for (String type : types) {
            finishType(type);
        }
    }

    public static class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
      }

    public static void main( String[] args ) throws NoSuchAlgorithmException, IllegalArgumentException, IOException, CsvException
    {
        boolean oneDirection = true;
        boolean manyOverOne = false;
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
            if (arg.equals("-noXmlMigration")) {
                noXmlMigration = true;
            }
            if (arg.equals("-rKTsDir")) {
                RKTS_DIR = args[i+1];
            }
            if (arg.equals("-exporttitles")) {
                exportTitles = true;
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
		CommonMigration.initClusters(exportTitles);
		if (exportTitles) {
		    titleswriter = Files.newBufferedWriter(Paths.get("titles.csv"));
		    csvWriter = new CSVWriter(titleswriter);
		}
		SymetricNormalization.normalizeOneDirection(oneDirection, manyOverOne);
		System.out.println("data dir is "+DATA_DIR);
        File theDir = new File(OUTPUT_DIR);
        if (!theDir.exists()) {
            System.out.println("considering that this is the first migration");
            firstMigration = true;
        }
        createDirIfNotExists(OUTPUT_DIR);
        long startTime = System.currentTimeMillis();
        // needs to be done first
        FEMCTransfer.transferFEMCWorks();
        // migrate outlines first to have the oldOutlineId -> newOutlineId correspondence, for externals
        if (!noXmlMigration) {
            migrateType(OUTLINE, "O");
            if (!exportTitles) {
                migrateType(PERSON, "P");
                migrateType(PLACE, "G");
                migrateType(OFFICE, "R");
                migrateType(CORPORATION, "C");
                migrateType(LINEAGE, "L");
                migrateType(TOPIC, "T");
            }
            migrateType(WORK, "W"); // also does pubinfos and imagegroups
            OutlineMigration.finishWorks();
            if (!exportTitles) {
                // interestingly, scanrequests don't hold any kind of information...
//              // migrateType(SCANREQUEST, "SR"); // requires works to be finished
                migrateType(PRODUCT, "PR");
                //EtextMigration.EtextInfos ei = EtextMigration.migrateOneEtext(ETEXT_DIR+"UCB-OCR/UT16936/UT16936-4905/UT16936-4905-0000.xml", true, new NullOutputStream(), true, ModelFactory.createDefaultModel(), true);
                //MigrationHelpers.modelToOutputStream(ei.etextModel, new FileOutputStream(new File("/tmp/mod.txt")), "etext", MigrationHelpers.OUTPUT_STTL, ei.etextId);
                EtextMigration.migrateEtexts();
            }
        }
        if (RKTS_DIR != null && !exportTitles) {
            rKTsTransfer.doTransfer();
        }
        if (!exportTitles) {
            EAPTransfer.transferEAP();
            GRETILTransfer.transferGRETIL();
            EAPFondsTransfer.EAPFondsDoTransfer();
            CUDLTransfer.CUDLDoTransfer();
            NSITransfer.transferNIS();
            HodgsonTransfer.transfer();
        }
        if (exportTitles) {
            csvWriter.close();
            titleswriter.close();
        }
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
