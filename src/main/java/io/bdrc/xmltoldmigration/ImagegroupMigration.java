package io.bdrc.xmltoldmigration;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class ImagegroupMigration {

	private static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;

	
	// for testing purposes only
	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        Resource item = m.createResource(BDR+"TestItem");
        m.add(item, RDF.type, m.getResource(BDO+"ItemImageAsset"));
        MigrateImagegroup(xmlDocument, m, item, "testItem", "1", "testItem");
        return m;
	}
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource item, String volumeName, String volumeNumber, String volumesName) {
		
		Element root = xmlDocument.getDocumentElement();
		
		//Resource main = m.createResource(VP+volumesName+"_"+volumeName);
		Resource main = m.createResource();
        //m.add(main, RDF.type, m.getResource(BDO+"Volume"));
        
        try {
            int intval = Integer.parseInt(volumeNumber);
            if (intval < 1) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup", "invalid volume number, must be a positive integer, got `"+volumeNumber+"`");
                m.add(main, m.getProperty(BDO, "volumeNumber"), m.createLiteral(volumeNumber));
            } else {
                m.add(main, m.getProperty(BDO, "volumeNumber"), m.createTypedLiteral(intval, XSDDatatype.XSDinteger));
            }
        } catch (NumberFormatException e) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup", "invalid volume number, must be a positive integer, got `"+volumeNumber+"`");
            m.add(main, m.getProperty(BDO, "volumeNumber"), m.createLiteral(volumeNumber));
        }
        
        m.add(item, m.getProperty(BDO+"itemHasVolume"), main);
        
		// adding the ondisk/onDisk description as vol:imageList
		NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (!type.equals("ondisk") && !type.equals("onDisk")) continue;
            Literal value = m.createLiteral(current.getTextContent().trim()); 
            m.add(main, m.getProperty(BDO+"volumeImageList"), value);
        }
		
        CommonMigration.addLog(m, root, item, IGXSDNS);
        CommonMigration.addDescriptions(m, root, main, IGXSDNS, false);
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "images");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("intro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "volumePagesIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("tbrcintro").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "volumePagesTbrcIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("text").trim();
            if (!value.isEmpty()) {
                if (value.startsWith("-")) {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup:text", "image group had a negative value for `text`: `"+value+"`");
                    value = "0";
                }
                m.add(main, m.getProperty(BDO, "volumePagesText"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            }
                
            
            value = current.getAttribute("total").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "volumePagesTotal"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
        }
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "qc");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            addSimpleElement("qcperson", "volumeQcPerson", current, m, main);
            addSimpleElement("qcdate", "volumeQcDate", current, m, main);
            addSimpleElement("qcnotes", "volumeQcNote", current, m, main);
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
            m.add(main, m.createProperty(ADM+propName), m.createLiteral(value));
        }
    }
	
}
