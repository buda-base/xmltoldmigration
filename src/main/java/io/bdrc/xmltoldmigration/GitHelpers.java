package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.api.Status;

public class GitHelpers {

    public static final String gitignore = "# Ignore everything\n" + 
            "*\n" + 
            "# Don't ignore directories, so we can recurse into them\n" + 
            "!*/\n" + 
            "# Don't ignore .gitignore and *.foo files\n" + 
            "!.gitignore\n" + 
            "!*.ttl\n" + 
            "";
    
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
    
    public static Map<String,Repository> typeRepo = new HashMap<>();
    
    public static void ensureGitRepo(String type) {
        if (typeRepo.containsKey(type))
            return;
        String dirpath = MigrationApp.OUTPUT_DIR+type;
        createDirIfNotExists(dirpath);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File dir = new File(dirpath);
        try {
            Repository repository = builder.setGitDir(dir)
              .readEnvironment() // scan environment GIT_* variables
              .findGitDir() // scan up the file system tree
              .build();
            if (!repository.getObjectDatabase().exists()) {
                System.out.println("create git repository in "+dirpath);
                repository.create();
                PrintWriter out = new PrintWriter(dirpath+".gitignore");
                out.println(gitignore);
                out.close();
            }
            typeRepo.put(type, repository);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Set<String> getChanges(String type) {
        Git git = new Git(typeRepo.get(type));
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
        git.close();
        return res;
    }
    
}
