package io.bdrc.xmltoldmigration.helpers;

import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.BrokenException;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;

public class SymetricNormalization {

    public static final int GENDER_M = 0;
    public static final int GENDER_F = 1;
    public static final int GENDER_U = 2;
    
    public static int addedTriples = 0;
    public static int removedTriples = 0;
    
    public static boolean oneDirection = true;
    
    public static class SymetryInfo {
        public final String symUri;
        // isMain is:
        // - 0 in case where it's the main one
        // - 1 in case the symetric prop is the main one
        // - 2 in case where both should be kept (ex: hasBrother)
        public final int isMain;
        
        public SymetryInfo(final String symUri, final int isMain) {
            this.symUri = symUri;
            this.isMain = isMain;
        }
    }
    
    //AbstractWorks_CheckThis - may need tweaking for item/work relations
    public static void normalizeOneDirection(boolean oneDirectionArg, boolean preferManyOverOne) {
        fillMap(preferManyOverOne);
        oneDirection = oneDirectionArg;
        ImagegroupMigration.addVolumeOf = false;
        ImagegroupMigration.addItemHasVolume = true;
        OutlineMigration.addWorkHaspart = false;
        OutlineMigration.addWorkPartOf = true;
        /*
        if (oneDirectionArg == true) {
            WorkMigration.addItemForWork = !preferManyOverOne;
            WorkMigration.addWorkHasItem = preferManyOverOne;
            ImagegroupMigration.addVolumeOf = !preferManyOverOne;
            ImagegroupMigration.addItemHasVolume = preferManyOverOne;
            OutlineMigration.addWorkHaspart = preferManyOverOne;
            OutlineMigration.addWorkPartOf = !preferManyOverOne;
        } else {
            ImagegroupMigration.addVolumeOf = true;
            ImagegroupMigration.addItemHasVolume = true;
            WorkMigration.addItemForWork = true;
            WorkMigration.addWorkHasItem = true;
            OutlineMigration.addWorkHaspart = true;
            OutlineMigration.addWorkPartOf = true;
        }
        */
    }
    
    public static Map<String,SymetryInfo> propInfos = new HashMap<>();
    
    public static void fillMap(boolean preferManyOverOne) {
        int manyInt = preferManyOverOne ? 1 : 0;
        int oneInt = preferManyOverOne ? 0 : 1;
        propInfos.put("placeContains", new SymetryInfo("placeLocatedIn", 0));
        propInfos.put("placeLocatedIn", new SymetryInfo("placeContains", 1));
        // let's not care about placeIsNear
        //propInfos.put("placeIsNear", new SymetryInfo("placeIsNear", 2));
        propInfos.put("instanceOf", new SymetryInfo("workHasInstance", 1));
        propInfos.put("workHasInstance", new SymetryInfo("instanceOf", 0));
        propInfos.put("serialMemberOf", new SymetryInfo("serialHasMember", 1));
        propInfos.put("serialHasMember", new SymetryInfo("serialMemberOf", 0));
        propInfos.put("serialInstanceOf", new SymetryInfo("serialHasInstance", 1));
        propInfos.put("serialHasInstance", new SymetryInfo("serialInstanceOf", 0));
        // TODO: these are handled in the code directly:
        // - workPartOf       vs. workHaspart
        // - workHasItem      vs. itemForWork
        //propInfos.put("personHasIncarnation", new SymetryInfo("personIncarnationOf", 1));
        //propInfos.put("incarnationOf", new SymetryInfo("hasIncarnation", 0));
        propInfos.put("personHasConsort", new SymetryInfo("personHasConsort", 2));
        propInfos.put("personTeacherOf", new SymetryInfo("personStudentOf", 0));
        propInfos.put("personStudentOf", new SymetryInfo("personTeacherOf", 1));
        //propInfos.put("instanceHasReproduction", new SymetryInfo("instanceReproductionOf", manyInt));
        //propInfos.put("instanceReproductionOf", new SymetryInfo("instanceHasReproduction", oneInt));        
    }
    
    public static SymetryInfo getKinSymInfo(String prop, int gender) {
        switch(prop) {
        case "hasSon":
        case "hasDaughter":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasFather", 0);
            case GENDER_F:
                return new SymetryInfo("hasMother", 0);
            default:
                return new SymetryInfo("hasParent", 0);
            }
        case "hasMother":
        case "hasFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasSon", 1);
            case GENDER_F:
                return new SymetryInfo("hasDaughter", 1);
            default:
                return new SymetryInfo("hasChild", 1);
            }
        case "hasWife":
            return new SymetryInfo("hasHusband", 2);
        case "hasHusband":
            return new SymetryInfo("hasWife", 2);
        // no inLaw in the database, we just skip
        case "hasSister":
        case "hasBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasBrother", 2);
            case GENDER_F:
                return new SymetryInfo("hasSister", 2);
            default:
                return new SymetryInfo("hasSibling", 2); // this could include younger/older
            }
        case "hasYoungerSister":
        case "hasYoungerBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasOlderBrother", 2);
            case GENDER_F:
                return new SymetryInfo("hasOlderSister", 2);
            default:
                return null;
            }
        case "hasOlderSister":
        case "hasOlderBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasYoungerBrother", 2);
            case GENDER_F:
                return new SymetryInfo("hasYoungerSister", 2);
            default:
                return null;
            }
        case "hasGrandMother":
        case "hasGrandFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasGrandSon", 1);
            case GENDER_F:
                return new SymetryInfo("hasGrandDaughter", 1);
            default:
                return new SymetryInfo("hasGrandChild", 1);
            }
        case "hasGrandDaughter":
        case "hasGrandSon":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasGrandFather", 0);
            case GENDER_F:
                return new SymetryInfo("hasGrandMother", 0);
            default:
                return new SymetryInfo("hasGrandParent", 0);
            }
        case "hasAunt":
        case "hasUncle":
        case "hasMaternalUncle":
        case "hasPaternalUncle":
        case "hasMaternalAunt":
        case "hasPaternalAunt":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasNephew", 0);
            case GENDER_F:
                return new SymetryInfo("hasNiece", 0);
            default:
                return new SymetryInfo("hasNibling", 0);
            }
        case "hasNephew":
        case "hasNiece":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasUncle", 1);
            case GENDER_F:
                return new SymetryInfo("hasAunt", 1);
            default:
                return new SymetryInfo("hasParentSibling", 1);
            }
        case "hasCousin":
            return new SymetryInfo("hasCousin", 2);
        }
        return null;
    }
    
    public static Map<String,Map<String,List<String>>> knownTriples = new HashMap<>();
    
    public static Map<String,Map<String,List<String>>> triplesToAdd = new HashMap<>();
    
    public static void reinit() {
        knownTriples = new HashMap<>();
        triplesToAdd = new HashMap<>();
    }

    //public static Map<String,Map<String,String>[2]> knownTriples = new HashMap<>();
    
    public static void addSymetricProperty(Model m, final String propertyName, final String sourceName, final String destName, final Integer gender) {
        SymetryInfo symInfos;
        if (MigrationHelpers.ridReplacements.containsKey(sourceName)) {
            // we're in a withdrawn resource, we don't want the symmetric property!
        	try {
            	m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
	        } catch (BrokenException e) {
	    		System.err.println("can't add symmetric properties for sourcename="+sourceName+", propertyName="+propertyName+", destName="+destName);
	    		e.printStackTrace();
	    	}
            return;
        }
        if (gender != null && !propertyName.equals("personHasConsort")) {
            symInfos = getKinSymInfo(propertyName, gender);
        } else {
            symInfos = propInfos.get(propertyName);
        }
        if (symInfos == null) {
        	try {
        		m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
        	} catch (BrokenException e) {
        		System.err.println("can't add symmetric properties for sourcename="+sourceName+", propertyName="+propertyName+", destName="+destName);
        		e.printStackTrace();
        	}
            return;
        }
        Map<String,List<String>> docTriples = knownTriples.computeIfAbsent(sourceName, (x -> new HashMap<String,List<String>>()));
        if (!oneDirection || symInfos.isMain != 0) {
        	try {
        		m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
        	} catch (BrokenException e) {
        		System.err.println("can't add symmetric properties for sourcename="+sourceName+", propertyName="+propertyName+", destName="+destName);
        		e.printStackTrace();
        	}
            List<String> knownObjects = docTriples.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            if (!knownObjects.contains(destName))
                knownObjects.add(destName);
        } else {
            removedTriples += 1;
        }
        if (!oneDirection || symInfos.isMain != 1) {
            Map<String,List<String>> symDocTriples = knownTriples.computeIfAbsent(destName, (x -> new HashMap<String,List<String>>()));
            List<String> knownSymObjects = symDocTriples.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            if (!knownSymObjects.contains(sourceName)) {
                knownSymObjects.add(sourceName);
                Map<String,List<String>> triplesToAddForDest = triplesToAdd.computeIfAbsent(destName, (x -> new HashMap<String,List<String>>()));
                List<String> objectsToAdd = triplesToAddForDest.computeIfAbsent(symInfos.symUri, (x -> new ArrayList<String>()));
                objectsToAdd.add(sourceName);
            }
        }
    }
    
    public static void addSymetricTriple(final String propertyName, final String sourceName, final String destName) {
            Map<String,List<String>> triplesToAddForSrc = triplesToAdd.computeIfAbsent(sourceName, (x -> new HashMap<String,List<String>>()));
            List<String> objectsToAdd = triplesToAddForSrc.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            objectsToAdd.add(destName);
    }
    
    public static void insertMissingTriplesInModel(final Model m, final String resourceName) {
        insertMissingTriplesInModel(m, resourceName, true);
    }
    
    public static void insertMissingTriplesInModel(final Model m, final String resourceName, boolean removeAtEnd) {
        Map<String,List<String>> docTriplesToAdd = triplesToAdd.get(resourceName);
        if (docTriplesToAdd == null)
            return;
        for (Map.Entry<String,List<String>> e : docTriplesToAdd.entrySet() ) {
            // no need to add the values in knownTriples
            for (String o : e.getValue() ) {
                m.add(m.getResource(BDR+resourceName), m.getProperty(BDO, e.getKey()), m.getResource(BDR+o));
            }
            addedTriples += e.getValue().size();
        }
        if (removeAtEnd)
            triplesToAdd.remove(resourceName);
    }
    
    public static void cleanModelFromDups(final Model m, final String type, final String resourceUri) {
        // TODO: clean up:
        // - (youngerSister|olderSister) + sister = remove sister
        // - same for brother, maternal/paternal uncle/aunt, parentSibling, child, parent, sibling, etc.
    }
    
}
