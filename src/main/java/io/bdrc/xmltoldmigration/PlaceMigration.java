package io.bdrc.xmltoldmigration;

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

	private static final String PLXSDNS = "http://www.tbrc.org/models/place#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADM;
	
    private static String getUriFromTypeSubtype(String type, String subtype) {
        switch (type) {
        case "tradition":
            return BDR+"Tradition"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "simple":
            return BDO+"place"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "eventType":
            return BDR+"Place"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        default:
               return "";
        }
    }
	
	public static Model MigratePlace(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(BDO + "Place"));
		String value = getTypeStr(root, m, main);
		m.add(main, m.getProperty(BDO, "placeType"), m.createResource(BDR + value));
		
		CommonMigration.addStatus(m, main, root.getAttribute("status"));

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
		
		addSimpleObjectProp("isLocatedIn", "placeLocatedIn", m, main, root);
		addSimpleObjectProp("near", "placeIsNear", m, main, root);
		addSimpleObjectProp("contains", "placeContains", m, main, root);
		
		// address
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "address");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			Resource address = m.createResource();
			//m.add(address, RDF.type, m.createResource(BDO+"PlaceAddress"));
			m.add(main, m.getProperty(BDO+"placeAddress"), address);
			addSimpleAttr(current.getAttribute("city"), "city", BDO+"addressCity", m, address);
			addSimpleAttr(current.getAttribute("country"), "country", BDO+"addressCountry", m, address);
			addSimpleAttr(current.getAttribute("number"), "number", BDO+"addressNumber", m, address);
			addSimpleAttr(current.getAttribute("postal"), "postal", BDO+"addressPostal", m, address);
			addSimpleAttr(current.getAttribute("state"), "state", BDO+"addressState", m, address);
			addSimpleAttr(current.getAttribute("street"), "street", BDO+"addressStreet", m, address);
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
		addSimpleAttr(tlmEl.getAttribute("accession"), "hasTLM_accession", ADM+"place_TLM_accession", m, main);
		addSimpleAttr(tlmEl.getAttribute("code"), "hasTLM_code", ADM+"place_TLM_code", m, main);
		addSimpleAttr(tlmEl.getAttribute("num"), "hasTLM_num", ADM+"place_TLM_num", m, main);
		NodeList nodeList = tlmEl.getElementsByTagNameNS(PLXSDNS, "taxonomy");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Resource tax = m.createResource(CommonMigration.BDR+current.getAttribute("rid"));
			m.add(main, m.getProperty(ADM+"placeTlmTaxonomy"), tax);
		}
		nodeList = tlmEl.getElementsByTagNameNS(PLXSDNS, "groups");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			addSimpleAttr(current.getAttribute("admin"), "hasTLM_admin", ADM+"place_TLM_admin", m, main);
			addSimpleAttr(current.getAttribute("adminEmail"), "hasTLM_adminEmail", ADM+"place_TLM_adminEmail", m, main);
			addSimpleAttr(current.getAttribute("librarian"), "hasTLM_librarian", ADM+"place_TLM_librarian", m, main);
			addSimpleAttr(current.getAttribute("librarianEmail"), "hasTLM_librarianEmail", ADM+"place_TLM_librarianEmail", m, main);
		}
	}
	
	public static void addSimpleAttr(String attrValue, String attrName, String ontoProp, Model m, Resource r) {
		if (attrValue.isEmpty()) return;
		Property prop = m.getProperty(ontoProp);
		m.add(r, prop, m.createLiteral(attrValue));
	}
	
	public static void addSimpleObjectProp(String propName, String ontoPropName, Model m, Resource main, Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, propName);
		Property prop = m.getProperty(BDO, ontoPropName);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String value = current.getAttribute("place").trim();
			if (value.isEmpty() || value.equals("NONE"))
			    return;
			Resource sub = m.createResource(BDR+value);
			m.add(main, prop, sub);
		}
	}
	
	public static String gisIdToUri(String gisId) {
	    switch (gisId) {
	    case "fromLex": return ADM+"place_id_lex";
	    case "fromTBRC": return ADM+"place_id_TBRC";
	    case "chgis_id": return BDO+"placeChgisId";
	    case "gb2260-2013": return BDO+"placeGB2260-2013";
	    case "WB_area_sq_km": return BDO+"WBArea";
	    case "WB_pop_2000": return BDO+"WB2000";
	    case "WB_pop_2010": return BDO+"WB2010";
	    default: return "";
	    }
	}
	
	public static void addGis(Model m, Element gis, Resource main) {
		NodeList nodeList = gis.getElementsByTagNameNS(PLXSDNS, "id");
		String value = null;
		Property prop;
		Literal l;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			value = gisIdToUri(value);
			if (value.isEmpty()) continue;
			prop = m.getProperty(value);
			value = current.getAttribute("value").trim();
			l = m.createLiteral(value);
			m.add(main, prop, l);
		}
		nodeList = gis.getElementsByTagNameNS(PLXSDNS, "coords");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("lat").trim();
			if (!value.isEmpty()) {
			    prop = m.getProperty(BDO+"placeLat");
			    l = m.createLiteral(value);
			    m.add(main, prop, l);
			}
			value = current.getAttribute("long").trim();
			if (!value.isEmpty()) {
			    prop = m.getProperty(BDO+"placeLong");
			    l = m.createLiteral(value);
			    m.add(main, prop, l);
			}
			prop = m.getProperty(BDO+"placeAccuracy");
			value = current.getAttribute("accuracy").trim();
			if(!value.isEmpty()) {
				l = m.createLiteral(value);
				m.add(main, prop, l);
			}
			prop = m.getProperty(BDO+"placeRegionPoly");
			value = current.getTextContent().trim();
			if(!value.isEmpty()) {
				l = m.createLiteral(value);
				m.add(main, prop, l);
			}
		}
		
	}
	
	public static String normalizePlaceType(String val) {
	    switch(val) {
	    case "khul":
	        return "Khul";
	    case "placeTypes:townshipSeats":
	        return "Shang";
	    case "placeTypes:srolRgyunGyiSaMing":
            return "SrolRgyunSaMing";
	    case "placeTypes:tshoPa":
            return "TshoBa";
        case "placeTypes:rgyalKhams":
            return "NotSpecified";
        case "placeTypes:traditionalPlaceName":
            return "srolRgyunSaMing";
        case "placeTypes:residentialHouse":
            return "NotSpecified";
	    }
	    // starts with "placeTypes:"
	    val = val.substring(11);
	    val = CommonMigration.normalizePropName(val, "Class");
	    return val;
	}
	
	public static String getTypeStr(Element root, Model m, Resource main) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "info");
		String value = "NotSpecified";
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type").trim();
			String normalizedValue = normalizePlaceType(value);
			if (normalizedValue.equals("NotSpecified")) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "unable to deterine place type, original type: `"+value+"`");
			}
			return normalizedValue;
		}
	    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "missing place type");
		return value;
	}
	
	public static void addEvents(Model m, Element root, Resource main) {
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "event");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value.isEmpty()) {
			    value = BDR+"PlaceEventTypeNotSpecified";
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "event", "missing type for an event");
	        } else {
	            // should start with "placeEventTypes:"
	            value = value.substring(16);
	            value = getUriFromTypeSubtype("eventType", value);
	        }
			Resource event = m.createResource();
			m.add(event, m.getProperty(BDO, "placeEventType"), m.getResource(value));
			value = current.getAttribute("circa").trim();
			if (!value.isEmpty()) {
				m.add(event, m.createProperty(BDO+"onOrAbout"), m.createLiteral(value));
			}
			Property prop = m.getProperty(BDO+"placeEvent");
			m.add(main, prop, event);
			addAffiliations(m, current, event);
			CommonMigration.addNotes(m, current, event, PLXSDNS);
			CommonMigration.addDescriptions(m, current, event, PLXSDNS);
		}
	}
	
	public static void addAffiliations(Model m, Element eventElement, Resource event) throws IllegalArgumentException {
		NodeList nodeList = eventElement.getElementsByTagNameNS(PLXSDNS, "affiliation");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String type = current.getAttribute("type");
			Resource target = null;
			Property prop = null;
			String value = current.getAttribute("rid");
			if (value.isEmpty()) continue;
			if (!type.equals("placeEventAffiliationTypes:lineage")) {
			    CommonMigration.addException(m, event, "invalid affiliation type value: `"+type+"` (should be `placeEventAffiliationTypes:lineage`)");
			}
            if (!value.startsWith("lineage:")) {
                CommonMigration.addException(m, event, "invalid affiliation rid value: `"+value+"` (should be `lineage:`)");
            } else {
                if (value.equals("lineage:Kadampa")) value = "lineage:Kadam";
                if (value.equals("lineage:Shije")) value = "lineage:Zhije";
                value = value.substring(8);
                String url =  getUriFromTypeSubtype("tradition", value);
                target = m.createResource(url);
                prop = m.getProperty(BDO+"placeEventAffiliation");
                m.add(event, prop, target);
            }
		}
	}
	
}
