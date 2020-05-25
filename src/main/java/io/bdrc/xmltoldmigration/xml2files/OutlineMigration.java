package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;
import static io.bdrc.libraries.Models.addStatus;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationApp;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.WorkModelInfo;

public class OutlineMigration {

	public static final String OXSDNS = "http://www.tbrc.org/models/outline#";
	
	public static boolean splitOutlines = false;
	
	public static MessageDigest md5;
	
    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
	
    public static boolean addWorkHaspart = true;
    public static boolean addWorkPartOf = false;

	public static Map<String,Boolean> ridsToIgnore = new HashMap<>();
	public static Map<String,String> ridsToConvert = new HashMap<>();
	static {
        ridsToIgnore.put("O2MS4765", true);
        ridsToIgnore.put("O2MS5129", true);
        ridsToIgnore.put("O1TLMXXX000011", true);
        ridsToIgnore.put("O1TLMXXX000012", true);
        ridsToIgnore.put("O3JW10074", true);
        ridsToIgnore.put("O3JW11025", true);
        ridsToIgnore.put("O3JW11874", true);
        ridsToIgnore.put("O3JW13595", true);
        ridsToIgnore.put("O3JW14444", true);
        ridsToIgnore.put("O3JW15385", true);
        ridsToIgnore.put("O3JW18061", true);
        ridsToIgnore.put("O3JW18930", true);
        ridsToIgnore.put("O3JW19779", true);
        ridsToIgnore.put("O4CTX297", true);
        ridsToIgnore.put("O3JW17161", true);
        ridsToIgnore.put("O2MS4381", true);
        ridsToIgnore.put("O4JW33589", true);
        ridsToIgnore.put("O3JW5309", true);
        ridsToIgnore.put("O5TAX003", true);
        ridsToIgnore.put("OTX2", true);
        ridsToIgnore.put("OTX5", true);
        ridsToIgnore.put("O4CTX325", true);
        ridsToIgnore.put("O4CTX313", true);
        ridsToIgnore.put("O4JW296", true);
        ridsToIgnore.put("O4JW313", true);
        ridsToIgnore.put("O4CTX298", true);
        ridsToIgnore.put("O4JW33649", true);
        ridsToIgnore.put("O10MS13722", true);
        ridsToIgnore.put("O2MS24613", true);
        ridsToIgnore.put("O1", true);
	    ridsToIgnore.put("O5JW1123", true);
	    ridsToIgnore.put("O5JW1071", true);
	    ridsToIgnore.put("O9TAXTBRC201605", true);
	    ridsToIgnore.put("O3JW16234", true);
	    ridsToIgnore.put("O3JW8867", true);
	    ridsToIgnore.put("O4JW33751", true);
	    ridsToIgnore.put("O4CTX296", true);
	    ridsToIgnore.put("O9TAXTBRC201605S", true);
	    ridsToIgnore.put("O9TAXTBRC201602", true);
	    ridsToIgnore.put("O9TAXTBRC201605DLD", true);
	    ridsToIgnore.put("OTX3", true);
	    ridsToIgnore.put("O9TAXTBRC201604", true);
	    ridsToIgnore.put("O5JW1109", true);
	    ridsToIgnore.put("O1HU51", true);
	    ridsToIgnore.put("O3JW20628", true);
	    ridsToIgnore.put("O4JW33653", true);
	    ridsToIgnore.put("O3JW7994", true);
        ridsToIgnore.put("O5TAX004", true);
        ridsToIgnore.put("O4JW33840", true);
        ridsToIgnore.put("OTX4", true);
        ridsToIgnore.put("O5JW18", true);
        ridsToIgnore.put("O10MS19652", true);
        ridsToIgnore.put("OTX1", true);
        ridsToIgnore.put("O3JW12746", true);
        ridsToIgnore.put("O5TAX006", true);
        ridsToIgnore.put("O4JW33844", true);
        ridsToIgnore.put("O4JW33784", true);
        ridsToIgnore.put("O4JW33827", true);
        ridsToIgnore.put("O5TAX005", true);
        ridsToIgnore.put("O5TAX002", true);
        ridsToIgnore.put("O5TAX007", true);
        ridsToIgnore.put("O5TAX007", true);
        ridsToIgnore.put("O5TAX001", true);
        ridsToIgnore.put("O5TAX008", true);
        ridsToIgnore.put("O4JW5431", true);
        
        // Kangyurs & Tengyurs
        ridsToIgnore.put("O1GS12980", true);
        ridsToIgnore.put("O1PD112371", true);
        ridsToIgnore.put("O1PD185519", true);
        ridsToIgnore.put("O01JW005", true);
        ridsToIgnore.put("O1PD11016", true);
        ridsToIgnore.put("O1PD19510", true);
        ridsToIgnore.put("O4CZ3720", true);
        ridsToIgnore.put("O01CT0007", true);
        ridsToIgnore.put("O1GS6011", true);
        ridsToIgnore.put("O1JC7630", true);
        ridsToIgnore.put("O1VI2", true);
        ridsToIgnore.put("O4CZ5369", true);
        ridsToIgnore.put("O2DB20796", true);
        ridsToIgnore.put("O2DB75712", true);
        ridsToIgnore.put("O1PD181215", true);
        ridsToIgnore.put("O2MS16391", true);
        ridsToIgnore.put("O00CR0008", true);
        
        // released outlines for withdrawn works:
        ridsToIgnore.put("O1GS129876", true);
        
        // Nyingma Gyubum
        // O1CT1002
        // O1CT1003
        // O21939
        // 
        
        // Rinchen Terdzo:
        // O20578
        // O4CZ337395
        // 
        
        ridsToConvert.put("O1AT3081AT374", null);
        ridsToConvert.put("O1AT3081AT380", null);
        ridsToConvert.put("O1GS392481GS39291", null);
        ridsToConvert.put("O1JM31JM426", null);
        ridsToConvert.put("O1KG40841KG4095", null);
        ridsToConvert.put("O1KG40841KG4096", null);
        ridsToConvert.put("O1KG40841KG4140", null);
        ridsToConvert.put("O1KG40841KG4141", null);
        ridsToConvert.put("O1KG40841KG4142", null);
        ridsToConvert.put("O1KG40841KG4144", null);
        ridsToConvert.put("O1KG40841KG4145", null);
        ridsToConvert.put("O1KG40841KG4146", null);
        ridsToConvert.put("O1KG40841KG4147", null);
        ridsToConvert.put("O1LS50811LS5085", null);
        ridsToConvert.put("O1LS50811LS5178", null);
        ridsToConvert.put("O1LS50811LS5313", null);
        ridsToConvert.put("O1LS50811LS5314", null);
        ridsToConvert.put("O2DB1024292DB102465", null);
        ridsToConvert.put("O2DB1024292DB102470", null);
        ridsToConvert.put("O2JT38952JT7614", null);
        ridsToConvert.put("O3LS125373LS13489", null);
	}
	
	
	
	static class CurNodeInt{
	    public int i = 0;
	}
	

    public static List<Element> getCreatorAncestorsForWork(String workId) {
        List<Element> res = new ArrayList<>();
        String fileName = MigrationApp.XML_DIR+"tbrc-works/"+workId+".xml";
        Document d = MigrationHelpers.documentFromFileName(fileName);
        Element root = d.getDocumentElement();
        // for non-sung bum, this seems to introduce more errors than it solves
        // so first determine if it's a sungbum :
        boolean issungbum = false;
        NodeList nodeList = root.getElementsByTagNameNS(WorkMigration.WXSDNS, "subject");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String topic = current.getAttribute("class").trim();
            if (topic.equals("T208")) {
                issungbum = true;
                break;
            }
        }
        if (!issungbum) {
            return res;
        }
        nodeList = root.getElementsByTagNameNS(WorkMigration.WXSDNS, "creator");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            if (!value.isEmpty() && !value.equals("hasMainAuthor")) {
                continue;
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty() || person.equals("Add to DLMS")) continue;
            res.add(current);
        }
        return res;
    }
	
	public static String getWorkId(Document xmlDocument) {
        Element root = xmlDocument.getDocumentElement();
        NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String workId = current.getAttribute("work").trim();
            if (!workId.isEmpty())
                return 'M'+workId;
        }
        ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "type", "missing work ID, cannot migrate outline");
        return null;
	}
	
	public static List<WorkModelInfo> MigrateOutline(Document xmlDocument) {
	    Model workModel = ModelFactory.createDefaultModel();
	    setPrefixes(workModel, "work");
	    String workId = getWorkId(xmlDocument);
	    if (workId == null || workId.isEmpty())
	        return null;
	    Resource work = createRoot(workModel, BDR+workId, BDO+"Work");
        //CommonMigration.addStatus(workModel, work, root.getAttribute("status"));
	    List<Element> ancestorCreators = new ArrayList<>();
	    return MigrateOutline(xmlDocument, workModel, work, ancestorCreators);
	}

	public static List<WorkModelInfo> MigrateOutline(Document xmlDocument, Model workModel, Resource rootWork, List<Element> ancestorCreators) {
		Model m;
		if (splitOutlines) {
		    m = ModelFactory.createDefaultModel();
		    setPrefixes(m, "work");
		} else {
		    m = workModel;
		}
		List<WorkModelInfo> res = new ArrayList<>();
		res.add(new WorkModelInfo(rootWork.getLocalName(), workModel));
		
        Resource admOutline = createAdminRoot(rootWork);

		Element root = xmlDocument.getDocumentElement();

		CurNodeInt curNodeInt = new CurNodeInt();
		curNodeInt.i = 0;
		String value;
		String legacyOutlineRID = root.getAttribute("RID");

        admOutline.addProperty(m.getProperty(BDO, "legacyOutlineNodeRID"), legacyOutlineRID);
        admOutline.addProperty(RDF.type, m.createResource(ADM+"Outline"));
        NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "NoType";
            }
            value = BDA+"OutlineType"+value.substring(0, 1).toUpperCase() + value.substring(1);
            m.add(admOutline, m.getProperty(ADM, "outlineType"), m.createResource(value));
        }

//        m.add(work, m.getProperty(ADM, "outline"), admOutline);
        value = root.getAttribute("pagination").trim();
        if (value.isEmpty() || value == "relative") {
            value = BDR+"PaginationRelative";
        } else {
            value = BDR+"PaginationAbsolute";            
        }
        m.add(rootWork, m.getProperty(BDO, "workPagination"), m.createResource(value));
        
        // if the outline names must really be migrated, do it here, they would be under
        // the tbr:outlineName property
        
		CommonMigration.addNotes(m, root, admOutline, OXSDNS);
		CommonMigration.addExternals(m, root, admOutline, OXSDNS);
		CommonMigration.addLog(m, root, admOutline, OXSDNS);
		CommonMigration.addDescriptions(m, root, admOutline, OXSDNS);
		CommonMigration.addLocations(m, admOutline, root, OXSDNS, rootWork.getLocalName(), legacyOutlineRID, legacyOutlineRID, null);
		
		// null?
		addCreators(m, admOutline, root, true, rootWork, null, ancestorCreators);
		
		// case where there's an unnecessary unique top node (ex: W1GS61415 / O1LS4227)
		final List<Element> nodeList2 = CommonMigration.getChildrenByTagName(root, OXSDNS, "node");
        final int nbChildren = nodeList2.size();
        Element node2 = root;
        if (nbChildren == 1) {
            node2 = nodeList2.get(0);
        }
		
		addNodes(m, rootWork, node2, rootWork.getLocalName(), curNodeInt, null, null, legacyOutlineRID, "", rootWork, res, ancestorCreators);
		//WorkMigration.exportTitleInfo(workModel);
		return res;
	}
	
	public static List<Element> addCreators(Model m, Resource rez, Element e, boolean isRoot, Resource rootWork, Resource nodeA, List<Element> oldElements) {
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "creator");

	    // first going through the list of ancestor elements to add the author to works corresponding
	    // to outline nodes 
	    if (nodeA != null) {
            for (Element current : oldElements) {
                String value = current.getAttribute("type").trim();
                if (value.isEmpty()) {
                    value = "hasMainAuthor";
                }
                if (isRoot) {
                    continue;
                }
                String person = current.getAttribute("person").trim(); // required
                person = MigrationHelpers.sanitizeRID(rez.getLocalName(), value, person);
                if (!MigrationHelpers.isDisconnected(person))
                    CommonMigration.addAgentAsCreator(null, nodeA.getModel().createResource(BDR+person), value, nodeA);
            }
	    }

        if (nodeList.size() == 0) {
            return oldElements;
        }
        
        List<Element> res = new ArrayList<>(oldElements);
	    
        // then through the actual list
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            String value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "hasMainAuthor";
            }
            if (isRoot && value.equals("hasScribe")) {
                Property prop = m.createProperty(ADM+"outlineAuthorStatement");
                Literal l = CommonMigration.getLiteral(current, "en", m, "hasScribe", rez.getLocalName(), null);
                if (l == null) continue;
                rez.addProperty(prop,  l);
                continue;
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty()) continue;
            if (person.equals("Add to DLMS")) {
                person = current.getTextContent().trim();
                if (!person.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_MISSING, rez.getLocalName(), rez.getLocalName(), "creator", "needs to be added to dlms: `"+value+"`");
            } else {
                if (value.equals("hasMainAuthor") && oldElements != null) {
                    // looking at W00JW501203, it seems that this introduced errors
                    res.add(current);
                }
                person = MigrationHelpers.sanitizeRID(rez.getLocalName(), value, person);
                if (!MigrationHelpers.isDisconnected(person))
                    CommonMigration.addAgentAsCreator(rez, m.createResource(BDR+person), value, nodeA);
            }
        }
        return res;
	}

	static List<String> keywordBlacklist = new ArrayList<>();
	static Pattern blacklistP;
	static {
	    keywordBlacklist.add("dpe skrun gsal bshad");
	    keywordBlacklist.add("sngon gleng");
	    keywordBlacklist.add("rtsom bsgrigs pa'i gtam");
	    keywordBlacklist.add("thor bu");
	    keywordBlacklist.add("spar byang smon tshig");
	    keywordBlacklist.add("mjug byang");
	    keywordBlacklist.add("rtsom pa pos do snang mdzad dgos pa'i gnad don 'ga' zhig");
	    keywordBlacklist.add("dus deb mngags nyo'i gsal brda");
	    keywordBlacklist.add("rtsom pa po'i ngo sprod mdor bsdus");
	    keywordBlacklist.add("bsdu sgrig pa'i gleng brjod");
	    keywordBlacklist.add("bsgrigs rjes kyi gtam");
	    keywordBlacklist.add("spar byang smon tshig");
	    keywordBlacklist.add("preface");
	    //keywordBlacklist.add("gleng gzhi"); means preface but also other things...
	    String patternS = "("+String.join("|", keywordBlacklist)+")";
	    blacklistP = Pattern.compile(patternS);
	}
	
    public static Boolean isText(Element e) {
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "title");
        
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            String title = current.getTextContent();
            if (blacklistP.matcher(title).find())
                return false;
        }
        return true;
    }
	
	public static Boolean isKarchak(Element e) {
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "title");
        
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            if (current.getTextContent().contains("dkar chag"))
                return true;
        }
        return false;
	}

    public static Boolean hasShortTitle(Element e) {
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "title");
        
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            if (StringUtils.countMatches(current.getTextContent().trim(), " ") > 2)
                return false;
        }
        return true;
    }

    public final static Map<String, Map<Integer,Literal>> workVolNames = new HashMap<>();
    
    public static String getMd5(String resId, int nbChars) {
        try {
            String message = resId;
            final byte[] bytesOfMessage = message.getBytes("UTF-8");
            final byte[] hashBytes = md5.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1, hashBytes);
            return String.format("%032X", bigInt).substring(0, nbChars);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static String getPartRID(String outlineRID, String workId, Integer partI) {
        return workId+"_"+getMd5(outlineRID, 6); 
        //return workId+"_"+String.format("%04d", partI);
    }
    
    public static String getPartRIDA(String outlineRID, String workId, Integer partI) {
        return "WA0XL"+getMd5(outlineRID, 12); 
        //return workId+"_"+String.format("%04d", partI);
    }
    
	public static CommonMigration.LocationVolPage addNode(Model m, Resource r, Element e, int i, String workId, CurNodeInt curNode, final CommonMigration.
LocationVolPage previousLocVP, String legacyOutlineRID, int partIndex, String thisPartTreeIndex, Resource rootWork, List<WorkModelInfo> res, List<Element> ancestorCreators) {
	    curNode.i = curNode.i+1;
	    String RID = e.getAttribute("RID").trim();
	    String nodeRID = getPartRID(RID, workId, curNode.i);
	    String ANodeRID = getPartRIDA(RID, workId, curNode.i);
        Resource node = m.createResource(BDR+nodeRID);
        String value = e.getAttribute("type");
        if (value == null || value.isEmpty()) {
            value = "text";
        }
        if (isKarchak(e)) {
            value = "tableOfContent";
        }
        Resource nodeA = null;
        if (("text".equals(value) || "collection".equals(value)) && !hasShortTitle(e) && isText(e)) {
             String otherAbstractRID = CommonMigration.abstractClusters.get(ANodeRID);
             if (otherAbstractRID == null) {
                 Model mA = ModelFactory.createDefaultModel();
                 setPrefixes(mA);
                 res.add(new WorkModelInfo(ANodeRID, mA));
                 nodeA = createRoot(mA, BDR+ANodeRID, BDO+"Work");
                 Resource admMainA = createAdminRoot(nodeA);
                 nodeA.addProperty(mA.createProperty(BDO, "language"), mA.createResource(BDR+"LangBo"));
                 addStatus(mA, admMainA, "released");
                 admMainA.addProperty(mA.getProperty(ADM, "metadataLegal"), mA.createResource(BDA+"LD_BDRC_CC0"));
                 node.addProperty(m.createProperty(BDO, "instanceOf"), nodeA);
                 nodeA.addProperty(mA.createProperty(BDO, "workHasInstance"), node);
             } else {
                 SymetricNormalization.addSymetricProperty(m, "instanceOf", nodeRID, otherAbstractRID, null);
             }
        }
        value = "PartType"+value.substring(0,1).toUpperCase()+value.substring(1);
        m.add(node, m.getProperty(BDO, "partType"), m.getResource(BDR+value));

        node.addProperty(m.createProperty(BDO, "partTreeIndex"), thisPartTreeIndex);
        
        if (!value.isEmpty()) {
            node.addProperty(m.getProperty(BDO, "legacyOutlineNodeRID"), RID);
            if (ridsToConvert.containsKey(RID)) {
                ridsToConvert.put(RID, nodeRID);
            }
        }
        
        m.add(node, RDF.type, m.getResource(BDO+"Instance"));
        m.add(node, m.getProperty(BDO, "partIndex"), m.createTypedLiteral(partIndex, XSDDatatype.XSDinteger));
        
        // what's parent? ignoring
        value = e.getAttribute("parent").trim();
        if (!value.isEmpty())
            System.out.println(workId+" has a node with parent "+value);
//            m.add(r, m.getProperty(BDO, "workPartOf"), m.createResource(BDR+value));
        
        if (addWorkHaspart)
            m.add(r, m.getProperty(BDO, "hasPart"), node);
        if (addWorkPartOf)
            m.add(node, m.getProperty(BDO, "partOf"), r);
        
        m.add(node, m.getProperty(BDO, "inRootInstance"), rootWork);
        
        boolean nameAdded = CommonMigration.addNames(m, e, node, OXSDNS, true, BDO+"partLabel");
        CommonMigration.addDescriptions(m, e, node, OXSDNS, false, nodeA);
        CommonMigration.addTitles(m, node, e, OXSDNS, !nameAdded, true, nodeA);
        
        Statement labelSta = node.getProperty(SKOS.prefLabel);
        String label = null;
        Literal labelL = null;
        if (labelSta != null) {
            label = labelSta.getLiteral().getString();
            labelL = labelSta.getLiteral();
        }
        
        CommonMigration.LocationVolPage locVP =
                CommonMigration.addLocations(m, node, e, OXSDNS, workId, legacyOutlineRID, RID, label);
        if (locVP != null) {
            locVP.RID = RID;
            if (labelL != null) {
                Map<Integer,Literal> volLabels = workVolNames.computeIfAbsent(workId, x -> new HashMap<Integer,Literal>());
                if (volLabels.containsKey(locVP.beginVolNum)) {
                    // es gibt ein problem
                }
                volLabels.put(locVP.beginVolNum, labelL);
            }
        }
        
        // check if outlines cross
        if (locVP != null && previousLocVP != null) {
            if (previousLocVP.endVolNum > locVP.beginVolNum
                    || (previousLocVP.endVolNum == locVP.beginVolNum && previousLocVP.endPageNum > locVP.beginPageNum)) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, legacyOutlineRID, RID, "starts (vol. "+locVP.beginVolNum+", p. "+locVP.beginPageNum+") before the end of previous node ["+
                        previousLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, previousLocVP.RID)+") (vol. "+previousLocVP.endVolNum+", p. "+previousLocVP.endPageNum+")");
            }
        }
        
        CommonMigration.addSubjects(m, (nodeA != null) ? nodeA : node, e, OXSDNS);
        
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "site");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            String type = null;
            value = current.getAttribute("type").trim().toLowerCase();
            Resource eventMain = node;
            String propLocalName = "instanceEvent";
            if (!value.isEmpty())  {
                switch(value) {
                case "started":
                    type = "OriginatedEvent";
                    eventMain = nodeA;
                    propLocalName = "workEvent";
                    break;
                case "completed":
                case "written":
                    type = "CompletedEvent";
                    eventMain = nodeA;
                    propLocalName = "workEvent";
                    break;
                case "edited":
                    type = "EditedEvent";
                    break;
                case "revealed":
                    type = "RevealedEvent";
                    eventMain = nodeA;
                    propLocalName = "workEvent";
                    break;
                case "printedat":
                    type = "PrintedEvent";
                    break;  
                default:
                    System.out.println("unknown site type: "+value);
                }
            }
            if (type == null) {
                type = "WorkEvent";
                eventMain = nodeA;
                propLocalName = "workEvent";
            }

            if (eventMain != null) {
                Model eventM = eventMain.getModel();
                Resource site = getFacetNode(FacetType.EVENT,  eventMain, eventM.createResource(BDO+type));
                eventMain.addProperty(eventM.getProperty(BDO, propLocalName), site);
                CommonMigration.addDates(current.getAttribute("circa"), site, r);
                value = current.getAttribute("place").trim();
                if (!value.isEmpty())
                    eventM.add(site, eventM.getProperty(BDO, "eventWhere"), eventM.getResource(BDR+value));

                // TODO: what about current.getTextContent()?
                value = current.getTextContent();
                if (value != null && !value.isEmpty()) {
                    site.addLiteral(eventM.getProperty(BDO, "eventText"), eventM.createLiteral(value));
                }
            }
        }
        
        ancestorCreators = addCreators(m, node, e, false, rootWork, nodeA, ancestorCreators);
        
        // sub nodes
        boolean hasChildren = addNodes(m, node, e, workId, curNode, locVP, RID, legacyOutlineRID, thisPartTreeIndex, rootWork, res, ancestorCreators);
        
        if (!hasChildren && (locVP == null)) {
//            labelSta = node.getProperty(m.getProperty(CommonMigration.PREFLABEL_URI));
//            label = null;
//            if (labelSta != null)
//                label = labelSta.getLiteral().getString();
            ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, legacyOutlineRID, RID, "`"+label+"` has no page indication");/*, title `"+
                    label+"`");*/
        }
        
        return locVP;
        
	}
	
	static String getPartTreeIndexStr(final int index, final int nbSiblings) {
	    if (nbSiblings < 10)
	        return Integer.toString(index);
	    if (nbSiblings < 100)
	        return String.format("%02d", index);
	    return String.format("%03d", index);
	}
	
	public static boolean addNodes(Model m, Resource r, Element e, String workId, CurNodeInt curNode, CommonMigration.LocationVolPage parentLocVP, String parentRID, String legacyOutlineRID, String curPartTreeIndex, Resource rootWork, List<WorkModelInfo> wmires, List<Element> ancestorCreators) {
	    CommonMigration.LocationVolPage endLocVP = null;
	    boolean res = false;
	    final List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "node");
	    final int nbChildren = nodeList.size();
        for (int i = 0; i < nbChildren; i++) {
            res = true;
            Element current = (Element) nodeList.get(i);
            final String thisPartTreeIndex;
            if (curPartTreeIndex.isEmpty()) {
                thisPartTreeIndex = getPartTreeIndexStr(i+1, nbChildren);
            } else {
                thisPartTreeIndex = curPartTreeIndex+"."+getPartTreeIndexStr(i+1, nbChildren);
            }
            endLocVP = addNode(m, r, current, i, workId, curNode, endLocVP, legacyOutlineRID, i+1, thisPartTreeIndex, rootWork, wmires, ancestorCreators);
            if (i == 0 && parentRID != null && endLocVP != null && parentLocVP != null) {
                // check if beginning of child node is before beginning of parent
                if (parentLocVP.beginVolNum > endLocVP.beginVolNum
                        || (parentLocVP.beginVolNum == endLocVP.beginVolNum && parentLocVP.beginPageNum > endLocVP.beginPageNum)) {
                    ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, legacyOutlineRID, endLocVP.RID, "starts (vol. "+endLocVP.beginVolNum+", p. "+endLocVP.beginPageNum+") before the beginning of parent node ["+
                            parentLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, parentLocVP.RID)+") (vol. "+parentLocVP.endVolNum+", p. "+parentLocVP.endPageNum+")");
                }
            }
        }
        if (parentRID != null && endLocVP != null && parentLocVP != null) {
            // check if beginning of child node is before beginning of parent
            if (parentLocVP.endVolNum < endLocVP.endVolNum
                    || (parentLocVP.endVolNum == endLocVP.endVolNum && parentLocVP.endPageNum < endLocVP.endPageNum)) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, legacyOutlineRID, endLocVP.RID, "ends (vol. "+endLocVP.endVolNum+", p. "+endLocVP.endPageNum+") after the end of parent node ["+
                        parentLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, parentLocVP.RID)+") (vol. "+parentLocVP.endVolNum+", p. "+parentLocVP.endPageNum+")");
            }
        }
        return res;
	}

   public static void addSimpleAttr(String attrValue, String propUrl, Model m, Resource r) {
        attrValue = attrValue.trim();
        if (attrValue.isEmpty()) return;
        Property prop = m.getProperty(propUrl);
        m.add(r, prop, m.createLiteral(attrValue));
    }
	
}
