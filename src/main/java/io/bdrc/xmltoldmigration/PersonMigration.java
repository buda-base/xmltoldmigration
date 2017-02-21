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


public class PersonMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
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
		m.add(main, RDF.type, m.createResource(PP + "Person"));
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		String lang = null;
		Literal value = null;
		
		// names
		
		NodeList nodeList = root.getElementsByTagNameNS(PXSDNS, "name");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			lang = CommonMigration.getBCP47(current, "bo-x-ewts");
			value = m.createLiteral(current.getTextContent().trim(), lang);
			prop = m.getProperty(PP, current.getAttribute("type"));
			m.add(main, prop, value);
			if (i == 0) {
				CommonMigration.addLabel(m, main, value);
			}
		}
		
		// gender
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "info");
		value = m.createLiteral(((Element) nodeList.item(0)).getAttribute("gender"));
		prop = m.getProperty(PP, "gender");
		m.add(main, prop, value);
		
		// events
		
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
			m.add(main, prop, m.createResource(PP+current.getAttribute("pid")));
		}
		
		// kinship
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "kinship");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addKinship(m, main, current);
		}
		
		// ofSect
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "ofSect");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			prop = m.getProperty(PP, "ofSect");
			m.add(main, prop, m.createResource(TP+current.getAttribute("sect")));
		}
		
		// incarnationOf
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "incarnationOf");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addIncarnation(m, main, current);
		}
		
		CommonMigration.addNotes(m, root, main, PXSDNS);
		
		CommonMigration.addExternals(m, root, main, PXSDNS);
		
		CommonMigration.addLog(m, root, main, PXSDNS);
		
		return m;
	}
	
	public static void addIncarnation(Model m, Resource r, Element e) {
		Resource incarnationOf = m.createResource(new AnonId());
		m.add(incarnationOf, RDF.type, m.createProperty(PP+"IncarnationOf"));
		m.add(r, m.getProperty(PP+"incarnationOf"), incarnationOf);
		String value = e.getAttribute("being");
		Resource being;
		if (value.startsWith("P")) {
			being = m.createResource(PP+value);
		} else {
			being = m.createResource(TP+value);
		}
		Property prop = m.getProperty(PP, "incarnationOf_being");
		m.add(incarnationOf, prop, being);
		value = e.getAttribute("relation");
		if (value != null && value != "") {
			prop = m.getProperty(PP, "incarnationOf_relation");
			m.add(incarnationOf, prop, m.createLiteral(value));
		}
		value = e.getAttribute("secondary");
		if (value != null && value != "") {
			prop = m.getProperty(PP, "incarnationOf_secondary");
			m.add(incarnationOf, prop, m.createLiteral(value));
		}
		
	}
	
	public static void addEvent(Model m, Resource person, Element e) {
		Resource subResource = m.createResource(new AnonId(e.getAttribute("pid")));
		String typeValue = CommonMigration.normalizePropName(e.getAttribute("type"), "Class");
		m.add(subResource, RDF.type, m.createProperty(PP+typeValue));
		Literal value = m.createLiteral(e.getAttribute("circa"));
		Property prop = m.getProperty(PP, "event_circa");
		m.add(subResource, prop, value);
		m.add(person, m.getProperty(PP+"event"), subResource);
	}
	
	public static void addKinship(Model m, Resource person, Element e) {
		String relation = e.getAttribute("relation");
		String with = e.getAttribute("person");
		Property prop;
		prop = m.createProperty(PP+relation);
		Resource r = m.createResource(PP+with);
		m.add(person, prop, r);
	}
	
	public static void addSeat(Model m, Resource person, Element e) {
		Resource subResource = m.createResource();
		Resource value = m.createResource(PP+"Seat");
		m.add(subResource, RDF.type, value);
		String circa = e.getAttribute("circa");
		if (circa != null && circa != "") {
			Property prop = m.getProperty(PP, "seat_circa");
			m.add(subResource, prop, m.createLiteral(circa));
		}
		NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Property prop = m.getProperty(PP, "seat_place");
			Resource r = m.createResource(PLP+current.getAttribute("pid"));
			//value = CommonMigration.getLitFromUri(m, PLP+current.getAttribute("pid"));
			m.add(subResource, prop, r);
		}
		m.add(person, m.getProperty(PP+"seat"), subResource);
	}

}
