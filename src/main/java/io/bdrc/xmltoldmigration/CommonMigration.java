package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.jena.rdf.model.Model;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CommonMigration  {

	public static final String CORE_PREFIX = "http://onto.bdrc.io/ontologies/bdrc/";
	public static final String COMMON_PREFIX = "http://purl.bdrc.io/ontology/common#";
	public static final String CORPORATION_PREFIX = "http://purl.bdrc.io/ontology/coroporation#";
	public static final String LINEAGE_PREFIX = "http://purl.bdrc.io/ontology/lineage#";
	public static final String OFFICE_PREFIX = "http://purl.bdrc.io/ontology/office#";
	public static final String OUTLINE_PREFIX = "http://purl.bdrc.io/ontology/outline#";
	public static final String PERSON_PREFIX = "http://purl.bdrc.io/ontology/person#";
	public static final String PLACE_PREFIX = "http://purl.bdrc.io/ontology/place#";
	public static final String TOPIC_PREFIX = "http://purl.bdrc.io/ontology/topic#";
	public static final String WORK_PREFIX = "http://purl.bdrc.io/ontology/work#";
	public static final String OWL_PREFIX = "http://www.w3.org/2002/07/owl#";
	public static final String RDF_PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String RDFS_PREFIX = "http://www.w3.org/2000/01/rdf-schema#";
	public static final String XSD_PREFIX = "http://www.w3.org/2001/XMLSchema#";
	
	public static void setPrefixes(Model m) {
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

	
	public static String getBCP47(String language, String encoding) {
		//TODO
		return "bo-x-ewts";
	}
	
	public static boolean documentValidAgainstXSD(Document document, String xsdName) {
		String xsdFullName = "src/main/resources/xsd/"+xsdName+".xsd";
		SchemaFactory factory = 
	            SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
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
            validator.validate(new DOMSource(document));
        }
        catch (SAXException ex) {
            System.out.println("xxx is not valid because ");
            System.out.println(ex.getMessage());
            return false;
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
}
