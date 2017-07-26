package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PubinfoMigration {

	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String PP = CommonMigration.PLACE_PREFIX;
	private static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;

	
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
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "work", "missing work ID!");
                return m;
            }
            main = m.createResource(BDR+value);
        }
        m.add(main, RDF.type, m.getResource(BDO+"Work"));
        MigratePubinfo(xmlDocument, m, main);
        return m;
	}
	
	// use this giving a wkr:Work as main argument to fill the work data
	public static Model MigratePubinfo(Document xmlDocument, Model m, Resource main) {
		
		Element root = xmlDocument.getDocumentElement();
		String rid = root.getAttribute("RID");
		
        // TODO: all these "en" defaults look strange...
        addSimpleElement("publisherName", BDO+"workPublisherName", "en", root, m, main);
        addSimpleElement("publisherLocation", BDO+"workPublisherLocation", "en", root, m, main);
        addSimpleElement("printery", BDO+"pubinfo_printery", "bo-x-ewts", root, m, main); //???
        addSimpleElement("publisherDate", BDO+"workPublisherDate", null, root, m, main);
        addSimpleElement("lcCallNumber", BDO+"workLcCallNumber", null, root, m, main);
        addSimpleElement("lccn", BDO+"workLccn", null, root, m, main);
        addSimpleElement("hollis", BDO+"workHollis", null, root, m, main);
        addSimpleElement("seeHarvard", BDO+"workSeeHarvard", null, root, m, main);
        addSimpleElement("pl480", BDO+"workPL480", null, root, m, main);
        addSimpleElement("isbn", BDO+"workIsbn", null, root, m, main);
        addSimpleElement("authorshipStatement", BDO+"workAuthorshipStatement", "bo-x-ewts", root, m, main);
        addSimpleElement("encoding", BDO+"workEncoding", null, root, m, main);
        addSimpleElement("dateOfWriting", BDO+"workDateOfWriting", null, root, m, main);
        addSimpleElement("extent", BDO+"workExtentStatement", null, root, m, main);
        addSimpleElement("illustrations", BDO+"workIllustrations", null, root, m, main);
        addSimpleElement("dimensions", BDO+"workDimensions", null, root, m, main);
        addSimpleElement("volumes", BDO+"pubinfo_volumes", null, root, m, main); //???
        addSimpleElement("seriesName", BDO+"workSeriesName", "bo-x-ewts", root, m, main);
        addSimpleElement("seriesNumber", BDO+"workSeriesNumber", null, root, m, main);
        addSimpleElement("tbrcHoldings", BDO+"workTbrcHoldings", null, root, m, main);
        addSimpleElement("biblioNote", BDO+"workBiblioNote", "en", root, m, main);
        addSimpleElement("sourceNote", BDO+"workSourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", BDO+"workEditionStatement", "bo-x-ewts", root, m, main);
        
        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        CommonMigration.addLog(m, root, main, WPXSDNS);
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "series");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("name").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "workSeriesName"), m.createLiteral(value));
            value = current.getAttribute("number").trim();
            if (!value.isEmpty()) {
                m.add(main, m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(value));
                m.add(main, m.getProperty(BDO, "workIsNumbered"), m.createTypedLiteral(true));
            }
            Property prop = m.getProperty(BDO, "workSeriesContent");
            Literal l = CommonMigration.getLiteral(current, "bo-x-ewts", m, "series", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(prop, l);
            Statement s = main.getProperty(m.getProperty(BDO, "workExpressionOf"));
            if (s != null) {
                l = s.getLiteral();
                main.removeAll(m.getProperty(BDO, "workExpressionOf"));
                main.addProperty(m.getProperty(BDO, "workNumberOf"), l);
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "printType");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "pubinfo_printType"), m.createLiteral(value)); //???
        }

        nodeList = root.getElementsByTagNameNS(WPXSDNS, "sourcePrintery");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "workHasSourcePrintery"), m.createResource(BDR+value));
            else {
                value = current.getTextContent().trim();
                if (!value.isEmpty()) {
                    m.add(main, m.getProperty(BDO, "workSourcePrintery_string"), m.createLiteral(value));
                } else {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "sourcePrintery", "missing source printery ID!");
                }
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "holding");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            //String value = CommonMigration.getSubResourceName(main, WP, "Holding", i+1);
            Resource holding = m.createResource();
            //m.add(holding, RDF.type, m.getResource(BDO+"Holding"));
            m.add(main, m.createProperty(BDO, "hasHolding"), holding);
            
            addSimpleElement("exception", BDO+"holding_exception", "bo-x-ewts", current, m, holding);
            String value;
            NodeList subNodeList = root.getElementsByTagNameNS(WPXSDNS, "shelf");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getTextContent().trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(BDO, "holding_shelf"), m.createLiteral(value));
                
                value = subCurrent.getAttribute("copies").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(BDO, "holding_copies"), m.createLiteral(value));
            }
            
            subNodeList = root.getElementsByTagNameNS(WPXSDNS, "library");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getAttribute("rid").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(BDO, "holding_library"), m.createResource(PP+value));
                else
                    CommonMigration.addException(m, main, "Pubinfo holding has no library RID!");
                
                value = subCurrent.getAttribute("code").trim();
                if (!value.isEmpty())
                    m.add(holding, m.createProperty(BDO, "holding_code"), m.createLiteral(value));
                
                // TODO: what about the text content?
            }
            
        }
        
		return m;
	}

	public static void addSimpleElement(String elementName, String propName, String defaultLang, Element root, Model m, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        String rid = root.getAttribute("RID");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = null;
            if (defaultLang != null) {
                Property prop = m.createProperty(propName);
                Literal l = CommonMigration.getLiteral(current, defaultLang, m, elementName, rid, null);
                main.addProperty(prop, l);
            } else {
                value = current.getTextContent().trim();
                if (value.isEmpty()) return;
                m.add(main, m.createProperty(propName), m.createLiteral(value));
            }
        }
    }
	
}
