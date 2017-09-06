package io.bdrc.xmltoldmigration.xml2files;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class ImagegroupMigration {

	private static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;

    private static Pattern imageP = Pattern.compile("^(.+)(\\d{4})( ?\\..+)$");
    private static Pattern basicP = Pattern.compile("[^|]+");
    public static void addImageList(String src, String mainId, String volNum, Model model, Resource main) {
        Matcher basicM = basicP.matcher(src);
        String prefix = "";
        String suffix = "";
        int i = -1;
        int total = 0;
        boolean first = true;
        StringBuilder dst = new StringBuilder();
        int lastOkInSeq = -1;
        List<String> missingPages = new ArrayList<>();
        while (basicM.find()) {
            if (basicM.group(0).indexOf('/') != -1)
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, "volume"+volNum, "string contains invalid character `/`: "+basicM.group(0));
            total = total +1;
            Matcher m = imageP.matcher(basicM.group(0));
            if (!m.find()) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, "volume"+volNum, "cannot understand image string "+basicM.group(0));
                if (lastOkInSeq != -1)
                    dst.append(":"+lastOkInSeq);
                if (!first)
                    dst.append("|");
                dst.append(basicM.group(0));
                prefix = "";
                i = -1;
                suffix = "";
                lastOkInSeq = -1;
                first = false;
                continue;
            }
            final int newInt = Integer.parseInt(m.group(2));
            if (i != -1 && newInt > i+1) {
                int rangeB = i+1;
                int rangeE = newInt-1;
                if (rangeB == rangeE)
                    missingPages.add(Integer.toString(rangeB));
                else
                    missingPages.add(rangeB+"-"+rangeE);
            }
            if (!m.group(1).equals(prefix) || !m.group(3).equals(suffix) || newInt != i+1) {
                if (lastOkInSeq != -1)
                    dst.append(":"+lastOkInSeq);
                if (!first)
                    dst.append("|");
                dst.append(m.group(0));
                prefix = m.group(1);
                i = newInt;
                suffix = m.group(3);
                lastOkInSeq = -1;
            } else {
                i = i +1;
                lastOkInSeq = newInt;
            }
            first = false;
        }
        if (lastOkInSeq != -1)
            dst.append(":"+lastOkInSeq);
        Literal value = model.createLiteral(dst.toString());
        model.add(main, model.getProperty(BDO+"volumeImageList"), value);
        model.add(main, model.getProperty(BDO+"volumeImageCount"), model.createTypedLiteral(total, XSDDatatype.XSDinteger));
        String missingImages = String.join(",", missingPages);
        model.add(main, model.getProperty(BDO+"volumeMissingImages"), model.createLiteral(missingImages));
    }
	
	// for testing purposes only
	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, "item");
        Resource item = m.createResource(BDR+"TestItem");
        m.add(item, RDF.type, m.getResource(BDO+"ItemImageAsset"));
        MigrateImagegroup(xmlDocument, m, item, "testItem", "1", "testItem");
        return m;
	}
	
	public static String getVolumeOf(Document xmlDocument) {
	    Element root = xmlDocument.getDocumentElement();
        String volumeOf = null;
        
        // adding the ondisk/onDisk description as vol:imageList
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "isVolumeOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
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
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource item, String volumeName, String volumeNumber, String volumesName) {
		
		Element root = xmlDocument.getDocumentElement();
		
		Resource main = m.createResource();
		
		main.addProperty(m.getProperty(ADM, "legacyImageGroupRID"), m.createLiteral(root.getAttribute("RID").trim()));
        
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
            addImageList(current.getTextContent().trim(), item.getLocalName(), volumeNumber, m, main);
        }
		
        CommonMigration.addLog(m, root, item, IGXSDNS);
        CommonMigration.addDescriptions(m, root, main, IGXSDNS);
        
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
