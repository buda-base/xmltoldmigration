package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;


public class PersonMigration {

	public static Model MigratePerson(Document xmlDocument) {
		if (!CommonMigration.documentValidAgainstXSD(xmlDocument, "person")) {
			return null;
		}
		Model m = ModelFactory.createDefaultModel();
		Resource main = m.createResource(CommonMigration.PERSON_PREFIX + "P1331");
		m.add(main, RDF.type, CommonMigration.PERSON_PREFIX + "Person");
		return m;
	}

}
