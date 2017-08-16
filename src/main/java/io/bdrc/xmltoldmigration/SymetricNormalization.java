package io.bdrc.xmltoldmigration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;

public class SymetricNormalization {

    public static final String BDO = CommonMigration.BDO;
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
        propInfos.put("placeContains", new SymetryInfo(BDO+"placeLocatedIn", 1));
        propInfos.put("placeLocatedIn", new SymetryInfo(BDO+"placeContains", 0));
        propInfos.put("placeIsNear", new SymetryInfo(BDO+"placeIsNear", 0));
        propInfos.put("workExpressionOf", new SymetryInfo(BDO+"workHasExpression", 1));
        propInfos.put("workHasExpression", new SymetryInfo(BDO+"workExpressionOf", 0));
        propInfos.put("workExpressionOf", new SymetryInfo(BDO+"workHasExpression", 1));
        // TODO: should be handled in the code directly:
        // - workPartOf       vs. workHaspart
        // - workHasItem      vs. itemForWork
        // TODO: create workHasNumber (sym. of workNumberOf)?
        propInfos.put("personHasIncarnation", new SymetryInfo(BDO+"personIncarnationOf", 1));
        propInfos.put("incarnationOf", new SymetryInfo(BDO+"hasIncarnation", 0));
        propInfos.put("personHasConsort", new SymetryInfo(BDO+"personHasConsort", 0));
        propInfos.put("personTeacherOf", new SymetryInfo(BDO+"personStudentOf", 0));
        propInfos.put("personStudentOf", new SymetryInfo(BDO+"personTeacherOf", 1));
    }
    
    public static SymetryInfo getKinSymInfo(String prop, int gender) {
        switch(prop) {
        case "hasSon":
        case "hasDaughter":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasFather", 0);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasMother", 0);
            default:
                return new SymetryInfo(BDO+"hasParent", 0);
            }
        case "hasMother":
        case "hasFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasSon", 1);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasDaughter", 1);
            default:
                return new SymetryInfo(BDO+"hasChild", 1);
            }
        case "hasWife":
        case "hasHusband":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasHusband", 2);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasWife", 2);
            default:
                return new SymetryInfo(BDO+"hasSpouse", 2);
            }
        // no inLaw in the database, we just skip
        case "hasSister":
        case "hasBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasBrother", 2);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasSister", 2);
            default:
                return new SymetryInfo(BDO+"hasSibling", 2); // this could include younger/older
            }
        case "hasYoungerSister":
        case "hasYoungerBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasOlderBrother", 2);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasOlderSister", 2);
            default:
                return null;
            }
        case "hasOlderSister":
        case "hasOlderBrother":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasYoungerBrother", 2);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasYoungerSister", 2);
            default:
                return null;
            }
        case "hasGrandMother":
        case "hasGrandFather":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasGrandSon", 1);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasGrandDaughter", 1);
            default:
                return new SymetryInfo(BDO+"hasGrandChild", 1);
            }
        case "hasGrandDaughter":
        case "hasGrandSon":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasGrandFather", 0);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasGrandMother", 0);
            default:
                return new SymetryInfo(BDO+"hasGrandParent", 0);
            }
        case "hasAunt":
        case "hasUncle":
        case "hasMaternalUncle":
        case "hasPaternalUncle":
        case "hasMaternalAunt":
        case "hasPaternalAunt":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasNephew", 0);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasNiece", 0);
            default:
                return new SymetryInfo(BDO+"hasNibling", 0);
            }
        case "hasNephew":
        case "hasNiece":
            switch (gender) {
            case GENDER_M:
                return new SymetryInfo(BDO+"hasUncle", 0);
            case GENDER_F:
                return new SymetryInfo(BDO+"hasAunt", 0);
            default:
                return new SymetryInfo(BDO+"hasParentSibling", 0);
            }
        case "hasCousin":
            return new SymetryInfo(BDO+"hasCousin", 2);
        }
        return null;
    }
    
    public static Map<String,Map<String,List<String>>> knownTriples = new HashMap<>();
    
    public static Map<String,Map<String,List<String>>> triplesToAdd = new HashMap<>(); 
    
    //public static Map<String,Map<String,String>[2]> knownTriples = new HashMap<>();
    
    public static void addSymetricProperty(Model m, final String propertyName, final String sourceUri, final String destUri, final Integer gender) {
        SymetryInfo symInfos;
        if (gender != null && !propertyName.equals("personHasConsort")) {
            symInfos = getKinSymInfo(propertyName, gender);
        } else {
            symInfos = propInfos.get(propertyName);
        }
        if (symInfos == null) {
            m.add(m.getResource(sourceUri), m.getProperty(BDO, propertyName), m.getResource(destUri));
            return;
        }
        Map<String,List<String>> docTriples = knownTriples.computeIfAbsent(sourceUri, (x -> new HashMap<String,List<String>>()));
        if (!oneDirection || symInfos.isMain != 1) {
            m.add(m.getResource(sourceUri), m.getProperty(BDO, propertyName), m.getResource(destUri));
            List<String> knownObjects = docTriples.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            if (!knownObjects.contains(destUri))
                knownObjects.add(destUri);
        }
        if (!oneDirection || symInfos.isMain == 1) {
            Map<String,List<String>> symDocTriples = knownTriples.computeIfAbsent(destUri, (x -> new HashMap<String,List<String>>()));
            List<String> knownSymObjects = symDocTriples.computeIfAbsent(propertyName, (x -> new ArrayList<String>()));
            if (!knownSymObjects.contains(sourceUri)) {
                knownSymObjects.add(sourceUri);
                Map<String,List<String>> triplesToAddForDest = triplesToAdd.computeIfAbsent(destUri, (x -> new HashMap<String,List<String>>()));
                List<String> objectsToAdd = triplesToAddForDest.computeIfAbsent(symInfos.symUri, (x -> new ArrayList<String>()));
                objectsToAdd.add(sourceUri);
            }
        }
    }
    
    public static void insertMissingTriplesInModel(final Model m, final String resourceUri) {
        Map<String,List<String>> docTriplesToAdd = triplesToAdd.get(resourceUri);
        if (docTriplesToAdd == null)
            return;
        for (Entry<String,List<String>> e : docTriplesToAdd.entrySet() ) {
            for (String o : e.getValue() ) {
                m.add(m.getResource(resourceUri), m.getProperty(BDO, e.getKey()), m.getResource(o));
            }
        }
    }
    
    public static void cleanModelFromDups(final Model m, final String type, final String resourceUri) {
        // TODO: clean up:
        // - (youngerSister|olderSister) + sister = remove sister
        // - same for brother, maternal/paternal uncle/aunt, parentSibling, child, parent, sibling, etc.
    }
    
}
