package io.bdrc.xmltoldmigration.xml2files;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;

public class EtextMigration {

    public static final String TEI_PREFIX = "http://www.tei-c.org/ns/1.0";
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    private static XPath xPath = initXpath();
    public static final Map<String, String> distributorToUri = new HashMap<>();
    
    public static boolean addEtextInItem = true;
    
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
    
    public static final List<String> paginatedProviders = Arrays.asList("UCB-OCR", "eKangyur");
    
    public static final Map<String,Boolean> blackList = new HashMap<>();
    static {
        blackList.put("UT1KG8475-WCSDT8_B", true); // nonsensical
        blackList.put("UT1GS53494-I1GS53496", true); // image file names changed too much
        blackList.put("UT00KG0552-I1PD35566", true); // rest: work is withdrawn
        blackList.put("UT00KG0549-I1PD35560", true);
        blackList.put("UT00KG0553-I1PD35568", true);
        blackList.put("UT00KG0550-I1PD35562", true);
        blackList.put("UT00KG0554-I1PD35570", true);
        blackList.put("UT1KG4237-I1PD97704", true);
        blackList.put("UT1KG4239-I1PD97684", true);
        blackList.put("UT1KG4239-I1PD97685", true);
        blackList.put("UT1KG4239-I1PD97686", true);
        blackList.put("UT1KG4239-I1PD97687", true);
        blackList.put("UT1KG4239-I1PD97688", true);
        blackList.put("UT1KG4239-I1PD97689", true);
        blackList.put("UT1KG4239-I1PD97690", true);
    }
    
    public static void migrateEtexts() {
        System.out.println("migrate etexts");
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
            boolean isPaginated = paginatedProviders.contains(distributor);
            boolean needsPageNameTranslation = distributor.equals("UCB-OCR");
            File[] filesL2 = fl1.listFiles();
            for (File fl2 : filesL2) {
                if (!fl2.isDirectory())
                    continue;
                //System.out.println("migrating "+provider+"/"+fl2.getName());
                String itemId = null;
                Model itemModel = ModelFactory.createDefaultModel();
                File[] filesL3 = fl2.listFiles();
                for (File fl3 : filesL3) {
                    if (!fl3.isDirectory() || blackList.containsKey(fl3.getName())) // blacklisting these which looks erroneous 
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
                           ei = migrateOneEtext(fl4.getAbsolutePath(), isPaginated, dst, needsPageNameTranslation);
                           dst.close();
                           if (ei == null) {
                               continue;
                           }
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
                       if (itemId == null) {
                           System.err.println("arg!");
                           System.err.println(ei.toString());
                           continue;
                       }
                       itemModel.add(ei.itemModel);
                       String dst = MigrationApp.getDstFileName("etext", ei.etextId);
                       MigrationHelpers.outputOneModel(ei.etextModel, ei.etextId, dst, "etext");
                    }
                }
                if (itemId != null) { // null in the case of blacklisted works
                    String dst = MigrationApp.getDstFileName("item", itemId);
                    MigrationHelpers.outputOneModel(itemModel, itemId, dst, "item");
                }
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

    public static int getVolumeNumber(String imageGroupId, Model m, String eTextId) {
        if (imageGroupId.startsWith("i")) // UT1KG14557_i1KG14561_0000
            imageGroupId = "I"+imageGroupId.substring(1);
        if (!imageGroupId.startsWith("I")) // UT30012_5742_0000
            imageGroupId = "I"+imageGroupId;
        final Literal oldId = m.createLiteral(imageGroupId);
        final Property volumeNumberP = m.getProperty(CommonMigration.BDO, "volumeNumber");
        final Property legacyIdP = m.getProperty(CommonMigration.ADM, "legacyImageGroupRID");
        final List<Statement> sl = m.listStatements(null, legacyIdP, oldId).toList();
        if (sl.size() == 0) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot find volume with legacy RID "+imageGroupId);
            return 1;
        }
        if (sl.size() > 1)
            System.err.println("two volumes have the legacy ID!");
        Resource volume = sl.get(0).getSubject().asResource();
        Statement s = volume.getProperty(volumeNumberP);
        if (s == null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "volume with legacy RID "+imageGroupId+" has no volume number");
            return 1;
        }
        return s.getInt();
    }
    
    public static Pattern p = Pattern.compile("^UT[^_]+_([^_]+)_(\\d+)$");
    public static int[] fillInfosFromId(String eTextId, Model model, Model itemModel) {
        Matcher m = p.matcher(eTextId);
        if (!m.find()) {
            return new int[] {1, 0}; // always the case, only a few cases
        }
        int seqNum = Integer.parseInt(m.group(2));
        int vol = 1;
        boolean volumeIsImageGroup = false;
        try {
            vol = Integer.parseInt(m.group(1));
            if (vol > 900) {
                volumeIsImageGroup = true; // case of UT21871_4205_0000 : 4205 is not volume 4205, it's I4205
            }
        } catch (NumberFormatException e) {
            volumeIsImageGroup = true;
        }
        if (volumeIsImageGroup) {
            if (itemModel == null) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot understand volume name "+m.group(2));
            } else {
                vol = getVolumeNumber(m.group(1), itemModel, eTextId);
        }
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
    
    private static String lastWorkId = null;
    public static void addItemToWork(String workId, String itemId, String etextId) {
        if (workId.equals(lastWorkId))
            return;
        final String workPath = MigrationApp.getDstFileName("work", workId);
        final Model workModel = MigrationHelpers.modelFromFileName(workPath);
        if (workModel == null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot read item model for image name translation on "+workPath);
            return;
        }
        final Resource workR = workModel.getResource(BDR+workId);
        workR.addProperty(workModel.getProperty(BDO, "workHasItem"), workModel.createResource(BDR+itemId));
        MigrationHelpers.outputOneModel(workModel, workId, workPath, "work");
        lastWorkId = workId;
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
    
    private static Model lastModel = null;
    private static String lastModelId = null;
    public static Model getItemModel(String workId, String etextId) {
        String imageItemId = "I"+workId.substring(1)+"_I001";
        if (lastModelId != null && lastModelId.equals(imageItemId)) {
            return lastModel;
        }
        String imageItemPath = MigrationApp.getDstFileName("item", imageItemId);
        Model imageItemModel = MigrationHelpers.modelFromFileName(imageItemPath);
        if (imageItemModel == null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot read item model for image name translation on "+imageItemPath);
            return null;
        }
        lastModelId = imageItemId;
        lastModel = imageItemModel;
        return imageItemModel;
    }
    
    public static EtextInfos migrateOneEtext(final String path, final boolean isPaginated, final OutputStream contentOut, final boolean needsPageNameTranslation) {
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
        CommonMigration.setPrefixes(itemModel, "item");
        final Model etextModel = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(etextModel, "etext");
        
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
                etextModel.getResource(BDR+"ItemEtext"+(isPaginated?"Paginated":"NonPaginated")));
        
        try {
            e = (Element) ((NodeList)xPath.evaluate("tei:idno[@type='TBRC_TEXT_RID']",
                    publicationStmt, XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
            return null;
        }
        final String etextId = e.getTextContent().trim().replace('-', '_');
        
        if (WorkMigration.addWorkHasItem)
            addItemToWork(workId, itemId, etextId);
        
        if (WorkMigration.addItemForWork)
            itemModel.add(itemModel.getResource(BDR+itemId),
                    etextModel.getProperty(BDO, "itemForWork"),
                    etextModel.getResource(BDR+workId));
        
        if (addEtextInItem)
            etextModel.add(etextModel.getResource(BDR+etextId),
                    etextModel.getProperty(BDO, "eTextInItem"),
                    etextModel.getResource(BDR+itemId));

        etextModel.add(etextModel.getResource(BDR+etextId),
                RDF.type,
                etextModel.getResource(BDR+"Etext"+(isPaginated?"Paginated":"NonPaginated")));
        
        Model imageItemModel = null;
        if (needsPageNameTranslation) {
            imageItemModel = getItemModel(workId, etextId);
            if (imageItemModel == null) {
                System.err.println("error: cannot retrieve item model for "+workId);
                return null;
            }
        }
        
        final int[] volSeqNumInfos = fillInfosFromId(etextId, etextModel, imageItemModel);
        itemModel.add(getItemEtextPart(itemModel, itemId, volSeqNumInfos[0], volSeqNumInfos[1]),
                itemModel.getProperty(BDO, "eTextResource"),
                itemModel.createResource(BDR+etextId));
        
        Map<String,Integer> imageNumPageNum = null;
        if (needsPageNameTranslation) {
            imageNumPageNum = ImageListTranslation.getImageNums(imageItemModel, volSeqNumInfos[0]);
        }
        
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
        
        EtextBodyMigration.MigrateBody(d, contentOut, etextModel, etextId, imageNumPageNum, needsPageNameTranslation, isPaginated);
        
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
