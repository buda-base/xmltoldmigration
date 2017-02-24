package io.bdrc.xmltoldmigration;

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


public class WorkMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
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
		
		//CommonMigration.addNotes(m, root, main, WXSDNS);
	    //CommonMigration.addExternals(m, root, main, WXSDNS);
	    //CommonMigration.addLog(m, root, main, WXSDNS);
		
		// titles
		
		NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "title");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value.isEmpty()) {
			    value = "title"; // ?
			}
			prop = m.getProperty(PP, value);
	        lang = CommonMigration.getBCP47(current, "bo-x-ewts");
            lit = m.createLiteral(current.getTextContent().trim(), lang);
			m.add(main, prop, lit);
			if (i == 0) {
				CommonMigration.addLabel(m, main, lit);
			}
		}

		return m;
		
	}

}
