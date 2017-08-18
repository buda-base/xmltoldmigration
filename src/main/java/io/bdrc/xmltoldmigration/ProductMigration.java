package io.bdrc.xmltoldmigration;

import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ProductMigration {
	
    // TODO: move to admin: namespace?
    
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
	private static final String PRXSDNS = "http://www.tbrc.org/models/product#";
	
	public static Model MigrateProduct(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "product");
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(ADM + "Product"));
		
		CommonMigration.addStatus(m, main, root.getAttribute("status"));
		
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
				Resource included = m.createResource(BDR + value);
				m.add(main, m.getProperty(ADM+"productInclude"), included);
			}
			addAllows(m, main, current);
			addOrgs(m, main, current);
		}
		
		return m;
	}
	
	public static void addOrgs(Model m, Resource r, Element e) {
		List<Element> nodeList = CommonMigration.getChildrenByTagName(e, PRXSDNS, "org");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addOrg(m, r, current, i);
		}
	}
	
	public static void addOrg(Model m, Resource r, Element orgElement, int i) {
		Resource org = m.createResource();
		//m.add(org, RDF.type, m.getResource(PRP+"ProductOrg"));
		String value = CommonMigration.normalizeString(orgElement.getAttribute("name"));
		if (!value.isEmpty()) {
			m.add(org, m.getProperty(CommonMigration.GENLABEL_URI), m.createLiteral(value, "en"));
		}
		m.add(r, m.getProperty(ADM+"productHasOrg"), org);
		addAllows(m, org, orgElement);
		// sub orgs
		addOrgs(m, org, orgElement);
	}
	
	public static void addAllows(Model m, Resource r, Element e) {
		List<Element> nodeList = CommonMigration.getChildrenByTagName(e, PRXSDNS, "allow");
		for (int j = 0; j < nodeList.size(); j++) {
			Element current = nodeList.get(j);
			String value = current.getTextContent().trim();
			m.add(r, m.getProperty(ADM+"productAllowByAddr"), m.createLiteral(value));
		}
	}
	
}
