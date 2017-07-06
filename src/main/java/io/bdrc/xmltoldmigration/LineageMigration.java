package io.bdrc.xmltoldmigration;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class LineageMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String LP = CommonMigration.LINEAGE_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String LXSDNS = "http://www.tbrc.org/models/lineage#";
	
	public static Model MigrateLineage(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		String value = getTypeStr(root);
		Resource main = m.createResource(LP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(LP + value));
		if (!value.equals("Lineage")) {
			m.add(main, RDF.type, m.createResource(LP + "Lineage"));
		}
		if (value.equals("NotSpecified")) {
            CommonMigration.addException(m, main, "missing lineage type");
        }
		
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		
    	CommonMigration.addNames(m, root, main, LXSDNS);
    	CommonMigration.addNotes(m, root, main, LXSDNS);
    	CommonMigration.addExternals(m, root, main, LXSDNS);
    	CommonMigration.addDescriptions(m, root, main, LXSDNS);
    	CommonMigration.addLog(m, root, main, LXSDNS);
    	CommonMigration.addLocations(m, main, root, LXSDNS, LP+"location", null, null);
		
        NodeList nodeList = root.getElementsByTagNameNS(LXSDNS, "object");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("RID").trim();
            if (!value.isEmpty()) {
                value =  CommonMigration.getPrefixFromRID(value)+value;
                m.add(main, m.getProperty(LP+"object"), m.getResource(value));
            }
        }
        
        nodeList = root.getElementsByTagNameNS(LXSDNS, "lineageRef");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("RID").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(LP+"ref"), m.getResource(LP+value));
        }
		
        List<Element> elList = CommonMigration.getChildrenByTagName(root, LXSDNS, "holder");
        for (int i = 0; i < elList.size(); i++) {
            Element current = (Element) elList.get(i);
            addHolder(m, main, current, i);
        }
        
        nodeList = root.getElementsByTagNameNS(LXSDNS, "alternative");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            elList = CommonMigration.getChildrenByTagName(current, LXSDNS, "holder");
            if (elList.isEmpty()) continue;
            value = CommonMigration.getSubResourceName(main, LP, "Alternative", i+1);
            Resource alternative = m.createResource(value);
            m.add(alternative, RDF.type, m.getResource(LP+"Alternative"));
            m.add(main, m.getProperty(LP+"altnernative"), alternative);
            for (int j = 0; j < elList.size(); j++) {
                Element holderElement = (Element) elList.get(j);
                addHolder(m, alternative, holderElement, j);
            }
        }
		
		return m;
	}
	
	public static void addHolder(Model m, Resource r, Element e, int i) {
	    String value = CommonMigration.getSubResourceName(r, LP, "Holder", i+1);
	    Resource holder = m.createResource(value);
	    m.add(holder, RDF.type, m.getResource(LP+"Holder"));
	    m.add(r, m.getProperty(LP+"holder"), holder);
	    
       CommonMigration.addNotes(m, e, holder, LXSDNS);
       CommonMigration.addDescriptions(m, e, holder, LXSDNS);
	    
	    NodeList nodeList = e.getElementsByTagNameNS(LXSDNS, "who");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty()) {
                value =  CommonMigration.getPrefixFromRID(value)+value;
                m.add(holder, m.getProperty(LP+"who"), m.getResource(value));
            }
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "downTo");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(LP+"downTo"), m.getResource(LP+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "downFrom");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(LP+"downFrom"), m.getResource(LP+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "work");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(LP+"work"), m.getResource(WP+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "received");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = CommonMigration.getSubResourceName(r, LP, "Received", j+1);
            Resource received = m.createResource(value);
            m.add(received, RDF.type, m.getResource(LP+"Received"));
            m.add(holder, m.getProperty(LP+"received"), received);
            value = current.getAttribute("RID");
            if (!value.isEmpty()) {
                if (value.contains(" ")) {
                    String [] parts = value.split(" ");
                    for (String part: parts) {
                        if (part.startsWith("#")) {
                            CommonMigration.addException(m, r, "received value contains unparsed strings: \""+part+"\"");
                            continue;
                        }
                        part =  CommonMigration.getPrefixFromRID(part)+part;
                        m.add(received, m.getProperty(LP+"from"), m.createResource(part));
                    }
                } else {
                    value =  CommonMigration.getPrefixFromRID(value)+value;
                    m.add(received, m.getProperty(LP+"from"), m.getResource(value));
                }
            }

            value = current.getAttribute("site");
            if (!value.isEmpty())
                m.add(received, m.getProperty(LP+"site"), m.getResource(PLP+value));

            value = current.getAttribute("circa");
            if (!value.isEmpty() && !value.equals("?"))
                m.add(received, m.getProperty(LP+"circa"), m.createLiteral(value));
        }
	}

	public static String getTypeStr(Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(LXSDNS, "info");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value.isEmpty()) {
			    value = "lineageTypes:NotSpecified";
			}
			if (value.equals("lineageTypes:rlung")) value = "lineageTypes:lung";
			value = value.substring(13);
			value = CommonMigration.normalizePropName(value, "Class");
		}
		if (value == null) {
		    value = "NotSpecified";
		}
		return value;
	}
	
}
