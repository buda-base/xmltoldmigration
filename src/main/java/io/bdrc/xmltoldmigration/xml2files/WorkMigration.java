package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.BDG;
import static io.bdrc.libraries.Models.VCARD;
import static io.bdrc.libraries.Models.FacetType;
import static io.bdrc.libraries.Models.FacetType.EVENT;
import static io.bdrc.libraries.Models.FacetType.NAME;
import static io.bdrc.libraries.Models.FacetType.VCARD_ADDR;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getAdminRoot;
import static io.bdrc.libraries.Models.getEvent;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import static io.bdrc.libraries.LangStrings.normalizeTibetan;
import static io.bdrc.libraries.LangStrings.EWTS_TAG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models;
import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;


public class WorkMigration {

	public static final String WXSDNS = "http://www.tbrc.org/models/work#";
	
	public static boolean splitItems = true;
	
	public static boolean addItemForWork = false;
	public static boolean addWorkHasItem = true;
	
	public static Map<String,List<String>> productWorks = new HashMap<>();
	
	public static List<WorkModelInfo> serialWorks = new ArrayList<>();
	public static List<WorkModelInfo> serialMembers = new ArrayList<>();
	
    private static HashMap<String, String> workAccessMap = new HashMap<>();
    private static HashMap<String, String> workLegalMap = new HashMap<>();
    private static HashMap<String, Boolean> workRestrictedInChina = new HashMap<>();
    
    public static Resource getAccess(Model itemModel, Resource work) {
        String legalUri = workAccessMap.get(work.getLocalName());
        if (legalUri != null) {
            return itemModel.createResource(legalUri);
        } else {
            return null;
        }
    }
    
    public static boolean isRestrictedInChina(Model itemModel, Resource work) {
        Boolean ric = workRestrictedInChina.get(work.getLocalName());
        if (ric != null) {
            return ric;
        } else {
            return false;
        }
    }
    
    public static Resource getLegal(Model itemModel, Resource work) {
        String legalUri = workLegalMap.get(work.getLocalName());
        if (legalUri != null) {
            return itemModel.createResource(legalUri);
        } else {
            return null;
        }
    }
	    
	// testing only
    public static List<WorkModelInfo> MigrateWork(Document xmlDocument) {
        Model m = ModelFactory.createDefaultModel();
        setPrefixes(m, "work");
        return MigrateWork(xmlDocument, m, new HashMap<>());
    }
    
    public static boolean isAbstract(final Model m, final String baseName) {
        if (m == null || baseName == null)
            return false; // ?
        final Resource work = m.getResource(BDR+baseName);
        Resource absWorkClass = m.createResource(BDO+"AbstractWork");
        return m.listStatements(work, RDF.type, absWorkClass).hasNext();
    }
    
    public static String getAbstractForRid(final String rid) {
        return "WA"+rid.substring(1);
    }
	
    public static List<Resource> getAbstractList(Model m, Resource mainA) {
        List<Resource> res = new ArrayList<>();
        Selector sel = new SimpleSelector(null, RDF.type, m.createResource(BDO+"AbstractWork"));
        StmtIterator iter = m.listStatements(sel);
        while (iter.hasNext()) {
            Resource next = iter.next().getSubject();
            if (!next.equals(mainA))
                res.add(next);
        }
        return res;
    }
    
    public static final class WorkModelInfo {
        public String resourceName;
        public Model m;
        
        public WorkModelInfo(String resourceName, Model m) {
            this.resourceName = resourceName;
            this.m = m;
        }
    }
    
    public static final Map<String,Boolean> etextInstances = new HashMap<>();
    
	public static List<WorkModelInfo> MigrateWork(Document xmlDocument, Model m, Map<String,Model> itemModels) {
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String workId = root.getAttribute("RID");
		String aWorkId = getAbstractForRid(workId);
		String status = root.getAttribute("status");
		List<WorkModelInfo> res = new ArrayList<>();
        
        // first check info:
        String infoNodeType = "";
        String infoNumber = "";
        String infoParentId = "";
        boolean isSeriesMember = false;
        NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            infoNodeType = current.getAttribute("nodeType").trim();
            infoNumber = current.getAttribute("number").trim();
            isSeriesMember = !infoNumber.isEmpty();
            infoParentId = current.getAttribute("parent").trim();
        }
        if (infoParentId.contains("LEGACY"))
            infoParentId = "";
       
        if (infoParentId.equals(workId)) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, workId, workId, "info", "parent set to the resource RID");
            infoParentId = "";
        }
        
        Model mA = null;
        Resource mainA = null;
        Model mS = null;
        Resource serialW = null;
        boolean isConceptual = false;
        Resource main = null;
        Resource admMain = null;
        String serialWorkId = "";
       
        if (isSeriesMember && !status.equals("withdrawn")) {
            String otherMemberRID = CommonMigration.seriesClusters.get(workId);
            if (otherMemberRID == null) {
                otherMemberRID = workId;
            }
            main = createRoot(m, BDR+workId, BDO+"SerialInstance");
            admMain = createAdminRoot(main);
            res.add(null);
            res.add(new WorkModelInfo(workId, m));
            main.addProperty(m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(infoNumber));

            String seriesMemberId = "WM" + workId.substring(1);
            mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            mainA = createRoot(mA, BDR+seriesMemberId, BDO+"SerialMember");
            Resource admMainA = createAdminRoot(mainA);
            addStatus(mA, admMainA, "released");
            admMainA.addProperty(mA.getProperty(ADM, "metadataLegal"), mA.createResource(BDA+"LD_BDRC_CC0"));
            main.addProperty(m.createProperty(BDO, "serialInstanceOf"), mainA);
            mainA.addProperty(mA.createProperty(BDO, "serialHasInstance"), main);
            res.add(new WorkModelInfo(seriesMemberId, mA));

            serialWorkId = CommonMigration.seriesMembersToWorks.get(otherMemberRID);
            if (serialWorkId == null) {
                if (infoParentId.isEmpty()) {
                    serialWorkId = "WS" + otherMemberRID.substring(1);
                } else {
                    serialWorkId = infoParentId;
                }
                CommonMigration.seriesMembersToWorks.put(otherMemberRID, serialWorkId);

                mS = ModelFactory.createDefaultModel();
                setPrefixes(mS);
                serialW = createRoot(mA, BDR+serialWorkId, BDO+"SerialWork");
                Resource admSerialW = createAdminRoot(serialW);
                addStatus(mS, admSerialW, "released");
                admSerialW.addProperty(mS.getProperty(ADM, "metadataLegal"), mS.createResource(BDA+"LD_BDRC_CC0"));
                res.add(new WorkModelInfo(serialWorkId, mS));
                mainA.addProperty(mA.createProperty(BDO, "serialMemberOf"), mA.createResource(BDR+serialWorkId));
                serialW.addProperty(mS.createProperty(BDO, "serialHasMember"), mainA);
            } else { // serialWork already created just link to it
                mainA.addProperty(mA.createProperty(BDO, "serialMemberOf"), mA.createResource(BDR+serialWorkId));
                SymetricNormalization.addSymetricProperty(mA, "serialMemberOf", seriesMemberId, serialWorkId, null);
            }
        } else if (infoNodeType.equals("conceptualWork") && !status.equals("withdrawn")) {
            main = createRoot(m, BDR+workId, BDO+"AbstractWork");
            admMain = createAdminRoot(main);
            isConceptual = true;
            res.add(null);
            res.add(new WorkModelInfo(workId, m));
        } else {
            if (infoNodeType.equals("unicodeText")) {
                main = createRoot(m, BDR+workId, BDO+"EtextInstance");
                res.add(null);
                res.add(null);
                res.add(null);
                res.add(new WorkModelInfo(workId, m));
                etextInstances.put(workId, true);
            } else {
                main = createRoot(m, BDR+workId, BDO+"Instance"); // physical?
                res.add(new WorkModelInfo(workId, m));
            }
            admMain = createAdminRoot(main);
            if (!status.equals("withdrawn")) {
                String otherAbstractRID = CommonMigration.abstractClusters.get(aWorkId);
                if (otherAbstractRID == null && !infoParentId.isEmpty())
                    otherAbstractRID = infoParentId;
                if (otherAbstractRID == null) {
                    mA = ModelFactory.createDefaultModel();
                    setPrefixes(mA);
                    res.add(new WorkModelInfo(aWorkId, mA));
                    mainA = createRoot(mA, BDR+aWorkId, BDO+"Work");
                    Resource admMainA = createAdminRoot(mainA);
                    addStatus(mA, admMainA, "released");
                    admMainA.addProperty(mA.getProperty(ADM, "metadataLegal"), mA.createResource(BDA+"LD_BDRC_CC0"));
                    main.addProperty(m.createProperty(BDO, "instanceOf"), mainA);
                    mainA.addProperty(mA.createProperty(BDO, "workHasInstance"), main);
                } else {
                    SymetricNormalization.addSymetricProperty(m, "instanceOf", workId, otherAbstractRID, null);
                }
            }
        }
		
		addStatus(m, admMain, status);        
        admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
		
		String value = null;
		Literal lit = null;
		Property prop;
		
		CommonMigration.addNotes(m, root, main, WXSDNS);
		// TODO: this could be handled better:
		if (mainA != null)
		    CommonMigration.addExternals(m, root, mainA, WXSDNS);
		else 
		    CommonMigration.addExternals(m, root, main, WXSDNS);
	    CommonMigration.addLog(m, root, admMain, WXSDNS);
		
	    CommonMigration.addTitles(m, main, root, WXSDNS, true, false, mainA);
	    // put a prefLabel on the serialW if needed
	    if (isSeriesMember) {
	        RDFNode serialWorkLabel = CommonMigration.seriesMembersToWorkLabels.get(serialWorkId);
	        if (serialWorkLabel == null ) {
	            Statement s = mainA.getProperty(SKOS.prefLabel);
	            if (s == null) {
	                s = main.getProperty(SKOS.prefLabel);
	            }
	            if (s != null && serialW != null) { // TODO: why would serialW be null?
	                serialW.addProperty(SKOS.prefLabel, s.getObject());
	                CommonMigration.seriesMembersToWorkLabels.put(serialWorkId, s.getObject());
	            }
	        }
	    }

	    if (mainA == null)
	        CommonMigration.addSubjects(m, main, root, WXSDNS);
	    else
	        CommonMigration.addSubjects(mA, mainA, root, WXSDNS);
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
		
		nodeList = root.getElementsByTagNameNS(WXSDNS, "archiveInfo");
		boolean hasArchiveInfo = false;
		boolean hasAccess = false;
		boolean hasLicense = false;
		String accessUri = null;
		String legalUri = null;
		boolean isRestrictedInChina = false;
		int nbvols = -1;
        for (int i = 0; i < nodeList.getLength(); i++) {
            hasArchiveInfo = true;
            current = (Element) nodeList.item(i);
            String licenseValue = current.getAttribute("license").trim();
            if (licenseValue.equals("copyright")) 
                licenseValue = BDA+"LD_BDRC_Copyright";
            else
                licenseValue = BDA+"LD_BDRC_PD";
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
            case "restrictedInChina":
                value = (licenseValue.contains("Copyright") ? "AccessFairUse" : "AccessOpen");
                isRestrictedInChina = true;
                break;
            default: value = ""; break;
            }
            if (!value.isEmpty()) {
                accessUri = BDA+value;
                hasAccess = true;
            }
            legalUri = licenseValue;

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
        if (hasArchiveInfo && !hasAccess) {
            accessUri = BDA+"AccessOpen";
        }        
        if (hasArchiveInfo && !hasLicense) {
            legalUri = BDA+"LD_BDRC_PD";
        }
        
        // these maps are queried in ImagegroupMigration and EtextMigration 
        // to fill in the corresponding Item via MigrationApp.moveAdminInfo()
        workAccessMap.put(workId, accessUri);
        workLegalMap.put(workId, legalUri);
        workRestrictedInChina.put(workId, isRestrictedInChina);

        // creator
        // this is a list of the abstract works of the part of the work
        List <Resource> subAbstracts = null; 
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
                if (!MigrationHelpers.isDisconnected(person)) {
                    if (mainA != null && !CommonMigration.creatorForInstance.contains(value)) {
                        if (subAbstracts == null) {
                            subAbstracts = getAbstractList(m, mainA);
                        }
                        for (Resource r : subAbstracts) {
                            CommonMigration.addAgentAsCreator(main, m.createResource(BDR+person), value, r);
                        }
                    }
                    if (!isConceptual) {
                        CommonMigration.addAgentAsCreator(main, m.createResource(BDR+person), value, mainA);
                    } else {
                        CommonMigration.addAgentAsCreator(null, m.createResource(BDR+person), value, main);
                    }
                }
            }
        }
        if (!isConceptual) {        
            // inProduct
            
            nodeList = root.getElementsByTagNameNS(WXSDNS, "inProduct");
            for (int i = 0; i < nodeList.getLength(); i++) {
                current = (Element) nodeList.item(i);
                String content = current.getTextContent().trim();
                value = current.getAttribute("pid").trim();
    
                if (content.startsWith("Collection:")) {
                    String cpUri = BDA+"CP04"+value.substring(value.length()-1);
                    admMain.addProperty(m.createProperty(ADM+"contentProvider"), m.createResource(cpUri));
                } else if (content.startsWith("Catalog:")) {
                    Property notep = m.getProperty(BDO+"note");
                    Resource note = null;
                    StmtIterator notes = main.listProperties(notep);
                    while (notes.hasNext()) {
                        Statement noteStmt = notes.next();
                        Statement noteText = noteStmt.getResource().getProperty(m.getProperty(BDO+"noteText"));
                        String noteTextStr = noteText.getString();
                        if (noteTextStr.startsWith("Catalog")) {
                            note = noteStmt.getResource();
                            m.remove(noteText);
                            break;
                        }
                    }
                    if (note == null) {
                        note = getFacetNode(FacetType.NOTE, main);
                        note.addProperty(m.getProperty(BDO+"noteText"), "Catalog");
                        main.addProperty(notep, note);
                    }
                    Resource cat = 
                            value.equals("PR1FEMC01") ? m.createResource(BDR+"W1FEMC01") : 
                           (value.equals("PR1FEMC02") ? m.createResource(BDR+"W1FEMC02") : m.createResource(BDA+value));
                    note.addProperty(m.getProperty(BDO+"noteWork"), cat);
                } else {
                    List<String> worksForProduct = productWorks.computeIfAbsent(value, x -> new ArrayList<String>());
                    worksForProduct.add(main.getLocalName());
                }
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
                    String itemRid = BDR+"I"+root.getAttribute("RID").substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX;
                    Resource item = m.createResource(itemRid);
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
            //exportTitleInfo(m);
        }
        SymetricNormalization.insertMissingTriplesInModel(m, root.getAttribute("RID"));
		return res;
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
	
	public static void exportTitleInfo(Model m) {
	    Selector sel = new SimpleSelector(null, RDF.type, m.createResource(BDO+"Work"));
	    StmtIterator iter = m.listStatements(sel);
        while (iter.hasNext()) {
            Resource next = iter.next().getSubject();
            String title = next.getLocalName()+",";
            Selector selaac = new SimpleSelector(next, m.createProperty(BDO, "creator"), (Node) null);
            StmtIterator iteraac = m.listStatements(selaac);
            while (iteraac.hasNext()) {
                Resource nextaac = iteraac.next().getObject().asResource();
                Resource role = nextaac.getPropertyResourceValue(m.createProperty(BDO, "role"));
                if (role != null && ("R0ER0019".equals(role.getLocalName()) || "R0ER0025".equals(role.getLocalName()))) {
                    Resource agent = nextaac.getPropertyResourceValue(m.createProperty(BDO, "agent"));
                    if (agent != null) {
                        title += agent.getLocalName()+":";
                        break;
                    }
                }
            }
            Statement s = next.getProperty(SKOS.prefLabel, "bo-x-ewts");
            if (s != null) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, "", "", "title: "+title+s.getString());
            }
        }
	}
	
}
