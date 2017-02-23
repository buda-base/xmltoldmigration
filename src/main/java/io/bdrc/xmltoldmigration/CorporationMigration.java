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


public class CorporationMigration {
	
	private static final String PRP = CommonMigration.CORPORATION_PREFIX;
	private static final String CXSDNS = "http://www.tbrc.org/models/corporation#";
	
	public static Model MigrateCorporation(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(PRP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(PRP + "Corporation"));
		Property prop = m.getProperty(PRP, "status");
		m.add(main, prop, root.getAttribute("status"));
		
		CommonMigration.addNotes(m, root, main, CXSDNS);
		
		CommonMigration.addExternals(m, root, main, CXSDNS);
		
		CommonMigration.addLog(m, root, main, CXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, CXSDNS, true);
		
		return m;
	}
	
}
