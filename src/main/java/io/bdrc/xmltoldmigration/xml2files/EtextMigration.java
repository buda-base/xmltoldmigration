package io.bdrc.xmltoldmigration.xml2files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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

import io.bdrc.xmltoldmigration.MigrationApp;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.GitHelpers;

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
        distributorToUri.put("DharmaDownload", prefix+"001");
        distributorToUri.put("DrikungChetsang", prefix+"002");
        distributorToUri.put("eKangyur", prefix+"003");
        distributorToUri.put("GuruLamaWorks", prefix+"004");
        distributorToUri.put("KarmaDelek", prefix+"005");
        distributorToUri.put("PalriParkhang", prefix+"006");
        distributorToUri.put("Shechen", prefix+"007");
        distributorToUri.put("TulkuSangag", prefix+"008");
        distributorToUri.put("UCB-OCR", prefix+"009");
        distributorToUri.put("VajraVidya", prefix+"010");
        distributorToUri.put("Various", prefix+"011");
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
        MigrationApp.createDirIfNotExists(MigrationApp.OUTPUT_DIR+"etexts");
        GitHelpers.ensureGitRepo("etext");
        GitHelpers.ensureGitRepo("etextcontent");
        String dirName = MigrationApp.ETEXT_DIR;
        File[] filesL1 = new File(dirName).listFiles();
        for (File fl1 : filesL1) {
            if (!fl1.isDirectory())
                continue;
            String distributor = fl1.getName();
            String distributorUri = distributorToUri.get(distributor);
            boolean isOcr = distributor.equals("UCB-OCR");
            File[] filesL2 = fl1.listFiles();
            for (File fl2 : filesL2) {
                if (!fl2.isDirectory())
                    continue;
                //System.out.println("migrating "+provider+"/"+fl2.getName());
                String itemId = null;
                Model itemModel = ModelFactory.createDefaultModel();
                File[] filesL3 = fl2.listFiles();
                for (File fl3 : filesL3) {
                    if (!fl3.isDirectory())
                        continue;
                    File[] filesL4 = fl3.listFiles();
                    for (File fl4 : filesL4) {
                       if (!fl4.isFile())
                           continue;
                       String name = fl4.getName();
                       if (name.startsWith("_") || !name.endsWith(".xml"))
                           continue;
                       String id = name.substring(0, name.length()-4);
                       String dstName = MigrationApp.getDstFileName("etextcontent", id, ".txt");
                       File dstFile = new File(dstName);
                       EtextInfos ei;
                       try {
                           if (!dstFile.exists())
                               dstFile.createNewFile();
                           FileOutputStream dst = new FileOutputStream(dstFile);
                           ei = migrateOneEtext(fl4.getAbsolutePath(), isOcr, dst);
                           dst.close();
                       } catch (IOException e1) {
                           e1.printStackTrace();
                           return;
                       }
                       if (itemId != null && !ei.itemId.equals(itemId))
                           ExceptionHelper.logException(ExceptionHelper.ET_GEN, fl2.getName(), fl2.getName(), "got two different itemIds: "+itemId+" and "+ei.itemId);
                       if (itemId == null) {
                           itemId = ei.itemId;
                           itemModel.add(itemModel.createResource(BDR+itemId),
                                   itemModel.getProperty(BDO, "eTextDistributor"),
                                   itemModel.createResource(distributorUri));
                       }
                       itemModel.add(ei.itemModel);
                       String dst = MigrationApp.getDstFileName("etext", ei.etextId);
                       MigrationHelpers.outputOneModel(ei.etextModel, ei.etextId, dst, "etext");
                    }
                }
                String dst = MigrationApp.getDstFileName("item", itemId);
                MigrationHelpers.outputOneModel(itemModel, itemId, dst, "item");
                // TODO: write work->item link
            }
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
        // TODO: replace q with ' ?
        ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot determine language of "+s);
        return m.createLiteral(s);
    }

    public static Pattern p = Pattern.compile("^UT[^_]+_([^_]+)_(\\d+)$");
    public static int[] fillInfosFromId(String eTextId, Model model) {
        Matcher m = p.matcher(eTextId);
        if (!m.find()) {
            switch (eTextId) {
            case "UT1KG6007_WPHWEQ_G_0000":
                return new int[] {1, 0};
            case "UT1KG6008_WEOAOB_D_0000":
                return new int[] {1, 0};
            case "UT1KG6085_W6JHPF_L_0000":
                return new int[] {1, 0};
            case "UT1KG6086_WWKP15_8_0000":
                return new int[] {1, 0};
            case "UT1KG6109_WXCTRH_H_0000":
                return new int[] {1, 0};
            case "UT1KG6132_WOBC5A_0_0000":
                return new int[] {1, 0};
            case "UT1KG6161_WI9IWD_U_0000":
                return new int[] {1, 0};
            case "UT1KG6330_W54ZNX_U_0000":
                return new int[] {1, 0};
            case "UT1KG8475_WCSDT8_B_0000":
                return new int[] {1, 0}; // TODO: strange case
            }
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot parse etext id "+eTextId);
            return new int[] {1, 0};
        }
        int seqNum = Integer.parseInt(m.group(2));
        int vol;
        try {
            vol = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            vol = 1;
        }
        if (seqNum == 0) {
            model.add(model.getResource(BDR+eTextId),
                model.getProperty(BDO+"eTextIsVolume"),
                model.createTypedLiteral(vol, XSDDatatype.XSDinteger));
        } else {
            model.add(model.getResource(BDR+eTextId),
                    model.getProperty(BDO+"eTextInVolume"),
                    model.createTypedLiteral(vol, XSDDatatype.XSDinteger));
            model.add(model.getResource(BDR+eTextId),
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
    
    public static EtextInfos migrateOneEtext(final String path, final boolean isOcr, final OutputStream contentOut) {
        final Document d = MigrationHelpers.documentFromFileName(path);
        Element fileDesc;
        try {
            fileDesc = (Element) ((NodeList)xPath.evaluate("/tei:TEI/tei:teiHeader/tei:fileDesc",
                    d.getDocumentElement(), XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            // Having to catch this is utter stupidity
            e1.printStackTrace();
            return null;
        }
        final Element titleStmt = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "titleStmt").item(0);
        final Element publicationStmt = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "publicationStmt").item(0);
        final Element sourceDesc = (Element) fileDesc.getElementsByTagNameNS(TEI_PREFIX, "sourceDesc").item(0);
        
        final Model itemModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(itemModel);
        final Model etextModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(etextModel);
        
        Element e;
        try {
            e = (Element) ((NodeList)xPath.evaluate("tei:bibl/tei:idno[@type='TBRC_RID']",
                    sourceDesc, XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
            return null;
        }
        final String workId = e.getTextContent().trim();
        final String itemId = itemIdFromWorkId(workId);

        itemModel.add(itemModel.getResource(BDR+itemId),
                RDF.type,
                etextModel.getResource(BDR+"ItemEtext"+(isOcr?"OCR":"Input")));
        
        try {
            e = (Element) ((NodeList)xPath.evaluate("tei:idno[@type='TBRC_TEXT_RID']",
                    publicationStmt, XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
            return null;
        }
        final String etextId = e.getTextContent().trim().replace('-', '_');
        
        etextModel.add(etextModel.getResource(BDR+etextId),
                etextModel.getProperty(BDO, "eTextInItem"),
                etextModel.getResource(BDR+itemId));
        // TODO: discriminate between OCR and input
        etextModel.add(etextModel.getResource(BDR+etextId),
                RDF.type,
                etextModel.getResource(BDR+"Etext"+(isOcr?"OCR":"Input")));
        
        final int[] volSeqNumInfos = fillInfosFromId(etextId, etextModel);
        itemModel.add(getItemEtextPart(itemModel, itemId, volSeqNumInfos[0], volSeqNumInfos[1]),
                itemModel.getProperty(BDO, "eTextResource"),
                itemModel.createResource(BDR+etextId));
        
        final NodeList titles = titleStmt.getElementsByTagNameNS(TEI_PREFIX, "title");
        final List<String> titlesList = new ArrayList<String>();
        for (int i = 0; i < titles.getLength(); i++) {
            Element title = (Element) titles.item(i);
            String titleStr = CommonMigration.normalizeString(title.getTextContent());
            if (titleStr.isEmpty())
                continue;
            if (!titlesList.contains(titleStr)) {
                etextModel.add(etextModel.getResource(BDR+etextId),
                        etextModel.getProperty(BDO, "eTextTitle"),
                        getLiteral(titleStr, etextModel, etextId));
            }
        }
        
        try {
            e = (Element) ((NodeList)xPath.evaluate("tei:bibl/tei:idno[@type='SRC_PATH']",
                    sourceDesc, XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
            return null;
        }
        etextModel.add(etextModel.getResource(BDR+etextId),
                etextModel.getProperty(BDO, "eTextSourcePath"),
                etextModel.createLiteral(e.getTextContent().trim()));
        
        EtextBodyMigration.MigrateBody(d, contentOut, etextModel, etextId);
        
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
