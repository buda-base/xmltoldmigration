package io.bdrc.xmltoldmigration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;

public class SymetricNormalization {

    public static final String BDO = CommonMigration.BDO;
    public static final String BDR = CommonMigration.BDR;
    public static final int GENDER_M = 0;
    public static final int GENDER_F = 1;
    public static final int GENDER_U = 2;
    
    public static boolean oneDirection = true;
    
    public static class SymetryInfo {
        public final String symUri;
        // isMain is:
        // - 0 in case where it's the main one
        // - 1 in case the symetric prop is the main one
        // - 2 in case where both should be kept (ex: hasBrother)
        public final int isMain;
        // list of the subproperties of the symetric property
        public List<String> symSubclasses = null;
        // the superclass of the symetric property
        public String symSubclassOf = null;
        
        public SymetryInfo(final String symUri, final int isMain) {
            this.symUri = symUri;
            this.isMain = isMain;
        }

        public SymetryInfo(final String symUri, final int isMain, List<String> symSubclasses, String symSubclassOf) {
            this.symUri = symUri;
            this.isMain = isMain;
            this.symSubclasses = symSubclasses;
            this.symSubclassOf = symSubclassOf;
        }
    }
    
    public static Map<String,SymetryInfo> propInfos = new HashMap<>();
    
    static {
        fillMap();
    }
    
    public static void fillMap() {
        propInfos.put("placeContains", new SymetryInfo("placeLocatedIn", 1));
        propInfos.put("placeLocatedIn", new SymetryInfo("placeContains", 0));
        propInfos.put("placeIsNear", new SymetryInfo("placeIsNear", 0));
        propInfos.put("workExpressionOf", new SymetryInfo("workHasExpression", 1));
        propInfos.put("workHasExpression", new SymetryInfo("workExpressionOf", 0));
        propInfos.put("workExpressionOf", new SymetryInfo("workHasExpression", 1));
        // TODO: these are handled in the code directly:
        // - workPartOf       vs. workHaspart
        // - workHasItem      vs. itemForWork
        // TODO: create workHasNumber (sym. of workNumberOf)?
        propInfos.put("personHasIncarnation", new SymetryInfo("personIncarnationOf", 1));
        propInfos.put("incarnationOf", new SymetryInfo("hasIncarnation", 0));
        propInfos.put("personHasConsort", new SymetryInfo("personHasConsort", 0));
        propInfos.put("personTeacherOf", new SymetryInfo("personStudentOf", 0));
        propInfos.put("personStudentOf", new SymetryInfo("personTeacherOf", 1));
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
        case "hasHusband":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo("hasHusband", 2);
            case GENDER_F:
                return new SymetryInfo("hasWife", 2);
            default:
                return new SymetryInfo("hasSpouse", 2);
            }
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
                return new SymetryInfo("hasUncle", 0);
            case GENDER_F:
                return new SymetryInfo("hasAunt", 0);
            default:
                return new SymetryInfo("hasParentSibling", 0);
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
        if (!oneDirection || symInfos.isMain != 1) {
            m.add(m.getResource(BDR+sourceName), m.getProperty(BDO, propertyName), m.getResource(BDR+destName));
            List<String> knownObjects = docTriples.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            if (!knownObjects.contains(destName))
                knownObjects.add(destName);
        }
        if (!oneDirection || symInfos.isMain == 1) {
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
