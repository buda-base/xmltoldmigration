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


public class PlaceMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String PLXSDNS = "http://www.tbrc.org/models/place#";
	
	public static Model MigratePlace(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(PLP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(PLP + "Place"));
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		CommonMigration.addNames(m, root, main, PLXSDNS);
		return m;
	}

}
