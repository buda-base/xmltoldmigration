package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationHelpers;


public class CorporationMigration {

	public static final String CXSDNS = "http://www.tbrc.org/models/corporation#";
	
	public static Model MigrateCorporation(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		MigrationHelpers.setPrefixes(m, "corporation");
		Element root = xmlDocument.getDocumentElement();
		Element current;
        Resource main = createRoot(m, BDR+root.getAttribute("RID"), BDO+"Corporation");
        Resource admMain = createAdminRoot(main);
		addStatus(m, admMain, root.getAttribute("status"));
		admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
		
		CommonMigration.addNames(m, root, main, CXSDNS);
		
		CommonMigration.addNotes(m, root, main, CXSDNS);
		
		CommonMigration.addExternals(m, root, main, CXSDNS);
		
		CommonMigration.addLog(m, root, admMain, CXSDNS, false);
		
		CommonMigration.addDescriptions(m, root, main, CXSDNS);
		
		// members
		
		NodeList nodeList = root.getElementsByTagNameNS(CXSDNS, "member");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = current.getAttribute("person");
			if (value.isEmpty()) continue;
			Resource person = m.createResource(BDR + value);
			value = current.getAttribute("type").trim();
			if (value.isEmpty() || value.equals("unknown")) {
			    value = BDO+"CorporationMember";
			} else {
			    value = CommonMigration.normalizePropName(value, null);
	            value = BDO+"CorporationMember"+value.substring(0, 1).toUpperCase() + value.substring(1);			    
			}
			Property prop = m.getProperty(BDO, "corporationHasMember");
            Resource member = getFacetNode(FacetType.CORP_MEMBER, main, m.createResource(value));
			m.add(main, prop, member);
			m.add(member, m.getProperty(BDO, "corporationMember"), person);
			m.add(member, RDF.type, m.createResource(value));
		}
		
		// regions (ignoring most attributes)
		
		nodeList = root.getElementsByTagNameNS(CXSDNS, "region");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = current.getAttribute("place");
			if (!value.isEmpty()) {
				Resource place = m.createResource(BDR + value);
				Property prop = m.getProperty(BDO+"corporationRegion");
				m.add(main, prop, place);
			}
		}
		
		return m;
	}
	
}
