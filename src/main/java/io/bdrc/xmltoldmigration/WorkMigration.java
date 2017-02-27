package io.bdrc.xmltoldmigration;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class WorkMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String PRP = CommonMigration.PRODUCT_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String VP = CommonMigration.VOLUMES_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
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
		String lang = null;
		String value = null;
		Literal lit = null;
		
		CommonMigration.addNotes(m, root, main, WXSDNS);
	    CommonMigration.addExternals(m, root, main, WXSDNS);
	    CommonMigration.addLog(m, root, main, WXSDNS);
		
		// titles
		
		NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "title");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value.isEmpty()) {
			    value = "bibliographicalTitle";
			}
			prop = m.getProperty(PP, value);
	        lang = CommonMigration.getBCP47(current, "bo-x-ewts");
            lit = m.createLiteral(current.getTextContent().trim(), lang);
			m.add(main, prop, lit);
			if (i == 0) {
				CommonMigration.addLabel(m, main, lit);
			}
		}
		
		// archiveInfo
		
		nodeList = root.getElementsByTagNameNS(WXSDNS, "archiveInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("license");
            if (value.isEmpty()) {
                value = "ccby"; // ?
            }
            prop = m.getProperty(WP, value);
            value = current.getAttribute("access");
            if (value.isEmpty()) {
                value = "temporarilyRestricted"; // ?
            }
            lit = m.createLiteral(value);
            m.add(main, prop, lit);
            
            
            value = current.getAttribute("status");
            if (value.isEmpty()) {
                value = "scanned"; // from xsd
            }
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
            value = current.getAttribute("person"); // required
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
            lang = CommonMigration.getBCP47(current, "en");
            value = current.getTextContent();
            m.add(main, m.getProperty(WP+"catalogInfo"), m.createLiteral(value, lang));
        }
        
        // scanInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "scanInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getTextContent();
            m.add(main, m.getProperty(WP+"scanInfo"), m.createLiteral(value, "en"));
        }
        
        // subject
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "subject");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("type");
            if (value.isEmpty()) {
                value = "subjectObjectProperty"; // TODO?
            }
            prop = m.getProperty(RP, "subject_"+value);
            value = current.getAttribute("class").trim();
            m.add(main, prop, m.createResource(TP+value));
        }
        
        // TODO: volumeMap, hasPubInfo
        
		return m;
		
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
