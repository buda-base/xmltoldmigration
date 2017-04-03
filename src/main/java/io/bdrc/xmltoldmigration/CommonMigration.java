package io.bdrc.xmltoldmigration;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

import openllet.core.exceptions.InternalReasonerException;

public class CommonMigration  {

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
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String XSD_PREFIX = "http://www.w3.org/2001/XMLSchema#";
	
	public static final Converter converter = new Converter();
	
	public static void setPrefixes(Model m) {
		m.setNsPrefix("", ROOT_PREFIX);
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
				+"\"@vocab\" : \""+ROOT_PREFIX+"\","
				//+"\"@language\" : \"en\"," // ?
			    +"\"crp\" : \""+CORPORATION_PREFIX+"\","
			    +"\"prd\" : \""+PRODUCT_PREFIX+"\","
			    +"\"owl\" : \""+OWL_PREFIX+"\","
			    +"\"plc\" : \""+PLACE_PREFIX+"\","
			    +"\"xsd\" : \""+XSD_PREFIX+"\","
			    +"\"rdfs\" : \""+RDFS_PREFIX+"\","
			    +"\"ofc\" : \""+OFFICE_PREFIX+"\","
			    +"\"out\" : \""+OUTLINE_PREFIX+"\","
			    +"\"lin\" : \""+LINEAGE_PREFIX+"\","
			    +"\"top\" : \""+TOPIC_PREFIX+"\","
			    +"\"rdf\" : \""+RDF_PREFIX+"\","
			    +"\"wor\" : \""+WORK_PREFIX+"\","
			    +"\"per\" : \""+PERSON_PREFIX+"\","
			    +"\"vol\" : \""+VOLUMES_PREFIX+"\","
			    +"\"desc\" : \""+DESCRIPTION_PREFIX+"\""
			    +"}";
	}
	
	public static String normalizeDescription(String desc) {
	    switch (desc) {
	    case "chapter":
	        return "chapters";
	    case "content":
            return "contents";
	    case "snar_bstan_number":
            return "snar_thang_number";
	    case "snr_thang_number":
            return "snar_thang_number";
	    case "gser_bris_numbr":
            return "gser_bris_number";
	    case "gser_birs_number":
            return "gser_bris_number";
	    case "gse_bris_number":
            return "gser_bris_number";
	    case "sger_bris_number":
            return "gser_bris_number";
	    case "gser_bri_numer":
            return "gser_bris_number";
	    case "gser_dris_number":
            return "gser_bris_number";
	    case "gser_bri_number":
            return "gser_bris_number";
	    case "gser_bris_nimber":
            return "gser_bris_number";
	    case "colopho":
            return "colophon";
	    case "colophn":
	        return "colophon";
	    case "colophone":
            return "colophon";
	    case "sde_gde_number":
            return "sde_dge_number";
	    case "de_dge_number":
            return "sde_dge_number";
	    case "sdg_dge_number":
            return "sde_dge_number";
	    case "sdr_dge_number":
            return "sde_dge_number";
	    case "stog_numbe":
            return "stog_number";
	    case "stog_unmber":
            return "stog_number";
	    case "StogNumber":
            return "stog_number";
	    case "toh_number":
            return "toh";
	    case "otani_number":
            return "otani";
	    case "SheyNumber":
            return "shey_number";
	    default:
	        return desc;
	    }
	}

	public static String addPrefixToDescription(String type) {
	    switch (type) {
        case "bon_bka_gyur_number":
        case "catalogue_number":
        case "gser_bris_number":
        case "lhasa_number":
        case "otani":
        case "otani_beijing":
        case "rKTsReference":
        case "sde_dge_number":
        case "shey_number":
        case "snar_thang_number":
        case "stog_number":
        case "toh":
        case "urga_number":
        case "vinayottaragrantha":
        case "libraryOfCongress":
        case "extent":
        case "chapters":
        case "incipit":
        case "colophon":
            return OUTLINE_PREFIX+type;
        case "nameLex":
        case "nameKR":
        case "gbdist":
        case "town_syl":
        case "town_py":
        case "town_ch":
        case "prov_py":
        case "gonpaPerEcumen":
        case "gonpaPer1000":
        case "dist_py":
            return PLACE_PREFIX+type;
        default:
            return DESCRIPTION_PREFIX+type;
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
	
	public static void addNote(Model m, Element e, Resource r, int i, Property p, Literal l) {
	    // some empty <note/> appear sometimes
	    if (e.getAttribute("work").isEmpty() && e.getAttribute("location").isEmpty() && e.getTextContent().trim().isEmpty()) {
	        return;
	    }
	    String resourceName = getSubResourceName(r, ROOT_PREFIX, "Note", i + 1);
		Resource note = m.createResource(resourceName);
		m.add(note, RDF.type, m.createProperty(ROOT_PREFIX+"Note"));
		Property prop = m.createProperty(ROOT_PREFIX+"note");
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
			prop = m.createProperty(ROOT_PREFIX+"note_work");
			m.add(note, prop, m.createResource(WORK_PREFIX+value));
		}
		value = e.getAttribute("location");
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"note_location");
			lit = m.createLiteral(value);
			m.add(note, prop, lit);
		}
		value = e.getTextContent().trim();
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"note_content");
			lit = m.createLiteral(value, "en");
			m.add(note, prop, lit);
		}
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
		String resourceName = getSubResourceName(r, ROOT_PREFIX, "External", i+1);
		Resource ext = m.createResource(resourceName);
		m.add(ext, RDF.type, m.createProperty(ROOT_PREFIX+"External"));
		Property prop = m.createProperty(ROOT_PREFIX+"external");
		Literal lit;
		m.add(r, prop, ext);
		String value = e.getAttribute("data");
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"external_data");
			m.add(ext, prop, m.createTypedLiteral(value, XSDDatatype.XSDanyURI));
		}
		value = e.getAttribute("source");
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"external_source");
			lit = m.createLiteral(value);
			m.add(ext, prop, lit);
		}
		value = e.getTextContent().trim();
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"external_content");
			lit = m.createLiteral(value);
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
		Resource logEntry = m.createResource(new AnonId());
		m.add(logEntry, RDF.type, m.createProperty(ROOT_PREFIX+"LogEntry"));
		Property prop = m.createProperty(ROOT_PREFIX+"log_entry");
		m.add(r, prop, logEntry);
		String value = e.getAttribute("when");
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"log_when");
			try {
			    m.add(logEntry, prop, literalFromXsdDate(m, value));
			} catch (DatatypeFormatException ex) {
			    addException(m, logEntry, "cannot convert log date properly, original date: '"+value+"'");
			}
		}
		value = e.getAttribute("who").trim();
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"log_who");
			m.add(logEntry, prop, m.createLiteral(value, "en"));
		}
		value = e.getTextContent().trim();
		if (!value.isEmpty()) {
			prop = m.createProperty(ROOT_PREFIX+"log_content");
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
		boolean labelGuessed = !guessLabel;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			if (current.getTextContent().trim().isEmpty()) continue;
			Property prop = m.getProperty(ROOT_PREFIX+"name");
			addCurrentString(current, "bo-x-ewts", m, r, prop, (!labelGuessed && i == 0));
		}
	}
	
   public static void addNames(Model m, Element e, Resource r, String XsdPrefix) {
       addNames(m, e, r, XsdPrefix, true);
    }
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "description");
		boolean labelGuessed = !guessLabel;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			String descriptionValue = current.getTextContent().trim();
			if (descriptionValue.isEmpty()) continue;
			String type = current.getAttribute("type");
			if (type.isEmpty()) type = "noType";
			type = normalizePropName(type, "description");
			// onDisk is treated separately in imageGroups, TODO: check if it appears somewhere else
			if (type.equals("ondisk") || type.equals("onDisk")) continue;
			if (type.equals("date")) 
			    addException(m, r, "resource contains a date description that should be changed into something meaningful");
			if (type.equals("note")) {
			    String resourceName = getSubResourceName(r, ROOT_PREFIX, "Note", i+1);
		        Resource note = m.createResource(resourceName);
		        m.add(note, RDF.type, m.createProperty(ROOT_PREFIX+"Note"));
	            m.add(r, m.getProperty(ROOT_PREFIX+"note"), note);
	            m.add(note, m.getProperty(ROOT_PREFIX+"note_content"), m.createLiteral(descriptionValue, "en"));
			    continue;
			}
			if (type.equals("nameLex")) {
			    String placeId = r.getLocalName();
			    descriptionValue = descriptionValue.replace(placeId, "").trim();
			    current.setTextContent(descriptionValue);
			}
			Property prop = m.getProperty(addPrefixToDescription(type));
			// for product and office the name is the first description type="contents"
			if (!labelGuessed && type.equals("contents")) {
			    addCurrentString(current, "en", m, r, prop, true);
			    labelGuessed = true;
			} else {
			    addCurrentString(current, "en", m, r, prop, false);
			}
		}
	}
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix) {
		addDescriptions(m, e, r, XsdPrefix, false);
	}
	
       public static void addTitles(Model m, Resource main, Element root, String XsdPrefix, boolean guessLabel) {
            List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "title");
            boolean labelGuessed = !guessLabel;
            for (int i = 0; i < nodeList.size(); i++) {
                Element current = (Element) nodeList.get(i);
                if (current.getTextContent().trim().isEmpty()) continue;
                String type = current.getAttribute("type");
                if (type.isEmpty()) {
                    type = "bibliographicalTitle";
                }
                Property prop = m.getProperty(WORK_PREFIX, type);
                addCurrentString(current, "en", m, main, prop, (i == 0 && !labelGuessed));
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
       
       private static void addLocationIntOrString(Model m, Resource main, Resource loc, Element current, String attributeName, String propname) {
           String value = current.getAttribute(attributeName).trim();
           if (!value.isEmpty()) {
               try {
                   int intval = Integer.parseInt(value);
                   if (intval < 1) {
                       addException(m, main, "in location: '"+propname+"' must be a positive integer, got '"+value+"'");
                       m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createLiteral(value));
                   } else {
                       m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createTypedLiteral(intval, XSDDatatype.XSDpositiveInteger));
                   }
               } catch (NumberFormatException e) {
                   addException(m, main, "in location: '"+propname+"' must be a positive integer, got '"+value+"'");
                   m.add(loc, m.getProperty(WORK_PREFIX, propname), m.createLiteral(value));
               }
           }
       }
       
       public static void addLocations(Model m, Resource main, Element root, String XsdPrefix, String propname) {
           List<Element> nodeList = CommonMigration.getChildrenByTagName(root, XsdPrefix, "location");
           for (int i = 0; i < nodeList.size(); i++) {
               Element current = (Element) nodeList.get(i);
               
               String value = getSubResourceName(main, WORK_PREFIX, "Location", i+1);
               Resource loc = m.createResource(value);
               
               value = current.getAttribute("type");
               if (value.isEmpty()) value = "page";
               value = value.equals("page") ? "LocationByPage" : "LocationByFolio";
               m.add(loc, RDF.type, m.createResource(WORK_PREFIX+value));
               
               m.add(main, m.getProperty(propname), loc);
               
               value = current.getAttribute("work");
               if (!value.isEmpty()) {
                   m.add(loc, m.getProperty(WORK_PREFIX, "work"), m.createResource(WORK_PREFIX+value));
               }
               
               addLocationIntOrString(m, main, loc, current, "vol", "volume");
               addLocationIntOrString(m, main, loc, current, "page", "page");
               addLocationIntOrString(m, main, loc, current, "phrase", "phrase");
               addLocationIntOrString(m, main, loc, current, "line", "line");
               
               value = current.getAttribute("side");
               if (!value.isEmpty())
                   m.add(loc, m.getProperty(WORK_PREFIX, "side"), m.createLiteral(value));
               
           }
       }
       
       public static void addException(Model m, Resource r, String exception) {
           m.add(r, m.getProperty(ROOT_PREFIX+"migration_exception"), m.createLiteral(exception));
           exception = "Error in resource "+r.getLocalName()+": "+exception;
           MigrationHelpers.writeLog(exception);
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
			return "-x-pinyin";
		case "libraryOfCongress":
			return "-x-loc";
		case "native":
			return "";
		case "none":
            return "";
		case "rma":
			return "-x-rma";
		case "sansDiacritics":
			return "-x-sans";
		case "withDiacritics":
			return "-x-with";
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
	
	public static String getBCP47(Element e, Model m, Resource r) {
	    String lang = e.getAttribute("lang");
	    String encoding = e.getAttribute("encoding");
	    // some entries have language in "type"
	    if (lang.isEmpty()) {
	        lang = e.getAttribute("type");
	        if (!lang.equals("sanskrit") && !lang.equals("tibetan")) lang = "";
	    }
	    String res = "en";
	    if (lang.equals("english") && (!encoding.isEmpty() && !encoding.equals("native"))) {
	        addException(m, r, "mixed english + encoding '"+encoding+"' turned into 'en-x-mixed', please convert other language to unicode");
	        return "en-x-mixed";
	    }
         
	    try {
	        res = getBCP47(lang, encoding);
	    } catch (IllegalArgumentException ex) {
	        addException(m, r, "lang+encoding invalid combination ('"+lang+"', '"+encoding+"') turned into 'en' language, exception message: "+ex.getMessage());
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
	
	public static String getBCP47(Element e, String dflt, Model m, Resource r) {
		String res = getBCP47(e, m, r);
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
	
	public static void addCurrentString(Element e, String dflt, Model m, Resource r, Property p, boolean addLabel) {
	    String value = e.getTextContent().trim();
	    if (value.isEmpty()) return;
	    String tag = getBCP47(e, dflt, m, r);
        if (tag.equals("bo") && !value.isEmpty()) {
            value = normalizeTibetan(value);
            if (converter.isCombining(value.charAt(0))) {
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
                String exceptionString = "Warnings during unicode conversion: "+String.join(", ", conversionWarnings);
                exceptionString += " initial ewts string: "+value;
                addException(m, r, exceptionString);
            } else {
                value = convertedValue;
                tag = "bo";
            }
            l = m.createLiteral(value, tag);
            m.add(r, p, l);
            if (addLabel) m.add(r, RDFS.label, l);
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
			if (child.getNodeType() == Node.ELEMENT_NODE&& name.equals(child.getLocalName())) {
				nodeList.add((Element) child);
			}
		}
	    return nodeList;
	  }

}
