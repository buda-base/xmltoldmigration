package io.bdrc.xmltoldmigration.xml2files;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;


public class PlaceMigration {

	public static final String PLXSDNS = "http://www.tbrc.org/models/place#";
    private static final String BDO = CommonMigration.ONTOLOGY_NS;
    private static final String BDR = CommonMigration.RESOURCE_NS;
    private static final String ADM = CommonMigration.ADM;
    private static final String VCARD = VCARD4.getURI();
	
    private static String getUriFromTypeSubtype(String type, String subtype) {
        switch (type) {
        case "tradition":
            return BDR+"Tradition"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "simple":
            return BDO+"place"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "eventType":
            return BDO+"Place"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        default:
               return "";
        }
    }
	
	public static Model MigratePlace(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "place");
		Element root = xmlDocument.getDocumentElement();
		Element current;
		Resource main = m.createResource(BDR + root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(BDO + "Place"));
		String value = getTypeStr(root, m, main);
		m.add(main, m.getProperty(BDO, "placeType"), m.createResource(BDR + "PlaceType"+value));
		
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
			addSimpleAttr(current.getAttribute("city"), "city", VCARD+"locality", m, address);
			addSimpleAttr(current.getAttribute("country"), "country", VCARD+"country-name", m, address);
			addSimpleAttr(current.getAttribute("postal"), "postal", VCARD+"postal-code", m, address);
			addSimpleAttr(current.getAttribute("state"), "state", VCARD+"region", m, address);
			String streetAddress = current.getAttribute("number").trim()+" "+current.getAttribute("street").trim();
			address.addProperty(m.getProperty(VCARD, "street-address"), m.createLiteral(streetAddress));
		}
		
		// tlm
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "tlm");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addTlm(m, main, current);
		}
		
		// adding monastery foundation events from persons (should be merged with the current founding event if present)
		PersonMigration.FoundingEvent fe = PersonMigration.placeEvents.get(main.getLocalName());
		if (fe != null) {
		    Resource event = m.createResource();
	        m.add(event, RDF.type, m.getResource(BDR+"PlaceFounded"));
	        CommonMigration.addDates(fe.circa, event, main);
	        m.add(event, m.createProperty(BDO, "eventWho"), m.createResource(BDR+fe.person));
	        Property prop = m.getProperty(BDO+"placeEvent");
	        m.add(main, prop, event);		    
		}
				
		SymetricNormalization.insertMissingTriplesInModel(m, root.getAttribute("RID"));
		
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
			m.add(main, m.getProperty(ADM+"place_TLM_taxonomy"), tax);
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
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String value = current.getAttribute("place").trim();
			if (value.isEmpty() || value.equals("NONE"))
			    return;
	        value = MigrationHelpers.sanitizeRID(main.getLocalName(), propName, value);
	        if (!MigrationHelpers.isDisconnected(value))
	            SymetricNormalization.addSymetricProperty(m, ontoPropName, main.getLocalName(), value, null);
		}
	}
	
	public static String gisIdToUri(String gisId) {
	    switch (gisId) {
	    case "fromLex": return ADM+"place_id_lex";
	    case "fromTBRC": return ADM+"place_id_TBRC";
	    case "chgis_id": return BDO+"placeChgisId";
	    case "gb2260-2013": return BDO+"placeGB2260-2013";
	    case "WB_area_sq_km": return BDO+"placeWBArea";
	    case "WB_pop_2000": return BDO+"placeWB2000";
	    case "WB_pop_2010": return BDO+"placeWB2010";
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
//	    case "placeTypes:srolRgyunGyiSaMing":
//            return "SrolRgyunGyiSaMing";
	    case "placeTypes:srolRgyunSaMing":
            return "SrolRgyunGyiSaMing";
	    case "placeTypes:tshoPa":
            return "TshoBa";
        case "placeTypes:rgyalKhams":
            return "RgyalKhab";
        case "placeTypes:traditionalPlaceName":
            return "SrolRgyunGyiSaMing";
        case "placeTypes:residentialHouse":
            return "GzimsKhang";
        case "placeTypes:notSpecified":
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
			    value = BDO+"PlaceEventTypeNotSpecified";
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "event", "missing type for an event");
	        } else {
	            // should start with "placeEventTypes:"
	            value = value.substring(16);
	            value = getUriFromTypeSubtype("eventType", value);
	        }
			Resource event = m.createResource();
			m.add(event, RDF.type, m.getResource(value));
			CommonMigration.addDates(current.getAttribute("circa"), event, main);
			value = current.getAttribute("circa").trim();
			Property prop = m.getProperty(BDO+"placeEvent");
			m.add(main, prop, event);
			addAffiliations(m, current, event, main);
			CommonMigration.addNotes(m, current, event, PLXSDNS);
			CommonMigration.addDescriptions(m, current, event, PLXSDNS);
		}
	}
	
	public static void addAffiliations(Model m, Element eventElement, Resource event, Resource main) throws IllegalArgumentException {
		NodeList nodeList = eventElement.getElementsByTagNameNS(PLXSDNS, "affiliation");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			String type = current.getAttribute("type");
			Resource target = null;
			Property prop = null;
			String value = current.getAttribute("rid");
			if (value.isEmpty()) continue;
			if (!type.equals("placeEventAffiliationTypes:lineage")) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "event/affiliation", "invalid affiliation type value: `"+type+"` (should be `placeEventAffiliationTypes:lineage`)");
			}
            if (!value.startsWith("lineage:")) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "event/affiliation", "invalid affiliation rid value: `"+value+"` (should be `lineage:`)");
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
