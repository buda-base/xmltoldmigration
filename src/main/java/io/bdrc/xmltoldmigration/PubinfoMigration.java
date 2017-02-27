package io.bdrc.xmltoldmigration;

import java.util.List;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PubinfoMigration {

	private static final String OP = CommonMigration.OUTLINE_PREFIX;
	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PLACE_PREFIX;
	private static final String TP = CommonMigration.TOPIC_PREFIX;
	private static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";

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
        
        // TODO: all these "en" defaults look strange...
        addSimpleElement("publisherName", "pubinfo_publisherName", "en", root, m, main);
        addSimpleElement("publisherLocation", "pubinfo_publisherLocation", "en", root, m, main);
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
        
        // TODO: holding, shelf, sourcePrintery, series, library, printType

        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        CommonMigration.addLog(m, root, main, WPXSDNS);
        
		return m;
	}

	public static void addSimpleElement(String elementName, String propName, String defaultLang, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String lang = null;
            if (defaultLang != null)
                lang = CommonMigration.getBCP47(root, defaultLang);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) {
                return;
            }
            if (lang != null)
                m.add(main, m.createProperty(WP+propName), m.createLiteral(value, lang));
            else
                m.add(main, m.createProperty(WP+propName), m.createLiteral(value));
        }
    }
	
}
