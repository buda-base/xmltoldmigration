package io.bdrc.xmltoldmigration.xml2files;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class OutlineMigration {

    private static final String BDA = CommonMigration.BDA;
    private static final String BDO = CommonMigration.BDO;
    private static final String BDR = CommonMigration.BDR;
    private static final String ADM = CommonMigration.ADM;
	public static final String OXSDNS = "http://www.tbrc.org/models/outline#";
	
	public static boolean splitOutlines = false;
	
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
	
	public static String getWorkId(Document xmlDocument) {
        Element root = xmlDocument.getDocumentElement();
        NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String workId = current.getAttribute("work").trim();
            if (!workId.isEmpty())
                return workId;
        }
        ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "type", "missing work ID, cannot migrate outline");
        return null;
	}
	
	public static Model MigrateOutline(Document xmlDocument) {
	    Model workModel = ModelFactory.createDefaultModel();
	    CommonMigration.setPrefixes(workModel, "work");
	    String workId = getWorkId(xmlDocument);
	    if (workId == null || workId.isEmpty())
	        return null;
        Resource work = workModel.createResource(BDR + workId);
        work.addProperty(RDF.type, workModel.getResource(BDO+"Work"));
        //CommonMigration.addStatus(workModel, work, root.getAttribute("status"));
	    return MigrateOutline(xmlDocument, workModel, work);
	}

	public static Model MigrateOutline(Document xmlDocument, Model workModel, Resource work) {
		Model m;
		if (splitOutlines) {
		    m = ModelFactory.createDefaultModel();
		    CommonMigration.setPrefixes(m, "work");
		} else {
		    m = workModel;
		}
		
        Resource admOutline = MigrationHelpers.getAdmResource(m, work.getLocalName());

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

        m.add(work, m.getProperty(ADM, "outline"), admOutline);
        value = root.getAttribute("pagination").trim();
        if (value.isEmpty() || value == "relative") {
            value = BDR+"PaginationRelative";
        } else {
            value = BDR+"PaginationAbsolute";            
        }
        m.add(work, m.getProperty(BDO, "workPagination"), m.createResource(value));
        
        // if the outline names must really be migrated, do it here, they would be under
        // the tbr:outlineName property
        
		CommonMigration.addNotes(m, root, admOutline, OXSDNS);
		CommonMigration.addExternals(m, root, admOutline, OXSDNS);
		CommonMigration.addLog(m, root, admOutline, OXSDNS);
		CommonMigration.addDescriptions(m, root, admOutline, OXSDNS);
		CommonMigration.addLocations(m, admOutline, root, OXSDNS, work.getLocalName(), legacyOutlineRID, legacyOutlineRID, null);
		
		addCreators(m, admOutline, root, true);
		
		// case where there's an unnecessary unique top node (ex: W1GS61415 / O1LS4227)
		final List<Element> nodeList2 = CommonMigration.getChildrenByTagName(root, OXSDNS, "node");
        final int nbChildren = nodeList2.size();
        Element node2 = root;
        if (nbChildren == 1) {
            node2 = nodeList2.get(0);
        }
		
		addNodes(m, work, node2, work.getLocalName(), curNodeInt, null, null, legacyOutlineRID, "");
		
		return m;
	}
	
	public static void addCreators(Model m, Resource r, Element e, boolean isRoot) {
        // creator
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "creator");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            String value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "hasMainAuthor";
            }
            if (isRoot && value.equals("hasScribe")) {
                Property prop = m.createProperty(ADM+"outlineAuthorStatement");
                Literal l = CommonMigration.getLiteral(current, "en", m, "hasScribe", r.getLocalName(), null);
                if (l == null) continue;
                r.addProperty(prop,  l);
                continue;
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty()) continue;
            if (person.equals("Add to DLMS")) {
                person = current.getTextContent().trim();
                if (!person.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_MISSING, r.getLocalName(), r.getLocalName(), "creator", "needs to be added to dlms: `"+value+"`");
            } else {
                person = MigrationHelpers.sanitizeRID(r.getLocalName(), value, person);
                if (!MigrationHelpers.isDisconnected(person))
                    CommonMigration.addAgentAsCreator(m, r, m.createResource(BDR+person), value);
            }
        }
	}

	public static CommonMigration.LocationVolPage addNode(Model m, Resource r, Element e, int i, String workId, CurNodeInt curNode, final CommonMigration.
LocationVolPage previousLocVP, String legacyOutlineRID, int partIndex, String thisPartTreeIndex) {
	    curNode.i = curNode.i+1;
	    String value = String.format("%04d", curNode.i);	    
        Resource node = m.createResource(BDR+workId+"_"+value);
        node.addProperty(m.createProperty(BDO, "workPartTreeIndex"), thisPartTreeIndex);
        String RID = e.getAttribute("RID").trim();
        if (!value.isEmpty()) {
            node.addProperty(m.getProperty(BDO, "legacyOutlineNodeRID"), RID);
            if (ridsToConvert.containsKey(RID)) {
                ridsToConvert.put(RID, workId+"_"+value);
            }
        }
        value = e.getAttribute("type");
        if (value.isEmpty()) {
            value = "Node";// TODO: ?
        }
        value = "OutlineType"+value.substring(0, 1).toUpperCase() + value.substring(1);
        m.add(node, RDF.type, m.getResource(BDO+"Work"));
        m.add(node, m.getProperty(BDO, "workPartIndex"), m.createTypedLiteral(partIndex, XSDDatatype.XSDinteger));
        // there should be no such monstruosity
        //m.add(node, m.getProperty(ADM, "outlineType"), m.getResource(BDR+value));
        
        // what's parent? ignoring
//        value = e.getAttribute("parent").trim();
//        if (!value.isEmpty())
//            m.add(r, m.getProperty(BDO, "workPartOf"), m.createResource(BDR+value));
        
        if (addWorkHaspart)
            m.add(r, m.getProperty(BDO, "workHasPart"), node);
        if (addWorkPartOf)
            m.add(node, m.getProperty(BDO, "workPartOf"), r);
        
        boolean nameAdded = CommonMigration.addNames(m, e, node, OXSDNS, true, BDO+"workPartLabel");
        CommonMigration.addDescriptions(m, e, node, OXSDNS);
        CommonMigration.addTitles(m, node, e, OXSDNS, !nameAdded, true);
        
        Statement labelSta = node.getProperty(m.getProperty(CommonMigration.PREFLABEL_URI));
        String label = null;
        if (labelSta != null)
            label = labelSta.getLiteral().getString();
        
        CommonMigration.LocationVolPage locVP =
                CommonMigration.addLocations(m, node, e, OXSDNS, workId, legacyOutlineRID, RID, label);
        if (locVP != null) locVP.RID = RID;
        
        // check if outlines cross
        if (locVP != null && previousLocVP != null) {
            if (previousLocVP.endVolNum > locVP.beginVolNum
                    || (previousLocVP.endVolNum == locVP.beginVolNum && previousLocVP.endPageNum > locVP.beginPageNum)) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, legacyOutlineRID, RID, "starts (vol. "+locVP.beginVolNum+", p. "+locVP.beginPageNum+") before the end of previous node ["+
                        previousLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, previousLocVP.RID)+") (vol. "+previousLocVP.endVolNum+", p. "+previousLocVP.endPageNum+")");
            }
        }
        
        CommonMigration.addSubjects(m, node, e, OXSDNS);
        
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "site");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            //value = CommonMigration.getSubResourceName(node, OP, "Site");
            Resource site = m.createResource();
            value = current.getAttribute("type").trim().toLowerCase();
            if (!value.isEmpty())  {
                String type = null;
                switch(value) {
                case "started":
                    type = "OriginatedEvent";
                    break;
                case "completed":
                case "written":
                    type = "CompletedEvent";
                    break;
                case "edited":
                    type = "EditedEvent";
                    break;
                case "revealed":
                    type = "RevealedEvent";
                    break;
                case "printedat":
                    type = "PrintedEvent";
                    break;  
                default:
                    System.out.println("unknown site type: "+value);
                }
                if (type != null)
                    m.add(site, RDF.type, m.getResource(BDO+type));
            }
            m.add(node, m.getProperty(BDO, "workEvent"), site);
            
            CommonMigration.addDates(current.getAttribute("circa"), site, r);
            
            value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(site, m.getProperty(BDO, "eventWhere"), m.getResource(BDR+value));

            // TODO: what about current.getTextContent()?
        }
        
        addCreators(m, node, e, false);
        
        // sub nodes
        boolean hasChildren = addNodes(m, node, e, workId, curNode, locVP, RID, legacyOutlineRID, thisPartTreeIndex);
        
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
	
	public static boolean addNodes(Model m, Resource r, Element e, String workId, CurNodeInt curNode, CommonMigration.LocationVolPage parentLocVP, String parentRID, String legacyOutlineRID, String curPartTreeIndex) {
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
            endLocVP = addNode(m, r, current, i, workId, curNode, endLocVP, legacyOutlineRID, i+1, thisPartTreeIndex);
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
