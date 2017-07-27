package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class CorporationMigration {

	private static final String CXSDNS = "http://www.tbrc.org/models/corporation#";
	
	public static Model MigrateCorporation(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(CommonMigration.BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(CommonMigration.BDO + "Corporation"));
		CommonMigration.addStatus(m, main, root.getAttribute("status"));
		
		CommonMigration.addNames(m, root, main, CXSDNS);
		
		CommonMigration.addNotes(m, root, main, CXSDNS);
		
		CommonMigration.addExternals(m, root, main, CXSDNS);
		
		CommonMigration.addLog(m, root, main, CXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, CXSDNS);
		
		// members
		
		NodeList nodeList = root.getElementsByTagNameNS(CXSDNS, "member");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			Resource member = m.createResource();
			value = current.getAttribute("person");
			if (value.isEmpty()) continue;
			Resource person = m.createResource(CommonMigration.BDR + value);
			value = current.getAttribute("type").trim();
			if (value.isEmpty()) {
				value = "notSpecified";
			}
			value = CommonMigration.normalizePropName(value, null);
			value = CommonMigration.BDR+"CorporationMemberType"+value.substring(0, 1).toUpperCase() + value.substring(1);
			Property prop = m.getProperty(CommonMigration.BDO, "corporationHasMember");
			m.add(main, prop, member);
			m.add(member, m.getProperty(CommonMigration.BDO, "corporationMember"), person);
			m.add(member, m.getProperty(CommonMigration.BDO, "corporationMemberType"), 
			        m.createResource(value));
		}
		
		// regions (ignoring most attributes)
		
		nodeList = root.getElementsByTagNameNS(CXSDNS, "region");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = current.getAttribute("place");
			if (!value.isEmpty()) {
				Resource place = m.createResource(CommonMigration.BDR + value);
				Property prop = m.getProperty(CommonMigration.BDO+"corporationRegion");
				m.add(main, prop, place);
			}
		}
		
		return m;
	}
	
}
