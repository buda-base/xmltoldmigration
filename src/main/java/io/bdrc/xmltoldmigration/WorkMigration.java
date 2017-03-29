package io.bdrc.xmltoldmigration;


import java.util.HashMap;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class WorkMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String PRP = CommonMigration.PRODUCT_PREFIX;
	private static final String VP = CommonMigration.VOLUMES_PREFIX;
	private static final String WXSDNS = "http://www.tbrc.org/models/work#";
	
	public static Model MigrateWork(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(WP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(WP + "Work"));
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		String value = null;
		Literal lit = null;
		
		CommonMigration.addNotes(m, root, main, WXSDNS);
	    CommonMigration.addExternals(m, root, main, WXSDNS);
	    CommonMigration.addLog(m, root, main, WXSDNS);
		
	    CommonMigration.addTitles(m, main, root, WXSDNS, true);
	    CommonMigration.addSubjects(m, main, root, WXSDNS);
		
		// archiveInfo
		
		NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "archiveInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("license").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(WP+"license"), m.createLiteral(value));
            
            value = current.getAttribute("access").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(WP+"access"), m.createLiteral(value));
            
            value = current.getAttribute("status");
            if (value.isEmpty()) value = "scanned"; // from xsd
            prop = m.getProperty(WP, "archiveInfo_status");
            m.add(main, prop, m.createLiteral(value));
            
            try {
                int nbvols = Integer.parseUnsignedInt(current.getAttribute("vols"));
                if (nbvols != 0) {
                    prop = m.getProperty(WP, "archiveInfo_vols");
                    lit = m.createTypedLiteral(nbvols, XSDDatatype.XSDpositiveInteger);
                    m.add(main, prop, lit);
                }
            } catch (NumberFormatException e) {}
        }

        // info
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            addSimpleAttr(current.getAttribute("nodeType"), "info_nodeType", m, main, "publishedWork");
            addSimpleAttr(current.getAttribute("number"), "info_number", m, main, null);
            addSimpleAttr(current.getAttribute("edition"), "info_edition", m, main, null);
            addSimpleAttr(current.getAttribute("seriesType"), "info_seriesType", m, main, null);
            value = current.getAttribute("numbered");
            if (!value.isEmpty()) {
                prop = m.getProperty(WP+"info_numbered");
                m.add(main, prop, m.createTypedLiteral(value, XSDDatatype.XSDboolean));
            }
            value = current.getAttribute("parent");
            if (!value.isEmpty()) {
                prop = m.getProperty(WP+"info_parent");
                m.add(main, prop, m.createResource(WP+value));
            }
        }
        
        // creator
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "creator");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("type");
            if (value.isEmpty()) {
                value = "hasMainAuthor";
            }
            prop = m.createProperty(WP+value);
            value = current.getAttribute("person").trim(); // required
            if (value.isEmpty()) continue;
            if (value.equals("Add to DLMS")) {
                value = current.getTextContent().trim();
                if (!value.isEmpty())
                    CommonMigration.addException(m, main,  "creator needs to be added to dlms: \""+value+"\"");
            } else 
                m.add(main, prop, m.createResource(PP+value));
        }
        
        // inProduct
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "inProduct");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("pid");
            m.add(main, m.getProperty(WP+"inProduct"), m.createResource(PRP+value));
        }
        
        // catalogInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "catalogInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            prop = m.getProperty(WP+"catalogInfo");
            CommonMigration.addCurrentString(current, "en", m, main, prop, false);
        }
        
        // scanInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "scanInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getTextContent();
            m.add(main, m.getProperty(WP+"scanInfo"), m.createLiteral(value, "en"));
        }
        
        Map<String,String> imageGroupList = getImageGroupList(xmlDocument);
        if (!imageGroupList.isEmpty()) {
            Resource volumes = m.createResource(VP+"V"+root.getAttribute("RID").substring(1));
            m.add(main, m.getProperty(WP+"hasVolumes"), volumes);
        }
        
		return m;
		
	}
	
	public static Map<String,String> getImageGroupList(Document d) {
	    Map<String,String> res = new HashMap<String,String>(); 
	    
	    Element root = d.getDocumentElement();
        NodeList volumes = root.getElementsByTagNameNS(WXSDNS, "volume");
        for (int j = 0; j < volumes.getLength(); j++) {
            Element volume = (Element) volumes.item(j);
            String value = volume.getAttribute("imagegroup").trim();
            if (value.isEmpty()) continue;
            if (!value.startsWith("I")) {
                System.err.println("Image group doesn't start with I! "+value);
                continue;
            }
            res.put(value, volume.getAttribute("num").trim());
        }
	    return res;
	}
	
	   public static void addSimpleAttr(String attrValue, String attrName, Model m, Resource r, String dflt) {
	        if (attrValue.isEmpty()) {
	            if (dflt == null) {
	                return;
	            } else {
	                attrValue = dflt;
	            }
	        }
	        Property prop = m.getProperty(WP+attrName);
	        m.add(r, prop, m.createLiteral(attrValue));
	    }
	
}
