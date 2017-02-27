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


public class OutlineMigration {

	private static final String OP = CommonMigration.OUTLINE_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PLACE_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String OXSDNS = "http://www.tbrc.org/models/outline#";

	public static Model MigrateOutline(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Resource main = m.createResource(OP + root.getAttribute("RID"));
		Property prop = m.getProperty(OP, "status");
		m.add(main, prop, root.getAttribute("status"));

		// fetch type in isOutlineOf
		NodeList nodeList = root.getElementsByTagNameNS(OXSDNS, "isOutlineOf");
        String value = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            
            value = current.getAttribute("type").trim();
            value = CommonMigration.normalizePropName(value, null);
            if (value.isEmpty()) {
                value = "Outline";//?
            }
            m.add(main, RDF.type, m.createResource(OP + value));
            if (!value.equals("Outline"))
                m.add(main, RDF.type, m.createResource(OP + "Outline"));
            
            value = current.getAttribute("work").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(OP+"isOutlineOf"), m.createProperty(WP+value));
        }
        
        value = root.getAttribute("webAccess").trim();
        if (!value.isEmpty())
            m.add(main, m.getProperty(OP+"webAccess"), m.createLiteral(value));
        
        value = root.getAttribute("pagination").trim();
        if (value.isEmpty()) value = "relative";
        m.add(main, m.getProperty(OP+"pagination"), m.createLiteral(value));
        
		CommonMigration.addNames(m, root, main, OXSDNS);

		CommonMigration.addNotes(m, root, main, OXSDNS);

		CommonMigration.addExternals(m, root, main, OXSDNS);

		CommonMigration.addLog(m, root, main, OXSDNS);

		CommonMigration.addDescriptions(m, root, main, OXSDNS);
		
		// TODO: creator
		
		CommonMigration.addLocations(m, main, root, OXSDNS, OP+"location");
		
		// TODO: parent (unused?)
		
		addViewIn(m, main, root);
		
		addNodes(m, main, root);
		
		return m;
	}
	
	public static void addNode(Model m, Resource r, Element e, int i) {
	    String value = e.getAttribute("RID");
	    if (value.isEmpty())
	        value = CommonMigration.getSubResourceName(r, OP, "Node", i+1);
	    else
	        value = OP+value;
        Resource node = m.createResource(value);
        value = e.getAttribute("type");
        if (value.isEmpty()) {
            value = "Node";// TODO: ?
        }
        value = CommonMigration.normalizePropName(value, "Class");
        m.add(node, RDF.type, m.getResource(OP+value));
        m.add(r, m.getProperty(OP+"hasNode"), node);
        
        addSimpleAttr(e.getAttribute("webAccess"), "webAccess", m, r);
        
        value = e.getAttribute("class").trim();
        if (!value.isEmpty())
            m.add(r, m.getProperty(OP+"node_class"), m.createResource(TP+value));
        
        value = e.getAttribute("parent").trim();
        if (!value.isEmpty())
            m.add(r, m.getProperty(OP+"node_parent"), m.createResource(OP+value));
        
        m.add(r, m.getProperty(OP+"hasNode"), node);
        
        CommonMigration.addNames(m, e, node, OXSDNS, false);
        CommonMigration.addDescriptions(m, e, node, OXSDNS);
        CommonMigration.addTitles(m, node, e, OXSDNS, false);
        
        CommonMigration.addLocations(m, node, e, OXSDNS, OP+"location");
        CommonMigration.addSubjects(m, node, e, OXSDNS);
        
        addViewIn(m, node, e);
        
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "browser");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            addSimpleAttr(current.getAttribute("class"), "browser_class", m, node);
            addSimpleAttr(current.getAttribute("func"),  "browser_func", m, node);
            addSimpleAttr(current.getAttribute("module"), "browser_module", m, node);
            addSimpleAttr(current.getAttribute("params"), "browser_params", m, node);
    
            // TODO: what about current.getTextContent()?
        }
        
         nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "site");
        for (int j = 0; j < nodeList.size(); j++) {
            Element current = (Element) nodeList.get(j);
            
            value = CommonMigration.getSubResourceName(node, OP, "Site");
            Resource site = m.createResource(value);
            value = e.getAttribute("type");
            if (value.isEmpty()) 
                value = "Site";// TODO: ?
            else
                value = "Site" + value.substring(0, 1).toUpperCase() + value.substring(1);
            value = CommonMigration.normalizePropName(value, "Class");
            m.add(site, RDF.type, m.getResource(OP+value));
            m.add(node, m.getProperty(OP+"hasSite"), site);
            
            addSimpleAttr(current.getAttribute("circa"), "site_circa", m, site);
            
            value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(site, m.getProperty(OP+"site_place"), m.getResource(PP+value));

            // TODO: what about current.getTextContent()?
        }
        
        // sub nodes
        addNodes(m, node, e);
	}
	

	
	public static void addNodes(Model m, Resource r, Element e) {
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "node");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            addNode(m, r, current, i);
        }
	}
	
	public static void addViewIn(Model m, Resource r, Element e) {
	    List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "viewIn");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            
            addSimpleAttr(current.getAttribute("label"), "viewIn_label", m, r);
            addSimpleAttr(current.getAttribute("list"), "viewIn_list", m, r);
            addSimpleAttr(current.getAttribute("target"), "viewIn_target", m, r);
            
            // TODO: what about e.getTextContent()?
        }
	}

   public static void addSimpleAttr(String attrValue, String propName, Model m, Resource r) {
        attrValue = attrValue.trim();
        if (attrValue.isEmpty()) return;
        Property prop = m.getProperty(OP+propName);
        m.add(r, prop, m.createLiteral(attrValue));
    }
	
}
