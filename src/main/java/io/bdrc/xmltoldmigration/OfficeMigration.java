package io.bdrc.xmltoldmigration;

import java.util.ArrayList;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class OfficeMigration {
	
	private static final String OP = CommonMigration.OFFICE_PREFIX;
	private static final String PRXSDNS = "http://www.tbrc.org/models/office#";
	
	public static Model MigrateOffice(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(OP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(OP + "Office"));
		Property prop = m.getProperty(OP, "status");
		m.add(main, prop, root.getAttribute("status"));
		
		CommonMigration.addNotes(m, root, main, PRXSDNS);
		
		CommonMigration.addExternals(m, root, main, PRXSDNS);
		
		CommonMigration.addLog(m, root, main, PRXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, PRXSDNS, true);
		
		
		return m;
	}
	
	
}
