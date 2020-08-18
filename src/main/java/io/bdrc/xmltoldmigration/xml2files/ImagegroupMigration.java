package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.XSD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;


public class ImagegroupMigration {

	public static final String IGXSDNS = "http://www.tbrc.org/models/imagegroup#";

    public static boolean addVolumeOf = false;
    public static boolean addItemHasVolume = true;

	
	// for testing purposes only
	public static Model MigrateImagegroup(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
	    MigrationHelpers.setPrefixes(m, "item");
        Resource item = createRoot(m, BDR+"WTestInstance", BDO+"ImageInstance");
        createAdminRoot(item);
        MigrateImagegroup(xmlDocument, m, item, "testVolName", 1, "testVolumesName", "testWork");
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
	
	public static void MigrateImagegroup(Document xmlDocument, Model m, Resource item, String volumeName, Integer volumeNumber, String volumesName, String workId) {
		
		Element root = xmlDocument.getDocumentElement();
		
        final String imageGroupRID = root.getAttribute("RID").trim();
        final String imageGroupStatus = root.getAttribute("status").trim();
		
		final String itemId = item.getLocalName();
		final String volumeId = imageGroupRID;
		
        Resource volR = m.createResource(BDR+volumeId);
        volR.addProperty(RDF.type, m.getResource(BDO+"ImageGroup"));
        // create AdminData if it doesn't already exist - should only be created 
        // when used in MigrationTest.testImagegroup() w/o previously the containing Item
        Resource admVol = getAdminData(volR);

        // we leave it for some time, as some requests count on it but we'll remove it
        // in the future
		admVol.addProperty(m.getProperty(ADM, "legacyImageGroupRID"), m.createLiteral(imageGroupRID));
        
        if (volumeNumber < 1) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup", "invalid volume number, must be a positive integer, got `"+volumeNumber+"`");
        }
        m.add(volR, m.getProperty(BDO, "volumeNumber"), m.createTypedLiteral(volumeNumber, XSDDatatype.XSDinteger));
        if (OutlineMigration.workVolNames.containsKey(workId)) {
            Map<Integer,Literal> volNames = OutlineMigration.workVolNames.get(workId);
            if (volNames.containsKey(volumeNumber)) {
                m.add(volR, SKOS.prefLabel, volNames.get(volumeNumber));
            }
        }
        
        if (addItemHasVolume)
            m.add(item, m.getProperty(BDO+"instanceHasVolume"), volR);
        
        if (addVolumeOf)
            m.add(volR, m.getProperty(BDO+"volumeOf"), item);
        
		// adding the ondisk/onDisk description as vol:imageList
        /*
		NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (type.equals("ondisk") || type.equals("onDisk")) {
                //ImageListTranslation.addImageList(current.getTextContent().trim(), imageGroupRID, volumeNumber, m, volR);
                continue;
            }
        }
        */
		
        addStatus(m, admVol, imageGroupStatus);
        CommonMigration.addLog(m, root, admVol, IGXSDNS);
        CommonMigration.addDescriptions(m, root, volR, IGXSDNS);
        admVol.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
        
        NodeList nodeList = root.getElementsByTagNameNS(IGXSDNS, "images");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("intro").trim();
            //if (!value.isEmpty())
            //    m.add(volR, m.getProperty(BDO, "volumePagesIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("tbrcintro").trim();
            if (!value.isEmpty())
                m.add(volR, m.getProperty(BDO, "volumePagesTbrcIntro"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            
            value = current.getAttribute("text").trim();
            if (!value.isEmpty()) {
                if (value.startsWith("-")) {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, volumesName, volumeName, "imagegroup:text", "image group had a negative value for `text`: `"+value+"`");
                    value = "0";
                }
                //m.add(volR, m.getProperty(BDO, "volumePagesText"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
            }
            value = current.getAttribute("total").trim();
            //if (!value.isEmpty())
            //    m.add(volR, m.getProperty(BDO, "volumePagesTotal"), m.createTypedLiteral(value, XSDDatatype.XSDinteger));
        }
        
        nodeList = root.getElementsByTagNameNS(IGXSDNS, "qc");
        // Todo: logentry of type ADM+ContentQC
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            Resource logEntry = getFacetNode(FacetType.LOG_ENTRY, BDA, admVol);
            logEntry.removeAll(RDF.type);
            logEntry.addProperty(RDF.type,  m.createResource(ADM+"ContentQC"));
            
            // person
            NodeList subNodeList = root.getElementsByTagNameNS(IGXSDNS, "qcperson");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element currentSub = (Element) subNodeList.item(j);
                String value = currentSub.getTextContent().trim();
                if (value.isEmpty()) {
                    return;
                }
                String uri = CommonMigration.logWhoToUri.get(value);
                if (uri == null) {
                    List<String> uris = CommonMigration.logWhoToUriList.get(value);
                    if (uris == null) {
                        m.add(logEntry, m.createProperty(ADM+"logWhoStr"), value);
                    } else {
                        for (String who : uris) {
                            m.add(logEntry, m.createProperty(ADM+"logWho"), m.createResource(who));
                        }
                    }
                } else {
                    m.add(logEntry, m.createProperty(ADM+"logWho"), m.createResource(uri));
                }
            }

            subNodeList = root.getElementsByTagNameNS(IGXSDNS, "qcnotes");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element currentSub = (Element) subNodeList.item(j);
                String value = currentSub.getTextContent().trim();
                if (value.isEmpty()) {
                    return;
                }
                value = CommonMigration.normalizeString(value, true);
                m.add(logEntry, m.createProperty(ADM+"logMessage"), m.createLiteral(value, "en"));
            }
            
            subNodeList = root.getElementsByTagNameNS(IGXSDNS, "qcdate");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element currentSub = (Element) subNodeList.item(j);
                String value = currentSub.getTextContent().trim();
                if (value.isEmpty()) {
                    return;
                }
                value = CommonMigration.normalizeString(value, true);
                Literal l = qcdateToXsdDate(value, m);
                if (l == null) {
                    m.add(logEntry, m.createProperty(ADM+"logMessage"), m.createLiteral(value));
                }
                m.add(logEntry, m.createProperty(ADM+"logDate"), l);
            }
        }
	}
	
	public static Literal qcdateToXsdDate(String qcdate, Model m) {
	    qcdate = qcdate.replace('/', '-').replace('.', '-');
	    qcdate = qcdate.replaceAll("^-", "");
	    qcdate = qcdate.replace("--", "-");
	    String year = null;
	    String month = null;
	    String day = null;
	    if (qcdate.matches("^\\d+$")) {
            year = qcdate;
        }
	    if (qcdate.matches("^\\d{6}$")) {
	        qcdate = qcdate.substring(0,2)+"-"+qcdate.substring(2,4)+"-"+qcdate.substring(4);
	    }
	    if (!qcdate.matches("^[0-9-]+$")) {
            return null;
        }
	    String[] parts = qcdate.split("-");
	    if (parts.length == 2) {
	        month = parts[0];
	        year = parts[1];
	    } else if (parts.length > 2){
	        month = parts[0];
	        day = parts[1];
            year = parts[2];
	    }
	    if (month != null && month.length() == 1) {
	        month = "0"+month;
	    }
	    if (day != null && day.length() == 1) {
            day = "0"+day;
        }
	    if (year.length() == 1) year = "200"+year;
	    if (year.length() == 2) year = "20"+year;
	    if (year.length() == 3) year = "2"+year;
	    if (day == null) {
	        if (month == null) {
	            return m.createTypedLiteral(year, XSD.gYear.getURI());
	        }
	        return m.createTypedLiteral(year+"-"+month, XSD.gYearMonth.getURI());
	    }
	    return m.createTypedLiteral(year+"-"+month+"-"+day, XSD.date.getURI());
	}
	
}
