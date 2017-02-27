package io.bdrc.xmltoldmigration;

import java.util.List;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
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
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String LP = CommonMigration.LINEAGE_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String LXSDNS = "http://www.tbrc.org/models/lineage#";
	
	public static Model MigrateLineage(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String value = getTypeStr(root);
		Resource main = m.createResource(LP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(LP + value));
		if (!value.equals("Lineage")) {
			m.add(main, RDF.type, m.createResource(LP + "Lineage"));
		}
		
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		
		CommonMigration.addNames(m, root, main, LXSDNS);
		CommonMigration.addNotes(m, root, main, LXSDNS);
		CommonMigration.addExternals(m, root, main, LXSDNS);
		CommonMigration.addLog(m, root, main, LXSDNS);
		
		// remaining fields: gis, event, isLocatedIn, near, contains, address, tlm, description
		
		return m;
	}

	public static String getTypeStr(Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(LXSDNS, "info");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value == null || value.isEmpty()) {
				System.err.println("No info for Place "+root.getAttribute("RID"));
				return "Place";
			}
			if (!value.startsWith("lineageTypes:")) {
				System.err.println("Invalid Place type '"+value+"' for Lineage "+root.getAttribute("RID"));
				return "Place";
			}
			value = value.substring(13);
			value = CommonMigration.normalizePropName(value, "Class");
			break;
		}
		if (value == "notSpecified") {
			return "Place";
		}
		return value;
	}
	
}
