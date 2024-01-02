package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.GitHelpers.ensureGitRepo;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.setPrefixes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RiotException;
import org.apache.jena.vocabulary.RDF;

import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;

//import io.bdrc.xmltoldmigration.helpers.GitHelpers;

/* This class does not do any actual migration of the rKTs data,
 * which is handled by https://github.com/BuddhistDigitalResourceCenter/rKTs-migration
 * here we just transfer the output of the migration to the git repositories
 */

public class rKTsTransfer {

    public static List<String> RIDList = new ArrayList<>();
    public static Map<String, Model> RidModels = new HashMap<>();
    
    public static void initLists() {
        initListsForRID("MW22084");
        //initListsForRID("MW30532");
        initListsForRID("MW4CZ5369");
        initListsForRID("MW1PD96682");
        initListsForRID("MW4CZ7445");
        initListsForRID("MW22703");
        initListsForRID("MW26071");
        initListsForRID("MW29468");
        initListsForRID("MW1PD96685");
        initListsForRID("MW22083");
        initListsForRID("MW1GS66030");
        initListsForRID("MW23703");
        initListsForRID("MW22704");
        initListsForRID("MW1KG13126");
        initListsForRID("MW1PD95844");
        initListsForRID("MW23702");
        initListsForRID("MW1PD96684");
        initListsForRID("MW1PD127393");
        initListsForRID("MW1KG14700");
        initListsForRID("MW4PD3142");
        initListsForRID("MW1KG12671");
        initListsForRID("MW3CN1302");
        initListsForRID("MW1KG13607");
        initListsForRID("MW4CZ45313");
        initListsForRID("MW3CN20612");
        initListsForRID("MW23702");
        initListsForRID("MW22704");
        initListsForRID("MW2KG5015");
        initListsForRID("MW2PD17098");
        initListsForRID("MW2KG5014");
        initListsForRID("MW30532");
        initListsForRID("MW1PD96682");
        initListsForRID("MW1PD95844");
        initListsForRID("MW1KG14783", "O0RK14783"); // ng
        initListsForRID("MW2PD17382", "O0RK17382"); // ng
        initListsForRID("MW21520", "O0RK21520");  // ng
        initListsForRID("MW21519", "O0RK21519"); // ng
        initListsForRID("MW2PD19897", "O0RK19897"); // ng
        initListsForRID("MW1KG11703", "O0RK11703"); // ng
        initListsForRID("MW2KG229028", "O0RK229028");
        initListsForRID("MW8LS15931", "O0RK15931");
        initListsForRID("MW2PD17098", "O0RK17098");
        initListsForRID("MW2KG209840", true);
        initListsForRID("MW2KG5016", true);
        initListsForRID("MW4CZ45315", true);
        initListsForRID("MW4CZ45314", true);
        initListsForRID("MW4CZ45313", true);
        initListsForRID("MW1BL4", true);
    }
    
    public static void initListsForRID(final String rid) {
        initListsForRID(rid, null);
    }
    
    public static void initListsForRID(final String rid, String oRID) {
        RIDList.add(rid);
        final Model m;
        if (OutlineMigration.splitOutlines) {
            m = ModelFactory.createDefaultModel();
            MigrationHelpers.setPrefixes(m);
            if (oRID == null)
                oRID = "O"+rid.substring(2);
            final Resource mainOutline = m.getResource(BDR+oRID);
            mainOutline.addProperty(RDF.type, m.createResource(BDO+"Outline"));
            mainOutline.addProperty(m.getProperty(BDO, "outlineOf"), m.getResource(BDR+rid));
            mainOutline.addProperty(m.getProperty(BDO, "authorshipStatement"), m.createLiteral("Outline created by Resources for Kanjur and Tanjur Studies (rKTs) at the University of Vienna (www.rkts.org), please report any issue to bruno@rkts.eu", "en"));
            final Resource admOutline = createAdminRoot(mainOutline);
            addStatus(m, admOutline, "released");
            admOutline.addProperty(m.getProperty(ADM, "primarilyImported"), );
        } else {
            final String workFileName = MigrationApp.getDstFileName("instance", rid, ".trig");
            m = MigrationHelpers.modelFromFileName(workFileName);
        }
        RidModels.put(rid, m);
    }
    
    public static void finishEditions() {
        for (String rid : RIDList) {
            Model m = RidModels.get(rid);
            if (OutlineMigration.splitOutlines) {
                final String orid = "O"+rid.substring(2);
                final String fileName = MigrationApp.getDstFileName("outline", orid);
                MigrationHelpers.outputOneModel(m, orid, fileName, "outline");
            } else {
                final String workFileName = MigrationApp.getDstFileName("instance", rid);
                MigrationHelpers.outputOneModel(m, rid, workFileName, "instance");
            }
        }
    }

    public static void doTransfer() {
    	MigrationApp.ensureGitRepo("work", MigrationApp.OUTPUT_DIR);
    	MigrationApp.ensureGitRepo("outline", MigrationApp.OUTPUT_DIR);
        initLists();
        final File dir = new File(MigrationApp.RKTS_DIR);
        final File[] directoryListing = dir.listFiles();
        System.out.println("transfering "+directoryListing.length+" works produced by rKTs migration from "+MigrationApp.RKTS_DIR);
        if (directoryListing != null) {
          for (File child : directoryListing) {
            final String fileBaseName = child.getName();
            if (!fileBaseName.endsWith(".ttl"))
                continue;
            final String rid = fileBaseName.substring(0, fileBaseName.length()-4);
            final int underIndex = rid.indexOf('_');
            if (underIndex != -1 || RidModels.containsKey(rid)) {
                String baseRid = rid;
                if (underIndex != -1)
                    baseRid = fileBaseName.substring(0, underIndex);
                final Model m =  RidModels.get(baseRid);
                if (m == null) {
                    System.err.println("rKTs-migration: cannot find "+baseRid+" ("+rid+")");
                    continue;
                }
                final InputStream in;
                try {
                    in = new FileInputStream(child);
                    m.read(in, null, "TTL");
                    in.close();
                } catch (IOException | RiotException e) {
                    System.err.println("can't read from "+child.getName());
                    e.printStackTrace();
                    continue;
                }
                
            } else {
                final Model m = ModelFactory.createDefaultModel();
                setPrefixes(m);
                final InputStream in;
                try {
                    in = new FileInputStream(child);
                    m.read(in, null, "TTL");
                    in.close();
                } catch (IOException | RiotException e) {
                    System.err.println("can't read from "+child.getName());
                    e.printStackTrace();
                    continue;
                }
                final String workName = fileBaseName.substring(0, fileBaseName.length()-4);
                final String workOutFileName = MigrationApp.getDstFileName("work", workName, ".trig");
                if (!workName.startsWith("WA0R")) {
                    Model existingM = MigrationHelpers.modelFromFileName(workOutFileName);
                    if (existingM != null) {
                        // maybe the sa-x-ndia strings should be removed?
                        m.add(existingM);
                        //Resource admin = m.createResource(BDA+workName);
                        //admin.removeAll(m.getProperty(ADM, "metadataLegal"));
                        //admin.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_rKTs_CC0"));
                    }
                } else {
                    Resource admin = m.createResource(BDA+workName);
                    admin.addProperty(RDF.type, m.createResource(ADM+"AdminData"));
                    admin.addProperty(m.getProperty(ADM, "adminAbout"), m.createResource(BDR+workName));
                    admin.addProperty(m.getProperty(ADM, "status"), m.createResource(BDA+"StatusReleased"));
                    admin.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_rKTs_CC0"));
                    // TODO: add log entry?
                }
                MigrationHelpers.outputOneModel(m, workName, workOutFileName, "work");
            }
            
          }
          finishEditions();
        } else {
            System.err.println("The rKTs directory you provided is not a valid directory");
        }
    }
}
