package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminRoot;
import static io.bdrc.libraries.Models.setPrefixes;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.WorkModelInfo;


public class PubinfoMigration {

	public static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";
	
	static final ISBNValidator isbnvalidator = ISBNValidator.getInstance(false);

	// used for testing only
	public static Model MigratePubinfo(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        setPrefixes(m, "work");
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
            
            main = createRoot(m, BDR+value, BDO+"Work");
            createAdminRoot(main);
        }
        MigratePubinfo(xmlDocument, m, main, new HashMap<String,Model>(), null);
        return m;
	}
	
	public static boolean isComputerInputDbuMed(String RID) {
	    switch(RID) {
	    case "W8LS25451":
	    case "W8LS25572":
	    case "W8LS25575":
	    case "W8LS25578":
	    case "W8LS25590":
	    case "W8LS25593":
	    case "W8LS26096":
	    case "W8LS26099":
	    case "W8LS26102":
	    case "W8LS26105":
	    case "W8LS26182":
	    case "W8LS26185":
	        return true;
	    }
	    return false;
	}

	public static void addLangScript(final Resource main, final Resource mainA, final String lang, final String script, final String langScript) {
	    if (main != null) {
	        Model m = main.getModel();
	        main.addProperty(m.getProperty(BDO, "language"), BDR+lang);
	        if (script != null)
	            main.addProperty(m.getProperty(BDO, "script"), BDR+script);
	    }
	    if (mainA != null && script != null) {
	        Model m = mainA.getModel();
	        mainA.addProperty(m.getProperty(BDO, "script"), BDR+script);
	    }
	}
    
    public static RDFNode getSeriesName(Element root, Model model) {
        Literal seriesNameLiteral = null;
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "seriesName");
        String rid = root.getAttribute("RID");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            seriesNameLiteral = CommonMigration.getLiteral(current, EWTS_TAG, model, "seriesName", rid, null);
        }
        return seriesNameLiteral;
    }
    
    public static RDFNode getSeriesNumber(Element root, Model model) {
        Literal seriesNumberLiteral = null;
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "seriesName");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (!value.isEmpty()) {
                seriesNumberLiteral = model.createLiteral(value);
            }
        }
        return seriesNumberLiteral;
    }
	
    // use this giving a bdr:Work as main argument to fill the work data
	public static Resource MigratePubinfo(final Document xmlDocument, final Model m, final Resource main, final Map<String,Model> itemModels, final Resource mainA) {
		Element root = xmlDocument.getDocumentElement();
		
		String workRid = root.getAttribute("RID").substring(1);
		if (!workRid.contains("FPL") && !workRid.contains("FEMC") &&  !workRid.contains("W1EAP")) {
		    addSimpleElement("publisherName", BDO+"workPublisherName", "en", root, m, main);
            addSimpleElement("publisherLocation", BDO+"workPublisherLocation", "en", root, m, main);
		}
        addSimpleElement("printery", BDO+"workPrintery", "bo-x-ewts", root, m, main);
        addSimpleDateElement("publisherDate", "PublishedEvent", root, main);
        addSimpleElementUC("lcCallNumber", BDO+"workLcCallNumber", null, root, m, main);
        addSimpleElement("lccn", BDO+"workLccn", null, root, m, main);
        addSimpleElement("hollis", BDO+"workHollis", null, root, m, main);
        addSimpleElement("seeHarvard", BDO+"workSeeHarvard", null, root, m, main);
        addSimpleElement("pl480", BDO+"workPL480", null, root, m, main);
        addSimpleElement("isbn", BDO+"workIsbn", null, root, m, main);
        addSimpleElement("authorshipStatement", BDO+"workAuthorshipStatement", EWTS_TAG, root, m, main);
        addSimpleDateElement("dateOfWriting", "CompletedEvent", root, main);
        addSimpleElement("extent", BDO+"workExtentStatement", null, root, m, main);
        addSimpleElement("illustrations", BDO+"workIllustrations", null, root, m, main);
        addSimpleElement("dimensions", BDO+"workDimensions", null, root, m, main);
        addSimpleElement("volumes", ADM+"workVolumesNote", null, root, m, main);
        addSimpleElement("biblioNote", BDO+"workBiblioNote", "en", root, m, main);
        addSimpleElement("sourceNote", BDO+"workSourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", BDO+"workEditionStatement", EWTS_TAG, root, m, main);
        
        // handle series info
        Resource serialWork = null;
        
        String serialWorkId = null;
        RDFNode seriesName = getSeriesName(root, m);
        Model mA = mainA.getModel();
        if (seriesName != null) {
            String otherRID = CommonMigration.seriesClusters.get(workRid);
            if (otherRID == null) {
                otherRID = workRid;
            }
            serialWorkId = CommonMigration.seriesMembersToWorks.get(otherRID);
            if (serialWorkId == null) {
                serialWorkId = "WS" + otherRID.substring(1);
                CommonMigration.seriesMembersToWorks.put(otherRID, serialWorkId);
                Model mS = ModelFactory.createDefaultModel();
                setPrefixes(mS);
                serialWork = createRoot(mS, BDR+serialWorkId, BDO+"SerialWork");
                Resource admSerialW = createAdminRoot(serialWork);
                mainA.addProperty(m.createProperty(BDO+"serialMemberOf"), serialWork);
            } else { // serialWork already created just link to it
                mainA.addProperty(m.createProperty(BDO+"serialMemberOf"), mA.createResource(BDO+serialWorkId));
            }
            mainA.addProperty(RDF.type, mA.createResource(BDO+"SerialMember"));
        }
        RDFNode seriesNumber = getSeriesNumber(root, m);
        if (seriesNumber != null) {
            main.addProperty(m.createProperty(BDO+"workSeriesNumber"), seriesNumber);
            main.addProperty(RDF.type, m.createResource(BDO+"SerialInstance"));
            mainA.addProperty(RDF.type, mA.createResource(BDO+"SerialMember"));
            main.addProperty(m.createProperty(BDO+"serialInstanceOf"), mainA);
        }
        
        // TODO: this goes in the item
        addSimpleElement("tbrcHoldings", BDO+"itemBDRCHoldingStatement", null, root, m, main);
        
        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        
        Resource admMain = getAdminRoot(main, true);
        CommonMigration.addLog(m, root, admMain, WPXSDNS);

        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "printType");
        boolean langTibetanDone = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            switch(value) {
            case "dbuMed":
                langTibetanDone = true;
                addLangScript(main, mainA, "LangBo", "ScriptDbuMed", "BoDbuMed");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoDbuMed"));
                if (isComputerInputDbuMed(main.getLocalName()))
                    m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeComputerInput"));
                else
                    m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
                break;
            case "dbuCan":
                langTibetanDone = true;
                addLangScript(main, mainA, "LangBo", "ScriptDbuCan", "BoDbuCan");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoDbuCan"));
                break;
            case "blockprint":
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Relief_WoodBlock"));
                break;
            case "longPalmLeaf":
                // short palm leaves are just... short... between 10 and 45cm, long ones are above 45cm
                // having the dimensions will make the distinction in a more satisfying way
                m.add(main, m.getProperty(BDO, "material"), m.createResource(BDR+"MaterialCoryphaPalmLeaf"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
                break;
            case "shortPalmLeaf":
                m.add(main, m.getProperty(BDO, "material"), m.createResource(BDR+"MaterialCoryphaPalmLeaf"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
                break;
            case "leporello":
                // FEMC leporellos are not always made of the same material
                m.add(main, m.getProperty(BDO, "binding"), m.createResource(BDR+"Binding_Continuous_Leporello"));
                break;
            case "computerInput":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeComputerInput"));
                break;
            case "OCR":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeOCR"));
                break;
            case "typeSet":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeTypeSet"));
                break;
            case "facsimile":
                m.add(main, m.getProperty(BDO, "workObjectType"), m.createResource(BDR+"ObjectTypeFacsimile"));
                break;
            default:
                break;
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "encoding");
        if (!langTibetanDone && nodeList.getLength() == 0 && main.getLocalName().startsWith("W1FPL")) {
            //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"PiMymr"));
            addLangScript(main, mainA, "LangPi", "ScriptMymr", "PiMymr");
        }
        if (!langTibetanDone && nodeList.getLength() == 1 && main.getLocalName().startsWith("W1FEMC")) {
            Node nd = nodeList.item(0); 
            String str = nd.getTextContent();
            if (str.contains("Pāli")) {
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"PiKhmr"));
                addLangScript(main, mainA, "LangPi", "ScriptKhmr", "PiKhmr");
            } else if (str.contains("Khmer")) {
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"KmKhmr"));
                addLangScript(main, mainA, "LangKm", "ScriptKhmr", "KmKhmr");
            } else { // for now default to Khmer
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"KmKhmr"));
                addLangScript(main, mainA, "LangKm", "ScriptKhmr", "KmKhmr");
            }
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) continue;
            value = value.toLowerCase();
            if (value.endsWith(".")) {
                value = value.substring(0, value.length()-1);
            }
            switch (value) {
            case "in tibetan":
            case "བོད་ཡིག":
            case "ྦོབོད་ཡིག":
            case "ྦབོད་ཡིག":
            case " ྐབོད་ཡིག":
            case "ྦོོབོད་ཡིག":
            case "བོ་དཡིག":
            case "ཡིག":
            case "ྐབོད་ཡིག":
            case "བོད་ཡི":
            case "བོད་ཡིངག":
            case "ྦོད་ཡིག":
            case "བོད་སྐད།":
            case "བིད་ཡིག":
            case "བོད་ཡིབ":
            case "བོད་ཡོག":
            case "བོདཡིག":
            case "བོད":
            case "བོད་":
            case "བོད་ཡིག་":
            case "བ་ོད་ཡིག":
            case "བོག་ཡིག":
            case "ྦིབོད་ཡིག":
            case "བོད་ཡིག༌":
            case "ོབོད་ཡིག":
            case "བོད་རིགས།":
            case "བོང་ཡིག":
            case "in tibetab":
            case "inntibetan":
            case "intibetan":
            case "in tibet":
            case "inn tibetan":
            case "in tibatan":
            case "ln tibetan":
            case "in tibean":
            case "in tibeta":
            case "in tibetabn":
            case "in toibetan":
            case "in tbetan":
            case "in tibetyan":
            case "in ttibetan":
            case "in tibeatan":
            case "in tebe":
            case "in tibetan;":
            case "in tibeatn":
            case "tibetan":
            case "in tibtan":
            case "im tibetan":
            case "in tiibetan":
            case "in titeian":
            case "in  tibetan":
            case "in་tibetan":
            case "in tibat":
            case "in tietan":
            case "oin tibetan":
            case "in tobetan":
            case "in ti betan":
            case "in tidetan":
            case "un tibetan":
            case "in tiobetan":
            case "ni tibetan":
            case "in tibtatan":
                if (!langTibetanDone) {
                    //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                }
                break;
            case "extendedwylie":
            case "estended wylie":
            case "extended wylie":
                addLangScript(main, mainA, "LangBo", "ScriptLatn", "BoEwts");
                break;
            case "in dzongkha":
                addLangScript(main, mainA, "LangDz", "ScriptTibt", "DzTibt");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"DzTibt"));
                break;
            case "བོད་དབྱིན།":
            case "དབྱིན་ཡིག":
            case "བོད་ཡིག  དབྱིན་ཡིག":
            case "བོད་དབྱིན":
            case "དབྱིན་བོད།":
            case "བོད་ཡིག english":
            case "in tibetan & english":
            case "in tibetan and english":
            case "in english and tibetan":
            case "in tibean & english":
            case "tibetan and english":
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn"));
                addLangScript(main, mainA, "LangEn", null, "EnLatn");
                if (!langTibetanDone) {
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                }
                break;
            case "in chinese":
            case "in chinece":
            case "chinese":
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"Zh")); // TODO
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani");
                break;
            case "in chinese & tibetan":
            case "in tibetan and chinese":
            case "in chinese and tibetan":
            case "in tibetan & chinese":
            case "in tibetan and chinise":
            case "in tibetan with chinese":
            case "in tibetan and chinece":
            case "in tibetan and chinses":
            case "in tibetan with chinece":
            case "in chinese，tibetan":
            case "in chinese in tibetan":
            case "in tibetan chinese":
            case "tobetan with chinece":
            case "in tibetab with chinece":
                if (!langTibetanDone) {
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                }
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani");
                break;
            case "in sanskrit":
                addLangScript(main, mainA, "LangSa", null, "Sa");
                break;
            case "བོད་ཡིག་དང་རྒྱ་ཡིག།":
            case "in sanskrit & tibetan":
            case "in sanskrit and tibetan":
            case "in tibetan and sanskrit":
            case "in tibetan & sanskrit":
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                addLangScript(main, mainA, "LangSa", null, "Sa");
                break;
            case "in mongolian":
            case "mongolian":
                addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong");
                break;
            case "in tibetan and mongol":
            case "in tibetan and mongolian":
            case "in mongolian and tibetan":
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong");
                break;
            case "english":
            case "in english":
            case "en":
                addLangScript(main, mainA, "LangEn", null, "EnLatn");
                break;
            case "in tibetan, english and chinese":
            case "in chinese, tibetan and english":
            case "in tibetan, chinese & english":
            case "in tibetan, chinece and english":
            case "tibetan, english and chinese":
            case "in tibetan chinese english":
            case "in tibetan, chinese and english":
            case "in chinese, english and tibetan":
            case "in english, tibetan and chinese":
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                addLangScript(main, mainA, "LangEn", null, "EnLatn");
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani");
                break;
            case "in tibetan; an excerpt in english":
            case "in tibetan; notes in english":
            case "in tibetan; preface in english":
            case "in tibetan; pref. in english":
            case "in tibetan, preface in english":
            case "in tibetan; prefatory in english":
            case "in tibetan; publisher's note in english":
            case "in tibetan; includes english terms":
            case "in tibetan; introduction in english":
            case "introduction in english":
            case "in tibetan; brief biography of author in english":
            case "in tibetan; preface and acknowledge in english":
            case "in tibetan; prologue and acknowledgements in tibetan and english":
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                addLangScript(main, mainA, "LangEn", null, "EnLatn");
                break;
            default:
                boolean langFound = false;
                if (value.contains("chinese")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani");
                }
                if (value.contains("english") || value.contains("དབྱིན") || value.contains("ཨིན")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangEn", null, "EnLatn");
                }
                if (value.contains("mongol")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong");
                }
                if (value.contains("tibet") || value.contains("བོད")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt");
                }
                if (value.contains("sanskrit") || value.contains("རྒྱ")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangSa", null, "Sa");
                }
                if (value.contains("dzongkha")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangDz", "ScriptTibt", "DzTibt");
                }
                if (value.contains("hindi")) {
                    langFound = true;
                    addLangScript(main, mainA, "LangHi", null, "Hi");
                    m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"Hi"));
                }
//                if (!langFound)
//                    System.out.println(main.getLocalName()+" "+value);
                // TODO: migration exception: add initial string
                break;
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
                setPrefixes(itemModel, "item");
                itemModels.put(itemName, itemModel);
            }
            Resource holding = itemModel.createResource(BDR+itemName);
            itemModel.add(holding, RDF.type, itemModel.getResource(BDO+"ItemPhysicalAsset"));
            if (WorkMigration.addItemForWork)
                itemModel.add(holding, itemModel.createProperty(BDO, "itemForWork"), itemModel.createResource(main.getURI()));
            if (WorkMigration.addWorkHasItem) {
                m.add(main, m.getProperty(BDO, "workHasItem"), m.createResource(BDR+itemName));
            }

            addSimpleElement("exception", BDO+"itemException", EWTS_TAG, current, itemModel, holding);
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
		return serialWork;
	}

	   public static void addSimpleElementUC(String elementName, String propName, String defaultLang, Element root, Model m, Resource main) {
	        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Element current = (Element) nodeList.item(i);
	            String value = null;
	            value = current.getTextContent().trim();
	            if (value.isEmpty()) return;
	            m.add(main, m.createProperty(propName), m.createLiteral(value.toUpperCase()));
	        }
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
                if (elementName.equals("isbn")) {
                    final String validIsbn = isbnvalidator.validate(value);
                    if (validIsbn != null) {
                        value = validIsbn;
                    } else {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "isbn", "invalid isbn: "+value);
                    }
                }
                
                m.add(main, m.createProperty(propName), m.createLiteral(value));
            }
        }
    }

    private static void addSimpleDateElement(String elementName, String eventType, Element root, Resource main) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) return;
            CommonMigration.addDatesToEvent(value, main, "workEvent", eventType);
        }
    }
	
}
