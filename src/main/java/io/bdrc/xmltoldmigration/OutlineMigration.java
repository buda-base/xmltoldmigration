package io.bdrc.xmltoldmigration;

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
	private static final String OXSDNS = "http://www.tbrc.org/models/outline#";

	public static Model MigrateOutline(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(OP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(OP + "Topic"));
		Property prop = m.getProperty(OP, "status");
		m.add(main, prop, root.getAttribute("status"));

		CommonMigration.addNames(m, root, main, OXSDNS);

		CommonMigration.addNotes(m, root, main, OXSDNS);

		CommonMigration.addExternals(m, root, main, OXSDNS);

		CommonMigration.addLog(m, root, main, OXSDNS);

		CommonMigration.addDescriptions(m, root, main, OXSDNS);
		
		return m;
	}
	
	
}
