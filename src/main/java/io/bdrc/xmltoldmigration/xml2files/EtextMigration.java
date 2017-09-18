package io.bdrc.xmltoldmigration.xml2files;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;

public class EtextMigration {

    public static final String TEI_PREFIX = "http://www.tei-c.org/ns/1.0";
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    private static XPath xPath = initXpath();
    public static final Map<String, String> distributorToUri = new HashMap<>();
    
    static {
        initDistributorToUri();
    }
    
    public static void initDistributorToUri() {
        String prefix = CommonMigration.BDR+"CP"; // ?
        distributorToUri.put("Dharma Download", prefix+"1");
        distributorToUri.put("Vajra Vidya", prefix+"2");
        distributorToUri.put("Tulku Sangag", prefix+"3");
    }
    
    public static XPath initXpath() {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xPath = factory.newXPath();
        HashMap<String, String> prefMap = new HashMap<String, String>() {{
            put("tei", TEI_PREFIX);
        }};
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xPath.setNamespaceContext(namespaces);
        return xPath;
    }
    
    public static void migrateEtexts() {
        try {
            migrateOneEtext("xml/EtextTest.xml");
        } catch (XPathExpressionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public static class EtextInfos {
        public Model itemModel;
        public Model etextModel;
        public String workId;
        public String itemId;
        public String etextId;
        
        public EtextInfos(Model itemModel, Model etextModel, String workId, String itemId, String etextId) {
            this.itemModel = itemModel;
            this.etextModel = etextModel;
            this.workId = workId;
            this.itemId = itemId;
            this.etextId = etextId;
        }
    }
    
    public static String itemIdFromWorkId(final String workId) {
        return "I"+workId.substring(1)+"_E001";
    }
    
    public static Literal getLiteral(String s, Model m, String etextId) {
        // if the first character is ascii then bo-x-ewts, else tibetan
        int c = s.charAt(0);
        if (c >= 0x0F00 && c <= 0x0FFF)
            return m.createLiteral(s, "bo");
        if (c <= 0x36F)
            return m.createLiteral(s, "bo-x-ewts");
        ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot determine language of "+s);
        return m.createLiteral(s);
    }

    public static Pattern p = Pattern.compile("^UT[^_]+_(\\d+)_(\\d+)$");
    public static int[] fillInfosFromId(String etextId, Model model) {
        Matcher m = p.matcher(etextId);
        if (!m.find()) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot parse etext id "+etextId);
            return null;
        }
        int seqNum = Integer.parseInt(m.group(2));
        int vol = Integer.parseInt(m.group(1));
        if (seqNum == 0) {
            model.add(model.getResource(BDR+"eTextId"),
                model.getProperty(BDO+"eTextIsVolume"),
                model.createTypedLiteral(vol, XSDDatatype.XSDinteger));
        } else {
            model.add(model.getResource(BDR+"eTextId"),
                    model.getProperty(BDO+"eTextInVolume"),
                    model.createTypedLiteral(vol, XSDDatatype.XSDinteger));
            model.add(model.getResource(BDR+"eTextId"),
                    model.getProperty(BDO+"eTextVolumeIndex"),
                    model.createTypedLiteral(seqNum, XSDDatatype.XSDinteger));
        }
        return new int[] {vol, seqNum};
    }
    
    public static Resource getItemEtextPart(Model itemModel, String itemId, int volume, int seqNum) {
        final Resource item = itemModel.getResource(BDR+itemId);
        final Property itemHasVolume = itemModel.getProperty(BDO, "itemHasVolume");
        final Property volumeHasEtext = itemModel.getProperty(BDO, "volumeHasEtext");
        Resource volumeRes = null;
        StmtIterator si = item.listProperties(itemHasVolume);
        while (si.hasNext()) {
            Statement s = si.next();
            Resource r = s.getResource();
            int i = r.getProperty(itemModel.getProperty(BDO, "volumeNumber")).getInt();
            if (volume == i) {
                volumeRes = r;
                break;
            }
        }
        if (volumeRes == null) {
            volumeRes = itemModel.createResource();
            item.addProperty(itemHasVolume, volumeRes);
            volumeRes.addProperty(itemModel.getProperty(BDO, "volumeNumber"), 
                    itemModel.createTypedLiteral(volume, XSDDatatype.XSDinteger));
        }
        Resource seqRes = itemModel.createResource();
        volumeRes.addProperty(volumeHasEtext, seqRes);
        seqRes.addProperty(itemModel.getProperty(BDO, "seqNum"), 
                itemModel.createTypedLiteral(seqNum, XSDDatatype.XSDinteger));
        // TODO: check for duplicates
        return seqRes;
    }
    
    public static EtextInfos migrateOneEtext(final String path) throws XPathExpressionException {
        final Document d = MigrationHelpers.documentFromFileName(path);
        final Element fileDesc = (Element) ((NodeList)xPath.evaluate("/tei:TEI/tei:teiHeader/tei:fileDesc",
                d.getDocumentElement(), XPathConstants.NODESET)).item(0);
        final Element titleStmt = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "titleStmt").item(0);
        final Element publicationStmt = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "publicationStmt").item(0);
        final Element sourceDesc = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "sourceDesc").item(0);
        
        final Model itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel);
        final Model etextModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(etextModel);
        
        Element e = (Element) ((NodeList)xPath.evaluate("tei:bibl/tei:idno[@type='TBRC_RID']",
                sourceDesc, XPathConstants.NODESET)).item(0);
        final String workId = e.getTextContent().trim();
        final String itemId = itemIdFromWorkId(workId);

        itemModel.add(itemModel.getResource(BDR+itemId),
                RDF.type,
                etextModel.getResource(BDR+"ItemEtextInput"));
        
        e = (Element) ((NodeList)xPath.evaluate("tei:idno[@type='TBRC_TEXT_RID']",
                publicationStmt, XPathConstants.NODESET)).item(0);
        final String etextId = e.getTextContent().trim().replace('-', '_');
        
        etextModel.add(etextModel.getResource(BDR+etextId),
                etextModel.getProperty(BDO, "eTextInItem"),
                etextModel.getResource(BDR+itemId));
        // TODO: discriminate between OCR and input
        etextModel.add(etextModel.getResource(BDR+etextId),
                RDF.type,
                etextModel.getResource(BDR+"EtextInput"));
        
        int[] volSeqNumInfos = fillInfosFromId(etextId, etextModel);
        
        itemModel.add(getItemEtextPart(itemModel, itemId, volSeqNumInfos[0], volSeqNumInfos[1]),
                itemModel.getProperty(BDO, "eTextResource"),
                itemModel.createResource(BDR+"etextId"));
        
        final NodeList titles = titleStmt.getElementsByTagNameNS(TEI_PREFIX, "title");
        final List<String> titlesList = new ArrayList<String>();
        for (int i = 0; i < titles.getLength(); i++) {
            Element title = (Element) titles.item(i);
            String titleStr = CommonMigration.normalizeString(title.getTextContent());
            if (!titlesList.contains(titleStr))
                titlesList.add(titleStr);
        }
        for (String s : titlesList) {
            etextModel.add(etextModel.getResource(BDR+etextId),
                    etextModel.getProperty(BDO, "eTextTitle"),
                    getLiteral(s, etextModel, etextId));
        }
        
        e = (Element) ((NodeList)xPath.evaluate("tei:bibl/tei:idno[@type='SRC_PATH']",
                sourceDesc, XPathConstants.NODESET)).item(0);
        etextModel.add(etextModel.getResource(BDR+etextId),
                etextModel.getProperty(BDO, "eTextSourcePath"),
                etextModel.createLiteral(e.getTextContent().trim()));
        
        return new EtextInfos(itemModel, etextModel, workId, itemId, etextId);
    }
    
    // https://stackoverflow.com/a/6392700/2560906
    public static class SimpleNamespaceContext implements NamespaceContext {

        private final Map<String, String> PREF_MAP = new HashMap<String, String>();

        public SimpleNamespaceContext(final Map<String, String> prefMap) {
            PREF_MAP.putAll(prefMap);       
        }

        public String getNamespaceURI(String prefix) {
            return PREF_MAP.get(prefix);
        }

        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }

    }
    
}
