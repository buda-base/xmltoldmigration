package io.bdrc.xmltoldmigration.xml2files;

import java.util.HashMap;
import java.util.Map;

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

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class PubinfoMigration {

	private static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
	
	// used for testing only
	public static Model MigratePubinfo(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, "work");
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
        MigratePubinfo(xmlDocument, m, main, new HashMap<String,Model>());
        return m;
	}
	
	// use this giving a wkr:Work as main argument to fill the work data
	public static Model MigratePubinfo(final Document xmlDocument, final Model m, final Resource main, final Map<String,Model> itemModels) {
		
		Element root = xmlDocument.getDocumentElement();
		
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
        addSimpleElement("authorshipStatement", BDO+"workAuthorshipStatement", CommonMigration.EWTS_TAG, root, m, main);
        addSimpleElement("encoding", BDO+"workEncoding", null, root, m, main);
        addSimpleElement("dateOfWriting", BDO+"workDateOfWriting", null, root, m, main);
        addSimpleElement("extent", BDO+"workExtentStatement", null, root, m, main);
        addSimpleElement("illustrations", BDO+"workIllustrations", null, root, m, main);
        addSimpleElement("dimensions", BDO+"workDimensions", null, root, m, main);
        addSimpleElement("volumes", ADM+"workVolumesNote", null, root, m, main);
        addSimpleElement("seriesName", BDO+"workSeriesName", CommonMigration.EWTS_TAG, root, m, main);
        addSimpleElement("seriesNumber", BDO+"workSeriesNumber", null, root, m, main);
        addSimpleElement("biblioNote", BDO+"workBiblioNote", "en", root, m, main);
        addSimpleElement("sourceNote", BDO+"workSourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", BDO+"workEditionStatement", CommonMigration.EWTS_TAG, root, m, main);
        
        // TODO: this goes in the item
        addSimpleElement("tbrcHoldings", BDO+"itemBDRCHoldingStatement", null, root, m, main);
        
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
            Literal l = CommonMigration.getLiteral(current, CommonMigration.EWTS_TAG, m, "series", main.getLocalName(), null);
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
            if (!value.isEmpty()) {
                value = BDR+"PrintType"+value.substring(0, 1).toUpperCase() + value.substring(1);
                m.add(main, m.getProperty(BDO, "workPrintType"), m.createResource(value));
            }
                
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
            String itemName = "I"+main.getLocalName().substring(1)+"_P"+String.format("%03d", i+1);
            Model itemModel = m;
            if (WorkMigration.splitItems) {
                itemModel = ModelFactory.createDefaultModel();
                CommonMigration.setPrefixes(itemModel, "item");
                itemModels.put(itemName, itemModel);
            }
            Resource holding = itemModel.createResource(BDR+itemName);
            itemModel.add(holding, RDF.type, itemModel.getResource(BDO+"ItemPhysicalAsset"));
            if (WorkMigration.addItemForWork)
                itemModel.add(holding, itemModel.createProperty(BDO, "itemForWork"), itemModel.createResource(main.getURI()));
            if (WorkMigration.addWorkHasItem) {
                m.add(main, m.getProperty(BDO, "workHasItemPhysicalAsset"), m.createResource(BDR+itemName));
            }

            addSimpleElement("exception", BDO+"itemException", CommonMigration.EWTS_TAG, current, itemModel, holding);
            String value;
            NodeList subNodeList = root.getElementsByTagNameNS(WPXSDNS, "shelf");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getTextContent().trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemShelf"), itemModel.createLiteral(value));
                
                value = subCurrent.getAttribute("copies").trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemCopies"), itemModel.createLiteral(value));
            }
            
            subNodeList = root.getElementsByTagNameNS(WPXSDNS, "library");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                value = subCurrent.getAttribute("rid").trim();
                if (!value.isEmpty())
                    itemModel.add(holding, itemModel.createProperty(BDO, "itemLibrary"), itemModel.createResource(BDR+value));
                else
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "holding", "Pubinfo holding has no library RID!");
                
                // ignore @code and content
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
                if (l != null)
                    main.addProperty(prop, l);
            } else {
                value = current.getTextContent().trim();
                if (value.isEmpty()) return;
                m.add(main, m.createProperty(propName), m.createLiteral(value));
            }
        }
    }
	
}
