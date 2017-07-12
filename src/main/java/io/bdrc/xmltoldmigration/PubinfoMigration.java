package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PubinfoMigration {

	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PLACE_PREFIX;
	private static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";

	
	// used for testing only
	public static Model MigratePubinfo(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        Element root = xmlDocument.getDocumentElement();
        Resource main = null;
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "isPubInfoFor");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("work");
            if (value.isEmpty()) {
                System.err.println("No work ID for pubinfo "+root.getAttribute("RID")+"!");
                return m;
            }
            main = m.createResource(WP+value);
        }
        m.add(main, RDF.type, m.getResource(WP+"Work"));
        MigratePubinfo(xmlDocument, m, main);
        return m;
	}
	
	// use this giving a wkr:Work as main argument to fill the work data
	public static Model MigratePubinfo(Document xmlDocument, Model m, Resource main) {
		
		Element root = xmlDocument.getDocumentElement();
		
        // TODO: all these "en" defaults look strange...
        addSimpleElement("publisherName", "pubinfo_publisherName", null, root, m, main);
        addSimpleElement("publisherLocation", "pubinfo_publisherLocation", null, root, m, main);
        addSimpleElement("printery", "pubinfo_printery", "bo-x-ewts", root, m, main);
        addSimpleElement("publisherDate", "pubinfo_publisherDate", null, root, m, main);
        addSimpleElement("lcCallNumber", "pubinfo_lcCallNumber", null, root, m, main);
        addSimpleElement("lccn", "pubinfo_lccn", null, root, m, main);
        addSimpleElement("hollis", "pubinfo_hollis", null, root, m, main);
        addSimpleElement("seeHarvard", "pubinfo_seeHarvard", null, root, m, main);
        addSimpleElement("pl480", "pubinfo_pl480", null, root, m, main);
        addSimpleElement("isbn", "pubinfo_isbn", null, root, m, main);
        addSimpleElement("authorshipStatement", "pubinfo_authorshipStatement", "bo-x-ewts", root, m, main);
        addSimpleElement("encoding", "pubinfo_encoding", null, root, m, main);
        addSimpleElement("dateOfWriting", "pubinfo_dateOfWriting", null, root, m, main);
        addSimpleElement("extent", "pubinfo_extent", null, root, m, main);
        addSimpleElement("illustrations", "pubinfo_illustrations", null, root, m, main);
        addSimpleElement("dimensions", "pubinfo_dimensions", null, root, m, main);
        addSimpleElement("volumes", "pubinfo_volumes", null, root, m, main);
        addSimpleElement("seriesName", "pubinfo_seriesName", "bo-x-ewts", root, m, main);
        addSimpleElement("seriesNumber", "pubinfo_seriesNumber", null, root, m, main);
        addSimpleElement("tbrcHoldings", "pubinfo_tbrcHoldings", null, root, m, main);
        addSimpleElement("biblioNote", "pubinfo_biblioNote", "en", root, m, main);
        addSimpleElement("sourceNote", "pubinfo_sourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", "pubinfo_editionStatement", "bo-x-ewts", root, m, main);
        
        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        CommonMigration.addLog(m, root, main, WPXSDNS);
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "series");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = CommonMigration.getSubResourceName(main, WP, "Series", i+1);
            Resource series = m.createResource(value);
            m.add(series, RDF.type, m.getResource(WP+"PubinfoSeries"));
            m.add(main, m.createProperty(WP+"hasPubinfoSeries"), series);
            value = current.getAttribute("name").trim();
            if (!value.isEmpty())
                m.add(series, m.getProperty(WP+"pubinfo_series_name"), m.createLiteral(value));
            
            value = current.getAttribute("number").trim();
            if (!value.isEmpty())
                m.add(series, m.getProperty(WP+"pubinfo_series_number"), m.createLiteral(value));
            
            Property prop = m.getProperty(WP+"pubinfo_series_content");
            CommonMigration.addCurrentString(current, "bo-x-ewts", m, series, prop, false);
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "printType");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(WP+"pubinfo_printType"), m.createLiteral(value));
        }

        nodeList = root.getElementsByTagNameNS(WPXSDNS, "sourcePrintery");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(WP+"hasSourcePrintery"), m.createResource(PP+value));
            else {
                value = current.getTextContent().trim();
                if (!value.isEmpty())
                    m.add(main, m.getProperty(WP+"pubinfo_sourcePrintery_string"), m.createLiteral(value));
            }
        }


        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "holding");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = CommonMigration.getSubResourceName(main, WP, "Holding", i+1);
            Resource holding = m.createResource(value);
            m.add(holding, RDF.type, m.getResource(WP+"Holding"));
            m.add(main, m.createProperty(WP+"hasHolding"), holding);
            
            addSimpleElement("exception", "holding_exception", "bo-x-ewts", current, m, holding);
            
            NodeList subNodeList = root.getElementsByTagNameNS(WPXSDNS, "shelf");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                //String lang = CommonMigration.getBCP47(subCurrent, "bo-x-ewts"); // TODO: clarify with ontology
                value = subCurrent.getTextContent().trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(WP+"holding_shelf"), m.createLiteral(value));
                
                value = subCurrent.getAttribute("copies").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(WP+"holding_copies"), m.createLiteral(value));
            }
            
            subNodeList = root.getElementsByTagNameNS(WPXSDNS, "library");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getAttribute("rid").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(WP+"holding_library"), m.createResource(PP+value));
                else
                    CommonMigration.addException(m, main, "Pubinfo holding has no library RID!");
                
                value = subCurrent.getAttribute("code").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(WP+"holding_code"), m.createLiteral(value));
                
                // TODO: what about the text content?
            }
            
        }
        
		return m;
	}

	public static void addSimpleElement(String elementName, String propName, String defaultLang, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = null;
            if (defaultLang != null) {
                Property prop = m.createProperty(WP+propName);
                CommonMigration.addCurrentString(current, defaultLang, m, main, prop, false);
            } else {
                value = current.getTextContent().trim();
                if (value.isEmpty()) return;
                m.add(main, m.createProperty(WP+propName), m.createLiteral(value));
            }
        }
    }
	
}
