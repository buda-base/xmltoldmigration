package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import io.bdrc.xmltoldmigration.helpers.GitHelpers;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

/* This class does not do any actual migration of the rKTs data,
 * which is handled by https://github.com/BuddhistDigitalResourceCenter/rKTs-migration
 * here we just transfer the output of the migration to the git repositories
 */

public class rKTsTransfer {

    public static List<String> RIDList = new ArrayList<>();
    public static Map<String, Model> RidModels = new HashMap<>();
    
    public static void initLists() {
        initListsForRID("W22084");
        initListsForRID("W30532");
        initListsForRID("W4CZ5369");
    }
    
    public static void initListsForRID(String rid) {
        RIDList.add(rid);
        final String workFileName = MigrationApp.getDstFileName("work", rid);
        Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        final InputStream in;
        try {
            in = new FileInputStream(new File(workFileName));
        } catch (FileNotFoundException e) {
            System.err.println("can't read from "+workFileName);
            return;
        }
        m.read(in, null, "TTL");
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        RidModels.put(rid, m);
    }
    
    public static void finishEditions() {
        for (String rid : RIDList) {
            Model m = RidModels.get(rid);
            final String workFileName = MigrationApp.getDstFileName("work", rid);
            MigrationHelpers.outputOneModel(m, rid, workFileName, "work");
        }
    }

    public static void doTransfer() {
        GitHelpers.ensureGitRepo("work");
        initLists();
        final File dir = new File(MigrationApp.RKTS_DIR);
        final File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
          for (File child : directoryListing) {
            final String fileBaseName = child.getName();
            if (!fileBaseName.endsWith(".ttl"))
                continue;
            final int underIndex = fileBaseName.indexOf('_'); 
            if (underIndex != -1) {
                final String rid = fileBaseName.substring(0, underIndex);
                final Model m =  RidModels.get(rid);
                if (m == null) {
                    System.err.println("hmm, I think I have a problem here...");
                    continue;
                }
                final InputStream in;
                try {
                    in = new FileInputStream(child);
                } catch (FileNotFoundException e) {
                    System.err.println("can't read from "+child.getName());
                    continue;
                }
                m.read(in, null, "TTL");
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                final Model m = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(m);
                final InputStream in;
                try {
                    in = new FileInputStream(child);
                } catch (FileNotFoundException e) {
                    System.err.println("can't read from "+child.getName());
                    continue;
                }
                m.read(in, null, "TTL");
                try {
                    in.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                final String workName = fileBaseName.substring(0, fileBaseName.length()-4);
                final String workOutFileName = MigrationApp.getDstFileName("work", workName);
                MigrationHelpers.outputOneModel(m, workName, workOutFileName, "work");
            }
            
          }
          finishEditions();
        } else {
            System.err.println("The rKTs directory you provided is not a valid directory");
        }
    }
}