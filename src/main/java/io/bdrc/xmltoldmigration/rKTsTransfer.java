package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.GitHelpers.ensureGitRepo;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDR;
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
        initListsForRID("MW30532");
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
    }
    
    public static void initListsForRID(String rid) {
        RIDList.add(rid);
        final String workFileName = MigrationApp.getDstFileName("instance", rid, ".trig");
        Model m = MigrationHelpers.modelFromFileName(workFileName);
        RidModels.put(rid, m);
    }
    
    public static void finishEditions() {
        for (String rid : RIDList) {
            Model m = RidModels.get(rid);
            final String workFileName = MigrationApp.getDstFileName("instance", rid);
            MigrationHelpers.outputOneModel(m, rid, workFileName, "work");
        }
    }

    public static void doTransfer() {
        ensureGitRepo("work", MigrationApp.OUTPUT_DIR);
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
                if (!workName.startsWith("W0R")) {
                    Model existingM = MigrationHelpers.modelFromFileName(workOutFileName);
                    if (existingM != null) {
                        // maybe the sa-x-ndia strings should be removed?
                        m.add(existingM);
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
