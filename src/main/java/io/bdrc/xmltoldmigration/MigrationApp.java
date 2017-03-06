package io.bdrc.xmltoldmigration;

import java.io.File;
import java.util.stream.Stream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;

/**
 * Hello world!
 *
 */
public class MigrationApp 
{
    
    // extract tbrc/ folder of exist-db backup here:
    public static final String DATA_DIR = "tbrc/";
    public static final String OUTPUT_DIR = "tbrc-jsonld/";
    
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
        String outfileName = fileName.substring(0, fileName.length()-3)+"jsonld";
        outfileName = OUTPUT_DIR+"tbrc-"+type+"s/"+outfileName;
        MigrationHelpers.convertOneFile(file.getAbsolutePath(), outfileName, type, true, fileName);
    }
    
    public static void migrateType(String type, String mustStartWith) {
        createDirIfNotExists(OUTPUT_DIR+"tbrc-"+type+"s");
        String dirName = DATA_DIR+"tbrc-"+type+"s";
        File[] files = new File(dirName).listFiles();
        System.out.println("converting "+files.length+" "+type+" files");
        Stream.of(files).parallel().forEach(file -> migrateOneFile(file, type, mustStartWith));
    }
    
    public static void main( String[] args )
    {
        createDirIfNotExists(OUTPUT_DIR);
        long startTime = System.currentTimeMillis();
    	//migrateType("place", "G");
    	//migrateType("office", "R");
        //migrateType("person", "P");
        //migrateType("corporation", "C");
        //migrateType("lineage", "L");
        migrateType("outline", "O");
    	long estimatedTime = System.currentTimeMillis() - startTime;
    	System.out.println("done in "+estimatedTime+" ms");
    }
}
