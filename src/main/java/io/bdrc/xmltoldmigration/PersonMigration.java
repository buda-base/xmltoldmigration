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
	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String PXSDNS = "http://www.tbrc.org/models/person#";
	
	public static Model MigratePerson(Document xmlDocument) {
		if (!CommonMigration.documentValidAgainstXSD(xmlDocument, "person")) {
			return null;
		}
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(PP + root.getAttribute("RID"));
		m.add(main, RDF.type, CommonMigration.getLitFromUri(m, PP + "Person"));
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		String lang = null;
		Node node = null;
		Literal value = null;
		
		// names
		
		NodeList nodeList = root.getElementsByTagNameNS(PXSDNS, "name");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			lang = CommonMigration.getBCP47(current);
			value = m.createLiteral(current.getTextContent(), lang);
			prop = m.getProperty(PP, current.getAttribute("type"));
			m.add(main, prop, value);
		}
		
		// gender
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "info");
		value = m.createLiteral(((Element) nodeList.item(0)).getAttribute("gender"));
		prop = m.getProperty(PP, "gender");
		m.add(main, prop, value);
		
		// events
		
		Resource subResource = null;
		nodeList = root.getElementsByTagNameNS(PXSDNS, "event");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addEvent(m, main, current);
		}
		
		// seat
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "seat");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addSeat(m, main, current);
		}
		
		// relations
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "teacherOf");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			prop = m.getProperty(PP, "teacherOf");
			value = CommonMigration.getLitFromUri(m, PP+current.getAttribute("pid"));
			m.add(main, prop, value);
		}
		
		CommonMigration.addNotes(m, root, main, PXSDNS);
		
		return m;
	}
	
	public static void addEvent(Model m, Resource person, Element e) {
		Resource subResource = m.createResource(new AnonId(e.getAttribute("pid")));
		String typeValue = CommonMigration.normalizePropName(e.getAttribute("type"), "Class");
		Literal value = CommonMigration.getLitFromUri(m, PP+typeValue);
		m.add(subResource, RDF.type, value);
		value = m.createLiteral(e.getAttribute("circa"));
		Property prop = m.getProperty(PP, "event_circa");
		m.add(subResource, prop, value);
		m.add(person, m.getProperty(PP+"event"), subResource);
	}
	
	public static void addSeat(Model m, Resource person, Element e) {
		Resource subResource = m.createResource();
		Literal value = CommonMigration.getLitFromUri(m, PP+"Seat");
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
			value = CommonMigration.getLitFromUri(m, PLP+current.getAttribute("pid"));
			m.add(subResource, prop, value);
		}
		m.add(person, m.getProperty(PP+"seat"), subResource);
	}

}
