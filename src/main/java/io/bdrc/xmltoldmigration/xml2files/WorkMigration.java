package io.bdrc.xmltoldmigration.xml2files;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;


public class WorkMigration {

    private static final String BDA = CommonMigration.BDA;
    private static final String BDO = CommonMigration.BDO;
    private static final String BDR = CommonMigration.BDR;
    @SuppressWarnings("unused")
    private static final String ADM = CommonMigration.ADM;
	public static final String WXSDNS = "http://www.tbrc.org/models/work#";
	
	public static boolean splitItems = true;
	
	public static boolean addItemForWork = false;
	public static boolean addWorkHasItem = true;
	
	public static Map<String,List<String>> productWorks = new HashMap<>();
	
    private static HashMap<String, Resource> workAccessMap = new HashMap<String, Resource>();
    private static HashMap<String, Resource> workLegalMap = new HashMap<String, Resource>();
    
    public static Resource getAcceess(String workId) {
        return workAccessMap.get(workId);
    }
    
    public static Resource getLLegal(String workId) {
        return workLegalMap.get(workId);
    }
	    
	// testing only
    public static Model MigrateWork(Document xmlDocument) {
        Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, "work");
        return MigrateWork(xmlDocument, m, new HashMap<>());
    }
    
    public static boolean isAbstract(final Model m, final String baseName) {
        if (m == null || baseName == null)
            return false; // ?
        final Resource r = m.getResource(BDR+baseName);
        final Resource workType = r.getPropertyResourceValue(m.createProperty(BDO, "workType"));
        if (workType == null)
            return false;
        return workType.getLocalName().equals("WorkTypeAbstractWork");
    }
	    
	public static Model MigrateWork(Document xmlDocument, Model m, Map<String,Model> itemModels) {
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String workId = root.getAttribute("RID");
        Resource main = m.createResource(BDR + root.getAttribute("RID"));
        Resource admMain = MigrationHelpers.getAdmResource(m, root.getAttribute("RID"));
		m.add(main, RDF.type, m.createResource(BDO + "Work"));
		
		CommonMigration.addStatus(m, admMain, root.getAttribute("status"));
		
		String value = null;
		Literal lit = null;
		Property prop;
		
		CommonMigration.addNotes(m, root, main, WXSDNS);
	    CommonMigration.addExternals(m, root, main, WXSDNS);
	    CommonMigration.addLog(m, root, admMain, WXSDNS);
		
	    CommonMigration.addTitles(m, main, root, WXSDNS, true, false);
	    CommonMigration.addSubjects(m, main, root, WXSDNS);
	    Map<String,Model> itemModelsFromDesc = CommonMigration.addDescriptions(m, root, main, WXSDNS);
	    if (itemModelsFromDesc != null) {
	        if (!splitItems) {
	            for (Model itemModel : itemModelsFromDesc.values()) {
	                m.add(itemModel);
	            }
	        } else {
	            itemModels.putAll(itemModelsFromDesc);
	        }
	    }
		
		// archiveInfo
		
		NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "archiveInfo");
		boolean hasAccess = false;
		boolean hasLicense = false;
		Resource workAccess = null;
		Resource workLicense = null;
		int nbvols = -1;
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String licenseValue = current.getAttribute("license").trim();
            if (licenseValue.equals("copyright")) 
                licenseValue = BDA+"LD_BDRC_Copyright";
            else
                licenseValue = BDA+"LD_BDRC_Open";
            hasLicense = true;
            
            value = current.getAttribute("access").trim();
            switch (value) {
            case "openAccess":
                value = "AccessOpen"; 
                break;
            case "fairUse": 
                // just in case...
                // https://github.com/BuddhistDigitalResourceCenter/library-issues/issues/59
                licenseValue = BDA+"LD_BDRC_Copyright"; 
                value = "AccessFairUse";
                break;
            case "restrictedSealed": value = "AccessRestrictedSealed"; break;
            case "temporarilyRestricted": value = "AccessRestrictedTemporarily"; break;
            case "restrictedByQuality": value = "AccessRestrictedByQuality"; break;
            case "restrictedByTbrc": value = "AccessRestrictedByTbrc"; break;
            case "restrictedInChina": value = "AccessRestrictedInChina"; break;
            default: value = ""; break;
            }
            if (!value.isEmpty()) {
                workAccess = m.createResource(BDA+value);
                hasAccess = true;
            }
            workLicense = m.createResource(licenseValue);

            String nbVolsStr = current.getAttribute("vols").trim();
            if (nbVolsStr.isEmpty())
                continue;
            
            try {
                nbvols = Integer.parseUnsignedInt(nbVolsStr);
                if (nbvols != 0) {
                    prop = m.getProperty(BDO, "workNumberOfVolumes");
                    lit = m.createTypedLiteral(nbvols, XSDDatatype.XSDinteger);
                    m.add(main, prop, lit);
                }
            } catch (NumberFormatException e) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "archiveInfo/vols", "cannot parse number of volumes `"+current.getAttribute("vols").trim()+"`");
            }
        }
        if (!hasAccess) {
            workAccess = m.createResource(BDA+"AccessOpen");
        }        
        if (!hasLicense) {
            workLicense = m.createResource(BDA+"LD_BDRC_Open");
        }
        
        // these maps are queried in ImagegroupMigration and EtextMigration 
        // to fill in the corresponding Item
        workAccessMap.put(workId, workAccess);
        workLegalMap.put(workId, workLicense);

        // info
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String nodeType = current.getAttribute("nodeType");
            switch (nodeType) {
            case "unicodeText": value = BDR+"WorkTypeUnicodeText"; break;
            case "conceptualWork": value = BDR+"WorkTypeAbstractWork"; break;
            case "publishedWork": value = BDR+"WorkTypePublishedWork"; break;
            case "series": value = BDR+"WorkTypeSeries"; break;
            default: value = ""; break;
            }
            if (!value.isEmpty()) {
                main.addProperty(m.getProperty(BDO, "workType"), m.getResource(value));
            }
            boolean numbered = false;
            // will be overwritten when reading the pubinfo
            value = current.getAttribute("number");
            if (!value.isEmpty()) {
                main.addProperty(m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(value));
                numbered = true;
            };
            value = current.getAttribute("numbered"); 
            if (!value.isEmpty()) {
                numbered = true;
            }
            if (numbered) {
                prop = m.getProperty(BDO, "workIsNumbered");
                m.add(main, prop, m.createTypedLiteral(true));
            }
            value = current.getAttribute("parent").trim();
            if (!value.isEmpty() && !value.contains("LEGACY")) {
                if (value.equals(main.getLocalName())) {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "info", "parent set to the resource RID");
                    continue;
                }
                if (numbered) {
                    SymetricNormalization.addSymetricProperty(m, "workNumberOf", main.getLocalName(), value, null);
                } else {
                    SymetricNormalization.addSymetricProperty(m, "workExpressionOf", main.getLocalName(), value, null);
                }
            }
        }
        
        // creator
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "creator");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("type").trim();
            if (value.isEmpty()) {
                value = "hasMainAuthor";
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty()) continue;
            if (person.equals("Add to DLMS")) {
                person = current.getTextContent().trim();
                if (!person.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "creator", "needs to be added to dlms: `"+value+"`");
            } else {
                person = MigrationHelpers.sanitizeRID(main.getLocalName(), value, person);
                if (!MigrationHelpers.isDisconnected(person))
                    CommonMigration.addAgentAsCreator(m, main, m.createResource(BDR+person), value);
            }
        }
        
        // inProduct
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "inProduct");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            value = current.getAttribute("pid").trim();
            List<String> worksForProduct = productWorks.computeIfAbsent(value, x -> new ArrayList<String>());
            worksForProduct.add(main.getLocalName());
            //m.add(main, m.getProperty(ADM, "workInProduct"), m.createResource(BDR+value));
        }
        
        // catalogInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "catalogInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            Literal l = CommonMigration.getLiteral(current, "en", m, "catalogInfo", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(m.getProperty(BDO, "workCatalogInfo"), l);
        }
        
        // scanInfo
        
        nodeList = root.getElementsByTagNameNS(WXSDNS, "scanInfo");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            Literal l = CommonMigration.getLiteral(current, "en", m, "scanInfo", main.getLocalName(), null);
            if (l == null) continue;
            main.addProperty(m.getProperty(BDO, "workScanInfo"), l);
        }
        
        NodeList volumes = root.getElementsByTagNameNS(WXSDNS, "volume");
        int lastVolume = 0;
        List<String> missingVolumes = new ArrayList<>();
        for (int j = 0; j < volumes.getLength(); j++) {
            // just adding an item if we have a volume list
            if (j == 0) {
                Resource item = m.createResource(BDR+"I"+root.getAttribute("RID").substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX);
                if (WorkMigration.addWorkHasItem)
                    m.add(main, m.getProperty(BDO, "workHasItem"), item);
            }
            // then curate the volume list to add missing volumes
            Element volume = (Element) volumes.item(j);
            String igId = volume.getAttribute("imagegroup").trim();
            if (igId.isEmpty()) continue;
            if (!igId.startsWith("I")) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "volume", "image group `"+igId+"` does not start with `I`");
                continue;
            }
            String num = volume.getAttribute("num").trim();
            if (num.isEmpty()) {
                ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "volume", "missing volume number for image group `"+igId+"`");
                continue;
            }
            int thisVol;
            try {
                thisVol = Integer.parseUnsignedInt(num);
            } catch (NumberFormatException e) {
                ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "volume", "cannot parse volume number `"+num+"` for image group `"+igId+"`");
                continue;
            }
            if (thisVol <= lastVolume) {
                ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "volume", "volume list is not in the correct order (`"+lastVolume+"` before for image group `"+thisVol+"`)");
                continue;
            }
            if (thisVol != lastVolume+1) {
                int rangeB = lastVolume+1;
                int rangeE = thisVol-1;
                if (rangeB == rangeE)
                    missingVolumes.add(Integer.toString(rangeB));
                else
                    missingVolumes.add(rangeB+"-"+rangeE);
            }
        }
        
        
        SymetricNormalization.insertMissingTriplesInModel(m, root.getAttribute("RID"));
        
		return m;
	}
	
	public static class ImageGroupInfo {
	    public String missingVolumes;
	    public Map<Integer,String> imageGroupList;
	    public int totalVolumes;
	    
	    public ImageGroupInfo(String missingVolumes, Map<Integer,String> imageGroupList, int totalVolumes) {
	        this.missingVolumes = missingVolumes;
	        this.imageGroupList = imageGroupList;
	        this.totalVolumes = totalVolumes;
	    }
	}
	
	public static ImageGroupInfo getImageGroupList(Document d, int nbVolsTotal) {
	    Map<Integer,String> res = new TreeMap<>(); 
	    Element root = d.getDocumentElement();
	    String rid = root.getAttribute("RID");
	    NodeList volumes = root.getElementsByTagNameNS(WXSDNS, "volume");
        int lastVolume = 0;
        List<String> missingVolumes = new ArrayList<>();
        Map<String,Boolean> hasImageGroup = new HashMap<String,Boolean>();
        for (int j = 0; j < volumes.getLength(); j++) {
            // then curate the volume list to add missing volumes
            Element volume = (Element) volumes.item(j);
            String igId = volume.getAttribute("imagegroup").trim();
            if (igId.isEmpty()) continue;
            if (!igId.startsWith("I")) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "volume", "image group `"+igId+"` does not start with `I`");
                continue;
            }
            if (hasImageGroup.containsKey(igId))
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "volume", "image group `"+igId+"` used twice in the volume map");
            hasImageGroup.put(igId, true);
            final Integer num;
            try {
                num = Integer.parseUnsignedInt(volume.getAttribute("num").trim());
            } catch (NumberFormatException ex) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "volume", "cannot parse volume number `"+volume.getAttribute("num").trim()+"` for image group `"+igId+"`");
                continue;
            }
            if (res.containsKey(num))
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "volume", "volume list has two or more image groups for volume `"+num+"`");
            res.put(num, igId);
        }
        for (Entry<Integer,String> e : res.entrySet()) {
            int thisVol = e.getKey();
            if (thisVol != lastVolume+1) {
                int rangeB = lastVolume+1;
                int rangeE = thisVol-1;
                if (rangeB == rangeE)
                    missingVolumes.add(Integer.toString(rangeB));
                else
                    missingVolumes.add(rangeB+"-"+rangeE);
            }
            lastVolume = thisVol;
        }
        if (nbVolsTotal != 0 && lastVolume != 0) {
            if (lastVolume > nbVolsTotal) {
                if (res.size() > nbVolsTotal)
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "archiveInfo/vols", "work claims it has `"+nbVolsTotal+"` volumes while referencing `"+res.size()+"` volumes in image groups");
                lastVolume = nbVolsTotal;
            } else if (lastVolume < nbVolsTotal) {
                int rangeB = lastVolume+1;
                int rangeE = nbVolsTotal;
                if (rangeB == rangeE)
                    missingVolumes.add(Integer.toString(rangeB));
                else
                    missingVolumes.add(rangeB+"-"+rangeE);
            }
        }
        String missingVols = String.join(",", missingVolumes);
        return new ImageGroupInfo(missingVols, res, lastVolume);
	}
	
}
