package io.bdrc.xmltoldmigration.xml2files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.VCARD4;
import org.apache.jena.vocabulary.XSD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlascopco.hunspell.Hunspell;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.EwtsFixer;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import openllet.core.exceptions.InternalReasonerException;

public class CommonMigration  {

	public static final String BDO = "http://purl.bdrc.io/ontology/core/";
	public static final String ADM = "http://purl.bdrc.io/ontology/admin/";
	public static final String BDA = "http://purl.bdrc.io/admindata/";
    public static final String BDG = "http://purl.bdrc.io/graph/";
	public static final String BDR = "http://purl.bdrc.io/resource/";
	public static final String VCARD = VCARD4.getURI();	
	
	public static final String USER = "MigrationApp";
	
	public static final String FPL_LIBRARY_ID = "G1TLMFPL000001";
	
	public static final String EWTS_TAG = "bo-x-ewts";
	public static final boolean lowerCaseLangTags = true;
	public static final String IMAGE_ITEM_SUFFIX = "";
	
	public static final int ET_LANG = ExceptionHelper.ET_LANG;
	
	public static final EwtsConverter converter = new EwtsConverter();
	public static final EwtsConverter converterAlalc = new EwtsConverter(true, true, false, false, EwtsConverter.Mode.ALALC);
	public static final String hunspellBoPath = "src/main/resources/hunspell-bo/";
    public static final Hunspell speller = new Hunspell(hunspellBoPath+"bo.dic", hunspellBoPath+"bo.aff");
    
    
    public static final Map<String, String> typeToRepo = new HashMap<>();
    public static final Map<String, String> logWhoToUri = new HashMap<>();
    public static final Map<String, Boolean> genreTopics = new HashMap<>();
    public static final Map<Integer, Boolean> isTraditional = new HashMap<>();
    public static final Map<String, String> creatorMigrations = new HashMap<>();
    
    public static MessageDigest md5;
    private static final int hashNbChars = 2;
    private static final int nbShaChars = 12;

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        fillLogWhoToUri();
        fillGenreTopics();
        getTcList();
        initCreatorMigrations();
        fillTypeToRepo();
    }
    
    private static void fillTypeToRepo() {
        typeToRepo.put("Corporation", BDA+"GR0001");
        
        typeToRepo.put("Etext", BDA+"GR0002");
        typeToRepo.put("EtextNonPaginated", BDA+"GR0002");
        typeToRepo.put("EtextPaginated", BDA+"GR0002");
        
        typeToRepo.put("Item", BDA+"GR0003");
        typeToRepo.put("ItemEtext", BDA+"GR0003");
        typeToRepo.put("ItemEtextNonPaginated", BDA+"GR0003");
        typeToRepo.put("ItemEtextPaginated", BDA+"GR0003");
        typeToRepo.put("ItemImageAsset", BDA+"GR0003");
        typeToRepo.put("ItemPhysicalAsset", BDA+"GR0003");
        
        typeToRepo.put("Lineage", BDA+"GR0004");
        typeToRepo.put("Place", BDA+"GR0005");
        typeToRepo.put("Person", BDA+"GR0006");
        typeToRepo.put("Product", BDA+"GR0011");
        typeToRepo.put("Topic", BDA+"GR0007");
        
        typeToRepo.put("Work", BDA+"GR0008");
        typeToRepo.put("AbstractWork", BDA+"GR0008");
        typeToRepo.put("PublishedWork", BDA+"GR0008");
        typeToRepo.put("SerialWork", BDA+"GR0008");
        typeToRepo.put("UnicodeWork", BDA+"GR0008");
        typeToRepo.put("VirtualWork", BDA+"GR0008");
        typeToRepo.put("UnspecifiedWorkClass", BDA+"GR0008");

        typeToRepo.put("EtextContent", BDA+"GR0009");
        typeToRepo.put("Role", BDA+"GR0010");
        typeToRepo.put("Product", BDA+"GR0011");
    }
    
    private static void initCreatorMigrations() {
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("creator-migrations.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                String[] key_val = line.split(",");
                creatorMigrations.put(key_val[0], key_val[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void getTcList() {
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("tclist.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                isTraditional.put(line.codePointAt(0), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static final String userNumFormat = "%05d";
    
    private static void fillGenreTopics() {
        final ClassLoader classLoader = CommonMigration.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("topics-genres.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                genreTopics.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String getMd5(String resId) {
        try {
            // keeping files from the same work together:
            final int underscoreIndex = resId.indexOf('_');
            String message = resId;
            if (underscoreIndex != -1)
                message = resId.substring(0, underscoreIndex);
            final byte[] bytesOfMessage = message.getBytes("UTF-8");
            final byte[] hashBytes = md5.digest(bytesOfMessage);
            BigInteger bigInt = new BigInteger(1,hashBytes);
            return String.format("%032x", bigInt).substring(0, hashNbChars);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Resource createRoot(Model m, String uri, String typeUri) {
        Resource rez = m.createResource(uri);
        if (typeUri != null) rez.addProperty(RDF.type, m.createResource(typeUri));
        rez.addLiteral(m.createProperty(BDO+"isRoot"), true);
        return rez;
    }
    
    /**
     * Returns an AdminData resource for the resource r. Beware that if r IS an AdminData resource this
     * method will return the same resource with an additional stmt declaring this resource to be 
     * adm:adminAbout itself.
     * 
     * @param res resource for which the corresponding AdminData resource is requested.
     * @param same if true then the 
     * @return
     */
    public static Resource getAdminData(Resource rez) {
        Model m = rez.getModel();
        Resource admR = m.createResource(BDA+rez.getLocalName());
        admR.addProperty(RDF.type, m.createResource(ADM+"AdminData"));
        m.add(admR, m.createProperty(ADM+"adminAbout"), rez);
        
        return admR;
    }
    
    public static Resource getRepoFor(String typeName) {
        String repoUri = typeToRepo.get(typeName);
        if (repoUri != null) {
            return ResourceFactory.createResource(repoUri);
        } else {
            return null;
        }
    }

    private static Resource getRezRepo(Resource rez) {
        Statement typeStmt = rez.getProperty(RDF.type);
        String typeName = typeStmt != null ? typeStmt.getObject().asResource().getLocalName(): null;
        Resource repoR = null;
        
        if (typeName != null) {
            repoR = getRepoFor(typeName);
            if (repoR != null)
                return repoR;
        } else { // this case covers Product (may be others) which is both AdminData and Product
            StmtIterator stmtItr = rez.listProperties(RDF.type);
            if (stmtItr != null) {
                while (stmtItr.hasNext()) {
                    Statement stmt = stmtItr.next();
                    String nm = stmt.getObject().asResource().getLocalName();
                    if ("AdminData".equals(nm)) {
                        continue;
                    } else {
                        repoR = getRepoFor(typeName);
                        if (repoR != null) {
                            return repoR;
                        }
                    }
                        
                }
            }
        }

        return null;
    }
    
    /**
     * Returns a root AdminData resource for the root resource rez. If rez IS a BDA: resource this
     * method will return the same resource with an additional stmt declaring this resource to be 
     * adm:adminAbout itself, and adm:isRoot true.
     * 
     * @param rez resource for which the corresponding AdminData resource is requested.
     * @return the root AdminData resource
     */
    public static Resource createAdminRoot(Resource rez) {
        Model m = rez.getModel();
        Resource admR = getAdminData(rez);
        Resource repoR = getRezRepo(rez);
        
        if (repoR != null) {
            // add GitInfo
            String gitInfoUri = BDA+mintId(admR, admR.getURI(), "GT");
            Resource gitInfo = m.createResource(gitInfoUri);
            admR.addProperty(m.createProperty(ADM+"gitInfo"), gitInfo);
            // fill in GitInfo
            gitInfo.addProperty(RDF.type, m.createResource(ADM+"GitInfo"));
            gitInfo.addProperty(m.createProperty(ADM+"gitRepo"), repoR);
            String rid = rez.getLocalName();
            gitInfo.addProperty(m.createProperty(ADM+"gitPath"), getMd5(rid)+"/"+rid+".trig");
        } else {
            // probably called from TaxonomyMigration - 
            // nothing to do since taxonomies aren't stored in their own repo
        }

        // TO BE REMOVED - not needed since presence of ?s adm:gitInfo ?o indicates that ?s is a root
        // AdminData
        admR.addLiteral(m.createProperty(BDO+"isRoot"), true);

        return admR;
    }

    public static Resource getAdminRoot(Resource rez) {
        return getAdminRoot(rez, false);
    }
    
    public static Resource getAdminRoot(Resource rez, boolean create) {
        Resource admR = getAdminRoot(rez.getModel());
        
        if (admR == null && create) {
            admR = createAdminRoot(rez);
        }
        
        return admR;
    }
    
    public static Resource getAdminRoot(Model m) {
        ResIterator resIt = m.listResourcesWithProperty(RDF.type, m.createResource(ADM+"AdminData"));
        while (resIt.hasNext()) {
            Resource admR = resIt.next();
            Statement stmt = m.getProperty(admR, m.createProperty(ADM+"gitInfo"));
            RDFNode node = stmt != null ? stmt.getObject() : null;
            if (node != null) {
                return admR;
            }
            // TO BE REMOVED
            if (m.containsLiteral(admR, m.createProperty(BDO+"isRoot"), true)) {
                return admR;
            }
        }
        
        return null;
    }
    
    private static Map<String, FacetType> strToFacetType = new HashMap<>();
    public enum FacetType {

        CREATOR("creator", "CR", BDO+"AgentAsCreator"), 
        EVENT("event", "EV", BDO+"Event"), 
        HOLDER("lineageHolder", "LH", BDO+"LineageHolder"), 
        LOG_ENTRY("logEntry", "LG", ADM+"LogEntry"), 
        NAME("name", "NM", BDO+"PersonName"),
        NOTE("note", "NT", BDO+"Note"),
        TITLE("title", "TT", BDO+"WorkTitle"),
        WORK_LOC("workLoc", "WL", BDO+"WorkLocation"),
        VCARD_ADDR("vcardAddr", "VA", VCARD+"Address")
        ;

        private String label;
        private String prefix;
        private String nodeTypeUri;

        private FacetType(String label, String prefix) {
            this(label, prefix, null);
        }

        private FacetType(String label, String prefix, String nodeTypeUri) {
            this.label = label;
            this.prefix = prefix;
            this.nodeTypeUri = nodeTypeUri;
            strToFacetType.put(prefix, this);
        }

        public static FacetType getType(String prefix) {
            return strToFacetType.get(prefix);
        }
        
        public String getPrefix() {
            return prefix;
        }

        public Resource getNodeType() {
            return ResourceFactory.createResource(nodeTypeUri);
        }

        @Override
        public String toString() {
            return label;
        }
    }
    
    /**
     * retrieves the adm:facetIndex for admin data resource, rootAdmRez, for the given facet type, increment the index and store
     * it back into the underlying model.
     * @param rootAdmRez admin data resource containing the adm:facetIndex to be used
     * @return
     */
    private static int getFacetIndex(Resource rootAdmRez) {
        Model m = rootAdmRez.getModel();
        Property inxP = m.createProperty(ADM+"facetIndex");
        Statement stmt = m.getProperty(rootAdmRez, inxP);
        
        int inx = 1;
        if (stmt != null) {
           inx = stmt.getInt();
           m.remove(stmt);
        }
        
        m.addLiteral(rootAdmRez, inxP, inx+1);
        return inx;
    }
    
    // returns hex string in uppercase
    private static String  bytesToHex(byte[] hash) {
        return DatatypeConverter.printHexBinary(hash);
    }
    
    public static String mintId(Resource rootAdmRez, String seed, String prefix) {
        try {
        String data = seed+getFacetIndex(rootAdmRez);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes("UTF-8"));
        return prefix+bytesToHex(hash).substring(0, nbShaChars);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    /**
     * Returns prefix + 8 character unique id based on a hash of the concatenation of the last three string arguments. 
     * This is itended to be used to generate ids for named nodes like Events, AgentAsCreator, WorkTitle
     * and PersonName.
     * 
     * @param facet enum type to provide a prefix will that distinguish what the id will identify
     * @param rez subject resource that refers to an Event, AgentAsCreator etc
     * @param user some String identifying the user or tool that is creating the id, make unique to the user
     * @param rootAdmRez adm:AdminData resource for the top-level subject, relevant particularly for :Works which may have many sub-parts
     * @return id = facet prefix + 8 character hash substring
     */
    private static String generateId(FacetType facet, Resource rez, String user, Resource rootAdmRez) {
        return mintId(rootAdmRez, rez.getLocalName()+user, facet.getPrefix());
    }
    
    /**
     * Creates a facet node in the BDR namespace w/ the default facet nodeType from the facet enum
     * 
     * @param facet the type of facet node to create
     * @param rez the resource the node is associated w/ such as via bdo:creator
     * @param user a string identifying the user or tool requesting the facet node
     * @return the newly minted facet node resource of rdf:type default nodeType for the facet
     */
    public static Resource getFacetNode(FacetType facet, Resource rez) {
        Resource nodeType = facet.getNodeType();
        return getFacetNode(facet, BDR, rez, nodeType);
    }

    /**
     * Creates a facet node in the BDR namespace w/ the supplied facet nodeType. This method takes a nodeType resource
     * for use with PersonName, WorkTitle and Events since these have many specialized sub-types.
     * 
     * @param facet the type of facet node to create
     * @param rez the resource the node is associated w/ such as via bdo:creator
     * @param user a string identifying the user or tool requesting the facet node
     * @param nodeType the class for the type of node
     * @return the newly minted facet node resource of rdf:type nodeType
     */
    public static Resource getFacetNode(FacetType facet, Resource rez, Resource nodeType) {
        return getFacetNode(facet, BDR, rez, nodeType);
    }

    public static Resource getFacetNode(FacetType facet, String nsUri, Resource rez, Resource nodeType) {
        Model m = rez.getModel();
        Resource rootAdm = getAdminRoot(rez);
        String id = generateId(facet, rez, USER, rootAdm);
        Resource facetNode = m.createResource(nsUri+id);
        facetNode.addProperty(RDF.type, nodeType);
        return facetNode;
    }

    private static String getCreatorRoleUri(String type) {
        if (type.startsWith("has"))
            type = type.substring(3);
        return BDR+creatorMigrations.get(type);
    }
    
    /**
     * Creates a new named AgentAsCreator node and adds bdo:creator node to the supplied work.
     * 
     * @param work that the AgentAsCreator is a creator for
     * @param person that is the creating agent
     * @param roleKey the name of the type pof role of the creator
     * @param rootAdmWork the root AdminData that contains the adm:facetIndex
     */
    public static void addAgentAsCreator(Resource work, Resource person, String roleKey) {
        Model m = work.getModel();
        Resource agentAsCreator = getFacetNode(FacetType.CREATOR, work);
        work.addProperty(m.createProperty(BDO+"creator"), agentAsCreator);
        agentAsCreator.addProperty(m.createProperty(BDO+"agent"), person);
        Resource role = m.createResource(getCreatorRoleUri(roleKey));
        agentAsCreator.addProperty(m.createProperty(BDO+"role"), role);
    }
    
    public static Resource getEvent(Resource r, String eventType, String eventProp) {
        Model m = r.getModel();
        Property prop = m.createProperty(BDO, eventProp);
        StmtIterator it = r.listProperties(prop);
        while (it.hasNext()) {
            Statement s = it.next();
            Resource event = s.getObject().asResource();
            Resource eventTypeR = event.getPropertyResourceValue(RDF.type);
            if (eventTypeR != null && eventTypeR.getLocalName().equals(eventType)) {
                return event;
            }
        }
        Resource event = m.createResource();
        m.add(event, RDF.type, m.createProperty(BDO+eventType));
        m.add(r, prop, event);
        return event;
    }
    
    public static void addDatesToEvent(String dateStr, Resource r, String eventProp, String eventType) {
        Resource event = getEvent(r, eventType, eventProp);
        addDates(dateStr, event, r);
    }
    
    public static void addDates(String dateStr, final Resource event) {
        addDates(dateStr, event, null);
    }
    
    public static void addDates(String dateStr, final Resource event, final Resource mainResource) {
        if (dateStr == null || dateStr.isEmpty())
            return;
        dateStr = normalizeString(dateStr);
        dateStr = dateStr.replaceAll(" ", "");
        dateStr = dateStr.replaceAll("\\[", "");
        dateStr = dateStr.replaceAll("\\]", "");
        if (dateStr.length() < 3)
            return;
        final Model m = event.getModel();
        if (dateStr.endsWith("?")) {
            dateStr = dateStr.substring(0, dateStr.length()-1);
        }
        if (dateStr.charAt(1) == '.') { // for b., d. and c. 
            dateStr = dateStr.substring(2);
        }
        if (dateStr.endsWith(".000000")) {
            dateStr = dateStr.substring(0, dateStr.length()-7);
        }
        try {
            final int exact = Integer.parseInt(dateStr);
            m.add(event, m.getProperty(BDO, "onYear"), m.createTypedLiteral(exact, XSDDatatype.XSDinteger));    
            return;
        } catch (NumberFormatException e) {}
        int slashidx = dateStr.indexOf('/');
        if (slashidx == -1) {
            slashidx = dateStr.indexOf('-');
            // if the date starts with -, it's not a separator
            if (slashidx == 0)
                slashidx = -1;
        }
        if (slashidx != -1) {
            String firstDate = dateStr.substring(0, slashidx);
            String secondDate = dateStr.substring(slashidx+1, dateStr.length());
            firstDate = firstDate.replace('u', '0');
            secondDate = secondDate.replace('u', '9');
            try {
                final int notBefore = Integer.parseInt(firstDate);
                m.add(event, m.getProperty(BDO, "notBefore"), m.createTypedLiteral(notBefore, XSDDatatype.XSDinteger));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            try {
                final int notAfter = Integer.parseInt(secondDate);
                m.add(event, m.getProperty(BDO, "notAfter"), m.createTypedLiteral(notAfter, XSDDatatype.XSDinteger));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            return;
        }
        if (dateStr.indexOf('u') != -1) {
            String firstDate = dateStr.replace('u', '0');
            String secondDate = dateStr.replace('u', '9');
            try {
                final int notBefore = Integer.parseInt(firstDate);
                m.add(event, m.getProperty(BDO, "notBefore"), m.createTypedLiteral(notBefore, XSDDatatype.XSDinteger));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            try {
                final int notAfter = Integer.parseInt(secondDate);
                m.add(event, m.getProperty(BDO, "notAfter"), m.createTypedLiteral(notAfter, XSDDatatype.XSDinteger));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            return;
        }
        m.add(event, m.getProperty(BDO, "onOrAbout"), m.createLiteral(dateStr));
        if (mainResource != null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
        }
    }
    
    public static void fillLogWhoToUri() {
        String prefix = BDR+"U"; // ?
        logWhoToUri.put("Gene Smith", prefix+String.format(userNumFormat, 1));
        logWhoToUri.put("agardner@sdrubin.org", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Alex Gardner", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Alexander Gardner", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Bumu Dega", prefix+String.format(userNumFormat, 3));
        logWhoToUri.put("Dega Bumu", prefix+String.format(userNumFormat, 3));
        logWhoToUri.put("Catherine Tsuji", prefix+String.format(userNumFormat, 4));
        logWhoToUri.put("Chris Tomlinson", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("ct", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Code Ferret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Code Feret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("chris", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("CodeFerret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Chungdak Nangpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("Chungdak Ngagpa", prefix+String.format(userNumFormat, 7));// is it the same name?
        logWhoToUri.put("Chungdak Ngakpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("cnakpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("Cuilan Liu", prefix+String.format(userNumFormat, 8));
        logWhoToUri.put("Gabi Coura", prefix+String.format(userNumFormat, 9));
        logWhoToUri.put("Harry Einhorn", prefix+String.format(userNumFormat, 10));
        logWhoToUri.put("Jann Ronis", prefix+String.format(userNumFormat, 11));
        logWhoToUri.put("jann ronis", prefix+String.format(userNumFormat, 11));
        logWhoToUri.put("Jeff Wallman", prefix+String.format(userNumFormat, 12));
        logWhoToUri.put("Jigme Namgyal", prefix+String.format(userNumFormat, 13));
        logWhoToUri.put("jm", prefix+String.format(userNumFormat, 14)); // ?
        logWhoToUri.put("Joe McClellan", prefix+String.format(userNumFormat, 15));
        logWhoToUri.put("Joseph McClellan", prefix+String.format(userNumFormat, 15));
        logWhoToUri.put("Karma Gongde", prefix+String.format(userNumFormat, 16));
        logWhoToUri.put("kgongde", prefix+String.format(userNumFormat, 16));
        logWhoToUri.put("Chozin", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("chozin", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Kelsang Lhamo", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("kelsang", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Kelsang", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("klhamo", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Konchok Tsering", prefix+String.format(userNumFormat, 18));
        logWhoToUri.put("Lobsang Shastri", prefix+String.format(userNumFormat, 19));
        logWhoToUri.put("lshastri", prefix+String.format(userNumFormat, 19));
        logWhoToUri.put("Michael R. Sheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("Michael Sheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("msheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("paldor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("Paldor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("pal dor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("Penghao Sun", prefix+String.format(userNumFormat, 22));
        logWhoToUri.put("Ralf Kramer", prefix+String.format(userNumFormat, 23));
        logWhoToUri.put("Ramon Prats", prefix+String.format(userNumFormat, 24));
        logWhoToUri.put("Rory Lindsay", prefix+String.format(userNumFormat, 25));
        logWhoToUri.put("Tendzin Parsons", prefix+String.format(userNumFormat, 26));
        logWhoToUri.put("Tenzin Dickyi", prefix+String.format(userNumFormat, 27));
        logWhoToUri.put("Arya Moallem", prefix+String.format(userNumFormat, 28));
        logWhoToUri.put("Awang Ciren", prefix+String.format(userNumFormat, 29));
        logWhoToUri.put("Chen Lai", prefix+String.format(userNumFormat, 30));
        logWhoToUri.put("Dennis Johnson", prefix+String.format(userNumFormat, 31));
        logWhoToUri.put("Dorjee Choden", prefix+String.format(userNumFormat, 32));
        logWhoToUri.put("dorjee choden", prefix+String.format(userNumFormat, 32));
        logWhoToUri.put("dzongsarlibrary", prefix+String.format(userNumFormat, 33));
        logWhoToUri.put("Erdene Baatar", prefix+String.format(userNumFormat, 34));
        logWhoToUri.put("Gyurmed Chodrak", prefix+String.format(userNumFormat, 35));
        logWhoToUri.put("Gyurme Chograg", prefix+String.format(userNumFormat, 35));
        logWhoToUri.put("Hachuluu", prefix+String.format(userNumFormat, 36));
        logWhoToUri.put("Haschuluu", prefix+String.format(userNumFormat, 36));
        logWhoToUri.put("Jamyang Lodoe", prefix+String.format(userNumFormat, 37));
        logWhoToUri.put("Jamyang.Lodoe", prefix+String.format(userNumFormat, 37));
        logWhoToUri.put("Jigme Tashi", prefix+String.format(userNumFormat, 38));
        logWhoToUri.put("John Canti", prefix+String.format(userNumFormat, 39));
        logWhoToUri.put("Khedup Gyatso", prefix+String.format(userNumFormat, 40));
        logWhoToUri.put("Legacy Converter", prefix+String.format(userNumFormat, 41));
        logWhoToUri.put("mangaram", prefix+String.format(userNumFormat, 42));
        logWhoToUri.put("Mara Canizzaro", prefix+String.format(userNumFormat, 43));
        logWhoToUri.put("mara canizzaro", prefix+String.format(userNumFormat, 43));
        logWhoToUri.put("Morris Hopkins", prefix+String.format(userNumFormat, 44));
        logWhoToUri.put("Ngawang Trinley", prefix+String.format(userNumFormat, 45));
        logWhoToUri.put("tenzang", prefix+String.format(userNumFormat, 45));// to be checked
        logWhoToUri.put("pbaduo", prefix+String.format(userNumFormat, 46));
        logWhoToUri.put("topic reclassify", prefix+String.format(userNumFormat, 47));
        logWhoToUri.put("zhangning", prefix+String.format(userNumFormat, 48));
        logWhoToUri.put("Arthur McKeown", prefix+String.format(userNumFormat, 49));
        logWhoToUri.put("Bruno Laine", prefix+String.format(userNumFormat, 50));
        logWhoToUri.put("chengdu", prefix+String.format(userNumFormat, 51));
        logWhoToUri.put("Chengdu", prefix+String.format(userNumFormat, 51));
        logWhoToUri.put("Chojor Radha", prefix+String.format(userNumFormat, 52));
        logWhoToUri.put("Elie Roux", prefix+String.format(userNumFormat, 53));
        logWhoToUri.put("Gelek Gyatso", prefix+String.format(userNumFormat, 54));
        logWhoToUri.put("Gelek.Gyatso", prefix+String.format(userNumFormat, 54));
        logWhoToUri.put("Georgia Kashnig", prefix+String.format(userNumFormat, 55));
        logWhoToUri.put("jw", prefix+String.format(userNumFormat, 56));
        logWhoToUri.put("monastery import", prefix+String.format(userNumFormat, 57));
        logWhoToUri.put("mongol import", prefix+String.format(userNumFormat, 58));
        logWhoToUri.put("Palri", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Nepal", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Parkhang", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Parkhang Nepal", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palris", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("palris", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("places-ensure-contains-has-name", prefix+String.format(userNumFormat, 60));
        logWhoToUri.put("Shallima Dellefant", prefix+String.format(userNumFormat, 61));
        logWhoToUri.put("sherabling", prefix+String.format(userNumFormat, 62)); // maybe NT?
        logWhoToUri.put("Shoko Mekata", prefix+String.format(userNumFormat, 63));
        logWhoToUri.put("Stacey Van Vleet", prefix+String.format(userNumFormat, 64));
        logWhoToUri.put("Tsering Dhondup", prefix+String.format(userNumFormat, 65));
        logWhoToUri.put("Tsering Dondrup", prefix+String.format(userNumFormat, 65));
        logWhoToUri.put("Tserings Wangdag and Dhondup", prefix+String.format(userNumFormat, 65)); // same ?
        logWhoToUri.put("Travis DeTour", prefix+String.format(userNumFormat, 66)); // same ?
    }
    
    public static void setPrefixes(Model m) {
        setPrefixes(m, false);
    }

    public static void setPrefixes(Model m, String type) {
        setPrefixes(m, type.equals("place"));
    }
	
    public static void setPrefixes(Model m, boolean addVcard) {
		m.setNsPrefix("", BDO);
		m.setNsPrefix("adm", ADM);
        m.setNsPrefix("bdr", BDR);
        m.setNsPrefix("bda", BDA);
        m.setNsPrefix("bdg", BDG);
		m.setNsPrefix("owl", OWL.getURI());
		m.setNsPrefix("rdf", RDF.getURI());
		m.setNsPrefix("rdfs", RDFS.getURI());
		m.setNsPrefix("skos", SKOS.getURI());
        m.setNsPrefix("xsd", XSD.getURI());
        m.setNsPrefix("rkts", "http://purl.rkts.eu/resource/");
        m.setNsPrefix("dila", "http://authority.dila.edu.tw/person/?fromInner=");
        m.setNsPrefix("sutta", "http://suttacentral.net/resource/");
		if (addVcard)
		    m.setNsPrefix("vcard", VCARD4.getURI());

	}
	
	public static Literal getLitFromUri(Model m, String uri) {
		//return m.createLiteral(m.shortForm(uri));
		return m.createLiteral(uri);
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type, String index) {
		String mainName = main.getLocalName();
		return prefix+mainName+"_"+type+index;
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type, int index) {
		return getSubResourceName(main, prefix, type, String.valueOf(index));
	}
	
	public static String getSubResourceName(Resource main, String prefix, String type) {
		return getSubResourceName(main, prefix, type, "");
	}
	
	public static String getDescriptionUriFromType(String type) {
	    String res = normalizePropName(type, "description");
	       switch (res) {
	        case "noType":                return RDFS.getURI()+"comment";
	        case "status":                return RDFS.getURI()+"comment";
	        case "authorship":            return BDO+"workAuthorshipStatement";
	        case "incipit":               return BDO+"workIncipit";
	        case "note":                  return BDO+"note";
	        case "notes":                 return BDO+"note";
	        case "chapter":               return BDO+"work_desc_chapters";
	        case "chapters":              return BDO+"work_desc_chapters";
	        case "content":               return RDFS.getURI()+"comment";
	        case "contents":              return RDFS.getURI()+"comment";
	        case "completionDate":        return BDO+"work_desc_completionDate"; // this one and the next one are handled separately
	        case "date":                  return ADM+"work_desc_date";
	        case "errata":                return BDO+"workErrata";
	        case "extent":                return BDO+"workExtentStatement";
	        case "id":                    return "__fpl";
	        case "libraryOfCongress":     return BDO+"work_desc_libraryOfCongress";
	        case "location":              return BDO+"workLocationStatement";
	        case "remarks":               return "__fpl";
	        case "room":                  return "__fpl";
	        case "summary":               return RDFS.getURI()+"comment";
	        case "snar_bstan_number":     return BDO+"workKaTenSiglaN";
	        case "snr_thang_number":      return BDO+"workKaTenSiglaN";
	        case "snar_thang_number":     return BDO+"workKaTenSiglaN"; 
	        case "gser_bris_numbr":       return BDO+"workKaTenSiglaG";
	        case "gser_birs_number":      return BDO+"workKaTenSiglaG";
	        case "gse_bris_number":       return BDO+"workKaTenSiglaG";
	        case "sger_bris_number":      return BDO+"workKaTenSiglaG";
	        case "gser_bri_numer":        return BDO+"workKaTenSiglaG";
	        case "gser_dris_number":      return BDO+"workKaTenSiglaG";
	        case "gser_bri_number":       return BDO+"workKaTenSiglaG";
	        case "gser_bris_nimber":      return BDO+"workKaTenSiglaG";
	        case "gser_bris_number":      return BDO+"workKaTenSiglaG";
	        case "colopho":               return BDO+"workColophon";
	        case "colophon":              return BDO+"workColophon";
	        case "colophn":               return BDO+"workColophon";
	        case "colophone":             return BDO+"workColophon";
	        case "sde_gde_number":        return BDO+"workKaTenSiglaD";
	        case "de_dge_number":         return BDO+"workKaTenSiglaD";
	        case "sdg_dge_number":        return BDO+"workKaTenSiglaD";
	        case "sdr_dge_number":        return BDO+"workKaTenSiglaD";
	        case "sde_dge_number":        return BDO+"workKaTenSiglaD";
	        case "lhasa_number":          return BDO+"workKaTenSiglaH";
	        case "stog_numbe":            return BDO+"workKaTenSiglaS";
	        case "stog_unmber":           return BDO+"workKaTenSiglaS";
	        case "stog_number":           return BDO+"workKaTenSiglaS";
	        case "stogNumber":            return BDO+"workKaTenSiglaS";
	        case "toh_number":            return BDO+"workKaTenSiglaD";
	        case "toh":                   return BDO+"workKaTenSiglaD";
	        case "otani_number":          return BDO+"workKaTenSiglaQ";
	        case "otani":                 return BDO+"workKaTenSiglaQ";
	        case "otani_beijing":         return BDO+"workKaTenSiglaQ";
	        case "sheyNumber":            return BDO+"workKaTenSiglaZ";
	        case "shey_number":           return BDO+"workKaTenSiglaZ";
	        case "rKTsReference":         return BDO+"workKaTenRefrKTs";
	        case "bon_bka_gyur_number":   return BDO+"workKaTenSiglaBon";
	        case "urga_number":           return BDO+"workKaTenSiglaU";
	        case "isIAO":                 return BDO+"workRefIsIAO";
	        case "catalogue_number":      return BDO+"workRefChokLing";
            case "gonpaPerEcumen":        return BDO+"placeGonpaPerEcumen";
	        case "nameLex":
	        case "nameKR":
	        case "gbdist":
	        case "town_syl":
	        case "town_py":
	        case "town_ch":
	        case "prov_py":
	        case "gonpaPer1000":
	        case "dist_py":
	        case "ondisk":
	        case "onDisk":
	        case "dld":
	        case "icon":
	        case "text": // TODO: migrate? how? https://github.com/BuddhistDigitalResourceCenter/xmltoldmigration/issues/22
	        case "dpl480":
	        case "featured":
	            return "__ignore";
	        default:
	            return null;
	        }
	}
	
	public static String normalizePropName(String toNormalize, String targetType) {
		String res = toNormalize.trim().replace("'", "").replace(" ", "_");
		if (targetType == "Class") {
			res = res.substring(0,1).toUpperCase() + res.substring(1);
		} else {
		    res = res.substring(0,1).toLowerCase() + res.substring(1);
		}
		return res;
	}
	
	public static Pattern whiteSpacePattern = Pattern.compile("[\u180E\\s\\p{Cntrl}]+", Pattern.UNICODE_CHARACTER_CLASS);
	
	public static String normalizeString(String toNormalize, boolean keepSpaces) {
	    if (keepSpaces)
	        return toNormalize.trim();
	    return whiteSpacePattern.matcher(toNormalize).replaceAll(" ").trim();
	}
	
	public static String normalizeString(String toNormalize) {
	    return normalizeString(toNormalize, false);
	}
    
    /**
     *Adds a new named Note node and adds bdo:noye node to the supplied rez.
     * 
     * @param rez respource the note is attached to
     * @param noteText String note text content
     * @param lang language of the noteText. If null set to "en"
     * @param loc the location statement for the note
     * @param ref Work resource from which the note is taken
     */
    public static void addNote(Resource rez, String noteText, String lang,  String loc, Resource ref) {
        Literal noteLit = null;
        if (noteText != null && !noteText.isEmpty()) {
            noteLit = rez.getModel().createLiteral(noteText, (lang != null ? lang : "en"));
        }
        addNote(rez, noteLit, loc, ref);
    }
    
    /**
     *Adds a new named Note node and adds bdo:noye node to the supplied rez.
     * 
     * @param rez respource the note is attached to
     * @param noteText Literal note text content
     * @param loc the location statement for the note
     * @param ref Work resource from which the note is taken
     */
    private static void addNote(Resource rez, Literal noteText, String loc, Resource ref) {
        Model m = rez.getModel();
        Resource noteR = getFacetNode(FacetType.NOTE, rez);
        Property prop = m.getProperty(BDO, "note");
        rez.addProperty(prop, noteR);
        
        if (noteText != null) {
            noteR.addProperty(m.createProperty(BDO+"noteText"), noteText);
        }
        if (loc != null && !loc.isEmpty()) {
            noteR.addProperty(m.createProperty(BDO+"noteLocationStatement"), loc);
        }
        if (ref != null) {
            noteR.addProperty(m.getProperty(BDO, "noteWork"), ref);
        }
    }

    private static void addNote(Model m, Element e, Resource rez, int i, Property p, Literal l) {
	    // some empty <note/> appear sometimes
	    if (e.getAttribute("work").isEmpty() && e.getAttribute("location").isEmpty() && e.getTextContent().trim().isEmpty()) {
	        return;
	    }
	    
	    Literal noteText = getLiteral(e, "en", m, "note", rez.getLocalName(), rez.getLocalName(), false);	    
	    String noteLoc = e.getAttribute("location").trim();
	    String workRid = e.getAttribute("work").trim();
	    Resource noteWork = null;
	    if (!workRid.isEmpty()) {
	        workRid = MigrationHelpers.sanitizeRID(rez.getLocalName(), "noteWork", workRid);
	        if (!MigrationHelpers.isDisconnected(workRid))
	            noteWork = m.createResource(BDR+workRid);
	    }
	    addNote(rez, noteText, noteLoc, noteWork);
	}
	
	public static void addNotes(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "note");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addNote(m, current, r, i, null, null);
		}
	}
	
	public static String normalizeToLUrl(String toLUrl) {
	    String res = toLUrl.replace("http", "https");
	    res = res.replace("//treasuryoflives.org", "//www.treasuryoflives.org");
	    res = res.replace("//beta.treasuryoflives.org", "//www.treasuryoflives.org");
	    return res;
	}

   public static String getRIDFromTbrcUrl(String tbrcUrl) {
        int i = tbrcUrl.indexOf("RID=");
        String res = tbrcUrl;
        if (i > 0) {
            res = res.substring(i+4);
        } else {
            // case of http://tbrc.org/#library_topic_Object-T151
            // and http://tbrc.org/?locale=bo#library_work_Object-W1PD107999
            i = res.indexOf("-");
            return res.substring(i+1);
        }
        i = res.indexOf("$");
        if (i > 0) {
            res = res.substring(0, i);
        }
        i = res.indexOf("#");
        if (i > 0) {
            res = res.substring(0, i);
        }
        i = res.indexOf("|");
        if (i > 0) {
            res = res.substring(i+1);
        }
        String newRID = OutlineMigration.ridsToConvert.get(res);
        if (newRID != null) return newRID;
        return res;
    }
	
	public static void addExternal(Model m, Element e, Resource rez, int i) {
	    Resource admR = getAdminData(rez);
		String value = e.getAttribute("data").trim();
		if (value.isEmpty()) return;
		if (value.contains("treasuryoflives.org")) {
		    value = normalizeToLUrl(value);
		    admR.addProperty(m.createProperty(ADM, "seeOtherToL"), m.createTypedLiteral(value, XSDDatatype.XSDanyURI));
		    return;
		}
		if (value.contains("blog.tbrc.org")) return;
		if (value.contains("tbrc.org")) {
		    value = getRIDFromTbrcUrl(value);
		    // TODO: map outline nodes to new ones
		    admR.addProperty(m.createProperty(RDFS.getURI(), "seeAlso"), m.createResource(BDR+value));
		}
	}
	
	public static void addExternals(Model m, Element e, Resource r, String XsdPrefix) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "external");
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			addExternal(m, current, r, i);
		}
	}
	
	public static Literal literalFromXsdDate(Model m, String s) {
		// was quite difficult to find...
	    XSDDateTime dateTime = (XSDDateTime)XSDDatatype.XSDdateTime.parse(s);
		return m.createTypedLiteral(dateTime);
	}
	
	public static void addLogEntry(Model m, Element e, Resource r) {
		if (e == null) return;
		Resource logEntry = getFacetNode(FacetType.LOG_ENTRY, r);
//		Resource logEntry = m.createResource();
		//m.add(logEntry, RDF.type, m.getProperty(BDO+"LogEntry"));
		Property prop = m.getProperty(ADM, "logEntry");
		m.add(r, prop, logEntry);
		String value = e.getAttribute("when");
		if (!value.isEmpty()) {
			prop = m.createProperty(ADM+"logDate");
			try {
			    m.add(logEntry, prop, literalFromXsdDate(m, value));
			} catch (DatatypeFormatException ex) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "log_entry", "cannot convert log date properly, original date: `"+value+"`");
			}
		}
		value = normalizeString(e.getAttribute("who"));
		if (!value.isEmpty() && !value.equals("unspecified")) {
			prop = m.createProperty(ADM+"logWho");
			String uri = logWhoToUri.get(value);
			if (uri == null) {
			    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "log_who", "unknown who: "+value);    
			} else {
			    m.add(logEntry, prop, m.createResource(uri));
			}
		}
		value = normalizeString(e.getTextContent(), true);
		if (!value.isEmpty()) {
			prop = m.createProperty(ADM+"logMessage");
			m.add(logEntry, prop, m.createLiteral(value, "en"));
		}
		
	}
	
	public static void addLog(Model m, Element e, Resource r, String XsdPrefix) {
		NodeList nodeList = e.getElementsByTagNameNS(XsdPrefix, "log");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element log = (Element) nodeList.item(i);
			NodeList logEntriesList = log.getElementsByTagNameNS(XsdPrefix, "entry");
			for (int j = 0; j < logEntriesList.getLength(); j++) {
				Element logEntry = (Element) logEntriesList.item(j);
				addLogEntry(m, logEntry, r);
			}
			logEntriesList = log.getElementsByTagName("entry");
			for (int k = 0; k < logEntriesList.getLength(); k++) {
				Element logEntry = (Element) logEntriesList.item(k);
				addLogEntry(m, logEntry, r);
			}
		}
	}
	
	// returns true if a PREFLABEL was added
	public static boolean addNames(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel, String additionalNameProp) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "name");
		Map<String,Boolean> labelDoneForLang = new HashMap<>();
		boolean res = false;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			if (current.getTextContent().trim().isEmpty()) continue;
			// not sure about the second one in case of an outline
            Literal l = getLiteral(current, EWTS_TAG, m, "name", r.getLocalName(), r.getLocalName());
            if (l != null) {
                if (guessLabel) {
                    String lang = l.getLanguage().substring(0, 2);
                    if (!labelDoneForLang.containsKey(lang)) {
                        r.addProperty(SKOS.prefLabel, l);
                        labelDoneForLang.put(lang, true);
                    } else {
                        r.addProperty(SKOS.altLabel, l);
                    }
                } else {
                    r.addProperty(RDFS.label, l);
                }
                if (additionalNameProp != null) {
                    r.addProperty(m.getProperty(additionalNameProp), l);
                }
                res = true;
            }
		}
		return res;
	}
	
   public static boolean addNames(Model m, Element e, Resource r, String XsdPrefix) {
       return addNames(m, e, r, XsdPrefix, true, null);
    }
	
   public static String descriptionTypeNeedsLang(String type) {
       switch (type) {
       case "incipit":
       case "colophon":
       case "colopho":
       case "colophn":
       case "colophone":
           return EWTS_TAG;
       case "authorship":
       case "summary":
       case "content":
       case "contents": // for office, corporation, etc., maybe not for works
           return "en";
       default:
           return null;
       }
   }
   
   public static Map<String,Model> addDescriptions(Model m, Element e, Resource r, String XsdPrefix) {
       return addDescriptions(m, e, r, XsdPrefix, false);
   }
   
	public static Map<String,Model> addDescriptions(Model m, Element e, Resource rez, String XsdPrefix, boolean guessLabel) {
		List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "description");
		Map<String,Boolean> labelDoneForLang = new HashMap<>();
        Resource fplItem = null;
        Resource admFplItem = null;
		Model resModel = null;
		String fplId = null;
		String fplRoom = null;
		String fplDescription = null;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
			String value = current.getTextContent().trim();
			if (value.isEmpty()) continue;
			String type = current.getAttribute("type").trim();
	        if (type.isEmpty())
	            type = "noType";
	        Literal lit;
	        // we add some spaghettis for the case of R8LS13081 which has no description type
	        // but needs to be added as label
	        String lang = descriptionTypeNeedsLang(type);
	        if (lang != null || (guessLabel && type.equals("noType"))) {
	            if (lang == null)
	                lang = "en";
	            lit = getLiteral(current, lang, m, "description", rez.getLocalName(), rez.getLocalName());
	            if (lit == null) continue;
	        } else {
	            lit = m.createLiteral(normalizeString(value));
	        }
	        if (type.equals("nameLex")) {
                String placeId = rez.getLocalName();
                current.setTextContent(current.getTextContent().replace(placeId, ""));
            }
            if (type.equals("note")) {
                Resource note = getFacetNode(FacetType.NOTE, rez);
                m.add(rez, m.getProperty(BDO+"note"), note);
                m.add(note, m.getProperty(BDO+"noteText"), lit);
                continue;
            }
            if (type.equals("completionDate") || type.equals("date")) {
                Resource event = getEvent(rez, "CompletedEvent", "workEvent");
                addDates(value, event, rez);
                continue;
            }
			String propUri = getDescriptionUriFromType(type);
			if (propUri != null && propUri.equals("__ignore")) 
			    continue;
			if (propUri == null) {
			    ExceptionHelper.logException(ExceptionHelper.ET_DESC, rez.getLocalName(), rez.getLocalName(), "description", "unhandled description type: "+type);
			    if (!guessLabel)
			        continue;
			}
//			if (!guessLabel && type.equals("noType"))
//			    l = m.createLiteral(l.getString()+" - was description with no type", l.getLanguage());
			if (propUri != null && propUri.equals("__fpl")) {
			    if (fplItem == null) {
			        resModel = ModelFactory.createDefaultModel();
			        setPrefixes(resModel, "item");
			        String workId = rez.getLocalName();
                    fplItem = resModel.createResource(BDR+"I"+workId.substring(1)+"_P001");
                    admFplItem = createAdminRoot(fplItem);
			        if (WorkMigration.addItemForWork) {
                        fplItem.addProperty(resModel.getProperty(BDO, "itemForWork"), rez);
			        }
			        addReleased(resModel, admFplItem);
			        fplItem.addProperty(RDF.type, resModel.getResource(BDO+"ItemPhysicalAsset"));
			        fplItem.addProperty(resModel.getProperty(BDO, "itemLibrary"), resModel.getResource(BDR+FPL_LIBRARY_ID));
			        if (WorkMigration.addWorkHasItem) {
			            rez.addProperty(resModel.getProperty(BDO+"workHasItem"), fplItem);
			        }
			    }
			    switch(type) {
			    case "id":
			        fplId = value;
			        if (fplRoom != null) {
			            fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"+fplId));
			        }
			        break;
			    case "room":
			        fplRoom = value;
			        if (fplId != null) {
                        fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"+fplId));
                    }
                    break;
			    case "remarks":
			        fplDescription = (fplDescription == null) ? value : fplDescription+"\n"+value;
			        break;
			    }
			    continue;
			}
			// for product and office the name is the first description type="contents", and we don't want to keep it in a description
            if (guessLabel && (type.equals("contents") || type.equals("noType"))) {
                lang = lit.getLanguage().substring(0, 2);
                if (!labelDoneForLang.containsKey(lang)) {
                    rez.addProperty(SKOS.prefLabel, lit);
                    labelDoneForLang.put(lang, true);
                } else {
                    rez.addProperty(SKOS.altLabel, lit);
                }
                continue;
            }
            rez.addProperty(m.getProperty(propUri), lit);
		}
		if ((fplId == null && fplRoom != null) ||
		        (fplId != null && fplRoom == null)) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, rez.getLocalName(), rez.getLocalName(), "description", "types `id` and `room` should both be present");
		    if (fplId == null)
		        fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"));
		    else
		        fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral("|"+fplId));
		}
		if (fplDescription != null) {
		    Resource fplVolume = resModel.createResource();
		    fplItem.addProperty(resModel.getProperty(BDO, "itemHasVolume"), fplVolume);
		    fplVolume.addProperty(RDF.type, resModel.getResource(BDO+"VolumePhysicalAsset"));
		    fplVolume.addProperty(resModel.getProperty(BDO, "volumeNumber"), resModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
		    fplVolume.addProperty(resModel.getProperty(BDO, "volumePhysicalDescription"), resModel.createLiteral(fplDescription, "en"));
		}
		if (resModel != null) {
		    Map<String,Model> itemModels = new HashMap<>();
		    itemModels.put(fplItem.getLocalName(), resModel);
		    return itemModels;
		} else {
		    return null;
		}
	}

	private static Resource getNodeType(String type, boolean outlineMode, Resource work) {
	    Model m = work.getModel();
	    
	    switch (type) {
	    case "titlePageTitle":
	    case "fullTitle":
	    case "subtitle":
	    case "runningTitle":
	    case "dkarChagTitle":
	    case "colophonTitle":
	    case "coverTitle":
	    case "halfTitle":
	    case "otherTitle":
	    case "spineTitle":
	    case "copyrightPageTitle":
	    case "bibliographicalTitle":
	        return m.createResource(BDO+"Work"+type.substring(0, 1).toUpperCase() + type.substring(1));
        case "sectionTitle":
        case "captionTitle":
            if (outlineMode)
                return m.createResource(BDO+"WorkRunningTitle");
            else
                return m.createResource(BDO+"WorkOtherTitle");
        case "portion":
            return m.createResource(BDO+"WorkTitlePortion");
	    default:
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, work.getLocalName(), work.getLocalName(), "title", "unknown title type `"+type+"`");
            return m.createResource(BDO+"WorkBibliographicalTitle");
	    }
	}
	
       public static void addTitles(Model m, Resource main, Element root, String XsdPrefix, boolean guessLabel, boolean outlineMode) {
            List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "title");
            Map<String,Boolean> labelDoneForLang = new HashMap<>();
            Map<String,Boolean> titleSeen = new HashMap<>();
            String typeUsedForLabel = null;
            for (int i = 0; i < nodeList.size(); i++) {
                Element current = (Element) nodeList.get(i);
                Literal lit = getLiteral(current, EWTS_TAG, m, "title", main.getLocalName(), main.getLocalName());
                String nextTitle = null;
                if (lit == null) continue;
                if (main.getLocalName().contains("FPL") && lit.getLanguage().equals("pi-x-iast") && lit.getString().contains("--")) {
                    String[] split = lit.getString().split("--");
                    if (!split[1].isEmpty()) {
                        nextTitle = split[1];
                        lit = m.createLiteral(split[0], "pi-x-iast");
                    }
                }
                final String litStr = lit.getString()+"@"+lit.getLanguage();
                if (titleSeen.containsKey(litStr))
                    continue;
                titleSeen.put(litStr, true);
                String type = current.getAttribute("type");
                if (type.isEmpty()) {
                    type = "bibliographicalTitle";
                }
                if (type.equals("incipit")) {
                    main.addProperty(m.getProperty(BDO, "workIncipit"), lit);
                    continue;
                }
                
                Resource nodeType = getNodeType(type, outlineMode, main);
                Resource titleNode = getFacetNode(FacetType.TITLE, main, nodeType);        
                titleNode.addProperty(RDFS.label, lit);
                main.addProperty(m.getProperty(BDO, "workTitle"), titleNode);
                
                if (nextTitle != null) {
                    titleNode = getFacetNode(FacetType.TITLE, main, nodeType);
                    titleNode.addProperty(RDFS.label, m.createLiteral(nextTitle, "pi-x-iast"));
                    main.addProperty(m.getProperty(BDO, "workTitle"), titleNode);
                }

                if (guessLabel) {
                    String lang = lit.getLanguage().substring(0, 2);
                    if (!labelDoneForLang.containsKey(lang) && (typeUsedForLabel == null || typeUsedForLabel.equals(type))) {
                        main.addProperty(SKOS.prefLabel, lit);
                        labelDoneForLang.put(lang, true);
                        typeUsedForLabel = type;
                    }
                    continue;
                }
            }
        }
       
       public static boolean isCommentaryTopic(String rid) {
           switch (rid) {
           case "T304":
           case "T3JT5054":
           case "T61":
           case "T4JW5424":
           case "T10MS12837":
           case "T132":
           case "T1488":
           case "T1491":
           case "T2397":
               return true;
           }
           return false;
       }
       
       // list of topics associated with languages. We simply remove them as the
       // the data already contains a language indication for the very large majority
       public static final Map<String,Boolean> langTopics = new HashMap<>();
       static {
           langTopics.put("T3CN1331", true); // Chinese
           langTopics.put("T2411", true); // Sanskrit
           langTopics.put("T3CN2027", true); // Mongolian
       }
       
       public static void addSubjects(Model m, Resource main, Element root, String XsdPrefix) {
           List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "subject");
           boolean needsCommentaryTopic = false;
           boolean hasCommentaryTopic = false;
           for (int i = 0; i < nodeList.size(); i++) {
               Element current = (Element) nodeList.get(i);
               String rid = current.getAttribute("class").trim();
               if (rid.isEmpty())
                   continue;
               if (isCommentaryTopic(rid))
                   hasCommentaryTopic = true;
               if (langTopics.containsKey(rid))
                   continue;
               String value = current.getAttribute("type").trim();
               String prop = null;
               switch (value) {
               case "isAboutPerson":
               case "isAboutCorporation":
               case "isAboutMeeting":
               case "isAboutPlace":
               case "isAboutClan":
               case "isAboutSect":
               case "isAboutText":
                   prop = BDO+"workIsAbout";
                   break;
               case "isAboutControlled":
               case "isAboutUncontrolled":
                   prop = BDO+"workIsAbout";
                   break;
               case "isInstanceOfGenre":
               case "isInstanceOf":
                   prop = BDO+"workGenre";
                   break;
               case "isCommentaryOn":
                   prop = BDO+"workIsAbout";
                   needsCommentaryTopic = true;
                   break;
               default:
                   prop = BDO+"workIsAbout";
                   break;
               }
               if (genreTopics.containsKey(rid)) {
                   prop = BDO+"workGenre"; 
               }
               rid = MigrationHelpers.sanitizeRID(main.getLocalName(), value, rid);
               if (!MigrationHelpers.isDisconnected(rid))
                   m.add(main, m.getProperty(prop), m.createResource(BDR+rid));
           }
           if (needsCommentaryTopic && !hasCommentaryTopic) {
               m.add(main, m.getProperty(BDO, "workGenre"), m.createResource(BDR+"T132"));
           }
       }
       
       private static int addLocationIntOrString(Model m, Resource main, Resource loc, Element current, String attributeName, String propname, Integer doNotAddIfEquals) {
           String value = current.getAttribute(attributeName).replaceAll(",$", "").trim();
           int res = -1;
           if (!value.isEmpty()) {
               try {
                   int intval = Integer.parseInt(value);
                   if (intval < 1) {
                       ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                       m.add(loc, m.getProperty(BDO, propname), m.createLiteral(value));
                   } else {
                       if (doNotAddIfEquals == null || intval != doNotAddIfEquals)
                           m.add(loc, m.getProperty(BDO, propname), m.createTypedLiteral(intval, XSDDatatype.XSDinteger));
                       res = intval;
                   }
               } catch (NumberFormatException e) {
                   ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                   m.add(loc, m.getProperty(BDO, propname), m.createLiteral(value));
               }
           }
           return res;
       }
       
       public static class LocationVolPage {
           public Integer endVolNum;
           public int endPageNum;
           public Integer beginVolNum;
           public int beginPageNum;
           public String RID;
           
           public LocationVolPage(Integer beginVolNum, int beginPageNum, Integer endVolNum, int endPageNum, String RID) {
               this.endVolNum = endVolNum;
               this.endPageNum = endPageNum;
               this.beginVolNum = beginVolNum;
               this.beginPageNum = beginPageNum;
               this.RID = RID;
           }
           
           public String toString() {
               return "encVolNum: "+endVolNum+", endPageNum: "+endPageNum+", beginVolNum: "+beginVolNum+", beginPageNum: "+beginPageNum+", RID: "+RID;
           }
       }
       
       public static LocationVolPage addLocations(Model m, Resource main, Element root, String XsdPrefix, String workId, String outlineId, String outlineNode, String outlineNodeTitle) {
           List<Element> nodeList = CommonMigration.getChildrenByTagName(root, XsdPrefix, "location");
           int i;
           int volume1 = -1;
           int page1 = -1;
           int page2 = -1;
           Resource loc = m.createResource();
           LocationVolPage res = null;
           for (i = 0; i < nodeList.size(); i++) {
               if (i > 1) {
                   ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\" too many locations, it should only have 2");
                   break;
               }
               Element current = (Element) nodeList.get(i);
               
               if (i == 0) {
                   //String value = getSubResourceName(main, WORK_PREFIX, "Location", i+1);
                   String value = current.getAttribute("type");
                   if (value.equals("folio")) {
                       loc.addProperty(m.getProperty(BDO, "workLocationByFolio"), m.createTypedLiteral(true));
                   }    
               }
               
               String value = current.getAttribute("work").trim();
               if (workId.isEmpty()) {
                   if (!value.isEmpty())
                       m.add(loc, m.getProperty(BDO, "workLocationWork"), m.createResource(BDR+value));
               } else if (!value.isEmpty() && !value.equals(workId)) {
                   String error = "title: \""+outlineNodeTitle+"\" has locations in work "+value+" instead of "+workId;
                   ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, error);
               }
               
               String endString = (i == 0) ? "" : "End";
               int volume = addLocationIntOrString(m, main, loc, current, "vol", "workLocation"+endString+"Volume", volume1);
               if (i == 0) volume1 = volume;
               if (i == 1 && volume != -1 && volume1 != -1 && volume < volume1) {
                   ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", end location volume is before beginning location volume");
               }
               int page = addLocationIntOrString(m, main, loc, current, "page", "workLocation"+endString+"Page", null);
               if (i == 0) {
                   page1 = page;
               } else {
                   page2 = page;
               }
               if (i == 1 && page != -1 && page1 != -1 && page < page1 && volume == volume1) {
                   ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", end location page is before beginning location");
               }
               addLocationIntOrString(m, main, loc, current, "phrase", "workLocation"+endString+"Phrase", null);
               addLocationIntOrString(m, main, loc, current, "line", "workLocation"+endString+"Line", null);

               if (i == 1 && page != -1) {
                   res = new LocationVolPage(volume1, page1, volume, page, null);
               }
               
               value = current.getAttribute("side");
               if (!value.isEmpty())
                   m.add(loc, m.getProperty(BDO, "workLocation"+endString+"Side"), m.createLiteral(value));
               
           }
           // only add locations with statements
           StmtIterator locProps = loc.listProperties();
           if (locProps.hasNext()) {
               m.add(main, m.getProperty(BDO, "workLocation"), loc);
               // comment to remove workLocationWork in outline nodes
               if (!workId.isEmpty())
                   m.add(loc, m.getProperty(BDO, "workLocationWork"), m.createResource(BDR+workId));
           }
           if (volume1 == -1 && (page1 == -1 || page2 == -1)) {
               ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", missing volume, beginpage or endpage");
           } else if (volume1 != -1 && (page1 == -1 || page2 == -1)) {
               ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", vol. "+volume1+", missing beginpage or endpage");
           }
           return res;
       }
       
       public static void addReleased(Model m, Resource r) {
           addStatus(m, r, "released");
       }
       
       public static void addStatus(Model m, Resource r, String status) {
           if (status == null || status.isEmpty()) return;
           String statusName = "Status"+status.substring(0, 1).toUpperCase() + status.substring(1);
           r.addProperty(m.getProperty(ADM+"status"), m.getResource(BDA+statusName));
       }
	
	// IMPORTANT: we're using canonical BCP47 forms, which means that the
	// script has an upper case first letter (ex: zh-Latn-pinyin), which
	// is then smashed by the annoying
	// https://github.com/jsonld-java/jsonld-java/issues/199
	// so we have a workaround when reading a file, see MigrationHelper
	public static String getBCP47Suffix(String encoding) {
		switch(encoding) {
		case "extendedWylie":
			return "-x-ewts";
		case "wadeGiles":
		    // transliteration of Chinese
			return lowerCaseLangTags ? "-latn-wadegile" : "-Latn-wadegile";
		case "pinyin":
			return lowerCaseLangTags ? "-latn-pinyin" : "-Latn-pinyin";
		case "libraryOfCongress":
			return "-alalc97"; // could also be -t-m0-alaloc
		case "native":
			return "";
		case "none":
            return "";
		case "rma":
			return "-x-rma"; // what's that?
		case "sansDiacritics":
			return "-x-ndia";
		case "withDiacritics":
			return "-x-iast";
		case "transliteration":
			return "-x-trans"; // not sure...
		case "acip":
			return "-x-acip";
		case "tbrcPhonetic":
			return "-x-phon-en-m-tbrc";
		case "alternatePhonetic":
			return "-x-phon-en"; // not sure about this one...
		case "syllables":
		    // the cases we have are essentially town_syl, which is a
		    // romanization that doesn't seem standard, a kind of phonetic?
			return "-x-syx";
		case "":
			return "";
		default:
		    throw new IllegalArgumentException("unknown encoding: "+encoding);
		}
	}
	
	public static String getIso639(String language) throws IllegalArgumentException {
		switch(language) {
		case "tibetan":
			return "bo";
		case "pali":
            return "pi";
		case "english":
			return "en";
		case "chinese":
			return "zh";
		case "sanskrit":
			return "sa";
		case "mongolian":
			return "mn";
		case "french":
			return "fr";
		case "russian":
			return "ru";
		case "zhangZhung":
			return "xzh";// iso 639-3
		case "dzongkha":
			return "dz";
		case "miNyag":
			return "mvm"; // not really sure...
		case "german":
			return "de";
		case "":
            return "en";
		case "japanese":
			return "ja";
		case "unspecified":
			// Jena checks tags against https://tools.ietf.org/html/rfc3066 stating:
		    // You SHOULD NOT use the UND (Undetermined) code unless the protocol
		    // in use forces you to give a value for the language tag, even if
		    // the language is unknown.  Omitting the tag is preferred.
		    throw new IllegalArgumentException("unknown language: "+language);
		default:
		    throw new IllegalArgumentException("unknown language: "+language);
		}
	}
	
	public static String getBCP47(String language, String encoding) throws IllegalArgumentException {
		if (language == null || language.isEmpty()) {
			if (encoding != null && !encoding.isEmpty()) {
			    if (encoding.equals("extendedWylie")) return EWTS_TAG;
			    if (encoding.equals("tbrcPhonetic")) return "bo-x-phon-en-m-tbrc";
				throw new IllegalArgumentException("encoding with no language!");
			}
			return null;
		}
		return getIso639(language)+getBCP47Suffix(encoding);
	}
	
	// from http://stackoverflow.com/a/14066594/2560906
	private static boolean isAllEwtsChars(String input) {
	    boolean res = true;
	    for (int i = 0; i < input.length(); i++) {
	        int c = input.charAt(i);
	        if ((c > 0x7F && c != 0x2019) || c == 'x') { // ’ is sometimes used instead of '
	            res = false;
	            break;
	        }
	    }
	    return res;
	}
	
	   private static boolean isAllLatn(String input) {
	        for (int i = 0; i < input.length(); i++) {
	            int c = input.charAt(i);
	            if (c > 0x36F) {
	                return false;
	            }
	        }
	        return true;
	    }

	    private static final List<Character> unihanPinyinDiacritics = Arrays.asList(
	            'Ā', 'Á', 'Ǎ', 'À', 
	            'ā', 'á', 'ǎ', 'à', 
	            'Ē', 'É', 'Ě', 'È', 
	            'ē', 'é', 'ě', 'è', 
	            'Ī', 'Í', 'Ǐ', 'Ì', 
	            'ī', 'í', 'ǐ', 'ì', 
	            'Ō', 'Ó', 'Ǒ', 'Ò', 
	            'ō', 'ó', 'ǒ', 'ò', 
	            'Ū', 'Ú', 'Ǔ', 'Ù', 
	            'ū', 'ú', 'ǔ', 'ù', 
	            'Ǖ', 'Ǘ', 'Ǚ', 'Ǜ', 'Ü',
	            'ǖ', 'ǘ', 'ǚ', 'ǜ', 'ü');
	   // test if the Pinyin has diacritics
       private static boolean isPinyinNDia(String input) {
           for (int i = 0; i < input.length(); i++) {
               int c = input.charAt(i);
               // if we encounter a number, it has diacritics:
               if (c > '0' && c < '9') {
                   return false;
               }
               if (unihanPinyinDiacritics.contains(c))
                   return false;
           }
           return true;
       }
	   
    public static boolean isAllTibetanUnicode(String input) {
        final int len = input.length();
        if (len == 0) return false;
        double nbNonTibUni = 0;
        for (int i = 0; i < len; i++) {
            int c = input.charAt(i);
            if ((c < 0x0F00 || c > 0x0FFF) && c != ' ') {
                nbNonTibUni += 1;
            }
        }
        return (nbNonTibUni / len < 0.1);
    }
    
    private static boolean isAllChineseUnicode(String input) {
        boolean isChinese = true;
        for (int i = 0; i < input.length(); i++) {
            int c = input.charAt(i);
            if (c < 0x2E00 && c != 0x00B7) {
                isChinese = false;
                break;
            }
        }
        return isChinese;
    }

    // check for traditional characters.
    // TODO: some strings, such as 阿毘達磨文献における思想の展開
    // are a mix of Hans and Hant (mostly Hant except 献 which is simplified for 獻)
    private static boolean isHant(String input) {
        final int length = input.length();
        for (int offset = 0; offset < length; ) {
           final int codepoint = input.codePointAt(offset);
           if (isTraditional.containsKey(codepoint)) {
               return true;
           }
           offset += Character.charCount(codepoint);
        }
        return false;
    }

    private static Pattern p = Pattern.compile("[\u0F40-\u0FBC]+");
    public static boolean isMostLikelyEwts(String input) {
        if (!isAllEwtsChars(input))
            return false;
        List<String> warns = new ArrayList<>();
        String uni = converter.toUnicode(input, warns, true);
        if (warns.size() > 0)
            return false;
        Matcher m = p.matcher(uni);
        while (m.find()) {
           if (!speller.isCorrect(m.group(0)))
               return false;
        }
        return true;
    }
	
	public static String getBCP47(Element e, String propertyHint, String RID, String subRID) {
	    String lang = e.getAttribute("lang");
	    String encoding = e.getAttribute("encoding");
	    // some entries have language in "type"
	    if (lang.isEmpty()) {
	        lang = e.getAttribute("type");
	        if (!lang.equals("sanskrit") && !lang.equals("tibetan")) lang = "";
	    }
	    String res = "en";
	    if (lang.equals("english") && (!encoding.isEmpty() && !encoding.equals("native"))) {
	        ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "mixed english + encoding `"+encoding+"` turned into `en-x-mixed`, please convert other language to unicode");
	        return "en-x-mixed";
	    }
	    try {
	        res = getBCP47(lang, encoding);
	    } catch (IllegalArgumentException ex) {
	        ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "lang+encoding invalid combination (`"+lang+"`, `"+encoding+"`) turned into `en` tag, exception message: "+ex.getMessage());
	    }
		String value = e.getTextContent().trim();
		// some values are wrongly marked as native instead of extendedWylie
		if ("bo".equals(res) && isAllEwtsChars(value)) {
			res = EWTS_TAG;// could be loc?
		}
		if ((res == null || !res.equals("bo")) && isAllTibetanUnicode(value)) {
            res = "bo";
        }
		if ((res == null || !res.equals("zh")) && isAllChineseUnicode(value)) {
            res = "zh";
        }
		if (res != null && res.equals("zh")) {
		    if (isHant(value)) {
		        res = lowerCaseLangTags ? "zh-hant" : "zh-Hant";
		    } else {
		        res = lowerCaseLangTags ? "zh-hans" : "zh-Hans";
		    }
		}
		if (res != null && res.toLowerCase().equals("zh-latn-pinyin") && isPinyinNDia(value)) {
		    res = res+"-x-ndia";
		}
		if ((res == null || res == "en") && isMostLikelyEwts(value)) {
		    res = EWTS_TAG;
		}
		if (res != null && res.equals("pi")) {
		    if (isAllLatn(value)) {
		        res = "pi-x-iast";
		    }
		    ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "lang+encoding invalid combination (`"+lang+"`, `"+encoding+"`) turned into `"+res+"` tag, Pali must always have a script.");
		}
		return res;
	}
	
	public static String getBCP47(Element e, String dflt, String propertyHint, String RID, String subRID) {
		String res = getBCP47(e, propertyHint, RID, subRID);
		if (dflt != null && (res == null || res.isEmpty())) {
			return dflt;
		}
		return res;
	}
	
	public static String normalizeTibetan(String s) {
	    String res = Normalizer.normalize(s, Normalizer.Form.NFD);
	    // Normalizer doesn't normalize deprecate characters such as 0x0F79
	    res = res.replace("\u0F79", "\u0FB3\u0F71\u0F80");
	    res = res.replace("\u0F77", "\u0FB2\u0F71\u0F80");
	    // it also doesn't normalize characters which use is discouraged:
	    res = res.replace("\u0F81", "\u0F71\u0F80");
	    res = res.replace("\u0F75", "\u0F71\u0F74");
	    res = res.replace("\u0F73", "\u0F71\u0F72");
	    return res;
	}
	
	public static String addEwtsShad(final String s) {
	    // we suppose that there is no space at the end
        if (s == null)
            return s;
        final int sLen = s.length();
        if (sLen < 2)
            return s;
        int last = s.codePointAt(sLen-1);
        if (last == 'a' || last == 'i' || last == 'e' || last == 'o')
            last = s.codePointAt(sLen-2);
        if (sLen > 2 && last == 'g' && s.codePointAt(sLen -3) == 'n')
            return s+" /";
        if (last == 'g' || last == 'k' || (sLen == 3 && last == 'h' && s.codePointAt(sLen -3) == 's') || (sLen > 3 && last == 'h' && s.codePointAt(sLen -3) == 's' && s.codePointAt(sLen -4) != 't'))
            return s;
        if (last < 'A' || last > 'z' || (last > 'Z' && last < 'a'))  // string doesn't end with tibetan letter
            return s;
	    return s+"/";
	}
	
	public static String normalizeEwts(final String s) {
	    return addEwtsShad(s.replace((char)0x2019, (char)0x27));
	}
	
	public static boolean isStandardTibetan(String s) {
	    String[] words = s.split("[ \u0F04-\u0F14\u0F20-\u0F34\u0F3A-\u0F3F]");
	    for (String word: words) {
	        if (!speller.spell(word)) return false; 
	    }
	    return words.length > 0;
	}
	
	public static boolean isDeva(String s) {
	    int c = s.charAt(0);
        if (c < 0x0900 || c > 0x097F)
            return false;
        return true;
	}
	
	static final Pattern englishP = Pattern.compile("\\b(of|is|it|and|that|has|have|for|not|as|if)\\b");
	public static boolean isLikelyEnglish(String value) {
	    Matcher m = englishP.matcher(value);
	    return m.find();
	}
	
	public static Literal getLiteral(Element e, String dflt, Model m, String propertyHint, String RID, String subRID) {
	    return getLiteral(e, dflt, m, propertyHint, RID, subRID, true);
	}
	
	public static Literal getLiteral(Element e, String dflt, Model m, String propertyHint, String RID, String subRID, boolean normalize) {
	        String value = e.getTextContent();
	        value = normalize ? normalizeString(value) : value.trim();
	        if (value.isEmpty()) return null;
	        if (value.indexOf('\ufffd') != -1)
	            ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "string contains invalid replacement character: `"+value+"`");
	        String tag = getBCP47(e, dflt, propertyHint, RID, subRID);
	        if (tag.equals("bo")) {
	            value = normalizeTibetan(value);
	            if (EwtsConverter.isCombining(value.charAt(0))) {
	                ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "Unicode string `"+value+"` starts with combining character");
	            }
	        }
	        if (tag.equals("sa")) {
	            if (value.contains("+"))
	                tag = "sa-x-ewts";
	            else if (isDeva(value))
	                tag = "sa-Deva";
	            else
	                tag = "sa-x-ndia";
	        }
	        if (tag.equals(EWTS_TAG)) {
	            if (RID.startsWith("W1FPL")) {
	                tag = "en";
	            } else {
    	            List<String> conversionWarnings = new ArrayList<String>();
    	            converter.toUnicode(value, conversionWarnings, true);
    	            if (conversionWarnings.size() > 0) {
    	                String fixed = EwtsFixer.getFixedStr(RID, value);
    	                if (fixed == null) {
    	                    if (isLikelyEnglish(value)) {
    	                        tag = "en";
    	                    } else {
    	                        value = normalizeEwts(value);	                        
    	                    }
    	                    ExceptionHelper.logEwtsException(RID, subRID, propertyHint, value, conversionWarnings);
    	                } else if ("LNG".equals(fixed))
    	                    tag = EwtsFixer.guessLang(value);
    	                else
    	                    value = fixed;
    	            } else {
    	                value = normalizeEwts(value);
    	            }
	            }
	        }
	        if (tag.equals("bo-alalc97")) {
	            List<String> conversionWarnings = new ArrayList<String>();
                converterAlalc.toUnicode(value, conversionWarnings, true);
                if (conversionWarnings.size() > 0)
                    ExceptionHelper.logEwtsException(RID, subRID, propertyHint, value, conversionWarnings);
	        }
	        return m.createLiteral(value, tag);
	}
	
	public static boolean documentValidates(Document document, Validator validator) {
	    return documentValidates(document, validator, "");
	}
	
	public static boolean documentValidates(Document document, Validator validator, String fileName) {
		Source xmlSource = new DOMSource(document);
		try {
            validator.validate(xmlSource);
        }
        catch (SAXException ex) {
            MigrationHelpers.writeLog("Document "+fileName+" is not valid because:");
            MigrationHelpers.writeLog(ex.getMessage());
            //ex.printStackTrace();
            return false;
        } catch (IOException e) {
            MigrationHelpers.writeLog("IO problem:");
            MigrationHelpers.writeLog(e.getMessage());
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean rdfOkInOntology(Model m, OntModel o) {
	    return rdfOkInOntology(m, o, "");
	}
	
	public static boolean rdfOkInOntology(Model m, OntModel o, String fileName) {
		o.addSubModel(m);
		ValidityReport vr;
		try {
			vr = o.validate();
		}
		catch(InternalReasonerException e) {
		    MigrationHelpers.writeLog(e.getMessage());
			return false;
		}
		if (vr == null) return true;
		if (!vr.isValid()) {
		    MigrationHelpers.writeLog("Model "+fileName+" not OK in ontology because:");
			Iterator<ValidityReport.Report> itr = vr.getReports();
			while(itr.hasNext()) {
				ValidityReport.Report report = itr.next();
				MigrationHelpers.writeLog(report.toString());
		    }
		}
		return vr.isValid();
	}
	
	// like getElementsByTagNameNS but not recursive (strange DOM doesn't have that)
	public static List<Element> getChildrenByTagName(Element parent, String xsdPrefix, String name) {
	    List<Element> nodeList = new ArrayList<Element>();
	    for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getLocalName())) {
				nodeList.add((Element) child);
			}
		}
	    return nodeList;
	  }

}
