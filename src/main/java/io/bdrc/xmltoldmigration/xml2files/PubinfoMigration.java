package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BF;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminRoot;
import static io.bdrc.libraries.Models.setPrefixes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.validator.routines.ISBNValidator;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration.WorkModelInfo;
import org.apache.jena.vocabulary.SKOS;


public class PubinfoMigration {

	public static final String WPXSDNS = "http://www.tbrc.org/models/pubinfo#";
	
	static final ISBNValidator isbnvalidator = ISBNValidator.getInstance(false);

	// used for testing only
	public static List<Model> MigratePubinfo(Document xmlDocument) {
	    List<Model> res = new ArrayList<>();
	    Model m = ModelFactory.createDefaultModel();
	    MigrationHelpers.setPrefixes(m, "work");
        res.add(m);
        Element root = xmlDocument.getDocumentElement();
        Resource main = null;
        Resource mainA = null;
        Resource mainII = null;
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "isPubInfoFor");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("work");
            if (value.isEmpty()) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "work", "missing work ID!");
                return res;
            }
            
            main = createRoot(m, BDR+'M'+value, BDO+"Instance");
            mainII = createRoot(m, BDR+value, BDO+"ImageInstance");
            createAdminRoot(main);
            Model mA = ModelFactory.createDefaultModel();
            res.add(mA);
            setPrefixes(mA);
            mainA = createRoot(m, BDR+"WA"+value.substring(1), BDO+"Work");
            createAdminRoot(main);
        }
        List<Resource> resFromCall = MigratePubinfo(xmlDocument, m, main, new HashMap<String,Model>(), mainA, mainII);
        if (resFromCall != null) {
            for (Resource r : resFromCall) {
                res.add(r.getModel());
            }
        }
        return res;
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

	public static void addLangScript(final Resource main, final Resource mainA, final String lang, String script, final String langScript, final String foundPrintType) {
	    if (main != null) {
	        if ("dbuCan".equals(foundPrintType)) script = "ScriptDbuCan";
	        if ("dbuMed".equals(foundPrintType)) script = "ScriptDbuMed";
	    }
	    if (main != null) {
	        Model m = main.getModel();
	        if (mainA == null)
	            main.addProperty(m.getProperty(BDO, "language"), m.createResource(BDR+lang));
	        if (script != null)
	            main.addProperty(m.getProperty(BDO, "script"), m.createResource(BDR+script));
	    }
	    if (mainA != null) {
	        Model m = mainA.getModel();
	        mainA.addProperty(m.getProperty(BDO, "language"), m.createResource(BDR+lang));
	    }
	}
    
    public static List<RDFNode> getSeriesName(Element root, Model model) {
        Literal seriesNameLiteral = null;
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "seriesName");
        String rid = root.getAttribute("RID");
        List<RDFNode> res = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            seriesNameLiteral = CommonMigration.getLiteral(current, EWTS_TAG, model, "seriesName", rid, null);
            if (seriesNameLiteral != null)
                res.add(seriesNameLiteral);
        }
        if (res.isEmpty())
            return null;
        return res;
    }
    
    public static RDFNode getSeriesNumber(Element root, Model model) {
        Literal seriesNumberLiteral = null;
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "seriesNumber");
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
	public static List<Resource> MigratePubinfo(final Document xmlDocument, final Model m, final Resource main, final Map<String,Model> itemModels, Resource mainA, Resource item) {
		Element root = xmlDocument.getDocumentElement();
		List<Resource> res = new ArrayList<>();
        Model mA = mainA != null ? mainA.getModel() : null;
        
		String workRid = root.getAttribute("RID").substring(1);
        addSimpleElement("printery", BDO+"workPrintery", "bo-x-ewts", root, m, main);
        addSimpleDateElement("publisherDate", "PublishedEvent", root, main, "instanceEvent");
        addSimpleIdElement("lcCallNumber", BF+"ShelfMarkLcc", root, m, main);
        addSimpleIdElement("lccn", BF+"Lccn", root, m, main);
        addSimpleIdElement("hollis", BDR+"HollisId", root, m, main);
        addSimpleIdElement("seeHarvard", BDR+"HarvardShelfId", root, m, main);
        addSimpleIdElement("pl480", BDR+"PL480", root, m, main);
        addSimpleIdElement("isbn", BF+"Isbn", root, m, main);
        addSimpleElement("authorshipStatement", BDO+"authorshipStatement", EWTS_TAG, root, m, main);
        addSimpleDateElement("dateOfWriting", "CompletedEvent", root, mainA, "workEvent");
        addSimpleElement("extent", BDO+"extentStatement", null, root, m, main);
        addSimpleElement("illustrations", BDO+"illustrations", null, root, m, main);
        addSimpleElement("dimensions", BDO+"dimensionsStatement", null, root, m, main);
        addSimpleElement("volumes", BDO+"volumesNote", null, root, m, main);
        addBiblioNote(root, m, main, item);
        addSimpleElement("sourceNote", BDO+"sourceNote", "en", root, m, main);
        addSimpleElement("editionStatement", BDO+"editionStatement", EWTS_TAG, root, m, main);
        
        final String status = root.getAttribute("status");
        
        // handle series info        
        List<RDFNode> seriesNames = getSeriesName(root, m);
        if (seriesNames != null) {
            //ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "XXX: "+workRid+": "+seriesName);
            if (mainA == null) {
                // this is the case where a work is in a series but is also an instance of another abstract work
                // for instance W1KG5476 is a series member but also shares its abstract work with W1KG23131
                // TODO: I think we have a problem if WorkMigration creates an abstract work (the mainA argument of this function)
                // which will become a member in this function, but is shared with non-members...
                mA = ModelFactory.createDefaultModel();
                setPrefixes(mA);
                mainA = createRoot(mA, BDR+"WA"+workRid.substring(1), BDO+"Work");
                res.add(mainA);
                createAdminRoot(mainA);
                main.addProperty(m.createProperty(BDO+"instanceOf"), mainA);
                mainA.addProperty(mA.createProperty(BDO+"workHasInstance"), main);
            }
            String otherRID = CommonMigration.seriesClusters.get(workRid);
            if (otherRID == null) {
                otherRID = workRid;
            }
            String serialWorkId = CommonMigration.seriesMembersToWorks.get(otherRID);
            if (serialWorkId == null && !status.equals("withdrawn")) { // need to create a SerialWork
                serialWorkId = "WAS" + otherRID.substring(1);
                CommonMigration.seriesMembersToWorks.put(otherRID, serialWorkId);
                Model mS = ModelFactory.createDefaultModel();
                setPrefixes(mS);
                // TODO: not sure what this was...
                //WorkMigration.addRedirection(otherRID, serialWorkId, mS);
                Resource serialWork = createRoot(mS, BDR+serialWorkId, BDO+"SerialWork");
                Resource admSerialW = createAdminRoot(serialWork);
                addStatus(mS, admSerialW, status);
                admSerialW.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
                // put a prefLabel on the serialWork if needed
                // at this point the label should == null
                List<RDFNode> serialWorkLabel = CommonMigration.seriesMembersToWorkLabels.get(serialWorkId);
                if (serialWorkLabel == null ) {
                    for (final RDFNode seriesName : seriesNames) {
                        serialWork.addProperty(SKOS.prefLabel, seriesName);
                    }
                    CommonMigration.seriesMembersToWorkLabels.put(serialWorkId, seriesNames);
                }
                res.add(serialWork);
            }
            //mainA.addProperty(RDF.type, mA.createResource(BDO+"SerialMember"));
            if (serialWorkId != null) {
                main.addProperty(m.createProperty(BDO+"serialInstanceOf"), m.createResource(BDR+serialWorkId));
                SymetricNormalization.addSymetricProperty(m, "serialInstanceOf", main.getLocalName(), serialWorkId, null);
            }
            main.addProperty(m.createProperty(BDO+"instanceOf"), mainA);
            mainA.addProperty(mA.createProperty(BDO+"workHasInstance"), main);
        }
        RDFNode seriesNumber = getSeriesNumber(root, m);
        if (seriesNumber != null) {
            main.addProperty(m.createProperty(BDO+"seriesNumber"), seriesNumber);
            main.addProperty(RDF.type, m.createResource(BDO+"SerialInstance"));
            if (mainA != null) { // TODO: this is a bit too simple
                //mainA.addProperty(RDF.type, mA.createResource(BDO+"SerialMember"));
                main.addProperty(m.createProperty(BDO+"instanceOf"), mainA);
            }
        }
        
        // tbrcHoldings is representing many unrelated things, mostly irrelevant now, see
        // https://github.com/buda-base/library-issues/issues/275
        //addSimpleElement("tbrcHoldings", BDO+"itemBDRCHoldingStatement", null, root, m, main);
        
        CommonMigration.addNotes(m, root, main, WPXSDNS);
        CommonMigration.addExternals(m, root, main, WPXSDNS);
        
        Resource admMain = getAdminRoot(main, true);
        CommonMigration.addLog(m, root, admMain, WPXSDNS, false);

        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "printType");
        boolean langTibetanDone = false;
        final Set<String> foundLangs = new HashSet<>();
        String foundPrintType = "";
        if (workRid.contains("FPL") || workRid.contains("FEMC") || workRid.contains("W1EAP")) {
            m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
        }
        boolean needsPublisher = false;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("type").trim();
            foundPrintType = value;
            switch(value) {
            case "dbuMed":
                //langTibetanDone = true;
                //foundLangs.add("bo");
                //addLangScript(main, mainA, "LangBo", "ScriptDbuMed", "BoDbuMed");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoDbuMed"));
                if (isComputerInputDbuMed(workRid)) {
                    m.add(main, m.getProperty(BDO, "contentMethod"), m.createResource(BDR+"ContentMethod_ComputerInput"));
                    m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Modern"));
                    needsPublisher = true;
                } else
                    m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
                break;
            case "dbuCan":
                //langTibetanDone = true;
                //foundLangs.add("bo");
                //addLangScript(main, mainA, "LangBo", "ScriptDbuCan", "BoDbuCan");
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
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
                m.add(main, m.getProperty(BDO, "format"), m.createResource(BDR+"FormatLongPalmLeaf"));
                break;
            case "shortPalmLeaf":
                m.add(main, m.getProperty(BDO, "material"), m.createResource(BDR+"MaterialCoryphaPalmLeaf"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Manuscript"));
                m.add(main, m.getProperty(BDO, "format"), m.createResource(BDR+"FormatShortPalmLeaf"));
                break;
            case "leporello":
                // FEMC leporellos are not always made of the same material
                m.add(main, m.getProperty(BDO, "binding"), m.createResource(BDR+"Binding_Continuous_Leporello"));
                break;
            case "computerInput":
                // computerInput is applied to all modern prints, not just the ones actually produces with computers
                m.add(main, m.getProperty(BDO, "contentMethod"), m.createResource(BDR+"ContentMethod_ComputerInput"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Modern"));
                needsPublisher = true;
                break;
            case "OCR":
                m.add(main, m.getProperty(BDO, "contentMethod"), m.createResource(BDR+"ContentMethod_OCR"));
                needsPublisher = true;
                break;
            case "typeSet":
                m.add(main, m.getProperty(BDO, "contentMethod"), m.createResource(BDR+"ContentMethod_TypeSet"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Modern"));
                needsPublisher = true;
                break;
            case "facsimile":
                m.add(main, m.getProperty(BDO, "contentMethod"), m.createResource(BDR+"ContentMethod_Facsimile"));
                m.add(main, m.getProperty(BDO, "printMethod"), m.createResource(BDR+"PrintMethod_Modern"));
                needsPublisher = true;
                break;
            default:
                break;
            }
        }
        if (!workRid.contains("FPL") && !workRid.contains("FEMC") &&  !workRid.contains("W1EAP")) {
            addSimplePubElement("publisherName", BDO+"publisherName", "en", root, m, main, needsPublisher);
            addSimplePubElement("publisherLocation", BDO+"publisherLocation", "en", root, m, main, needsPublisher);
        }
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "encoding");
        if (!langTibetanDone && nodeList.getLength() == 0 && (workRid.startsWith("W1FPL") || workRid.startsWith("W1EAP"))) {
            //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"PiMymr"));
            addLangScript(main, mainA, "LangPi", "ScriptMymr", "PiMymr", foundPrintType);
            foundLangs.add("pi");
        }
        if (!langTibetanDone && nodeList.getLength() == 1 && workRid.startsWith("W1FEMC")) {
            Node nd = nodeList.item(0); 
            String str = nd.getTextContent();
            if (str.contains("Pāli")) {
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"PiKhmr"));
                addLangScript(main, mainA, "LangPi", "ScriptKhmr", "PiKhmr", foundPrintType);
                foundLangs.add("pi");
            } else if (str.contains("Khmer")) {
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"KmKhmr"));
                addLangScript(main, mainA, "LangKm", "ScriptKhmr", "KmKhmr", foundPrintType);
                foundLangs.add("km");
            } else { // for now default to Khmer
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"KmKhmr"));
                addLangScript(main, mainA, "LangKm", "ScriptKhmr", "KmKhmr", foundPrintType);
                foundLangs.add("km");
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
            case "in tibtetan":
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
                foundLangs.add("bo");
                if (!langTibetanDone) {
                    //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"BoTibt"));
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                }
                break;
            case "extendedwylie":
            case "estended wylie":
            case "extended wylie":
                foundLangs.add("bo");
                addLangScript(main, mainA, "LangBo", "ScriptLatn", "BoEwts", foundPrintType);
                break;
            case "in dzongkha":
                foundLangs.add("dz");
                addLangScript(main, mainA, "LangDz", "ScriptTibt", "DzTibt", foundPrintType);
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
                foundLangs.add("en");
                foundLangs.add("bo");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"EnLatn"));
                addLangScript(main, mainA, "LangEn", null, "EnLatn", foundPrintType);
                if (!langTibetanDone) {
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                }
                break;
            case "in chinese":
            case "in chinece":
            case "chinese":
                foundLangs.add("zh");
                //m.add(main, m.getProperty(BDO, "workLangScript"), m.createResource(BDR+"Zh")); // TODO
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani", foundPrintType);
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
                foundLangs.add("bo");
                foundLangs.add("zh");
                if (!langTibetanDone) {
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                }
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani", foundPrintType);
                break;
            case "in sanskrit":
                foundLangs.add("sa");
                addLangScript(main, mainA, "LangSa", null, "Sa", foundPrintType);
                break;
            case "བོད་ཡིག་དང་རྒྱ་ཡིག།":
            case "in sanskrit & tibetan":
            case "in sanskrit and tibetan":
            case "in tibetan and sanskrit":
            case "in tibetan & sanskrit":
                foundLangs.add("bo");
                foundLangs.add("sa");
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                addLangScript(main, mainA, "LangSa", null, "Sa", foundPrintType);
                break;
            case "in mongolian":
            case "mongolian":
                foundLangs.add("cmg");
                addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong", foundPrintType);
                break;
            case "in tibetan and mongol":
            case "in tibetan and mongolian":
            case "in mongolian and tibetan":
                foundLangs.add("bo");
                foundLangs.add("cmg");
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong", foundPrintType);
                break;
            case "english":
            case "in english":
            case "en":
                foundLangs.add("en");
                addLangScript(main, mainA, "LangEn", null, "EnLatn", foundPrintType);
                break;
            case "in khmer":
                foundLangs.add("km");
                addLangScript(main, mainA, "LangKm", "ScriptKhmr", "KmKhmr", foundPrintType);
                break;
            case "in pāli":
                foundLangs.add("pi");
                addLangScript(main, mainA, "LangPi", "ScriptKhmr", "PiKhmr", foundPrintType);
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
                foundLangs.add("bo");
                foundLangs.add("zh");
                foundLangs.add("en");
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                addLangScript(main, mainA, "LangEn", null, "EnLatn", foundPrintType);
                addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani", foundPrintType);
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
                foundLangs.add("bo");
                foundLangs.add("en");
                if (!langTibetanDone)
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                addLangScript(main, mainA, "LangEn", null, "EnLatn", foundPrintType);
                break;
            default:
                if (value.contains("chinese")) {
                    foundLangs.add("zh");
                    addLangScript(main, mainA, "LangZh", "ScriptHani", "ZhHani", foundPrintType);
                }
                if (value.contains("english") || value.contains("དབྱིན") || value.contains("ཨིན")) {
                    foundLangs.add("en");
                    addLangScript(main, mainA, "LangEn", null, "EnLatn", foundPrintType);
                }
                if (value.contains("mongol")) {
                    foundLangs.add("cmn");
                    addLangScript(main, mainA, "LangMn", "ScriptMong", "MnMong", foundPrintType);
                }
                if (value.contains("german")) {
                    foundLangs.add("de");
                    addLangScript(main, mainA, "LangDe", "ScriptLatn", "DeLatn", foundPrintType);
                }
                if (value.contains("french")) {
                    foundLangs.add("fr");
                    addLangScript(main, mainA, "LangFr", "ScriptLatn", "FrLatn", foundPrintType);
                }
                if (value.contains("burmese")) {
                    foundLangs.add("my");
                    addLangScript(main, mainA, "LangMy", "ScriptMymr", "MyMymr", foundPrintType);
                }
                if (value.contains("tibet") || value.contains("བོད")) {
                    foundLangs.add("bo");
                    addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
                }
                if (value.contains("sanskrit") || value.contains("རྒྱ")) {
                    foundLangs.add("sa");
                    addLangScript(main, mainA, "LangSa", null, "Sa", foundPrintType);
                }
                if (value.contains("dzongkha")) {
                    foundLangs.add("dz");
                    addLangScript(main, mainA, "LangDz", "ScriptTibt", "DzTibt", foundPrintType);
                }
                if (value.contains("hindi")) {
                    foundLangs.add("hi");
                    addLangScript(main, mainA, "LangHi", null, "Hi", foundPrintType);
                }
                if (foundLangs.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "encoding", "cannot find language in encoding string: "+value);
                // TODO: migration exception: add initial string
                break;
            }
        }
        if (foundLangs.size() == 0) {
            addLangScript(main, mainA, "LangBo", "ScriptTibt", "BoTibt", foundPrintType);
        }
        // checking if we have at least one title in the language of the work (let's not check when there are multiple languages):
        if (foundLangs.size() == 1 && root.getAttribute("status").equals("released")) {
            String langofwork = foundLangs.iterator().next();
            // we map Dzongkha to Tibetan
            if (langofwork == "dz")
                langofwork = "bo";
            StmtIterator preflabeli = main.listProperties(SKOS.prefLabel);
            boolean titlefound = false;
            List<Statement> toremove = new ArrayList<>();
            while (preflabeli.hasNext()) {
                Statement s = preflabeli.next();
                String langoflabel = s.getLanguage();
                if (langoflabel.startsWith(langofwork)) {
                    titlefound = true;
                } else {
                    // move the preflabel to an altlabel:
                    toremove.add(s);
                }
            }
            if (titlefound) {
                // don't move the prefLabels to altLabels if there's no other prefLabels
                for (Statement s : toremove) {
                    m.remove(s);
                    m.add(s.getSubject(), SKOS.altLabel, s.getObject());
                    if (mainA != null) {
                        mA.remove(s);
                        mA.add(s.getSubject(), SKOS.altLabel, s.getObject());
                    }
                }
           } else {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "encoding", "no title found in the work language ("+langofwork+")");
            }
        }
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "sourcePrintery");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getAttribute("place").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "hasSourcePrintery"), m.createResource(BDR+value));
            else {
                value = current.getTextContent().trim();
                if (!value.isEmpty()) {
                    m.add(main, m.getProperty(BDO, "sourcePrinteryStatement"), m.createLiteral(value));
                } else {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, root.getAttribute("RID"), root.getAttribute("RID"), "sourcePrintery", "missing source printery ID!");
                }
            }
        }
        
        nodeList = root.getElementsByTagNameNS(WPXSDNS, "holding");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String itemName = "IT"+main.getLocalName().substring(1)+"_"+String.format("%03d", i+1);
            Model itemModel = m;
            if (WorkMigration.splitItems) {
                itemModel = ModelFactory.createDefaultModel();
                MigrationHelpers.setPrefixes(itemModel, "item");
                itemModels.put(itemName, itemModel);
            }
            Resource holding = itemModel.createResource(BDR+itemName);
            itemModel.add(holding, RDF.type, itemModel.getResource(BDO+"Item"));
            if (WorkMigration.addItemForWork)
                itemModel.add(holding, itemModel.createProperty(BDO, "itemForInstance"), itemModel.createResource(main.getURI()));
            if (WorkMigration.addWorkHasItem) {
                m.add(main, m.getProperty(BDO, "instanceHasItem"), m.createResource(BDR+itemName));
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
		return res;
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

    public static void addBiblioNote(Element root, Model m, Resource main, Resource item) {
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, "biblioNote");
        String rid = root.getAttribute("RID");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            Literal l = CommonMigration.getLiteral(current, "en", m, "biblioNote", rid, null);
            if (l == null)
                continue;
            String s = l.getString();
            if (item != null && (rid.startsWith("MW1NLM") || rid.startsWith("MW1FEMC") || rid.startsWith("MW1EAP") || s.startsWith("image") || s.startsWith("Scan") || s.startsWith("scan") || s.startsWith("copy made") || s.startsWith("Copy made"))) {
                item.addProperty(m.createProperty(BDO, "scanInfo"), l);
            } else {
                main.addProperty(m.createProperty(BDO, "biblioNote"), l);
            }
            
        }
    }

	public static Pattern nullpubP = Pattern.compile("^\\[* ?[sS]\\. ?[nld]");
	public static boolean isEmptyPubValue(String value) {
	    if (value.isEmpty()) return true;
	    boolean isnullpub = nullpubP.matcher(value).find();
	    if (isnullpub) return true;
	    return value.startsWith("[n.");
	}
	   public static void addSimplePubElement(String elementName, String propName, String defaultLang, Element root, Model m, Resource main, boolean needsPublisher) {
	        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
	        String rid = root.getAttribute("RID");
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Element current = (Element) nodeList.item(i);
	            String value = current.getTextContent().trim();
	            if (isEmptyPubValue(value)) {
	                if (!needsPublisher) continue;
	                value = elementName.equals("publisherName") ? "[s.n.]" : "[s.l.]";
	            }
                Property prop = m.createProperty(propName);
                Literal l = CommonMigration.getLiteral(current, defaultLang, m, elementName, rid, null);
                if (l != null)
                    main.addProperty(prop, l);
	        }
	    }
	
	   public static void addSimpleIdElement(String elementName, String typeUri, Element root, Model m, Resource main) {
	        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
	        for (int i = 0; i < nodeList.getLength(); i++) {
	            Element current = (Element) nodeList.item(i);
	            String value = null;
	                value = current.getTextContent().trim();
	                if (value.isEmpty()) return;
	                if (elementName.equals("lcCallNumber")) {value = value.toUpperCase();}
	                if (elementName.equals("isbn")) {
	                    final String validIsbn = isbnvalidator.validate(value);
	                    if (validIsbn != null) {
	                        value = validIsbn;
	                    } else {
	                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "isbn", "invalid isbn: "+value);
	                    }
	                }
	            CommonMigration.addIdentifier(main, typeUri, value);
	        }
	    }
	
    private static void addSimpleDateElement(String elementName, String eventType, Element root, Resource main, String propLocalName) {
        if (main == null)
            return;
        
        NodeList nodeList = root.getElementsByTagNameNS(WPXSDNS, elementName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) return;
            // n.d
            if (value.contains("n") && value.contains("d") && value.length() < 10) {
                return;
            }
            CommonMigration.addDatesToEvent(value, main, propLocalName, eventType);
        }
    }
	
}
