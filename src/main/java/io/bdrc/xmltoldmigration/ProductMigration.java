package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ProductMigration {
	
	private static final String PRP = CommonMigration.PRODUCT_PREFIX;
	private static final String PRXSDNS = "http://www.tbrc.org/models/product#";
	
	public static Model MigrateProduct(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(PRP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(PRP + "Product"));
		Property prop = m.getProperty(PRP, "status");
		m.add(main, prop, root.getAttribute("status"));
		
		CommonMigration.addNotes(m, root, main, PRXSDNS);
		
		CommonMigration.addExternals(m, root, main, PRXSDNS);
		
		CommonMigration.addLog(m, root, main, PRXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, PRXSDNS, true);
		
		// access (contains everything
		NodeList nodeList = root.getElementsByTagNameNS(PRXSDNS, "access");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			NodeList subNodeList = current.getElementsByTagNameNS(PRXSDNS, "include");
			for (int j = 0; j < subNodeList.getLength(); j++) {
				Element subCurrent = (Element) subNodeList.item(j);
				String value = subCurrent.getAttribute("RID");
				Resource included = m.createResource(PRP + root.getAttribute("RID"));
				prop = m.getProperty(PRP+"include");
				m.add(main, prop, included);
			}
			addOrgs(m, main, current);
		}
		
		return m;
	}
	
	public static void addOrgs(Model m, Resource r, Element e) {
		NodeList nodeList = e.getElementsByTagNameNS(PRXSDNS, "org");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			addOrg(m, r, current, i);
		}
	}
	
	public static void addOrg(Model m, Resource r, Element orgElement, int i) {
		String value = CommonMigration.getSubResourceName(r, PRP, "Org", i+1);
		Resource org = m.createResource(value);
		m.add(org, RDF.type, m.getResource(PRP+"Org"));
		value = orgElement.getAttribute("name");
		if (!value.isEmpty()) {
			m.add(org, RDFS.label, m.createLiteral(value, "en"));
		}
		m.add(r, m.getProperty(PRP+"hasOrg"), org);
		// allow
		NodeList nodeList = orgElement.getElementsByTagNameNS(PRXSDNS, "allow");
		for (int j = 0; j < nodeList.getLength(); j++) {
			Element current = (Element) nodeList.item(j);
			value = current.getTextContent().trim();
			m.add(org, m.getProperty(PRP+"allow"), m.createLiteral(value));
		}
		// sub orgs
		addOrgs(m, org, orgElement);
	}
	
}
