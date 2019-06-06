package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.setPrefixes;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;


public class ImagegroupMigration {

	public static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";

    public static boolean addVolumeOf = false;
    public static boolean addItemHasVolume = true;

	
	// for testing purposes only
	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        setPrefixes(m, "item");
        Resource item = createRoot(m, BDR+"TestItem", BDO+"ItemImageAsset");
        createAdminRoot(item);
        MigrateImagegroup(xmlDocument, m, item, "testItem", 1, "testItem");
        return m;
	}
	
	public static String getVolumeOf(Document xmlDocument) {
	    Element root = xmlDocument.getDocumentElement();
        String volumeOf = null;
        
        // adding the ondisk/onDisk description as vol:imageList
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "isVolumeOf");
        for (int i = 0; i < nodeList.getLength(); ) {
            Element current = (Element) nodeList.item(i);
            volumeOf = current.getAttribute("work").trim();
            break;
        }
        return volumeOf;
	}

    public static boolean getOnDisk(Document d) {
        // adding the ondisk/onDisk description as vol:imageList
        Element root = d.getDocumentElement();
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (!type.equals("ondisk") && !type.equals("onDisk")) continue;
            return true;
        }
        return false;
    }
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource item, String volumeName, Integer volumeNumber, String volumesName) {
		
		Element root = xmlDocument.getDocumentElement();
		
        final String imageGroupRID = root.getAttribute("RID").trim();
        final String imageGroupStatus = root.getAttribute("status").trim();
		
		final String itemId = item.getLocalName();
		final String volumeId = "V"+itemId.substring(1)+"_"+imageGroupRID;
		
        Resource volR = m.createResource(BDR+volumeId);
        volR.addProperty(RDF.type, m.getResource(BDO+"VolumeImageAsset"));
        // create AdminData if it doesn't already exist - should only be created 
        // when used in MigrationTest.testImagegroup() w/o previously the containing Item
        Resource admVol = getAdminData(volR);

		admVol.addProperty(m.getProperty(ADM, "legacyImageGroupRID"), m.createLiteral(imageGroupRID));
        
        if (volumeNumber < 1) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup", "invalid volume number, must be a positive integer, got `"+volumeNumber+"`");
        }
        m.add(volR, m.getProperty(BDO, "volumeNumber"), m.createTypedLiteral(volumeNumber, XSDDatatype.XSDinteger));
        
        if (addItemHasVolume)
            m.add(item, m.getProperty(BDO+"itemHasVolume"), volR);
        
        if (addVolumeOf)
            m.add(volR, m.getProperty(BDO+"volumeOf"), item);
        
		// adding the ondisk/onDisk description as vol:imageList
		NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (!type.equals("ondisk") && !type.equals("onDisk")) continue;
            ImageListTranslation.addImageList(current.getTextContent().trim(), imageGroupRID, volumeNumber, m, volR);
        }
		
        addStatus(m, admVol, imageGroupStatus);
        CommonMigration.addLog(m, root, admVol, IGXSDNS);
        CommonMigration.addDescriptions(m, root, volR, IGXSDNS);
        admVol.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "images");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("intro").trim();
            if (!value.isEmpty())
                m.add(volR, m.getProperty(BDO, "volumePagesIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("tbrcintro").trim();
            if (!value.isEmpty())
                m.add(volR, m.getProperty(BDO, "volumePagesTbrcIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("text").trim();
            if (!value.isEmpty()) {
                if (value.startsWith("-")) {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup:text", "image group had a negative value for `text`: `"+value+"`");
                    value = "0";
                }
                m.add(volR, m.getProperty(BDO, "volumePagesText"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            }
            value = current.getAttribute("total").trim();
            if (!value.isEmpty())
                m.add(volR, m.getProperty(BDO, "volumePagesTotal"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
        }
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "qc");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            addSimpleElement("qcperson", "volumeQcPerson", current, m, admVol);
            addSimpleElement("qcdate", "volumeQcDate", current, m, admVol);
            addSimpleElement("qcnotes", "volumeQcNote", current, m, admVol);
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
