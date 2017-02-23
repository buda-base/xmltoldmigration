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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PlaceMigration {

	private static final String RP = CommonMigration.ROOT_PREFIX;
	private static final String PP = CommonMigration.PERSON_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String PLP = CommonMigration.PLACE_PREFIX;
	private static final String PLXSDNS = "http://www.tbrc.org/models/place#";
	
	private static final Map<String,String> lineageTypeToSomething = new HashMap<String,String>();
	static {
		// TODO: should be mapped to topics... currently O5TAX003 nodes
		lineageTypeToSomething.put("Nyingma", "O5TAX0035TAX001");
		lineageTypeToSomething.put("Kadam", "O5TAX0035TAX0034JW33790");
		lineageTypeToSomething.put("Geluk", "O5TAX0035TAX003");
		lineageTypeToSomething.put("Kagyu", "O5TAX0035TAX002");
		lineageTypeToSomething.put("KarmaKagyu", "O5TAX00310MS14537");
		lineageTypeToSomething.put("MarpaKagyu", "O5TAX00310MS14554");
		lineageTypeToSomething.put("DrigungKagyu", "O5TAX0034JW33807");
		lineageTypeToSomething.put("DrukpaKagyu", "O5TAX0034JW33808");
		lineageTypeToSomething.put("BaromKagyu", "O5TAX00310MS14535");
		lineageTypeToSomething.put("ShangpaKagyu", "O5TAX00310MS14540");
		lineageTypeToSomething.put("TodrukKachangyu", "O5TAX00310MS14542");
		lineageTypeToSomething.put("ZurmangKagyu", "O5TAX00310MS14543");
		lineageTypeToSomething.put("TsalpaKagyu", "O5TAX00310MS14551");
		lineageTypeToSomething.put("YazangKagyu", "O5TAX00310MS14552");
		lineageTypeToSomething.put("YelpaKagyu", "O5TAX00310MS14553");
		lineageTypeToSomething.put("TaklungKagyu", "O5TAX00310MS14544");
		lineageTypeToSomething.put("NedoKagyu", "O5TAX00310MS14538");
		lineageTypeToSomething.put("TaklungKagyu", "O5TAX00310MS14555");
		lineageTypeToSomething.put("Sakya", "O5TAX0035TAX004");
		lineageTypeToSomething.put("NgorSakya", "O5TAX00310MS14539");
		lineageTypeToSomething.put("Jonang", "O5TAX0035TAX006");
		lineageTypeToSomething.put("Bodong", "O5TAX0035TAX007");
		lineageTypeToSomething.put("Chod", "O5TAX00310MS14536");
		lineageTypeToSomething.put("Zhije", "O5TAX00310MS14541");
		lineageTypeToSomething.put("Bon", "O5TAX0035TAX008");
		lineageTypeToSomething.put("Zhalu", "O5TAX0034JW33836");
		lineageTypeToSomething.put("Rime", "O5TAX0035TAX005");
	}
	
	public static Model MigratePlace(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String value = getTypeStr(root);
		Resource main = m.createResource(PLP + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(PLP + value));
		if (!value.equals("Place")) {
			m.add(main, RDF.type, m.createResource(PLP + "Place"));
		}
		Property prop = m.getProperty(RP, "status");
		m.add(main, prop, root.getAttribute("status"));
		CommonMigration.addNames(m, root, main, PLXSDNS);
		
		CommonMigration.addNotes(m, root, main, PLXSDNS);
		
		CommonMigration.addExternals(m, root, main, PLXSDNS);
		
		CommonMigration.addLog(m, root, main, PLXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, PLXSDNS);
		
		addEvents(m, root, main);
		
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "gis");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addGis(m, current, main);
		}
		
		addSimpleObjectProp("isLocatedIn", m, main, root);
		addSimpleObjectProp("near", m, main, root);
		addSimpleObjectProp("contains", m, main, root);
		
		// address
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "address");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			value = CommonMigration.getSubResourceName(main, PLP, "Address");
			Resource address = m.createResource(value);
			m.add(address, RDF.type, m.createResource(PLP + "Address"));
			m.add(main, m.getProperty(PLP+"hasAddress"), address);
			addSimpleAttr(current.getAttribute("city"), "city", m, address);
			addSimpleAttr(current.getAttribute("country"), "country", m, address);
			addSimpleAttr(current.getAttribute("number"), "number", m, address);
			addSimpleAttr(current.getAttribute("postal"), "postal", m, address);
			addSimpleAttr(current.getAttribute("state"), "state", m, address);
			addSimpleAttr(current.getAttribute("street"), "street", m, address);
		}
		
		// tlm
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "tlm");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addTlm(m, main, current);
		}
		
		return m;
	}

	public static void addTlm(Model m, Resource main, Element tlmEl) {
		addSimpleAttr(tlmEl.getAttribute("accession"), "hasTLM_accession", m, main);
		addSimpleAttr(tlmEl.getAttribute("code"), "hasTLM_code", m, main);
		addSimpleAttr(tlmEl.getAttribute("num"), "hasTLM_num", m, main);
		NodeList nodeList = tlmEl.getElementsByTagNameNS(PLXSDNS, "taxonomy");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Resource tax = m.createResource(CommonMigration.OUTLINE_PREFIX+current.getAttribute("rid"));
			m.add(main, m.getProperty(PLP+"hasTLM_taxonomy"), tax);
		}
		nodeList = tlmEl.getElementsByTagNameNS(PLXSDNS, "groups");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			addSimpleAttr(current.getAttribute("admin"), "hasTLM_admin", m, main);
			addSimpleAttr(current.getAttribute("adminEmail"), "hasTLM_adminEmail", m, main);
			addSimpleAttr(current.getAttribute("librarian"), "hasTLM_librarian", m, main);
			addSimpleAttr(current.getAttribute("librarianEmail"), "hasTLM_librarianEmail", m, main);
		}
	}
	
	public static void addSimpleAttr(String attrValue, String attrName, Model m, Resource r) {
		if (attrValue.isEmpty()) return;
		Property prop = m.getProperty(PLP+attrName);
		m.add(r, prop, m.createLiteral(attrValue));
	}
	
	public static void addSimpleObjectProp(String propName, Model m, Resource main, Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, propName);
		Property prop = m.getProperty(PLP+propName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String value = current.getAttribute("place");
			Resource sub = m.createResource(PLP+value);
			m.add(main, prop, sub);
		}
	}
	
	public static void addGis(Model m, Element gis, Resource main) {
		String name = CommonMigration.getSubResourceName(main, PLP, "Gis");
		Resource gisResource = m.createResource(name);
		m.add(main, m.getProperty(PLP+"hasGIS"), gisResource);
		m.add(gisResource, RDF.type, m.createResource(PLP + "Gis"));
		NodeList nodeList = gis.getElementsByTagNameNS(PLXSDNS, "id");
		String value = null;
		Property prop;
		Literal l;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			prop = m.getProperty(PLP+value);
			value = current.getAttribute("value").trim();
			l = m.createLiteral(value);
			m.add(gisResource, prop, l);
		}
		nodeList = gis.getElementsByTagNameNS(PLXSDNS, "coords");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			prop = m.getProperty(PLP+"lat");
			value = current.getAttribute("lat").trim();
			l = m.createLiteral(value);
			m.add(gisResource, prop, l);
			prop = m.getProperty(PLP+"long");
			value = current.getAttribute("long").trim();
			l = m.createLiteral(value);
			m.add(gisResource, prop, l);
			prop = m.getProperty(PLP+"accuracy");
			value = current.getAttribute("accuracy").trim();
			if(!value.isEmpty()) {
				l = m.createLiteral(value);
				m.add(gisResource, prop, l);
			}
		}
		
	}
	
	public static String getTypeStr(Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "info");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			// starts with "placeTypes:"
			value = value.substring(11);
			value = CommonMigration.normalizePropName(value, "Class");
			break;
		}
		return value;
	}
	
	public static void addEvents(Model m, Element root, Resource main) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "event");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			// starts with "placeEventTypes:"
			value = value.substring(16);
			value = CommonMigration.normalizePropName(value, "Class");
			String name = CommonMigration.getSubResourceName(main, PLP, "Event", i+1);
			Resource event = m.createResource(PLP + name);
			m.add(event, RDF.type, m.getResource(PLP+value));
			value = current.getAttribute("circa").trim();
			if (!value.isEmpty()) {
				m.add(event, m.createProperty(PLP+"event_circa"), m.createLiteral(value));
			}
			Property prop = m.getProperty(PLP+"hasEvent");
			m.add(main, prop, event);
			addAffiliations(m, current, event);
			CommonMigration.addNotes(m, current, event, PLXSDNS);
			CommonMigration.addDescriptions(m, current, event, PLXSDNS);
		}
	}
	
	public static void addAffiliations(Model m, Element eventElement, Resource event) {
		NodeList nodeList = eventElement.getElementsByTagNameNS(PLXSDNS, "affiliation");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String type = current.getAttribute("type");
			Resource target;
			Property prop;
			String value = current.getAttribute("rid");
			switch (type) {
			case "placeEventAffiliationTypes:lineage":
				// rid starts with "lineage:"
				value = value.substring(8);
				String rid = lineageTypeToSomething.get(value);
				target = m.createResource(CommonMigration.OUTLINE_PREFIX+rid);
				prop = m.getProperty(PLP+"affiliatedWith_lineage");
				break;
			case "placeEventAffiliationTypes:corporation":
				target = m.createResource(CommonMigration.CORPORATION_PREFIX+value);
				prop = m.getProperty(PLP+"affiliatedWith_corporation");
				break;
			default: // "placeEventAffiliationTypes:office"
				target = m.createResource(CommonMigration.OFFICE_PREFIX+value);
				prop = m.getProperty(PLP+"affiliatedWith_office");
				break;
			}
			m.add(event, prop, target);
		}
	}
	
}