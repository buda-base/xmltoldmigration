package io.bdrc.xmltoldmigration.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;

import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;

public class SymetricNormalization {

    public static final String BDO = CommonMigration.BDO;
    public static final String BDR = CommonMigration.BDR;
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
    
    public static void normalizeOneDirection(boolean oneDirectionArg, boolean preferManyOverOne) {
        fillMap(preferManyOverOne);
        oneDirection = oneDirectionArg;
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
    }
    
    public static Map<String,SymetryInfo> propInfos = new HashMap<>();
    
    public static void fillMap(boolean preferManyOverOne) {
        int manyInt = preferManyOverOne ? 1 : 0;
        int oneInt = preferManyOverOne ? 0 : 1;
        propInfos.put("placeContains", new SymetryInfo("placeLocatedIn", manyInt));
        propInfos.put("placeLocatedIn", new SymetryInfo("placeContains", oneInt));
        // let's not care about placeIsNear
        //propInfos.put("placeIsNear", new SymetryInfo("placeIsNear", 2));
        propInfos.put("workExpressionOf", new SymetryInfo("workHasExpression", oneInt));
        propInfos.put("workHasExpression", new SymetryInfo("workExpressionOf", manyInt));
        propInfos.put("workExpressionOf", new SymetryInfo("workHasExpression", oneInt));
        propInfos.put("workNumberOf", new SymetryInfo("workHasNumber", oneInt));
        propInfos.put("workHasNumber", new SymetryInfo("workNumberOf", manyInt));
        // TODO: these are handled in the code directly:
        // - workPartOf       vs. workHaspart
        // - workHasItem      vs. itemForWork
        //propInfos.put("personHasIncarnation", new SymetryInfo("personIncarnationOf", 1));
        //propInfos.put("incarnationOf", new SymetryInfo("hasIncarnation", 0));
        propInfos.put("personHasConsort", new SymetryInfo("personHasConsort", 2));
        propInfos.put("personTeacherOf", new SymetryInfo("personStudentOf", manyInt));
        propInfos.put("personStudentOf", new SymetryInfo("personTeacherOf", oneInt));
    }
    
    public static SymetryInfo getKinSymInfo(String prop, int gender) {
        switch(prop) {
        case "hasSon":
        case "hasDaughter":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasFather", 1);
            case GENDER_F:
                return new SymetryInfo("hasMother", 1);
            default:
                return new SymetryInfo("hasParent", 1);
            }
        case "hasMother":
        case "hasFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasSon", 0);
            case GENDER_F:
                return new SymetryInfo("hasDaughter", 0);
            default:
                return new SymetryInfo("hasChild", 0);
            }
        case "hasWife":
            return new SymetryInfo("hasHusband", 1);
        case "hasHusband":
            return new SymetryInfo("hasWife", 0);
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
                return new SymetryInfo("hasOlderBrother", 1);
            case GENDER_F:
                return new SymetryInfo("hasOlderSister", 1);
            default:
                return null;
            }
        case "hasOlderSister":
        case "hasOlderBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasYoungerBrother", 0);
            case GENDER_F:
                return new SymetryInfo("hasYoungerSister", 0);
            default:
                return null;
            }
        case "hasGrandMother":
        case "hasGrandFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasGrandSon", 0);
            case GENDER_F:
                return new SymetryInfo("hasGrandDaughter", 0);
            default:
                return new SymetryInfo("hasGrandChild", 0);
            }
        case "hasGrandDaughter":
        case "hasGrandSon":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasGrandFather", 1);
            case GENDER_F:
                return new SymetryInfo("hasGrandMother", 1);
            default:
                return new SymetryInfo("hasGrandParent", 1);
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
    
    //public static Map<String,Map<String,String>[2]> knownTriples = new HashMap<>();
    
    public static void addSymetricProperty(Model m, final String propertyName, final String sourceName, final String destName, final Integer gender) {
        SymetryInfo symInfos;
        if (gender != null && !propertyName.equals("personHasConsort")) {
            symInfos = getKinSymInfo(propertyName, gender);
        } else {
            symInfos = propInfos.get(propertyName);
        }
        if (symInfos == null) {
            m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
            return;
        }
        Map<String,List<String>> docTriples = knownTriples.computeIfAbsent(sourceName, (x -> new HashMap<String,List<String>>()));
        if (!oneDirection || symInfos.isMain != 0) {
            m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
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
