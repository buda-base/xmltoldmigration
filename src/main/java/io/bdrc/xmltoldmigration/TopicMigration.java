package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TopicMigration {

	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String TXSDNS = "http://www.tbrc.org/models/topic#";

	public static Model MigrateTopic(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(TP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(TP + "Topic"));
		Property prop = m.getProperty(TP, "status");
		m.add(main, prop, root.getAttribute("status"));

		CommonMigration.addNames(m, root, main, TXSDNS);

		CommonMigration.addNotes(m, root, main, TXSDNS);

		CommonMigration.addExternals(m, root, main, TXSDNS);

		CommonMigration.addLog(m, root, main, TXSDNS);

		CommonMigration.addDescriptions(m, root, main, TXSDNS);

        NodeList nodeList = root.getElementsByTagNameNS(TXSDNS, "seeAlso");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String value = current.getAttribute("rid").trim();
            m.add(main, m.getProperty(TP+"seeAlso"), m.createProperty(TP+value));
        }
		
		return m;
	}
	
	
}