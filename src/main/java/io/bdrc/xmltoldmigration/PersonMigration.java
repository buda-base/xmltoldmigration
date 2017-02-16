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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PersonMigration {
	
	private static final String CP = CommonMigration.COMMON_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String PXSDNS = "http://www.tbrc.org/models/person#";
	
	public static Model MigratePerson(Document xmlDocument) {
		//if (!CommonMigration.documentValidAgainstXSD(xmlDocument, "person")) {
		//	return null;
		//}
		Model m = ModelFactory.createDefaultModel();
		Element current = xmlDocument.getDocumentElement();
		Resource main = m.createResource(PP + current.getAttribute("RID"));
		m.add(main, RDF.type, m.createLiteral(PP + "Person"));
		Property prop = m.getProperty(CP, "status");
		m.add(main, prop, current.getAttribute("status"));
		String lang = null;
		Node node = null;
		Literal value = null;
		
		// names
		
		NodeList nodeList = xmlDocument.getElementsByTagNameNS(PXSDNS, "name");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			lang = CommonMigration.getBCP47(current);
			value = m.createLiteral(current.getTextContent(), lang);
			prop = m.getProperty(PP, current.getAttribute("type"));
			m.add(main, prop, value);
		}
		
		// gender
		
		nodeList = xmlDocument.getElementsByTagNameNS(PXSDNS, "info");
		value = m.createLiteral(((Element) nodeList.item(0)).getAttribute("gender"));
		prop = m.getProperty(PP, "gender");
		m.add(main, prop, value);
		
		// events
		
		Resource subResource = null;
		nodeList = xmlDocument.getElementsByTagNameNS(PXSDNS, "event");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addEvent(m, main, current);
		}
		
		// seat
		
		nodeList = xmlDocument.getElementsByTagNameNS(PXSDNS, "seat");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addSeat(m, main, current);
		}
		
		return m;
	}
	
	public static void addEvent(Model m, Resource person, Element e) {
		Resource subResource = m.createResource(new AnonId(e.getAttribute("pid")));
		String typeValue = CommonMigration.normalizePropName(e.getAttribute("type"), "Class");
		Literal value = m.createLiteral(PP+typeValue);
		m.add(subResource, RDF.type, value);
		value = m.createLiteral(e.getAttribute("circa"));
		Property prop = m.getProperty(PP, "event_circa");
		m.add(subResource, prop, value);
		m.add(person, m.getProperty(PP+"event"), subResource);
	}
	
	public static void addSeat(Model m, Resource person, Element e) {
		Resource subResource = m.createResource();
		Literal value = m.createLiteral(PP+"Seat");
		m.add(subResource, RDF.type, value);
		String circa = e.getAttribute("circa");
		if (circa != null && circa != "") {
			value = m.createLiteral(circa);
			Property prop = m.getProperty(PP, "seat_circa");
			m.add(subResource, prop, value);
		}
		NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Property prop = m.getProperty(PP, "seat_place");
			value = m.createLiteral(PLP+current.getAttribute("pid"));
			m.add(subResource, prop, value);
		}
		m.add(person, m.getProperty(PP+"seat"), subResource);
	}

}
