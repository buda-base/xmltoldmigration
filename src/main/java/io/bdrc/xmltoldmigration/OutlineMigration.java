package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class OutlineMigration {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    private static final String TBR = CommonMigration.TBR_PREFIX;
	private static final String OXSDNS = "http://www.tbrc.org/models/outline#";
	
	public static boolean splitOutlines = false;

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
	    CommonMigration.setPrefixes(workModel);
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
		    CommonMigration.setPrefixes(m);
		} else {
		    m = workModel;
		}
		
		Resource workInOutlineModel = m.getResource(work.getURI()); 

		Element root = xmlDocument.getDocumentElement();

		CurNodeInt curNodeInt = new CurNodeInt();
		curNodeInt.i = 0;
		Resource mainOutline = null;
		String value;

        mainOutline = m.createResource();
        mainOutline.addProperty(m.getProperty(ADM, "workLegacyNode"), root.getAttribute("RID"));
        mainOutline.addProperty(RDF.type, m.createResource(ADM+"Outline"));
        workInOutlineModel.addProperty(m.getProperty(ADM, "outline"), mainOutline);
        NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "NoType";
            }
            value = BDR+"OutlineType"+value.substring(0, 1).toUpperCase() + value.substring(1);
            m.add(mainOutline, m.getProperty(ADM, "outlineType"), m.createResource(value));
        }

        value = root.getAttribute("pagination").trim();
        if (value.isEmpty() || value == "relative") {
            value = BDR+"PaginationRelative";
        } else {
            value = BDR+"PaginationAbsolute";            
        }
        m.add(work, m.getProperty(BDO, "workPagination"), m.createResource(value));
        
        // if the outline names must really be migrated, do it here, they would be under
        // the tbr:outlineName property
        
		CommonMigration.addNotes(m, root, mainOutline, OXSDNS);
		CommonMigration.addExternals(m, root, mainOutline, OXSDNS);
		CommonMigration.addLog(m, root, mainOutline, OXSDNS);
		CommonMigration.addDescriptions(m, root, mainOutline, OXSDNS);
		CommonMigration.addLocations(m, mainOutline, root, OXSDNS, work.getLocalName());
		
		addCreators(m, mainOutline, root);
		
		addNodes(m, work, root, work.getLocalName(), curNodeInt, null, null);
		
		return m;
	}
	
	public static void addCreators(Model m, Resource r, Element e) {
        // creator
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "creator");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            Property prop = m.createProperty(ADM+"outlineAuthorStatement");
            Literal l = CommonMigration.getLiteral(current, "en", m, "catalogInfo", r.getLocalName(), null);
            if (l == null) continue;
            r.addProperty(prop,  l);
        }
	}
	
	public static CommonMigration.LocationVolPage addNode(Model m, Resource r, Element e, int i, String workId, CurNodeInt curNode, CommonMigration.
LocationVolPage previousLocVP) {
	    curNode.i = curNode.i+1;
	    String value = String.format("%04d", curNode.i);	    
        Resource node = m.createResource(BDR+workId+"_"+value);
        String RID = e.getAttribute("RID").trim();
        if (!value.isEmpty()) {
            node.addProperty(m.getProperty(ADM, "workLegacyNode"), RID);
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
        // there should be no such monstruosity
        //m.add(node, m.getProperty(ADM, "outlineType"), m.getResource(BDR+value));
        
        // what's parent? ignoring
//        value = e.getAttribute("parent").trim();
//        if (!value.isEmpty())
//            m.add(r, m.getProperty(BDO, "workPartOf"), m.createResource(BDR+value));
        
        m.add(r, m.getProperty(BDO, "workHasPart"), node);
        
        boolean nameAdded = CommonMigration.addNames(m, e, node, OXSDNS);
        CommonMigration.addDescriptions(m, e, node, OXSDNS);
        CommonMigration.addTitles(m, node, e, OXSDNS, !nameAdded);
        
        CommonMigration.LocationVolPage locVP =
                CommonMigration.addLocations(m, node, e, OXSDNS, workId);
        if (locVP != null) locVP.RID = RID;
        
        // check if outlines cross
        if (locVP != null && previousLocVP != null) {
            if (previousLocVP.endVolNum > locVP.beginVolNum
                    || (previousLocVP.endVolNum == locVP.beginVolNum && previousLocVP.endPageNum > locVP.beginPageNum)) {
                ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, RID, "starts (vol. "+locVP.beginVolNum+", p. "+locVP.beginPageNum+") before the end of previous node ["+
                        previousLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, previousLocVP.RID)+") (vol. "+previousLocVP.endVolNum+", p. "+previousLocVP.endPageNum+")");
            }
        }
        
        CommonMigration.addSubjects(m, node, e, OXSDNS);
        
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "site");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            //value = CommonMigration.getSubResourceName(node, OP, "Site");
            Resource site = m.createResource();
            value = e.getAttribute("type");
            if (value.isEmpty()) 
                value = "Site";// TODO: ?
            else {
                value = "WorkSiteType" + value.substring(0, 1).toUpperCase() + value.substring(1);
                m.add(site, m.getProperty(BDO, "workSiteType"), m.getResource(BDR+value));
            }
            m.add(node, m.getProperty(BDO, "workHasSite"), site);
            
            addSimpleAttr(current.getAttribute("circa"), BDO+"onOrAbout", m, site);
            
            value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(site, m.getProperty(BDO, "workSitePlace"), m.getResource(BDR+value));

            // TODO: what about current.getTextContent()?
        }
        
        addCreators(m, node, e);
        
        // sub nodes
        addNodes(m, node, e, workId, curNode, locVP, RID);
        
        return locVP;
        
	}
	

	
	public static void addNodes(Model m, Resource r, Element e, String workId, CurNodeInt curNode, CommonMigration.LocationVolPage parentLocVP, String parentRID) {
	    CommonMigration.LocationVolPage endLocVP = null;
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "node");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            endLocVP = addNode(m, r, current, i, workId, curNode, endLocVP);
            if (i == 0 && parentRID != null && endLocVP != null && parentLocVP != null) {
                // check if beginning of child node is before beginning of parent
                if (parentLocVP.beginVolNum > endLocVP.beginVolNum
                        || (parentLocVP.beginVolNum == endLocVP.beginVolNum && parentLocVP.beginPageNum > endLocVP.beginPageNum)) {
                    ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, endLocVP.RID, "starts (vol. "+endLocVP.beginVolNum+", p. "+endLocVP.beginPageNum+") before the beginning of parent node ["+
                            parentLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, parentLocVP.RID)+") (vol. "+parentLocVP.endVolNum+", p. "+parentLocVP.endPageNum+")");
                }
            }
        }
        if (parentRID != null && endLocVP != null && parentLocVP != null) {
            // check if beginning of child node is before beginning of parent
            if (parentLocVP.endVolNum < endLocVP.endVolNum
                    || (parentLocVP.endVolNum == endLocVP.endVolNum && parentLocVP.endPageNum < endLocVP.endPageNum)) {
                ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, endLocVP.RID, "ends (vol. "+endLocVP.endVolNum+", p. "+endLocVP.endPageNum+") after the end of parent node ["+
                        parentLocVP.RID+"]("+ExceptionHelper.getUri(ExceptionHelper.ET_OUTLINE, workId, parentLocVP.RID)+") (vol. "+parentLocVP.endVolNum+", p. "+parentLocVP.endPageNum+")");
            }
        }
	}

   public static void addSimpleAttr(String attrValue, String propUrl, Model m, Resource r) {
        attrValue = attrValue.trim();
        if (attrValue.isEmpty()) return;
        Property prop = m.getProperty(propUrl);
        m.add(r, prop, m.createLiteral(attrValue));
    }
	
}
