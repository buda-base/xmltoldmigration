package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.IntStream;

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

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.EAPTransfer;
import io.bdrc.xmltoldmigration.MigrationApp;
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
	
    static HashMap<String, String> workAccessMap = new HashMap<>();
    private static HashMap<String, String> workLegalMap = new HashMap<>();
    public static HashMap<String, Boolean> workRestrictedInChina = new HashMap<>();
    
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
        MigrationHelpers.setPrefixes(m, "work");
        return MigrateWork(xmlDocument, m, new HashMap<>());
    }
    
    public static boolean isAbstract(final Model m, final String baseName) {
        if (m == null || baseName == null)
            return false; // ?
        final Resource work = m.getResource(BDR+baseName);
        Resource absWorkClass = m.createResource(BDO+"Work");
        return m.listStatements(work, RDF.type, absWorkClass).hasNext();
    }
    
    public static String getAbstractForRid(final String rid) {
        return "WA"+rid.substring(1);
    }
	
    public static List<Resource> getAbstractList(Model m, Resource mainA) {
        List<Resource> res = new ArrayList<>();
        Selector sel = new SimpleSelector(null, RDF.type, m.createResource(BDO+"Work"));
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
    
    public static void addRedirection(String oldRid, String newRid, Model m) {
        Resource oldRes = m.createResource(BDR+oldRid);
        Resource newRes = m.createResource(BDR+newRid);
        Resource admOldRes = createAdminRoot(oldRes);
        addStatus(m, admOldRes, "withdrawn");
        admOldRes.addProperty(m.createProperty(ADM, "replaceWith"), newRes);
    }
    
    public static String normalizeScanInfo(final Resource main, String s, final Element root) {
        if (s.contains("atluj")) {
            int cidx = s.indexOf("Comments: ");
            String comments = "";
            if (cidx != -1) {
                comments = " "+s.substring(cidx);
            }
            String address = "Scanned at M/S Satluj Infotech Images, E-45, Sector 27 Noida, District Gautam Buddha Nagar, U.P. 201301 via New Delhi, India for the Buddhist Digital Resource Center.";
            if (s.contains("ingh")) {
                if (s.contains("infotech")) {
                    address = "Scanned at M/S Satluj Infotech Images, 63-F Sujan Singh Park, New Delhi, India for the Buddhist Digital Resource Center.";
                } else {
                    address = "Scanned by M/S Satluj Siti Enterprises, 63-F Sujan Singh Park, New Delhi, India for the Buddhist Digital Resource Center.";                    
                }
                
            }
            return address+comments;
        }
        if (s.startsWith("Scanned at Tibetan Buddhist Resource Center, 150 West 17th St, New York City") || s.startsWith("Scanned at Tibetan Buddhist Resource Center, 1430") || s.startsWith("Scanned at Scanned in partnership with")) {
            // this is the most complex case. The baseline is:
            // if the scanInfo says NY or Cambridge:
            //     if it's before mid-2012, it's NY and we can keep the original string
            //     if it's after April 2016, it doesn't mean anything and we should keep just the comment
            //     if it's between the two it's Cambridge (and it should be set as such, keeping the comments)
            try {
              int i = Integer.parseInt(s.substring(s.length()-4));
              if (i < 2012) {
                  return s;
              }
              if (i > 2016) {
                  int cidx = s.indexOf("Comments: ");
                  if (cidx == -1)
                      return null;
                  return s.substring(cidx+10).trim();
              }
            }
            catch (NumberFormatException nfe) {}
            int[] sryearmonth = approximateSrYear(root);
            if (sryearmonth == null) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "scanInfo", "can't find scanrequest date");
                return s;
            }
            if (sryearmonth[0] < 2012 || (sryearmonth[0] == 2012 && sryearmonth[1] < 7)) {
                return s;
            }
            if (sryearmonth[0] > 2016 || (sryearmonth[0] == 2016 && sryearmonth[1] > 4)) {
                int cidx = s.indexOf("Comments: ");
                if (cidx == -1)
                    return null;
                return s.substring(cidx+10).trim();
            }
            return s;
        }
        return s;
    }
    
    public static int[] approximateSrYear(final Element root) {
        NodeList nodeList = root.getElementsByTagNameNS(WXSDNS, "log");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element log = (Element) nodeList.item(i);
            NodeList logEntriesList = log.getElementsByTagNameNS(WXSDNS, "entry");
            for (int j = 0; j < logEntriesList.getLength(); j++) {
                Element logEntry = (Element) logEntriesList.item(j);
                if (logEntry.getTextContent().toLowerCase().startsWith("added volumemap for scan request")) {
                    String date = logEntry.getAttribute("when");
                    if (date != null && !date.isEmpty()) {
                        int year = Integer.parseInt(date.substring(0,4));
                        int month = Integer.parseInt(date.substring(5,7));
                        return IntStream.of(year, month).toArray();
                    }
                }
            }
            logEntriesList = log.getElementsByTagName("entry");
            for (int k = 0; k < logEntriesList.getLength(); k++) {
                Element logEntry = (Element) logEntriesList.item(k);
                if (logEntry.getTextContent().toLowerCase().startsWith("added volumemap for scan request")) {
                    String date = logEntry.getAttribute("when");
                    if (date != null && !date.isEmpty()) {
                        int year = Integer.parseInt(date.substring(0,4));
                        int month = Integer.parseInt(date.substring(5,7));
                        return IntStream.of(year, month).toArray();
                    }
                }
            }
        }
        return null;
    }
    
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
        Resource admMainA = null;
        Model mS = null;
        Resource serialW = null;
        Resource main = null;
        Resource admMain = null;
        String serialWorkId = "";
        boolean canonicalConceptualWork = false;
       
        if (isSeriesMember && !status.equals("withdrawn")) {
            String otherMemberRID = CommonMigration.seriesClusters.get(workId);
            if (otherMemberRID == null) {
                otherMemberRID = workId;
            }
            main = createRoot(m, BDR+"M"+workId, BDO+"SerialInstance");
            admMain = createAdminRoot(main);
            res.add(null);
            res.add(new WorkModelInfo("M"+workId, m));
            main.addProperty(m.getProperty(BDO, "workSeriesNumber"), m.createLiteral(infoNumber));

            String seriesMemberId = "WA" + workId.substring(1);
            mA = ModelFactory.createDefaultModel();
            setPrefixes(mA);
            mainA = createRoot(mA, BDR+seriesMemberId, BDO+"SerialMember");
            admMainA = createAdminRoot(mainA);
            addStatus(mA, admMainA, "released");
            main.addProperty(m.createProperty(BDO, "instanceOf"), mainA);
            mainA.addProperty(mA.createProperty(BDO, "workHasInstance"), main);
            res.add(new WorkModelInfo(seriesMemberId, mA));
            serialWorkId = CommonMigration.seriesMembersToWorks.get(otherMemberRID);
            
            if (serialWorkId == null) {
                if (infoParentId.isEmpty()) {
                    serialWorkId = "WAS" + otherMemberRID.substring(1);
                } else {
                    serialWorkId = "WAS" + infoParentId.substring(1);
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
            addRedirection(workId, aWorkId, m);
            main = null;
            admMain = null;
            mainA = createRoot(m, BDR+aWorkId, BDO+"Work");
            admMainA = createAdminRoot(mainA);
            canonicalConceptualWork = EAPTransfer.rKTsRIDMap.containsValue(aWorkId);
            mA = ModelFactory.createDefaultModel();
            addStatus(m, admMainA, "released");
            res.add(null);
            res.add(new WorkModelInfo(aWorkId, m));
        } else {
            if (infoNodeType.equals("unicodeText")) {
                addRedirection(workId, "IE"+workId.substring(1), m);
                main = createRoot(m, BDR+"IE"+workId.substring(1), BDO+"EtextInstance");
                res.add(null);
                res.add(null);
                res.add(null);
                res.add(new WorkModelInfo("IE"+workId.substring(1), m));
                etextInstances.put(workId, true);
            } else {
                main = createRoot(m, BDR+'M'+workId, BDO+"Instance"); // physical?
                res.add(new WorkModelInfo('M'+workId, m));
            }
            admMain = createAdminRoot(main);
            if (!status.equals("withdrawn") && !workId.startsWith("W1EAP") && !workId.startsWith("W1FPL") && !workId.startsWith("W1FEMC")) {
                String otherAbstractRID = CommonMigration.abstractClusters.get(aWorkId);
                if (otherAbstractRID == null && !infoParentId.isEmpty()) {
                    otherAbstractRID = WorkMigration.getAbstractForRid(infoParentId);
                }
                mA = ModelFactory.createDefaultModel();
                setPrefixes(mA);
                if (res.size() == 1) {
                    res.add(new WorkModelInfo(aWorkId, mA));
                } else {
                    res.set(1, new WorkModelInfo(aWorkId, mA));
                }
                mainA = createRoot(mA, BDR+aWorkId, BDO+"Work");
                admMainA = createAdminRoot(mainA);
                if (otherAbstractRID == null) {
                    addStatus(mA, admMainA, status);
                    main.addProperty(m.createProperty(BDO, "instanceOf"), mainA);
                    mainA.addProperty(mA.createProperty(BDO, "workHasInstance"), main);
                } else {
                    addRedirection(aWorkId, otherAbstractRID, mA);
                    // we don't put the has instance property... it would be better conceptually but
                    // it would make the queries slower and harder to write
                    //mainA.addProperty(mA.createProperty(BDO, "workHasInstance"), main);
                    SymetricNormalization.addSymetricProperty(m, "instanceOf", 'M'+workId, otherAbstractRID, null);
                }
            }
        }
		
        if (admMain != null) {
            addStatus(m, admMain, status);
            admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
        } 
        if (admMainA != null) {
            admMainA.addProperty(mA.getProperty(ADM, "metadataLegal"), mA.createResource(BDA+"LD_BDRC_CC0"));
        }
		
		String value = null;
		Literal lit = null;
		Property prop;
		if (admMain != null) {
		    CommonMigration.addNotes(m, root, main, WXSDNS);
		    CommonMigration.addExternals(m, root, main, WXSDNS);
		} else if (admMainA != null) {
		    CommonMigration.addNotes(mA, root, mainA, WXSDNS);
		    CommonMigration.addExternals(mA, root, mainA, WXSDNS);
		}
		
		// log entries go on the work if possible:
		if (admMainA != null) {
		    CommonMigration.addLog(mA, root, admMainA, WXSDNS, false);
		} else if (admMain != null) {
		    CommonMigration.addLog(m, root, admMain, WXSDNS, false);
		}

		if (!canonicalConceptualWork) {
		    // don't migrate the titles of the canonical conceptual works, they will be handled by the rKTs data
		    CommonMigration.addTitles(m, main, root, WXSDNS, true, false, mainA);
		}
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

	    if (mainA != null)
	        CommonMigration.addSubjects(mA, mainA, root, WXSDNS);
	    //else
	    //    CommonMigration.addSubjects(m, main, root, WXSDNS);
	        
	    
	    if (main != null) {
    	    Map<String,Model> itemModelsFromDesc = CommonMigration.addDescriptions(m, root, main, WXSDNS, false, mainA);
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
                        prop = m.getProperty(BDO, "numberOfVolumes");
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
            workAccessMap.put('M'+workId, accessUri);
            workLegalMap.put('M'+workId, legalUri);
            workRestrictedInChina.put('M'+workId, isRestrictedInChina);
            if (isRestrictedInChina) {
                admMain.addLiteral(m.getProperty(ADM, "restrictedInChina"), isRestrictedInChina);
                if (mainA != null) {
                    admMainA.addLiteral(mA.getProperty(ADM, "restrictedInChina"), isRestrictedInChina);
                }
            }
        }
	    
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
            // temporary fix: let's not add creators for abstract works, as most of them are canonical texts
            if (main == null && !value.equals("hasMainAuthor")) {
                continue;
            }
            String person = current.getAttribute("person").trim(); // required
            if (person.isEmpty()) continue;
            if (person.equals("Add to DLMS")) {
                person = current.getTextContent().trim();
                if (!person.isEmpty())
                    ExceptionHelper.logException(ExceptionHelper.ET_MISSING, main.getLocalName(), main.getLocalName(), "creator", "needs to be added to dlms: `"+value+"`");
            } else {
                person = MigrationHelpers.sanitizeRID(root.getAttribute("RID"), value, person);
                if (!MigrationHelpers.isDisconnected(person)) {
                    if (mainA != null && !CommonMigration.creatorForInstance.contains(value)) {
                        if (subAbstracts == null) {
                            subAbstracts = getAbstractList(m, mainA);
                        }
                        for (Resource r : subAbstracts) {
                            CommonMigration.addAgentAsCreator(main, m.createResource(BDR+person), value, r);
                        }
                    }
                    CommonMigration.addAgentAsCreator(main, m.createResource(BDR+person), value, mainA);
                }
            }
        }
        if (main != null) {        
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
                    note.addProperty(m.getProperty(BDO+"noteSource"), cat);
                } else {
                    List<String> worksForProduct = productWorks.computeIfAbsent(value, x -> new ArrayList<String>());
                    worksForProduct.add(workId);
                }
            }
            
            // catalogInfo
        
            nodeList = root.getElementsByTagNameNS(WXSDNS, "catalogInfo");
            for (int i = 0; i < nodeList.getLength(); i++) {
                current = (Element) nodeList.item(i);
                Literal l = CommonMigration.getLiteral(current, "en", m, "catalogInfo", main.getLocalName(), null);
                if (l == null) continue;
                if (mainA != null)
                    mainA.addProperty(mA.getProperty(BDO, "catalogInfo"), l);
            }
            
            // scanInfo
            
            nodeList = root.getElementsByTagNameNS(WXSDNS, "scanInfo");
            for (int i = 0; i < nodeList.getLength(); i++) {
                current = (Element) nodeList.item(i);
                Literal l = CommonMigration.getLiteral(current, "en", m, "scanInfo", main.getLocalName(), null);
                if (l == null) continue;
                String s = l.getString();
                s = normalizeScanInfo(main, s, root);
                if (s == null) continue;
                l = m.createLiteral(s, "en");
                main.addProperty(m.getProperty(BDO, "scanInfo"), l);
            }

            NodeList volumes = root.getElementsByTagNameNS(WXSDNS, "volume");
            int lastVolume = 0;
            List<String> missingVolumes = new ArrayList<>();
            for (int j = 0; j < volumes.getLength(); j++) {
                // just adding an item if we have a volume list
                if (j == 0) {
                    String itemRid = BDR+"W"+root.getAttribute("RID").substring(1)+CommonMigration.IMAGE_ITEM_SUFFIX;
                    Resource item = m.createResource(itemRid);
                    if (WorkMigration.addWorkHasItem)
                        m.add(main, m.getProperty(BDO, "instanceHasReproduction"), item);
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
	
    public static final class Volinfo {
        public Integer volnum;
        public String imagegroup;
        
        Volinfo(Integer volnum, String imagegroup) {
            this.volnum = volnum;
            this.imagegroup = imagegroup;
        }
    }
	
	public static class ImageGroupInfo {
	    public String missingVolumes;
	    public List<Volinfo> imageGroupList;
	    public int totalVolumes;
	    
	    public ImageGroupInfo(String missingVolumes, List<Volinfo> imageGroupList, int totalVolumes) {
	        this.missingVolumes = missingVolumes;
	        this.imageGroupList = imageGroupList;
	        this.totalVolumes = totalVolumes;
	    }
	}
	
	public static ImageGroupInfo getImageGroupList(Document d, int nbVolsTotal) {
	    List<Volinfo> res = new ArrayList<>(); 
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
            //if (res.containsKey(num))
            //    ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "volume", "volume list has two or more image groups for volume `"+num+"`");
            res.add(new Volinfo(num, igId));
        }
        for (Volinfo vi : res) {
            int thisVol = vi.volnum;
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
	    if (!MigrationApp.exportTitles) {
	        return;
	    }
	    Selector sel = new SimpleSelector(null, RDF.type, m.createResource(BDO+"Work"));
	    StmtIterator iter = m.listStatements(sel);
        while (iter.hasNext()) {
            Resource next = iter.next().getSubject();
            String[] values = new String[3];
            
            values[0] = next.getLocalName();
            Selector selaac = new SimpleSelector(next, m.createProperty(BDO, "workHasInstance"), (RDFNode) null);
            StmtIterator iteraac = m.listStatements(selaac);
            String title = "";
            while (iteraac.hasNext()) {
                Statement saac = iteraac.next();
                Resource nextaac = saac.getObject().asResource();
                if (!nextaac.getLocalName().startsWith("M")) continue;
                values[1] = nextaac.getLocalName();
            }
            selaac = new SimpleSelector(next, m.createProperty(BDO, "creator"), (Node) null);
            iteraac = m.listStatements(selaac);
            List<String> creators = new ArrayList<>();
            boolean hasCommentator = false;
            while (iteraac.hasNext()) {
                Resource nextaac = iteraac.next().getObject().asResource();
                Resource role = nextaac.getPropertyResourceValue(m.createProperty(BDO, "role"));
                if (hasCommentator == false && role != null && ("R0ER0019".equals(role.getLocalName()) || "R0ER0025".equals(role.getLocalName()))) {
                    Resource agent = nextaac.getPropertyResourceValue(m.createProperty(BDO, "agent"));
                    if (agent != null)
                        creators.add(agent.getLocalName());
                } else if (role != null && "R0ER0014".equals(role.getLocalName())) {
                    if (!hasCommentator)
                        creators.clear();
                    hasCommentator = true;
                    Resource agent = nextaac.getPropertyResourceValue(m.createProperty(BDO, "agent"));
                    if (agent != null)
                        creators.add(agent.getLocalName());
                }
            }
            Collections.sort(creators);
            for (String c : creators) {
                title += c+":";
            }
            selaac = new SimpleSelector(next, m.createProperty(BDO, "language"), (Node) null);
            iteraac = m.listStatements(selaac);
            List<String> languages = new ArrayList<>();
            while (iteraac.hasNext()) {
                Resource nextaac = iteraac.next().getObject().asResource();
                languages.add(nextaac.getLocalName());
            }
            Collections.sort(languages);
            for (String l : languages) {
                title += l+":";
            }
            selaac = new SimpleSelector(next, SKOS.prefLabel, (Node) null);
            iteraac = m.listStatements(selaac);
            String label = null;
            while (iteraac.hasNext()) {
                Literal nextaac = iteraac.next().getObject().asLiteral();
                if (nextaac.getLanguage().equals("bo-x-ewts") || label == null)
                    label = nextaac.getString();
            }
            if (label == null) {
                return;
            }
            title += label;
            title = title.replace("\"", "").replace(",", "");
            values[2] = title;
            MigrationApp.csvWriter.writeNext(values);
        }
	}
	
}
