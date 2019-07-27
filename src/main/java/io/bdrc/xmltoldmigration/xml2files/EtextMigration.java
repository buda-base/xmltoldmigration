package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;
import static io.bdrc.libraries.GitHelpers.ensureGitRepo;

import java.io.File;
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

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationApp;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.ImageListTranslation;

public class EtextMigration {

    public static final String TEI_PREFIX = "http://www.tei-c.org/ns/1.0";
    public static boolean testMode = false;
    private static XPath xPath = initXpath();
    public static final Map<String, String> distributorToUri = new HashMap<>();
    
    public static boolean addEtextInItem = true;
    
    static {
        initDistributorToUri();
    }
    
    public static void initDistributorToUri() {
        String prefix = BDA+"CP"; // ?
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
        @SuppressWarnings("serial")
        HashMap<String, String> prefMap = new HashMap<String, String>() {{ 
            put("tei", TEI_PREFIX); 
        }};
        SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
        xPath.setNamespaceContext(namespaces);
        return xPath;
    }

    public static final List<String> paginatedProviders = Arrays.asList("UCB-OCR", "eKangyur");
    
    public static final Map<String,Boolean> blackListL2 = new HashMap<>();
    public static final Map<String,Boolean> blackListL3 = new HashMap<>();
    public static final Map<String,Boolean> blackListL4 = new HashMap<>();
    static {
        blackListL3.put("UT1KG8475-WCSDT8_B", true); // nonsensical
        blackListL3.put("UT1PD45495-012", true);
        blackListL3.put("UT3JT13306-329", true);
        blackListL4.put("UT22082_007_0014.xml", true); // empty
        blackListL4.put("UT1KG14_008_0014.xml", true);
        blackListL4.put("UT1KG14_036_0026.xml", true);
        blackListL4.put("UT1KG14_053_0038.xml", true);
        blackListL3.put("UT1GS53494-I1GS53496", true); // image file names changed too much
        blackListL3.put("UT00KG0552-I1PD35566", true); // rest: work is withdrawn
        blackListL3.put("UT00KG0549-I1PD35560", true);
        blackListL3.put("UT00KG0553-I1PD35568", true);
        blackListL3.put("UT00KG0550-I1PD35562", true);
        blackListL3.put("UT00KG0554-I1PD35570", true);
        blackListL3.put("UT1KG4237-I1PD97704", true);
        blackListL2.put("UT1KG4239", true);
        blackListL4.put("UT1PD45495-011-0002.xml", true);
        blackListL4.put("UT1PD45495-011-0003.xml", true);
        blackListL4.put("UT1PD45495-011-0004.xml", true);
        blackListL4.put("UT1PD45495-011-0005.xml", true);
        blackListL4.put("UT1PD45495-011-0007.xml", true);
        blackListL4.put("UT1PD45495-011-0008.xml", true);
        blackListL4.put("UT1PD45495-011-0009.xml", true);
        blackListL4.put("UT1PD45495-011-00010.xml", true);
        blackListL4.put("UT1PD45495-011-00011.xml", true);
        blackListL4.put("UT1PD45495-011-00012.xml", true);
        blackListL4.put("UT1PD45495-011-00013.xml", true);
        blackListL4.put("UT1PD45495-011-00014.xml", true);
        blackListL4.put("UT1PD45495-011-00015.xml", true);
        blackListL4.put("UT1PD45495-011-00015.xml", true);
        blackListL4.put("UT1KG4884-017-0001.xml", true);
        blackListL4.put("UT1KG4884-017-0002.xml", true);
        blackListL4.put("UT1KG4884-017-0003.xml", true);
        blackListL4.put("UT1KG4884-017-0005.xml", true);
        blackListL4.put("UT1KG4884-017-0007.xml", true);
        blackListL4.put("UT1KG4884-018-0001.xml", true);
        blackListL4.put("UT1KG4884-017-0002.xml", true);
        blackListL4.put("UT1KG4884-017-0003.xml", true);
        blackListL4.put("UT1KG4884-017-0008.xml", true);
    }   
    
    public static void migrateEtexts() {
        System.out.println("migrate etexts");
        MigrationApp.createDirIfNotExists(MigrationApp.OUTPUT_DIR+"etexts");
        ensureGitRepo("etext", MigrationApp.OUTPUT_DIR);
        ensureGitRepo("etextcontent", MigrationApp.OUTPUT_DIR);
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
                if (!fl2.isDirectory() || blackListL2.containsKey(fl2.getName()))
                    continue;
                //System.out.println("migrating "+provider+"/"+fl2.getName());
                String itemId = null;
                Model itemModel = ModelFactory.createDefaultModel();
                setPrefixes(itemModel, "item");
                boolean firstItemModel = true;
                File[] filesL3 = fl2.listFiles();
                for (File fl3 : filesL3) {
                    if (!fl3.isDirectory() || blackListL3.containsKey(fl3.getName())) // blacklisting these which looks erroneous 
                        continue;
                    File[] filesL4 = fl3.listFiles();
                    for (File fl4 : filesL4) {
                       if (!fl4.isFile())
                           continue;
                       String name = fl4.getName();
                       if (name.startsWith("_") || !name.endsWith(".xml") || blackListL4.containsKey(name))
                           continue;
                       String id = name.substring(0, name.length()-4).replace('-', '_');
                       String dstName = MigrationApp.getDstFileName("etextcontent", id, ".txt");
                       File dstFile = new File(dstName);
                       EtextInfos ei;
                       try {
                           if (!dstFile.exists())
                               dstFile.createNewFile();
                           FileOutputStream dst = new FileOutputStream(dstFile);
                           ei = migrateOneEtext(fl4.getAbsolutePath(), isPaginated, dst, needsPageNameTranslation, itemModel, firstItemModel, distributorUri);
                           firstItemModel = false;
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
                       }
                       if (itemId == null) {
                           System.err.println("arg!");
                           System.err.println(ei.toString());
                           continue;
                       }
                       
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
        public Model etextModel;
        public String workId;
        public String itemId;
        public String etextId;
        
        public EtextInfos(Model etextModel, String workId, String itemId, String etextId) {
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
        final Property volumeNumberP = m.getProperty(BDO, "volumeNumber");
        final Property legacyIdP = m.getProperty(ADM, "legacyImageGroupRID");
        final List<Statement> sl = m.listStatements(null, legacyIdP, oldId).toList();
        if (sl.size() == 0) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot find volume with legacy RID "+imageGroupId);
            return 1;
        }
        if (sl.size() > 1)
            System.err.println("two volumes have the legacy ID!");
        Resource volumeAdm = sl.get(0).getSubject().asResource();
        Resource volume = volumeAdm.getPropertyResourceValue(m.getProperty(ADM, "adminAbout"));
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
                ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "cannot understand volume name "+m.group(1));
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
    public static void addItemToWork(String workId, String itemId, String etextId, boolean isPaginated) {
        if (workId.equals(lastWorkId))
            return;
        final String workPath = MigrationApp.getDstFileName("work", workId, ".trig");
        final Model workModel = MigrationHelpers.modelFromFileName(workPath);
        if (workModel == null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot read item model for image name translation on "+workPath);
            return;
        }
        final Resource workR = workModel.getResource(BDR+workId);
        Property p = workModel.getProperty(BDO, "workHasItem");
        workR.addProperty(p, workModel.createResource(BDR+itemId));
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
            volumeRes = getFacetNode(FacetType.VOLUME,  item, itemModel.getResource(BDO+"VolumeEtextAsset"));
            item.addProperty(itemHasVolume, volumeRes);
            volumeRes.addProperty(itemModel.getProperty(BDO, "volumeNumber"), 
                    itemModel.createTypedLiteral(volume, XSDDatatype.XSDinteger));
            volumeRes.addProperty(itemModel.getProperty(BDO, "volumeOf"), item);
        }
        
        Resource seqRes = getFacetNode(FacetType.ETEXT_REF,  item);
        volumeRes.addProperty(volumeHasEtext, seqRes);
        if (seqNum != 0)
            seqRes.addProperty(itemModel.getProperty(BDO, "seqNum"), 
                itemModel.createTypedLiteral(seqNum, XSDDatatype.XSDinteger));
        else 
            seqRes.addProperty(itemModel.getProperty(BDO, "seqNum"), 
                    itemModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
        // TODO: check for duplicates
        return seqRes;
    }
    
    private static Model lastModel = null;
    private static String lastModelId = null;
    public static Model getItemModel(String workId, String etextId) {
        String imageItemId = "I"+workId.substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX;
        if (lastModelId != null && lastModelId.equals(imageItemId)) {
            return lastModel;
        }
        String imageItemPath = MigrationApp.getDstFileName("item", imageItemId, ".trig");
        Model imageItemModel = MigrationHelpers.modelFromFileName(imageItemPath);
        if (imageItemModel == null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, etextId, etextId, "cannot read item model for image name translation on "+imageItemPath);
            return null;
        }
        lastModelId = imageItemId;
        lastModel = imageItemModel;
        return imageItemModel;
    }
    
    public static EtextInfos migrateOneEtext(String path, boolean isPaginated, OutputStream contentOut, boolean needsPageNameTranslation, Model itemModel, boolean first, String providerUri) {
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

        final Model etextModel = ModelFactory.createDefaultModel();
        setPrefixes(etextModel, "etext");
        
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
        
        try {
            e = (Element) ((NodeList)xPath.evaluate("tei:idno[@type='TBRC_TEXT_RID']",
                    publicationStmt, XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
            return null;
        }
        final String etextId = e.getTextContent().trim().replace('-', '_');
        Resource etext = createRoot(etextModel, BDR+etextId, BDO+"Etext"+(isPaginated?"Paginated":"NonPaginated"));

        if (first) { // initialize the :ItemEtext
            Resource work = itemModel.getResource(BDR+workId);
            Resource item = createRoot(itemModel, BDR+itemId, BDO+"ItemEtext"+(isPaginated?"Paginated":"NonPaginated"));

            // Item AdminData
            Resource admItem = createAdminRoot(item);                           
            admItem.addProperty(itemModel.getProperty(ADM, "contentProvider"), itemModel.createResource(providerUri));
            admItem.addProperty(itemModel.getProperty(ADM, "metadataLegal"), itemModel.createResource(BDA+"LD_BDRC_CC0"));
            MigrationApp.moveAdminInfo(itemModel, work, admItem);
            addReleased(itemModel, admItem);

            // Item metadata

            if (WorkMigration.addWorkHasItem)
                addItemToWork(workId, itemId, etextId, isPaginated);

            if (WorkMigration.addItemForWork) {
                item.addProperty(itemModel.getProperty(BDO, "itemForWork"), work);
            }
        }

        if (addEtextInItem)
            etextModel.add(etext,
                    etextModel.getProperty(BDO, "eTextInItem"),
                    etextModel.getResource(BDR+itemId));

        etextModel.add(etext,
                RDF.type,
                etextModel.getResource(BDO+"Etext"+(isPaginated?"Paginated":"NonPaginated")));
        
        Resource admEtext = getAdminRoot(etext, true);
        addReleased(etextModel, admEtext);
        
        Model imageItemModel = null;
        if (isPaginated && !testMode) {
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
                etextModel.add(etext,
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
        etextModel.add(etext,
                etextModel.getProperty(BDO, "eTextSourcePath"),
                etextModel.createLiteral(e.getTextContent().trim()));
        
        EtextBodyMigration.MigrateBody(d, contentOut, etextModel, etextId, imageNumPageNum, needsPageNameTranslation, isPaginated);
        
        return new EtextInfos(etextModel, workId, itemId, etextId);
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
