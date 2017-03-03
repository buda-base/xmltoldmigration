package io.bdrc.xmltoldmigration;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ImagegroupMigration {

	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String VP = CommonMigration.VOLUMES_PREFIX;
	private static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";

	
	// for testing purposes only
	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        Resource volumes = m.createResource(VP+"TestVolumes");
        m.add(volumes, RDF.type, m.getResource(VP+"Volumes"));
        MigrateImagegroup(xmlDocument, m, volumes, "testVolume");
        return m;
	}
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource volumes, String volumeName) {
		
		Element root = xmlDocument.getDocumentElement();
		
		Resource main = m.createResource(VP+volumeName);
        m.add(main, RDF.type, m.getResource(VP+"Volume"));
        
        m.add(volumes, m.getProperty(VP+"hasVolume"), main);
        
		// adding the ondisk/onDisk description as vol:imageList
		NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (!type.equals("ondisk") && !type.equals("onDisk")) continue;
            Literal value = m.createLiteral(current.getTextContent().trim()); 
            m.add(main, m.getProperty(VP+"imageList"), value);
        }
		
        CommonMigration.addLog(m, root, volumes, IGXSDNS);
        CommonMigration.addDescriptions(m, root, main, IGXSDNS, false);
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "images");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("intro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_intro"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            
            value = current.getAttribute("tbrcintro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_tbrcintro"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            
            value = current.getAttribute("text").trim();
            if (!value.isEmpty()) {
                if (value.startsWith("-")) {
                    CommonMigration.addException(m, main, "image group had a negative value for 'text': "+value);
                    value = "0";
                }
                m.add(main, m.getProperty(VP+"pages_text"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
            }
                
            
            value = current.getAttribute("total").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(VP+"pages_total"), m.createTypedLiteral(value, XSDDatatype.XSDnonNegativeInteger));
        }
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "qc");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            addSimpleElement("qcperson", "qcperson", current, m, main);
            addSimpleElement("qcdate", "qcdate", current, m, main);
            addSimpleElement("qcnotes", "qcnotes", current, m, main);
        }

	}

	public static void addSimpleElement(String elementName, String propName, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) {
                return;
            }
            m.add(main, m.createProperty(VP+propName), m.createLiteral(value));
        }
    }
	
}
