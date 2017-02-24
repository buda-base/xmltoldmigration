package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import openllet.core.exceptions.InternalReasonerException;

public class CommonMigration  {

	public static final String CORE_PREFIX = "http://onto.bdrc.io/ontologies/bdrc/";
	public static final String DESCRIPTION_PREFIX = "http://onto.bdrc.io/ontology/description#";
	public static final String ROOT_PREFIX = "http://purl.bdrc.io/ontology/root/";
	public static final String COMMON_PREFIX = "http://purl.bdrc.io/ontology/common#";
	public static final String CORPORATION_PREFIX = "http://purl.bdrc.io/ontology/coroporation#";
	public static final String LINEAGE_PREFIX = "http://purl.bdrc.io/ontology/lineage#";
	public static final String OFFICE_PREFIX = "http://purl.bdrc.io/ontology/office#";
	public static final String PRODUCT_PREFIX = "http://purl.bdrc.io/ontology/product#";
	public static final String OUTLINE_PREFIX = "http://purl.bdrc.io/ontology/outline#";
	public static final String PERSON_PREFIX = "http://purl.bdrc.io/ontology/person#";
	public static final String PLACE_PREFIX = "http://purl.bdrc.io/ontology/place#";
	public static final String TOPIC_PREFIX = "http://purl.bdrc.io/ontology/topic#";
	public static final String VOLUMES_PREFIX = "http://purl.bdrc.io/ontology/volumes#";
	public static final String WORK_PREFIX = "http://purl.bdrc.io/ontology/work/";
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String XSD_PREFIX = "http://www.w3.org/2001/XMLSchema#";
	
	public static void setPrefixes(Model m) {
		m.setNsPrefix("com", COMMON_PREFIX);
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
			    +"\"com\" : \""+COMMON_PREFIX+"\","
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
		String res = toNormalize.replace("'", "");
		if (targetType == "Class") {
			res = res.substring(0,1).toUpperCase() + res.substring(1);
		}
		return res;
	}
	
	public static void addNote(Model m, Element e, Resource r, int i) {
		String resourceName = getSubResourceName(r, ROOT_PREFIX, "Note", i+1);
		Resource note = m.createResource(resourceName);
		m.add(note, RDF.type, m.createProperty(ROOT_PREFIX+"Note"));
		Property prop = m.createProperty(ROOT_PREFIX+"note");
		Literal lit;
		m.add(r, prop, note);
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
			lit = m.createLiteral(value);
			m.add(note, prop, lit);
		}
	}
	
	public static void addNotes(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "note");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addNote(m, current, r, i);
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
			m.add(logEntry, prop, literalFromXsdDate(m, value));
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
	
	public static void addNames(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "name");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			String lang = getBCP47(current, "bo-x-ewts");
			Literal value = m.createLiteral(current.getTextContent().trim(), lang);
			Property prop = m.getProperty(ROOT_PREFIX+"name");
			m.add(r, prop, value);
			if (i == 0) {
				addLabel(m, r, value);
			}
		}
	}
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "description");
		boolean labelGuessed = !guessLabel;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			String lang = getBCP47(current, "en");
			Literal value = m.createLiteral(current.getTextContent().trim(), lang);
			String type = current.getAttribute("type");
			if (type.isEmpty()) type = "description";
			Property prop = m.getProperty(DESCRIPTION_PREFIX+type);
			m.add(r, prop, value);
			// for product, the name is the first description type="contents"
			if (!labelGuessed && type == "contents") {
				m.add(r, RDFS.label, value);
				labelGuessed = true;
			}
		}
	}
	
	public static void addDescriptions(Model m, Element e, Resource r, String XsdPrefix) {
		addDescriptions(m, e, r, XsdPrefix, false);
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
			System.out.println("unknown encoding: "+encoding);
			return "";
		}
	}
	
	public static String getIso639(String language) {
		switch(language) {
		case "tibetan":
			return "bo";
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
		case "japanese":
			return "ja";
		case "unspecified":
			// https://www.w3.org/International/questions/qa-no-language#undetermined
			return "und";
		default:
			System.out.println("unknown language: "+language);
			return "";
		}
	}
	
	public static String getBCP47(String language, String encoding) {
		if (language == null || language.isEmpty()) {
			if (encoding != null && !encoding.isEmpty()) {
				System.out.println("encoding with no language!");
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
	
	public static String getBCP47(Element e) {
		String res = getBCP47(e.getAttribute("lang"), e.getAttribute("encoding"));
		String value = e.getTextContent().trim();
		// some values are wrongly marked as native instead of extendedWylie
		if (res != null && res.equals("bo") && isAllASCII(value)) {
			res = "bo-x-ewts";// could be loc?
		}
		return res;
	}
	
	public static String getBCP47(Element e, String dflt) {
		String res = getBCP47(e);
		if (res == null || res.isEmpty()) {
			return dflt;
		}
		return res;
	}
	
	public static boolean documentValidates(Document document, Validator validator) {
		Source xmlSource = new DOMSource(document);
		try {
            validator.validate(xmlSource);
        }
        catch (SAXException ex) {
            System.err.println("Document is not valid because:");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
            return false;
        } catch (IOException e) {
        	System.err.println("IO problem:");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean rdfOkInOntology(Model m, OntModel o) {
		o.addSubModel(m);
		ValidityReport vr;
		try {
			vr = o.validate();
		}
		catch(InternalReasonerException e) {
			System.out.print(e.getMessage());
			return false;
		}
		if (!vr.isValid()) {
			Iterator<ValidityReport.Report> itr = vr.getReports();
			while(itr.hasNext()) {
				ValidityReport.Report report = itr.next();
		        System.out.print(report.toString());
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
