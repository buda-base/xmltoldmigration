package io.bdrc.xmltoldmigration;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlascopco.hunspell.Hunspell;

import io.bdrc.ewtsconverter.EwtsConverter;
import openllet.core.exceptions.InternalReasonerException;

public class CommonMigration  {

	public static final String ONTOLOGY_PREFIX = "http://purl.bdrc.io/ontology/";
	public static final String ADMIN_PREFIX = "http://purl.bdrc.io/ontology/admin/";
	public static final String DATA_PREFIX = "http://purl.bdrc.io/data/";
	public static final String RESOURCE_PREFIX = "http://purl.bdrc.io/resource/";
    public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
    public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
    public static final String XSD_PREFIX = "http://www.w3.org/2001/XMLSchema#";
	public static final String DESCRIPTION_PREFIX = "http://onto.bdrc.io/ontology/description#";
	public static final String ROOT_PREFIX = "http://purl.bdrc.io/ontology/root#";
	public static final String CORPORATION_PREFIX = "http://purl.bdrc.io/ontology/corporation#";
	public static final String LINEAGE_PREFIX = "http://purl.bdrc.io/ontology/lineage#";
	public static final String OFFICE_PREFIX = "http://purl.bdrc.io/ontology/office#";
	public static final String PRODUCT_PREFIX = "http://purl.bdrc.io/ontology/product#";
	public static final String OUTLINE_PREFIX = "http://purl.bdrc.io/ontology/outline#";
	public static final String PERSON_PREFIX = "http://purl.bdrc.io/ontology/person#";
	public static final String PLACE_PREFIX = "http://purl.bdrc.io/ontology/place#";
	public static final String TOPIC_PREFIX = "http://purl.bdrc.io/ontology/topic#";
	public static final String VOLUMES_PREFIX = "http://purl.bdrc.io/ontology/volumes#";
	public static final String WORK_PREFIX = "http://purl.bdrc.io/ontology/work#";
	
	public static final String BDO = ONTOLOGY_PREFIX;
    public static final String BDD = DATA_PREFIX;
    public static final String BDR = RESOURCE_PREFIX;
    public static final String ADM = ADMIN_PREFIX;
	
	public static final int ET_LANG = ExceptionHelper.ET_LANG;
	
	public static final EwtsConverter converter = new EwtsConverter();
	public static final String hunspellBoPath = "src/main/resources/hunspell-bo/";
    public static final Hunspell speller = new Hunspell(hunspellBoPath+"bo.dic", hunspellBoPath+"bo.aff");
    
    public static final Map<String, String> logWhoToUri = new HashMap<>();
    static {
        fillLogWhoToUri();
    }
    
    public static void fillLogWhoToUri() {
        String prefix = RESOURCE_PREFIX+"U"; // ?
        logWhoToUri.put("Gene Smith", prefix+"1");
        logWhoToUri.put("agardner@sdrubin.org", prefix+"2");
        logWhoToUri.put("Alex Gardner", prefix+"2");
        logWhoToUri.put("Alexander Gardner", prefix+"2");
        logWhoToUri.put("Bumu Dega", prefix+"3");
        logWhoToUri.put("Dega Bumu", prefix+"3");
        logWhoToUri.put("Catherine Tsuji", prefix+"4");
        logWhoToUri.put("Chozin", prefix+"5");
        logWhoToUri.put("chozin", prefix+"5");
        logWhoToUri.put("Chris Tomlinson", prefix+"6");
        logWhoToUri.put("ct", prefix+"6");
        logWhoToUri.put("Code Ferret", prefix+"6");
        logWhoToUri.put("chris", prefix+"6");
        logWhoToUri.put("CodeFerret", prefix+"6");
        logWhoToUri.put("Chungdak Nangpa", prefix+"7");
        logWhoToUri.put("Chungdak Ngagpa", prefix+"7");// is it the same name?
        logWhoToUri.put("Chungdak Ngakpa", prefix+"7");
        logWhoToUri.put("cnakpa", prefix+"7");
        logWhoToUri.put("Cuilan Liu", prefix+"8");
        logWhoToUri.put("Gabi Coura", prefix+"9");
        logWhoToUri.put("Harry Einhorn", prefix+"10");
        logWhoToUri.put("Jann Ronis", prefix+"11");
        logWhoToUri.put("jann ronis", prefix+"11");
        logWhoToUri.put("Jeff Wallman", prefix+"12");
        logWhoToUri.put("Jigme Namgyal", prefix+"13");
        logWhoToUri.put("jm", prefix+"14"); // ?
        logWhoToUri.put("Joe McClellan", prefix+"15");
        logWhoToUri.put("Joseph McClellan", prefix+"15");
        logWhoToUri.put("Karma Gongde", prefix+"16");
        logWhoToUri.put("kgongde", prefix+"16");
        logWhoToUri.put("Kelsang Lhamo", prefix+"17");
        logWhoToUri.put("kelsang", prefix+"17");
        logWhoToUri.put("Kelsang", prefix+"17");
        logWhoToUri.put("klhamo", prefix+"17");
        logWhoToUri.put("Konchok Tsering", prefix+"18");
        logWhoToUri.put("Lobsang Shastri", prefix+"19");
        logWhoToUri.put("lshastri", prefix+"19");
        logWhoToUri.put("Michael R. Sheehy", prefix+"20");
        logWhoToUri.put("Michael Sheehy", prefix+"20");
        logWhoToUri.put("msheehy", prefix+"20");
        logWhoToUri.put("paldor", prefix+"21");
        logWhoToUri.put("Paldor", prefix+"21");
        logWhoToUri.put("pal dor", prefix+"21");
        logWhoToUri.put("Penghao Sun", prefix+"22");
        logWhoToUri.put("Ralf Kramer", prefix+"23");
        logWhoToUri.put("Ramon Prats", prefix+"24");
        logWhoToUri.put("Rory Lindsay", prefix+"25");
        logWhoToUri.put("Tendzin Parsons", prefix+"26");
        logWhoToUri.put("Tenzin Dickyi", prefix+"27");
        logWhoToUri.put("Arya Moallem", prefix+"28");
        logWhoToUri.put("Awang Ciren", prefix+"29");
        logWhoToUri.put("Chen Lai", prefix+"30");
        logWhoToUri.put("Dennis Johnson", prefix+"31");
        logWhoToUri.put("Dorjee Choden", prefix+"32");
        logWhoToUri.put("dorjee choden", prefix+"32");
        logWhoToUri.put("dzongsarlibrary", prefix+"33");
        logWhoToUri.put("Erdene Baatar", prefix+"34");
        logWhoToUri.put("Gyurmed Chodrak", prefix+"35");
        logWhoToUri.put("Gyurme Chograg", prefix+"35");
        logWhoToUri.put("Hachuluu", prefix+"36");
        logWhoToUri.put("Haschuluu", prefix+"36");
        logWhoToUri.put("Jamyang Lodoe", prefix+"37");
        logWhoToUri.put("Jamyang.Lodoe", prefix+"37");
        logWhoToUri.put("Jigme Tashi", prefix+"38");
        logWhoToUri.put("John Canti", prefix+"39");
        logWhoToUri.put("Khedup Gyatso", prefix+"40");
        logWhoToUri.put("Legacy Converter", prefix+"41");
        logWhoToUri.put("mangaram", prefix+"42");
        logWhoToUri.put("Mara Canizzaro", prefix+"43");
        logWhoToUri.put("mara canizzaro", prefix+"43");
        logWhoToUri.put("Morris Hopkins", prefix+"44");
        logWhoToUri.put("Ngawang Trinley", prefix+"45");
        logWhoToUri.put("tenzang", prefix+"45");// to be checked
        logWhoToUri.put("pbaduo", prefix+"46");
        logWhoToUri.put("topic reclassify", prefix+"47");
        logWhoToUri.put("zhangning", prefix+"48");
        logWhoToUri.put("Arthur McKeown", prefix+"49");
        logWhoToUri.put("Bruno Laine", prefix+"50");
        logWhoToUri.put("chengdu", prefix+"51");
        logWhoToUri.put("Chengdu", prefix+"51");
        logWhoToUri.put("Chojor Radha", prefix+"52");
        logWhoToUri.put("Elie Roux", prefix+"53");
        logWhoToUri.put("Gelek Gyatso", prefix+"54");
        logWhoToUri.put("Gelek.Gyatso", prefix+"54");
        logWhoToUri.put("Georgia Kashnig", prefix+"55");
        logWhoToUri.put("jw", prefix+"56");
        logWhoToUri.put("monastery import", prefix+"57");
        logWhoToUri.put("mongol import", prefix+"58");
        logWhoToUri.put("Palri", prefix+"59");
        logWhoToUri.put("Palri Nepal", prefix+"59");
        logWhoToUri.put("Palri Parkhang", prefix+"59");
        logWhoToUri.put("Palri Parkhang Nepal", prefix+"59");
        logWhoToUri.put("Palris", prefix+"59");
        logWhoToUri.put("palris", prefix+"59");
        logWhoToUri.put("places-ensure-contains-has-name", prefix+"60");
        logWhoToUri.put("Shallima Dellefant", prefix+"61");
        logWhoToUri.put("sherabling", prefix+"62"); // maybe NT?
        logWhoToUri.put("Shoko Mekata", prefix+"63");
        logWhoToUri.put("Stacey Van Vleet", prefix+"64");
        logWhoToUri.put("Tsering Dhondup", prefix+"65");
        logWhoToUri.put("Tsering Dondrup", prefix+"65");
        logWhoToUri.put("Tserings Wangdag and Dhondup", prefix+"65"); // same ?
    }
    
	public static void setPrefixes(Model m) {
		m.setNsPrefix("", ONTOLOGY_PREFIX);
		m.setNsPrefix("adm", ADMIN_PREFIX);
		m.setNsPrefix("bdd", DATA_PREFIX);
		m.setNsPrefix("bdr", RESOURCE_PREFIX);
		m.setNsPrefix("root", ROOT_PREFIX);
		m.setNsPrefix("per", PERSON_PREFIX);
		m.setNsPrefix("prd", PRODUCT_PREFIX);
		m.setNsPrefix("wor", WORK_PREFIX);
		m.setNsPrefix("out", OUTLINE_PREFIX);
		m.setNsPrefix("plc", PLACE_PREFIX);
		m.setNsPrefix("top", TOPIC_PREFIX);
		m.setNsPrefix("lin", LINEAGE_PREFIX);
		m.setNsPrefix("vol", VOLUMES_PREFIX);
		m.setNsPrefix("crp", CORPORATION_PREFIX);
		m.setNsPrefix("ofc", OFFICE_PREFIX);
		m.setNsPrefix("owl", OWL_PREFIX);
		m.setNsPrefix("rdf", RDF_PREFIX);
		m.setNsPrefix("rdfs", RDFS_PREFIX);
		m.setNsPrefix("xsd", XSD_PREFIX);
		m.setNsPrefix("desc", DESCRIPTION_PREFIX);
	}
	
	public static String getJsonLDContext() {
		return "{"
				+"\"@vocab\" : \""+ONTOLOGY_PREFIX+"\","
				+"\"\" : \""+ONTOLOGY_PREFIX+"\","
				+"\"adm\" : \""+ADMIN_PREFIX+"\","
				+"\"bdd\" : \""+DATA_PREFIX+"\","
				+"\"bdr\" : \""+RESOURCE_PREFIX+"\","
                +"\"rdf\" : \""+RDF_PREFIX+"\","
                +"\"owl\" : \""+OWL_PREFIX+"\","
                +"\"xsd\" : \""+XSD_PREFIX+"\","
                +"\"rdfs\" : \""+RDFS_PREFIX+"\","
			    +"\"crp\" : \""+CORPORATION_PREFIX+"\","
			    +"\"prd\" : \""+PRODUCT_PREFIX+"\","
			    +"\"plc\" : \""+PLACE_PREFIX+"\","
			    +"\"ofc\" : \""+OFFICE_PREFIX+"\","
			    +"\"out\" : \""+OUTLINE_PREFIX+"\","
			    +"\"lin\" : \""+LINEAGE_PREFIX+"\","
			    +"\"top\" : \""+TOPIC_PREFIX+"\","
			    +"\"wor\" : \""+WORK_PREFIX+"\","
			    +"\"per\" : \""+PERSON_PREFIX+"\","
			    +"\"vol\" : \""+VOLUMES_PREFIX+"\","
			    +"\"desc\" : \""+DESCRIPTION_PREFIX+"\""
			    +"}";
	}
	
	public static String normalizeDescription(String desc) {
	    switch (desc) {
	    case "chapter": 	          return "work_desc_chapters";
	    case "chapters":              return "work_desc_chapters";
	    case "content":               return "work_desc_contents";
	    case "contents":              return "work_desc_contents";
	    case "completionDate":        return "work_desc_completionDate";
	    case "date":                  return "work_desc_date";
	    case "errata":                return "work_desc_errata";
	    case "extent":                return "work_desc_extent";
	    case "id":                    return "work_desc_id";
	    case "libraryOfCongress":     return "work_desc_libraryOfCongress";
	    case "location":              return "work_desc_location";
	    case "remarks":               return "work_desc_remarks";
	    case "room":                  return "work_desc_room";
	    case "snar_bstan_number":     return "workKaTenSiglaN";
	    case "snr_thang_number":      return "workKaTenSiglaN";
	    case "snar_thang_number":     return "workKaTenSiglaN"; 
	    case "gser_bris_numbr":       return "workKaTenSiglaG";
	    case "gser_birs_number":      return "workKaTenSiglaG";
	    case "gse_bris_number":       return "workKaTenSiglaG";
	    case "sger_bris_number":      return "workKaTenSiglaG";
	    case "gser_bri_numer":        return "workKaTenSiglaG";
	    case "gser_dris_number":      return "workKaTenSiglaG";
	    case "gser_bri_number":       return "workKaTenSiglaG";
	    case "gser_bris_nimber":      return "workKaTenSiglaG";
	    case "gser_bris_number":      return "workKaTenSiglaG";
	    case "colopho":               return "workColophon";
	    case "colophon":              return "workColophon";
	    case "colophn":               return "workColophon";
	    case "colophone":             return "workColophon";
        case "sde_gde_number":        return "workKaTenSiglaD";
        case "de_dge_number":         return "workKaTenSiglaD";
        case "sdg_dge_number":        return "workKaTenSiglaD";
        case "sdr_dge_number":        return "workKaTenSiglaD";
        case "sde_dge_number":        return "workKaTenSiglaD";
        case "lhasa_number":          return "workKaTenSiglaH";
	    case "stog_numbe":            return "workKaTenSiglaS";
	    case "stog_unmber":           return "workKaTenSiglaS";
	    case "StogNumber":            return "workKaTenSiglaS";
	    case "stog_number":           return "workKaTenSiglaS";
	    case "toh_number":            return "workKaTenRefToh";
	    case "toh":                   return "workKaTenRefToh";
	    case "otani_number":          return "workKaTenSiglaQ";
	    case "otani":                 return "workKaTenSiglaQ";
	    case "otani_beijing":         return "workKaTenSiglaQ";
	    case "SheyNumber":            return "workKaTenSiglaZ";
        case "shey_number":           return "workKaTenSiglaZ";
	    case "rKTsReference":         return "workKaTenRefrKTs";
	    case "bon_bka_gyur_number":   return "workKaTenSiglaBon";
	    case "urga_number":           return "workKaTenSiglaU";
	    case "IsIAO":                 return "workRefIsIAO";
	    case "catalogue_number":      return "workRefChokLing";
        case "nameLex":               return "place_name_lex";
        case "nameKR":                return "place_name_kr";
        case "gbdist":                return "place_gb_dist";
        case "town_syl":              return "place_town_syl";
        case "town_py":               return "place_town_py";
        case "town_ch":               return "place_town_ch";
        case "prov_py":               return "place_prov_py";
        case "gonpaPerEcumen":        return "place_gonpa_per1000";
        case "gonpaPer1000":          return "place_gonpa_perEcumen";
        case "dist_py":               return "place_dist_py";
        case "ondisk":
        case "onDisk":
        case "dld":
        case "dpl480":
        case "featured":
            return "__ignore";
	    default:
	        return null;
	    }
	}
	
	public static Literal getLitFromUri(Model m, String uri) {
		//return m.createLiteral(m.shortForm(uri));
		return m.createLiteral(uri);
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type, String index) {
		String mainName = main.getLocalName();
		return prefix+mainName+"_"+type+index;
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type, int index) {
		return getSubResourceName(main, prefix, type, String.valueOf(index));
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type) {
		return getSubResourceName(main, prefix, type, "");
	}
	
	public static void addLabel(Model m, Resource r, Literal l) {
		m.add(r, RDFS.label, l);
	}
	
	public static String normalizePropName(String toNormalize, String targetType) {
		String res = toNormalize.trim().replace("'", "").replace(" ", "_");
		if (targetType == "Class") {
			res = res.substring(0,1).toUpperCase() + res.substring(1);
		} else {
		    res = res.substring(0,1).toLowerCase() + res.substring(1);
		}
		if (targetType == "description") {
            res = normalizeDescription(res);
        }
		return res;
	}
	
	public static Pattern whiteSpacePattern = Pattern.compile("[\\s\\p{Cntrl}]+", Pattern.UNICODE_CHARACTER_CLASS);
	
	public static String normalizeString(String toNormalize, boolean keepSpaces) {
	    if (keepSpaces)
	        return toNormalize.trim();
	    return whiteSpacePattern.matcher(toNormalize).replaceAll(" ").trim();
	}
	
	public static String normalizeString(String toNormalize) {
	    return normalizeString(toNormalize, false);
	}
	
	public static void addNote(Model m, Element e, Resource r, int i, Property p, Literal l) {
	    // some empty <note/> appear sometimes
	    if (e.getAttribute("work").isEmpty() && e.getAttribute("location").isEmpty() && e.getTextContent().trim().isEmpty()) {
	        return;
	    }
		Resource note = m.createResource();
		// really?
		//m.add(note, RDF.type, m.getProperty(BDO, "Note"));
		Property prop = m.getProperty(BDO, "note");
		Literal lit;
		if (p == null) {
		    m.add(r, prop, note);
		} else {
		    Resource axiom = m.createResource(OWL2.Axiom);
		    axiom.addProperty(OWL2.annotatedSource, r);
		    axiom.addProperty(OWL2.annotatedProperty, p);
		    axiom.addProperty(OWL2.annotatedTarget, l);
		    axiom.addLiteral(prop, note);
		}
		String value = e.getAttribute("work");
		if (!value.isEmpty()) {
			prop = m.getProperty(BDO, "noteWork");
			m.add(note, prop, m.createResource(BDR+value));
		}
		value = e.getAttribute("location");
		if (!value.isEmpty()) {
			prop = m.getProperty(BDO, "noteLoc");
			lit = m.createLiteral(value);
			m.add(note, prop, lit);
		}
		prop = m.getProperty(BDO, "noteText");
		lit = getLiteral(e, "en", m, "note", r.getLocalName(), r.getLocalName(), false);
		if (lit != null)
		    m.add(note, prop, lit);
	}
	
	public static void addNotes(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "note");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addNote(m, current, r, i, null, null);
		}
	}
	
	public static void addNotes(Model m, Element e, Resource r, Property p, Literal l, String XsdPrefix) {
	    List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "note");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            addNote(m, current, r, i, p, l);
        }
	}
	
	public static void addExternal(Model m, Element e, Resource r, int i) {
		Resource ext = m.createResource();
		Property prop = m.createProperty(BDO+"external");
		Literal lit;
		m.add(r, prop, ext);
		String value = e.getAttribute("data");
		if (!value.isEmpty()) {
			prop = m.createProperty(BDO+"external_data");
			m.add(ext, prop, m.createTypedLiteral(value, XSDDatatype.XSDanyURI));
			// catch errors?
		}
		value = e.getAttribute("source");
		if (!value.isEmpty()) {
			prop = m.createProperty(BDO+"external_source");
			lit = m.createLiteral(value);
			m.add(ext, prop, lit);
		}
		value = e.getTextContent().trim();
		if (!value.isEmpty()) {
			prop = m.createProperty(BDO+"external_content");
			lit = m.createLiteral(value, "en");
			m.add(ext, prop, lit);
		}
	}
	
	public static void addExternals(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "external");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addExternal(m, current, r, i);
		}
	}
	
	public static Literal literalFromXsdDate(Model m, String s) {
		// was quite difficult to find...
	    XSDDateTime dateTime = (XSDDateTime)XSDDatatype.XSDdateTime.parse(s);
		return m.createTypedLiteral(dateTime);
	}
	
	public static void addLogEntry(Model m, Element e, Resource r) {
		if (e == null) return;
		Resource logEntry = m.createResource();
		//m.add(logEntry, RDF.type, m.getProperty(BDO+"LogEntry"));
		Property prop = m.getProperty(BDO, "log_entry");
		m.add(r, prop, logEntry);
		String value = e.getAttribute("when");
		if (!value.isEmpty()) {
			prop = m.createProperty(BDO+"logWhen");
			try {
			    m.add(logEntry, prop, literalFromXsdDate(m, value));
			} catch (DatatypeFormatException ex) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "log_entry", "cannot convert log date properly, original date: `"+value+"`");
			}
		}
		value = normalizeString(e.getAttribute("who"));
		if (!value.isEmpty() && !value.equals("unspecified")) {
			prop = m.createProperty(BDO+"logWho");
			String uri = logWhoToUri.get(value);
			if (uri == null) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "log_who", "unknown who: "+value);    
			} else {
			    m.add(logEntry, prop, m.createLiteral(value, "en"));
			}
		}
		value = normalizeString(e.getTextContent(), true);
		if (!value.isEmpty()) {
			prop = m.createProperty(BDO+"logMessage");
			m.add(logEntry, prop, m.createLiteral(value, "en"));
		}
		
	}
	
	public static void addLog(Model m, Element e, Resource r, String XsdPrefix) {
		NodeList nodeList = e.getElementsByTagNameNS(XsdPrefix, "log");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element log = (Element) nodeList.item(i);
			NodeList logEntriesList = log.getElementsByTagNameNS(XsdPrefix, "entry");
			for (int j = 0; j < logEntriesList.getLength(); j++) {
				Element logEntry = (Element) logEntriesList.item(j);
				addLogEntry(m, logEntry, r);
			}
			logEntriesList = log.getElementsByTagName("entry");
			for (int k = 0; k < logEntriesList.getLength(); k++) {
				Element logEntry = (Element) logEntriesList.item(k);
				addLogEntry(m, logEntry, r);
			}
		}
	}
	
	public static void addNames(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "name");
		// TODO: test
		Map<String,Boolean> labelDoneForLang = new HashMap<>();
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			if (current.getTextContent().trim().isEmpty()) continue;
			// not sure about the second one in case of an outline
            Literal l = getLiteral(current, "bo-x-ewts", m, "name", r.getLocalName(), r.getLocalName());
            if (l != null) {
                r.addProperty(m.getProperty(BDO, "name"), l);
                if (guessLabel && !labelDoneForLang.containsKey(l.getLanguage())) {
                    r.addProperty(m.getProperty(RDFS_PREFIX, "label"), l);
                    labelDoneForLang.put(l.getLanguage(), true);
                }
            }
		}
	}
	
   public static void addNames(Model m, Element e, Resource r, String XsdPrefix) {
       addNames(m, e, r, XsdPrefix, true);
    }
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "description");
		Map<String,Boolean> labelDoneForLang = new HashMap<>();
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			Literal l = getLiteral(current, "en", m, "description", r.getLocalName(), r.getLocalName());
			if (l == null) continue;
			String type = current.getAttribute("type");
	         if (type.equals("nameLex")) {
                String placeId = r.getLocalName();
                current.setTextContent(current.getTextContent().replace(placeId, ""));
            }
			if (type.isEmpty()) type = "noType";
			String newType = normalizePropName(type, "description");
			if (newType != null && newType.equals("__ignore")) continue;
			if (newType == null || newType.isEmpty()) {
			    ExceptionHelper.logException(ExceptionHelper.ET_DESC, r.getLocalName(), r.getLocalName(), "description", "unhandled description type: "+type);
			    continue;
			}
			type = newType;
			// onDisk is treated separately in imageGroups, TODO: check if it appears somewhere else
			if (type.equals("date")) 
			    ExceptionHelper.logException(ExceptionHelper.ET_DESC, r.getLocalName(), r.getLocalName(), "description", "a description of type date should be changed into something meaningful");
			if (type.equals("note")) {
		        Resource note = m.createResource();
		        m.add(note, RDF.type, m.createProperty(BDO+"Note"));
	            m.add(r, m.getProperty(BDO+"note"), note);
	            m.add(note, m.getProperty(BDO+"noteText"), l);
			    continue;
			}
			// for product and office the name is the first description type="contents", and we don't want to keep it in a description
            if (guessLabel && !labelDoneForLang.containsKey(l.getLanguage()) && type.equals("work_desc_contents")) {
                r.addProperty(m.getProperty(RDFS_PREFIX, "label"), l);
                labelDoneForLang.put(l.getLanguage(), true);
                continue;
            }
            Property prop = m.getProperty(BDO,type);
            r.addProperty(prop, l);
		}
	}
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix) {
		addDescriptions(m, e, r, XsdPrefix, false);
	}
	
       public static void addTitles(Model m, Resource main, Element root, String XsdPrefix, boolean guessLabel) {
            List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "title");
            Map<String,Boolean> labelDoneForLang = new HashMap<>();
            for (int i = 0; i < nodeList.size(); i++) {
                Element current = (Element) nodeList.get(i);
                if (current.getTextContent().trim().isEmpty()) continue;
                String type = current.getAttribute("type");
                if (type.isEmpty()) {
                    type = "bibliographicalTitle";
                }
                Property prop = m.getProperty(WORK_PREFIX, type);
                Literal l = getLiteral(current, "bo-x-ewts", m, "description", main.getLocalName(), main.getLocalName());
                if (l == null) continue;
                main.addProperty(prop,  l);
                if (guessLabel && !labelDoneForLang.containsKey(l.getLanguage())) {
                    main.addProperty(m.getProperty(RDFS_PREFIX, "label"), l);
                    labelDoneForLang.put(l.getLanguage(), true);
                }
            }
        }
       
       public static void addSubjects(Model m, Resource main, Element root, String XsdPrefix) {
           List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "subject");
           for (int i = 0; i < nodeList.size(); i++) {
               Element current = (Element) nodeList.get(i);
               String value = current.getAttribute("type");
               if (value.isEmpty()) {
                   value = "noType";
               }
               Property prop = m.getProperty(ROOT_PREFIX, "subject_"+value);
               value = current.getAttribute("class").trim();
               if (!value.isEmpty()) {
                   value =  CommonMigration.getPrefixFromRID(value)+value;
                   m.add(main, prop, m.createResource(value));
               }
           }
       }
       
       private static int addLocationIntOrString(Model m, Resource main, Resource loc, Element current, String attributeName, String propname) {
           String value = current.getAttribute(attributeName).trim();
           int res = -1;
           if (!value.isEmpty()) {
               try {
                   int intval = Integer.parseInt(value);
                   if (intval < 1) {
                       ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                       m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createLiteral(value));
                   } else {
                       m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createTypedLiteral(intval, XSDDatatype.XSDpositiveInteger));
                       res = intval;
                   }
               } catch (NumberFormatException e) {
                   ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                   m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createLiteral(value));
               }
           }
           return res;
       }
       
       public static void addLocations(Model m, Resource main, Element root, String XsdPrefix, String propname, String propname1, String propname2, String workId) {
           List<Element> nodeList = CommonMigration.getChildrenByTagName(root, XsdPrefix, "location");
           int i;
           int volume1 = -1;
           int page1 = -1;
           for (i = 0; i < nodeList.size(); i++) {
               Element current = (Element) nodeList.get(i);
               
               String value = getSubResourceName(main, WORK_PREFIX, "Location", i+1);
               Resource loc = m.createResource(value);
               
               value = current.getAttribute("type");
               if (value.isEmpty()) value = "page";
               value = value.equals("page") ? "LocationByPage" : "LocationByFolio";
               m.add(loc, RDF.type, m.createResource(WORK_PREFIX+value));
               String localName = main.getLocalName();
               // convention: if propname2 is not null, then we're in the case where the first property
               // is beginsAt and the second is endsAt, we handle it accordingly
               if (propname1 != null && nodeList.size() > 1) {
                   switch (i) {
                   case 0:
                       m.add(main, m.getProperty(propname1), loc);
                       break;
                   case 1:
                       m.add(main, m.getProperty(propname2), loc);
                       break;
                   case 2:
                       ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, main.getLocalName(), "location", "too many locations, it should only have 2");
                   default:
                       m.add(main, m.getProperty(propname), loc);
                   }
               } else {
                   m.add(main, m.getProperty(propname), loc);
               }
               
               value = current.getAttribute("work");
               if (!value.isEmpty()) {
                   m.add(loc, m.getProperty(WORK_PREFIX, "work"), m.createResource(WORK_PREFIX+value));
               }
               
               int volume = addLocationIntOrString(m, main, loc, current, "vol", "volume");
               if (i == 0) volume1 = volume;
               if (i == 1 && propname1 != null && volume != -1 && volume1 != -1 && volume < volume1) {
                   ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, main.getLocalName(), "location", "end location volume is before beginning location volume");
               }
               int page = addLocationIntOrString(m, main, loc, current, "page", "page");
               if (i == 0) page1 = page;
               if (i == 1 && propname1 != null && page != -1 && page1 != -1 && page < page1 && volume == volume1) {
                   ExceptionHelper.logException(ExceptionHelper.ET_OUTLINE, workId, main.getLocalName(), "location", "end location page is before beginning location");
               }
               addLocationIntOrString(m, main, loc, current, "phrase", "phrase");
               addLocationIntOrString(m, main, loc, current, "line", "line");
               
               value = current.getAttribute("side");
               if (!value.isEmpty())
                   m.add(loc, m.getProperty(WORK_PREFIX, "side"), m.createLiteral(value));
               
           }
       }
       
       public static void addException(Model m, Resource r, String exception, int type) {
           addException(m, r, exception);
       }
       
       public static void addException(Model m, Resource r, String exception) {
           m.add(r, m.getProperty(ROOT_PREFIX+"migration_exception"), m.createLiteral(exception));
           exception = "Error in resource "+r.getLocalName()+": "+exception;
           MigrationHelpers.writeLog(exception);
       }
	
       public static void addPropWithType(Model m, Resource r, String propName, String typeName, Property typeProperty, Literal l) {
           Property name = m.getProperty(ONTOLOGY_PREFIX+"name");
           r.addProperty(m.getProperty(ONTOLOGY_PREFIX+propName), 
                   m.createResource()
                       .addProperty(typeProperty, m.getResource(RESOURCE_PREFIX+typeName))
                       .addProperty(name, l));
       }
       
       public static void addStatus(Model m, Resource r, String status) {
           if (status == null || status.isEmpty()) return;
           String statusName = "Status"+status.substring(0, 1).toUpperCase() + status.substring(1);
           r.addProperty(m.getProperty(ONTOLOGY_PREFIX+"status"), m.getResource(ONTOLOGY_PREFIX+statusName));
       }
       
	public static String getPrefixFromRID(String rid) {
	    // warning: should be made more reliable
	    if (rid.startsWith("W")) return WORK_PREFIX;
	    if (rid.startsWith("T")) return TOPIC_PREFIX;
	    if (rid.startsWith("P")) return PERSON_PREFIX;
	    if (rid.startsWith("G")) return PLACE_PREFIX;
	    if (rid.startsWith("R")) return OFFICE_PREFIX;
	    if (rid.startsWith("L")) return LINEAGE_PREFIX;
	    if (rid.startsWith("C")) return CORPORATION_PREFIX;
	    if (rid.startsWith("O")) return OUTLINE_PREFIX;
	    throw new IllegalArgumentException("cannot infer prefix from RID "+rid);
	}
	
	public static String getBCP47Suffix(String encoding) {
		switch(encoding) {
		case "extendedWylie":
			return "-x-ewts";
		case "wadeGiles":
			return "-x-wade";
		case "pinyin":
			return "-Latn-pinyin";
		case "libraryOfCongress":
			return "-x-loc";
		case "native":
			return "";
		case "none":
            return "";
		case "rma":
			return "-x-rma";
		case "sansDiacritics":
			return "-x-ndia";
		case "withDiacritics":
			return "-x-wdia";
		case "transliteration":
			return "-x-trans";
		case "acip":
			return "-x-acip";
		case "tbrcPhonetic":
			return "-x-tbrc";
		case "alternatePhonetic":
			return "-x-alt";
		case "syllables":
			return "-x-syx";
		case "":
			return "";
		default:
		    throw new IllegalArgumentException("unknown encoding: "+encoding);
		}
	}
	
	public static String getIso639(String language) throws IllegalArgumentException {
		switch(language) {
		case "tibetan":
			return "bo";
		case "pali":
            return "pi";
		case "english":
			return "en";
		case "chinese":
			return "zh";
		case "sanskrit":
			return "sa";
		case "mongolian":
			return "mn";
		case "french":
			return "fr";
		case "russian":
			return "ru";
		case "zhangZhung":
			return "xzh";// iso 639-3
		case "dzongkha":
			return "dz";
		case "miNyag":
			return "mvm"; // not really sure...
		case "german":
			return "de";
		case "":
            return "en";
		case "japanese":
			return "ja";
		case "unspecified":
			// Jena checks tags against https://tools.ietf.org/html/rfc3066 stating:
		    // You SHOULD NOT use the UND (Undetermined) code unless the protocol
		    // in use forces you to give a value for the language tag, even if
		    // the language is unknown.  Omitting the tag is preferred.
		    throw new IllegalArgumentException("unknown language: "+language);
		default:
		    throw new IllegalArgumentException("unknown language: "+language);
		}
	}
	
	public static String getBCP47(String language, String encoding) throws IllegalArgumentException {
		if (language == null || language.isEmpty()) {
			if (encoding != null && !encoding.isEmpty()) {
			    if (encoding.equals("extendedWylie")) return "bo-x-ewts";
			    if (encoding.equals("tbrcPhonetic")) return "bo-x-tbrc";
				throw new IllegalArgumentException("encoding with no language!");
			}
			return null;
		}
		return getIso639(language)+getBCP47Suffix(encoding);
	}
	
	// from http://stackoverflow.com/a/14066594/2560906
	private static boolean isAllASCII(String input) {
	    boolean isASCII = true;
	    for (int i = 0; i < input.length(); i++) {
	        int c = input.charAt(i);
	        if (c > 0x7F) {
	            isASCII = false;
	            break;
	        }
	    }
	    return isASCII;
	}

    private static boolean isAllTibetanUnicode(String input) {
        boolean isTibetan = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if ((c < 0x0F00 || c > 0x0FFF) && c != ' ') {
                isTibetan = false;
                break;
            }
        }
        return isTibetan;
    }
    
    private static boolean isAllChineseUnicode(String input) {
        boolean isChinese = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c < 0x2E00 && c != '·') {
                isChinese = false;
                break;
            }
        }
        return isChinese;
    }
	
	public static String getBCP47(Element e, String propertyHint, String RID, String subRID) {
	    String lang = e.getAttribute("lang");
	    String encoding = e.getAttribute("encoding");
	    // some entries have language in "type"
	    if (lang.isEmpty()) {
	        lang = e.getAttribute("type");
	        if (!lang.equals("sanskrit") && !lang.equals("tibetan")) lang = "";
	    }
	    String res = "en";
	    if (lang.equals("english") && (!encoding.isEmpty() && !encoding.equals("native"))) {
	        ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "mixed english + encoding `"+encoding+"` turned into `en-x-mixed`, please convert other language to unicode");
	        return "en-x-mixed";
	    }
         
	    try {
	        res = getBCP47(lang, encoding);
	    } catch (IllegalArgumentException ex) {
	        ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "lang+encoding invalid combination (`"+lang+"`, `"+encoding+"`) turned into `en` tag, exception message: "+ex.getMessage());
	    }
		String value = e.getTextContent().trim();
		// some values are wrongly marked as native instead of extendedWylie
		if (res != null && res.equals("bo") && isAllASCII(value)) {
			res = "bo-x-ewts";// could be loc?
		}
		if (res != null && !res.equals("bo") && isAllTibetanUnicode(value)) {
            res = "bo";
        }
		if (res != null && !res.equals("zh") && isAllChineseUnicode(value)) {
            res = "zh";
        }
		return res;
	}
	
	public static String getBCP47(Element e, String dflt, String propertyHint, String RID, String subRID) {
		String res = getBCP47(e, propertyHint, RID, subRID);
		if (dflt != null && (res == null || res.isEmpty())) {
			return dflt;
		}
		return res;
	}
	
	public static String normalizeTibetan(String s) {
	    String res = Normalizer.normalize(s, Normalizer.Form.NFD);
	    // Normalizer doesn't normalize deprecate characters such as 0x0F79
	    res = res.replace("\u0F79", "\u0FB3\u0F71\u0F80");
	    res = res.replace("\u0F77", "\u0FB2\u0F71\u0F80");
	    // it also doesn't normalize characters which use is discouraged:
	    res = res.replace("\u0F81", "\u0F71\u0F80");
	    res = res.replace("\u0F75", "\u0F71\u0F74");
	    res = res.replace("\u0F73", "\u0F71\u0F72");
	    return res;
	}
	
	public static boolean isStandardTibetan(String s) {
	    String[] words = s.split("[ \u0F04-\u0F14\u0F20-\u0F34\u0F3A-\u0F3F]");
	    for (String word: words) {
	        if (!speller.spell(word)) return false; 
	    }
	    return words.length > 0;
	}

	public static void addCurrentString(Element e, String dflt, Model m, Resource r, Property p, boolean addLabel) {
	    addCurrentString(e, dflt, m, r, p, addLabel, "", r);
	}
	
	public static Literal getLiteral(Element e, String dflt, Model m, String propertyHint, String RID, String subRID) {
	    return getLiteral(e, dflt, m, propertyHint, RID, subRID, true);
	}
	
	public static Literal getLiteral(Element e, String dflt, Model m, String propertyHint, String RID, String subRID, boolean normalize) {
	        String value = e.getTextContent();
	        value = normalize ? normalizeString(value) : value.trim();
	        if (value.isEmpty()) return null;
	        String tag = getBCP47(e, dflt, propertyHint, RID, subRID);
	        if (tag.equals("bo") && !value.isEmpty()) {
	            value = normalizeTibetan(value);
	            if (EwtsConverter.isCombining(value.charAt(0))) {
	                ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "Unicode string `"+value+"` starts with combining character");
	            }
	        }
	        if (tag.equals("bo-x-ewts")) {
	            List<String> conversionWarnings = new ArrayList<String>();
	            converter.toUnicode(value, conversionWarnings, true);
	            if (conversionWarnings.size() > 0) {
	                ExceptionHelper.logEwtsException(RID, subRID, propertyHint, value, conversionWarnings);
	            }
	        }
	        return m.createLiteral(value, tag);
	}
	
	public static void addCurrentString(Element e, String dflt, Model m, Resource r, Property p, boolean addLabel, String propertyHint, Resource main) {
	    String value = normalizeString(e.getTextContent());
	    if (value.isEmpty()) return;
	    String tag = getBCP47(e, dflt, propertyHint, main.getLocalName(), main.getLocalName());
        if (tag.equals("bo") && !value.isEmpty()) {
            value = normalizeTibetan(value);
            if (EwtsConverter.isCombining(value.charAt(0))) {
                addException(m, r, "Unicode string '"+value+"' starts with combining character");
            }
        }
        Literal l = m.createLiteral(value, tag);
        m.add(r, p, l);
        if (addLabel) m.add(r, RDFS.label, l);
        if (tag.equals("bo-x-ewts")) {
            List<String> conversionWarnings = new ArrayList<String>();
            String convertedValue = converter.toUnicode(value, conversionWarnings, true);
            if (conversionWarnings.size() > 0) {
                ExceptionHelper.logEwtsException(r.getLocalName(), r.getLocalName(), propertyHint, value, conversionWarnings);
            } // else {
//                value = convertedValue;
//                tag = "bo";
//            }
            // we don't convert to unicode anymore
//            l = m.createLiteral(value, tag);
//            m.add(r, p, l);
//            if (addLabel) m.add(r, RDFS.label, l);
        }
	}
	
	public static boolean documentValidates(Document document, Validator validator) {
	    return documentValidates(document, validator, "");
	}
	
	public static boolean documentValidates(Document document, Validator validator, String fileName) {
		Source xmlSource = new DOMSource(document);
		try {
            validator.validate(xmlSource);
        }
        catch (SAXException ex) {
            MigrationHelpers.writeLog("Document "+fileName+" is not valid because:");
            MigrationHelpers.writeLog(ex.getMessage());
            //ex.printStackTrace();
            return false;
        } catch (IOException e) {
            MigrationHelpers.writeLog("IO problem:");
            MigrationHelpers.writeLog(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean rdfOkInOntology(Model m, OntModel o) {
	    return rdfOkInOntology(m, o, "");
	}
	
	public static boolean rdfOkInOntology(Model m, OntModel o, String fileName) {
		o.addSubModel(m);
		ValidityReport vr;
		try {
			vr = o.validate();
		}
		catch(InternalReasonerException e) {
		    MigrationHelpers.writeLog(e.getMessage());
			return false;
		}
		if (!vr.isValid()) {
		    MigrationHelpers.writeLog("Model "+fileName+" not OK in ontology because:");
			Iterator<ValidityReport.Report> itr = vr.getReports();
			while(itr.hasNext()) {
				ValidityReport.Report report = itr.next();
				MigrationHelpers.writeLog(report.toString());
		    }
		}
		return vr.isValid();
	}
	
	// like getElementsByTagNameNS but not recursive (strange DOM doesn't have that)
	public static List<Element> getChildrenByTagName(Element parent, String xsdPrefix, String name) {
	    List<Element> nodeList = new ArrayList<Element>();
	    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getLocalName())) {
				nodeList.add((Element) child);
			}
		}
	    return nodeList;
	  }

}
