package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class CommonMigration  {

	public static final String CORE_PREFIX = "http://onto.bdrc.io/ontologies/bdrc/";
	public static final String ROOT_PREFIX = "http://purl.bdrc.io/ontology/root/";
	public static final String COMMON_PREFIX = "http://purl.bdrc.io/ontology/common#";
	public static final String CORPORATION_PREFIX = "http://purl.bdrc.io/ontology/coroporation#";
	public static final String LINEAGE_PREFIX = "http://purl.bdrc.io/ontology/lineage#";
	public static final String OFFICE_PREFIX = "http://purl.bdrc.io/ontology/office#";
	public static final String OUTLINE_PREFIX = "http://purl.bdrc.io/ontology/outline#";
	public static final String PERSON_PREFIX = "http://purl.bdrc.io/ontology/person#";
	public static final String PLACE_PREFIX = "http://purl.bdrc.io/ontology/place#";
	public static final String TOPIC_PREFIX = "http://purl.bdrc.io/ontology/topic#";
	public static final String WORK_PREFIX = "http://purl.bdrc.io/ontology/work/";
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String XSD_PREFIX = "http://www.w3.org/2001/XMLSchema#";
	
	public static PrefixMapping pm;
	
	public static void setPrefixes(Model m) {
		m.setNsPrefix("", ROOT_PREFIX);
		m.setNsPrefix("com", COMMON_PREFIX);
		m.setNsPrefix("per", PERSON_PREFIX);
		m.setNsPrefix("wor", WORK_PREFIX);
		m.setNsPrefix("out", OUTLINE_PREFIX);
		m.setNsPrefix("plc", PLACE_PREFIX);
		m.setNsPrefix("top", TOPIC_PREFIX);
		m.setNsPrefix("lin", LINEAGE_PREFIX);
		m.setNsPrefix("crp", CORPORATION_PREFIX);
		m.setNsPrefix("ofc", OFFICE_PREFIX);
		m.setNsPrefix("owl", OWL_PREFIX);
		m.setNsPrefix("rdf", RDF_PREFIX);
		m.setNsPrefix("rdfs", RDFS_PREFIX);
		m.setNsPrefix("xsd", XSD_PREFIX);
	}
	
	public static Literal getLitFromUri(Model m, String uri) {
		return m.createLiteral(m.shortForm(uri));
	}
	
//	private static Map<String, Property>  generatedProperties = new HashMap<String, Property>(); 
//	
//	public static Property getProperty(String prefix, String name) {
//		Property res = generatedProperties.get(prefix+name);
//		if (res == null) {
//			res = ResourceFactory.createProperty(prefix, name);
//			generatedProperties.put(prefix+name, res);
//		}
//		return res;
//	}
	
	public static String normalizePropName(String toNormalize, String targetType) {
		String res = toNormalize.replace("'", "");
		if (targetType == "Class") {
			res = res.substring(0,1).toUpperCase() + res.substring(1);
		}
		return res;
	}
	
	public static void addNote(Model m, Element e, Resource r) {
		Resource note = m.createResource(new AnonId());
		m.add(note, RDF.type, getLitFromUri(m, ROOT_PREFIX+"Note"));
		Property prop = m.createProperty(ROOT_PREFIX+"note");
		Literal lit;
		m.add(r, prop, note);
		String value = e.getAttribute("work");
		if (value != "") {
			prop = m.createProperty(ROOT_PREFIX+"note_work");
			lit = getLitFromUri(m, WORK_PREFIX+value);
			m.add(note, prop, lit);
		}
		value = e.getAttribute("location");
		if (value != "") {
			prop = m.createProperty(ROOT_PREFIX+"note_location");
			lit = m.createLiteral(value);
			m.add(note, prop, lit);
		}
		value = e.getTextContent();
		if (value != "") {
			prop = m.createProperty(ROOT_PREFIX+"note_content");
			lit = m.createLiteral(value);
			m.add(note, prop, lit);
		}
	}
	
	public static void addNotes(Model m, Element e, Resource r, String XsdPrefix) {
		NodeList nodeList = e.getElementsByTagNameNS(XsdPrefix, "note");
		for (int i = 0; i < nodeList.getLength(); i++) {
			addNote(m, (Element) nodeList.item(i), r);
		}
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
		if (language == "" || language == null) {
			if (encoding != "" && encoding != null) {
				System.out.println("encoding with no language!");
			}
			return null;
		}
		return getIso639(language)+getBCP47Suffix(encoding);
	}
	
	public static String getBCP47(Element e) {
		return getBCP47(e.getAttribute("lang"), e.getAttribute("encoding"));
	}
	
	public static boolean documentValidAgainstXSD(Document document, String xsdName) {
		String xsdFullName = "src/main/resources/xsd/"+xsdName+".xsd";
		Source xmlSource = new DOMSource(document);
		SchemaFactory factory = 
	            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema;
		try {
			schema = factory.newSchema(new File(xsdFullName));
		}
		catch (SAXException ex) {
			System.out.println("xsd file looks invalid...");
			return false;
		}
		Validator validator = schema.newValidator();
		try {
            validator.validate(xmlSource);
        }
        catch (SAXException ex) {
            System.out.println("Document is not valid because:");
            System.out.println(ex.getMessage());
            ex.printStackTrace();
            return false;
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
}
