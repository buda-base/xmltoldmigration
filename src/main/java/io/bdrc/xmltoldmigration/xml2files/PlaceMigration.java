package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.VCARD;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;
import static io.bdrc.libraries.Models.FacetType.EVENT;
import static io.bdrc.libraries.Models.FacetType.VCARD_ADDR;

import java.util.HashMap;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;


public class PlaceMigration {

	public static final String PLXSDNS = "http://www.tbrc.org/models/place#";
	
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
    
    private static Resource placeTypeNotSpec = getNotSpecifiedPlaceType();
    private static HashMap<String, Resource> val2type = new HashMap<>();
    
    private static Resource getNotSpecifiedPlaceType() {
        OntModel ontModel = MigrationHelpers.getOntologyModel();
        
        ResIterator iter = ontModel.listSubjectsWithProperty(SKOS.notation, "notSpecified");
        if (iter.hasNext()) {
            return iter.next();
        } else {
            System.err.println("PlaceMigration could not find ontology resource for PlaceType notSpecified");
            System.exit(1);
            return null;
        }
    }
    
    private static Resource lookupTypeValue(String typeValue, Resource main) {
        Resource placeType = val2type.get(typeValue);
        if (placeType != null) {
            return placeType;
        }

        OntModel ontModel = MigrationHelpers.getOntologyModel();
        ResIterator iter = ontModel.listSubjectsWithProperty(SKOS.notation, typeValue);
        if (iter.hasNext()) {
            placeType = iter.next();
        } else {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "unknown original type: "+typeValue);
            placeType = placeTypeNotSpec;
        }
        
        val2type.put(typeValue, placeType);
        return placeType;
    }

    private static String normalizePlaceType(String val) {
        switch(val) {
        case "khul":
            return "khul";
        case "placeTypes:townshipSeats":
            return "shang";
        case "placeTypes:srolRgyunSaMing":
            return "srolRgyunGyiSaMing";
        case "placeTypes:tshoPa":
            return "tshoBa";
        case "placeTypes:rgyalKhams":
            return "rgyalKhab";
        case "placeTypes:traditionalPlaceName":
            return "srolRgyunGyiSaMing";
        case "placeTypes:residentialHouse":
            return "gzimsKhang";
        case "placeTypes:notSpecified":
            return "notSpecified";
        }
        // starts with "placeTypes:"
        val = val.substring(11);
        val = CommonMigration.normalizePropName(val, "Class");
        return val;
    }
    
    private static Resource getPlaceType(Element root, Model model, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "info");
        String typeValue = "";
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            typeValue = current.getAttribute("type").trim();
            if (typeValue.isEmpty()) {
                continue;
            } else {
                break;
            }
        }
        
        if (typeValue.isEmpty()) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "missing place type");
            typeValue = "notSpecified";
        } else if (typeValue.equals("notSpecified")) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "original type: notSpecified");
        }
        
        typeValue = normalizePlaceType(typeValue);
        
        return lookupTypeValue(typeValue, main);
    }
	
	public static Model MigratePlace(Document xmlDocument) {
		Model model = ModelFactory.createDefaultModel();
		setPrefixes(model, "place");
		Element root = xmlDocument.getDocumentElement();
		Element current;
        Resource main = createRoot(model, BDR+root.getAttribute("RID"), BDO+"Place");
        Resource admMain = createAdminRoot(main);
		Resource placeType = getPlaceType(root, model, main);
		model.add(main, model.getProperty(BDO, "placeType"), placeType);
		
		addStatus(model, admMain, root.getAttribute("status"));
		admMain.addProperty(model.getProperty(ADM, "metadataLegal"), model.createResource(BDA+"LD_BDRC_CC0"));

		CommonMigration.addNames(model, root, main, PLXSDNS);
		
		CommonMigration.addNotes(model, root, main, PLXSDNS);
		
		CommonMigration.addExternals(model, root, main, PLXSDNS);
        
        CommonMigration.addDescriptions(model, root, main, PLXSDNS);
        
        addEvents(model, root, main);
		
		CommonMigration.addLog(model, root, admMain, PLXSDNS);
		
		NodeList nodeList = root.getElementsByTagNameNS(PLXSDNS, "gis");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addGis(model, current, main);
		}
		
		addSimpleObjectProp("isLocatedIn", "placeLocatedIn", model, main, root);
		addSimpleObjectProp("near", "placeIsNear", model, main, root);
		addSimpleObjectProp("contains", "placeContains", model, main, root);
		
		// address
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "address");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			Resource address = getFacetNode(VCARD_ADDR, VCARD, main, VCARD_ADDR.getNodeType());
			model.add(main, model.getProperty(BDO+"placeAddress"), address);
			addSimpleAttr(current.getAttribute("city"), "city", VCARD+"locality", model, address);
			addSimpleAttr(current.getAttribute("country"), "country", VCARD+"country-name", model, address);
			addSimpleAttr(current.getAttribute("postal"), "postal", VCARD+"postal-code", model, address);
			addSimpleAttr(current.getAttribute("state"), "state", VCARD+"region", model, address);
			String streetAddress = current.getAttribute("number").trim()+" "+current.getAttribute("street").trim();
			address.addProperty(model.getProperty(VCARD, "street-address"), model.createLiteral(streetAddress));
		}
		
		// tlm
		nodeList = root.getElementsByTagNameNS(PLXSDNS, "tlm");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addTlm(model, admMain, current);
		}
		
		// adding monastery foundation events from persons (should be merged with the current founding event if present)
		PersonMigration.FoundingEvent fe = PersonMigration.placeEvents.get(main.getLocalName());
		if (fe != null) {
		    Resource event = getFacetNode(EVENT, main,  model.getResource(BDO+"PlaceFounded"));
	        CommonMigration.addDates(fe.circa, event, main);
	        model.add(event, model.createProperty(BDO, "eventWho"), model.createResource(BDR+fe.person));
	        Property prop = model.getProperty(BDO+"placeEvent");
	        model.add(main, prop, event);		    
		}
				
		SymetricNormalization.insertMissingTriplesInModel(model, root.getAttribute("RID"));
		
		return model;
	}

	public static void addTlm(Model m, Resource main, Element tlmEl) {
		addSimpleAttr(tlmEl.getAttribute("accession"), "hasTLM_accession", ADM+"place_TLM_accession", m, main);
		addSimpleAttr(tlmEl.getAttribute("code"), "hasTLM_code", ADM+"place_TLM_code", m, main);
		addSimpleAttr(tlmEl.getAttribute("num"), "hasTLM_num", ADM+"place_TLM_num", m, main);
		NodeList nodeList = tlmEl.getElementsByTagNameNS(PLXSDNS, "taxonomy");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Resource tax = m.createResource(BDR+current.getAttribute("rid"));
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
	    Literal lit;
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Element current = (Element) nodeList.item(i);
	        value = current.getAttribute("type");
	        value = gisIdToUri(value);
	        if (value.isEmpty()) continue;
	        prop = m.getProperty(value);
	        value = current.getAttribute("value").trim();
	        lit = m.createLiteral(value);
	        if (prop.getNameSpace().contains("admin")) {
	            m.add(getAdminData(main), prop, lit);
	        } else {
	            m.add(main, prop, lit);
	        }
	    }
	    nodeList = gis.getElementsByTagNameNS(PLXSDNS, "coords");
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Element current = (Element) nodeList.item(i);
	        value = current.getAttribute("lat").trim();
	        if (!value.isEmpty()) {
	            prop = m.getProperty(BDO+"placeLat");
	            lit = m.createLiteral(value);
	            m.add(main, prop, lit);
	        }
	        value = current.getAttribute("long").trim();
	        if (!value.isEmpty()) {
	            prop = m.getProperty(BDO+"placeLong");
	            lit = m.createLiteral(value);
	            m.add(main, prop, lit);
	        }
	        prop = m.getProperty(BDO+"placeAccuracy");
	        value = current.getAttribute("accuracy").trim();
	        if(!value.isEmpty()) {
	            lit = m.createLiteral(value);
	            m.add(main, prop, lit);
	        }
	        prop = m.getProperty(BDO+"placeRegionPoly");
	        value = current.getTextContent().trim();
	        if(!value.isEmpty()) {
	            lit = m.createLiteral(value);
	            m.add(main, prop, lit);
	        }
	    }

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
			Resource event = getFacetNode(EVENT, BDR, main, m.createResource(value));
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
