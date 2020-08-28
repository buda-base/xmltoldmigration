package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.getFacetNode;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationHelpers;


public class ProductMigration {
    
	public static final String PRXSDNS = "http://www.tbrc.org/models/product#";
	
    // Products are migrated as pure admindata so notes, descriptions etc are all
    // "about" admindata
	public static Model MigrateProduct(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		MigrationHelpers.setPrefixes(m, "product");
		Element root = xmlDocument.getDocumentElement();
		Resource main = m.createResource(BDA+root.getAttribute("RID"));
		main.addProperty(RDF.type, m.createResource(ADM + "Product"));
		Resource admMain = createAdminRoot(main);
		if (MigrationHelpers.ricrid.containsKey(root.getAttribute("RID"))) {
		    admMain.addLiteral(admMain.getModel().createProperty(ADM, "isRestrictedInChina"), true);
		}

		addStatus(m, admMain, root.getAttribute("status"));

		CommonMigration.addNotes(m, root, admMain, PRXSDNS);

		CommonMigration.addExternals(m, root, admMain, PRXSDNS);
		
		CommonMigration.addLog(m, root, admMain, PRXSDNS, false);
		
		CommonMigration.addDescriptions(m, root, main, PRXSDNS, true);
		
		// access (contains everything
        Element current;
		NodeList nodeList = root.getElementsByTagNameNS(PRXSDNS, "access");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			NodeList subNodeList = current.getElementsByTagNameNS(PRXSDNS, "include");
			for (int j = 0; j < subNodeList.getLength(); j++) {
				Element subCurrent = (Element) subNodeList.item(j);
				String value = subCurrent.getAttribute("RID");
				Resource included = m.createResource(BDA + value);
				m.add(admMain, m.getProperty(ADM, "productInclude"), included);
			}
			addAllows(m, admMain, current);
			addOrgs(m, admMain, current);
		}

		List<String> workRIDs = WorkMigration.productWorks.get(main.getLocalName());
		if (workRIDs != null) {
		    for (final String workRID : workRIDs) {
		        m.add(main, m.createProperty(ADM, "productHasDigitalInstance"), m.createProperty(BDR+workRID));
		    }
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
	
	public static void addOrg(Model m, Resource rez, Element orgElement, int i) {
	    Resource org = getFacetNode(FacetType.PRODUCT_ORG, BDA, rez);
		String value = CommonMigration.normalizeString(orgElement.getAttribute("name"));
		if (!value.isEmpty()) {
			m.add(org, RDFS.label, m.createLiteral(value, "en"));
		}
		m.add(rez, m.getProperty(ADM+"productHasOrg"), org);
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
