package io.bdrc.xmltoldmigration.helpers;

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

import io.bdrc.xmltoldmigration.MigrationApp;

import org.eclipse.jgit.api.Status;

public class GitHelpers {

    public static final String gitignore = "# Ignore everything\n" + 
            "*\n" + 
            "# Don't ignore directories, so we can recurse into them\n" + 
            "!*/\n" + 
            "# Don't ignore .gitignore and *.foo files\n" + 
            "!.gitignore\n" + 
            "!*.trig\n" + 
            "";
    
    public static Map<String,Repository> typeRepo = new HashMap<>();
    
    public static void ensureGitRepo(String type) {
        if (typeRepo.containsKey(type))
            return;
        String dirpath = MigrationApp.OUTPUT_DIR+type+"s-20220922";
        MigrationApp.createDirIfNotExists(dirpath);
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        File gitDir = new File(dirpath+"/.git");
        File wtDir = new File(dirpath);
        try {
            Repository repository = builder.setGitDir(gitDir)
              .setWorkTree(wtDir)
              //.setMustExist( true )
              .readEnvironment() // scan environment GIT_* variables
              .build();
            if (!repository.getObjectDatabase().exists()) {
                System.out.println("create git repository in "+dirpath);
                repository.create();
                PrintWriter out = new PrintWriter(dirpath+"/.gitignore");
                out.println(gitignore);
                out.close();
            }
            typeRepo.put(type, repository);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static Set<String> getChanges(String type) {
        Repository r = typeRepo.get(type); 
        if (r == null)
            return null;
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
        git.close();
        return res;
    }

    public static void commitChanges(String type, String commitMessage) {
        Repository r = typeRepo.get(type); 
        if (r == null)
            return;
        Git git = new Git(r);
        try {
            git.add().addFilepattern(".").call();
            git.commit().setMessage(commitMessage).call();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        git.close();
    }
    
}
