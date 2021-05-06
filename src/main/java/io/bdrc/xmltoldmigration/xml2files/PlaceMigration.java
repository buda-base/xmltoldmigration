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
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
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
        // val = CommonMigration.normalizePropName(val, "Class");
        return val;
    }
    
    private static Map<String,String> typeToLocalName = new HashMap<>();
    static {
        typeToLocalName.put("yulSde", "PT0001");
        typeToLocalName.put("rangSkyongKhul", "PT0002");
        typeToLocalName.put("rangSkyongLjongs", "PT0003");
        typeToLocalName.put("zamPa", "PT0004");
        typeToLocalName.put("rong", "PT0005");
        typeToLocalName.put("durKhrod", "PT0006");
        typeToLocalName.put("skor", "PT0007");
        typeToLocalName.put("grongKhyer", "PT0008");
        typeToLocalName.put("dengRabsSaGnas", "PT0009");
        typeToLocalName.put("rgyalKhab", "PT0010");
        typeToLocalName.put("rdzong", "PT0011");
        typeToLocalName.put("chus", "PT0012");
        typeToLocalName.put("cholKha", "PT0013");
        typeToLocalName.put("saKhul", "PT0014");
        typeToLocalName.put("gzhisKa", "PT0015");
        typeToLocalName.put("gruKha", "PT0016");
        typeToLocalName.put("nagsTshal", "PT0017");
        typeToLocalName.put("mkharRnying", "PT0018");
        typeToLocalName.put("beHu", "PT0019");
        typeToLocalName.put("sdeTsho", "PT0020");
        typeToLocalName.put("riKhrod", "PT0021");
        typeToLocalName.put("sbasYul", "PT0022");
        typeToLocalName.put("smanKhang", "PT0023");
        typeToLocalName.put("gzimsKhang", "PT0024");
        typeToLocalName.put("khyimTshang", "PT0025");
        typeToLocalName.put("gling", "PT0026");
        typeToLocalName.put("rgyalPhran", "PT0027");
        typeToLocalName.put("mtsho", "PT0028");
        typeToLocalName.put("yulChen", "PT0029");
        typeToLocalName.put("dpeMdzodKhang", "PT0030");
        typeToLocalName.put("mda'", "PT0031");
        typeToLocalName.put("khriSde", "PT0032");
        typeToLocalName.put("maNiRdoPhung", "PT0033");
        typeToLocalName.put("gzimsShag", "PT0034");
        typeToLocalName.put("tshong'Dus", "PT0035");
        typeToLocalName.put("sgrubPhug", "PT0036");
        typeToLocalName.put("dgonPa", "PT0037");
        typeToLocalName.put("bshadGrwa", "PT0038");
        typeToLocalName.put("khamsTshan", "PT0039");
        typeToLocalName.put("grwaTshang", "PT0040");
        typeToLocalName.put("blaBrang", "PT0041");
        typeToLocalName.put("riBo", "PT0042");
        typeToLocalName.put("laKha", "PT0043");
        typeToLocalName.put("riRgyud", "PT0044");
        typeToLocalName.put("grongRdal", "PT0045");
        typeToLocalName.put("khriSkor", "PT0046");
        typeToLocalName.put("rangByung'KhorYug", "PT0047");
        typeToLocalName.put("'brogSde", "PT0048");
        typeToLocalName.put("btsunDgon", "PT0050");
        typeToLocalName.put("phoBrang", "PT0051");
        typeToLocalName.put("glingKha", "PT0052");
        typeToLocalName.put("gnasChen", "PT0053");
        typeToLocalName.put("rdoRing", "PT0054");
        typeToLocalName.put("thang", "PT0055");
        typeToLocalName.put("sngarGyiRdzong", "PT0056");
        typeToLocalName.put("khul", "PT0057");
        typeToLocalName.put("sdeDponMnga'Ris", "PT0058");
        typeToLocalName.put("parKhang", "PT0059");
        typeToLocalName.put("zhingChen", "PT0060");
        typeToLocalName.put("ru", "PT0061");
        typeToLocalName.put("sgang", "PT0062");
        typeToLocalName.put("rtenGzhiYulLung", "PT0063");
        typeToLocalName.put("sgrubGrwa", "PT0064");
        typeToLocalName.put("chuBo", "PT0065");
        typeToLocalName.put("chuRgyud", "PT0066");
        typeToLocalName.put("slobGrwa", "PT0067");
        typeToLocalName.put("tshoBa", "PT0068");
        typeToLocalName.put("brtenPaGnasKhang", "PT0069");
        typeToLocalName.put("yulPhran", "PT0070");
        typeToLocalName.put("chuMig", "PT0071");
        typeToLocalName.put("mchodRten", "PT0072");
        typeToLocalName.put("stongSde", "PT0073");
        typeToLocalName.put("lhaKhang", "PT0074");
        typeToLocalName.put("gtsugLagKhang", "PT0075");
        typeToLocalName.put("bangSo", "PT0076");
        typeToLocalName.put("shang", "PT0077");
        typeToLocalName.put("srolRgyunGyiSaMing", "PT0078");
        typeToLocalName.put("srolRgyunSaKhul", "PT0079");
        typeToLocalName.put("gterGnas", "PT0080");
        typeToLocalName.put("ruSde", "PT0081");
        typeToLocalName.put("phu", "PT0082");
        typeToLocalName.put("lungPa", "PT0083");
        typeToLocalName.put("grongTsho", "PT0084");
        typeToLocalName.put("grongSde", "PT0085");
        typeToLocalName.put("rdzongSridGzhungGnasSa", "PT0086");
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
            return null;
        } else if (typeValue.equals("notSpecified")) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info/type", "original type: notSpecified");
            return null;
        }
        
        typeValue = normalizePlaceType(typeValue);
        
        String localName = typeToLocalName.get(typeValue);
        if (localName == null)
            return null;
        
        return model.createResource(BDR+localName);
    }
	
	public static Model MigratePlace(Document xmlDocument) {
		Model model = ModelFactory.createDefaultModel();
		MigrationHelpers.setPrefixes(model, "place");
		Element root = xmlDocument.getDocumentElement();
		Element current;
        Resource main = createRoot(model, BDR+root.getAttribute("RID"), BDO+"Place");
        Resource admMain = createAdminRoot(main);
        if (MigrationHelpers.ricrid.containsKey(root.getAttribute("RID"))) {
            admMain.addLiteral(admMain.getModel().createProperty(ADM, "restrictedInChina"), true);
        }
		Resource placeType = getPlaceType(root, model, main);
		if (placeType != null)
		    model.add(main, model.getProperty(BDO, "placeType"), placeType);
		
		addStatus(model, admMain, root.getAttribute("status"));
		admMain.addProperty(model.getProperty(ADM, "metadataLegal"), model.createResource(BDA+"LD_BDRC_CC0"));

		CommonMigration.addNames(model, root, main, PLXSDNS);
		
		CommonMigration.addNotes(model, root, main, PLXSDNS);
		
		CommonMigration.addExternals(model, root, main, PLXSDNS);
        
        CommonMigration.addDescriptions(model, root, main, PLXSDNS);
        
        addEvents(model, root, main);
		
		CommonMigration.addLog(model, root, admMain, PLXSDNS, false);
		
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
	        if (value.equals("chgis_id")) {
	            // The CHGIS Ids in our database are not the actual ones, so we
	            // compute the correct ones instead:
	            CommonMigration.addIdentifier(main, BDR+"CHGISId", "TBRC_"+main.getLocalName());
	            continue;
	        }
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
			    value = BDO+"PlaceEventNotSpecified";
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
                prop = m.getProperty(BDO+"associatedTradition");
                m.add(event, prop, target);
            }
		}
	}
	
}
