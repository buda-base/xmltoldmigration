package io.bdrc.xmltoldmigration;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.AUT;
import static io.bdrc.libraries.Models.ADR;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDG;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.getAdminRoot;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.shared.BrokenException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.VCARD4;
import org.apache.jena.vocabulary.XSD;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.jena.sttl.STriGWriter;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.xml2files.CommonMigration;
import io.bdrc.xmltoldmigration.xml2files.CorporationMigration;
import io.bdrc.xmltoldmigration.xml2files.ImagegroupMigration;
import io.bdrc.xmltoldmigration.xml2files.LineageMigration;
import io.bdrc.xmltoldmigration.xml2files.OfficeMigration;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration;
import io.bdrc.xmltoldmigration.xml2files.PersonMigration;
import io.bdrc.xmltoldmigration.xml2files.PlaceMigration;
import io.bdrc.xmltoldmigration.xml2files.ProductMigration;
import io.bdrc.xmltoldmigration.xml2files.PubinfoMigration;
import io.bdrc.xmltoldmigration.xml2files.ScanrequestMigration;
import io.bdrc.xmltoldmigration.xml2files.TaxonomyMigration;
import io.bdrc.xmltoldmigration.xml2files.TopicMigration;
import io.bdrc.xmltoldmigration.xml2files.WorkMigration;
import openllet.jena.PelletReasonerFactory;


public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	protected static Map<String,Object> typeToFrameObject = new HashMap<>();
	
	public static Writer logWriter;
	
	public static boolean writefiles = true;
	public static boolean checkagainstOwl = false;
	public static boolean checkagainstXsd = false;
	public static boolean deleteDbBeforeInsert = true;
	
	public static final String BDU = "http://purl.bdrc.io/resource-nc/user/";
    public static final String BF = "http://id.loc.gov/ontologies/bibframe/";
    public static final String LOCDT = "http://id.loc.gov/datatypes/edtf/";
	
	public static PrefixMap prefixMap = getPrefixMap();
	
	public static final String DB_PREFIX = "bdrc_";
	// types in target DB
    public static final String CORPORATION = "corporation";
    public static final String LINEAGE = "lineage";
    public static final String OFFICE = "office";
    public static final String OUTLINE = "outline";
    public static final String PERSON = "person";
    public static final String PLACE = "place";
    public static final String TOPIC = "topic";
    public static final String VOLUMES = "volumes";
    public static final String INSTANCE = "instance";
    public static final String IINSTANCE = "iinstance";
    public static final String EINSTANCE = "einstance";
    public static final String ITEMS = "items";
    public static final String ITEM = "item";
    public static final String WORK = "work";
    // types in source DB and not in target DB
    public static final String IMAGEGROUP = "imagegroup";
    public static final String PRODUCT = "product";
    public static final String PUBINFO = "pubinfo";
    public static final String SCANREQUEST = "scanrequest";
    public static final String VOLUME = "volume";
    public static final String TAXONOMY = "taxonomy";
    
    public static final int OUTPUT_STTL = 0;
    public static final int OUTPUT_JSONLD = 1;
    public static final int OUTPUT_TRIG = 2;
    
    public static Lang sttl;
    public static Lang strig;
    public static Context ctx;
    
    public static final Map<String,String> typeToXsdPrefix = new HashMap<>();
    
    public static final Map<String, Boolean> disconnectedRIds;
    public static final Map<String, Boolean> nokForLending;
    public static final Map<String, String> ridReplacements;
    public static final Map<String, String> tol;
    public static final Map<String, String> oclcW;
    public static final Map<String, String> instanceClusters;
    public static final Map<String, Boolean> removeW;
    public static final Map<String, Boolean> ricWithOutline;
    
    public static final Map<String, Boolean> mwInCopyright;
    public static final Map<String, Boolean> mwCopyrightClaimed;
    public static final Map<String, Boolean> mwCopyrightUndetermined;
    public static final Map<String, List<String>> personTraditions;
    
    public static final Map<String, Boolean> ricrid;

    static {
        try {
            logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Well, that's a stupid try/catch...
            e.printStackTrace();
        }
        
        instanceClusters = setupInstanceClusters();
        ricWithOutline = setupRICWithoutlines();
        removeW = setupRemoveW();
        disconnectedRIds = setupDisconnectedRIDs();
        nokForLending = setupSimpleList("nokforcdl.txt");
        mwInCopyright = setupSimpleList("mw-copyrighted.csv");
        mwCopyrightClaimed = setupSimpleList("mw-copyright-claimed.csv");
        mwCopyrightUndetermined = setupSimpleList("mw-copyright-undetermined.csv");
        ricrid = setupRICRID();
        ridReplacements = setupRIDReplacements();
        personTraditions = setupPersonTraditions();
        tol = setupTol();
        oclcW = setupOclcW();
        setupSTTL();
        typeToXsdPrefix.put(CORPORATION, CorporationMigration.CXSDNS);
        typeToXsdPrefix.put(LINEAGE, LineageMigration.LXSDNS);
        typeToXsdPrefix.put(OFFICE, OfficeMigration.OXSDNS);
        typeToXsdPrefix.put(OUTLINE, OutlineMigration.OXSDNS);
        typeToXsdPrefix.put(PERSON, PersonMigration.PXSDNS);
        typeToXsdPrefix.put(PLACE, PlaceMigration.PLXSDNS);
        typeToXsdPrefix.put(TOPIC, TopicMigration.TXSDNS);
        //typeToXsdPrefix.put(ITEMS, CorporationMigration.CXSDNS);
        //typeToXsdPrefix.put(ITEM, CorporationMigration.CXSDNS);
        typeToXsdPrefix.put(WORK, WorkMigration.WXSDNS);
        typeToXsdPrefix.put(IMAGEGROUP, ImagegroupMigration.IGXSDNS);
        typeToXsdPrefix.put(PRODUCT, ProductMigration.PRXSDNS);
        typeToXsdPrefix.put(PUBINFO, PubinfoMigration.WPXSDNS);
        typeToXsdPrefix.put(SCANREQUEST, ScanrequestMigration.SRXSDNS);
        //typeToXsdPrefix.put(VOLUME, CorporationMigration.CXSDNS);
        //typeToXsdPrefix.put(TAXONOMY, CorporationMigration.CXSDNS);
    }
    
    public static boolean isDisconnected(String RID) {
        return disconnectedRIds.containsKey(RID);
        //return false;
    }
	
    public static Map<String, Boolean> setupDisconnectedRIDs() {
        final Map<String,Boolean> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("disconnectedRIDs.txt");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String line = reader.readLine();
                 res.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Map<String, Boolean> setupRICWithoutlines() {
        final Map<String,Boolean> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("ricwithoutline.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String line = reader.readLine();
                 res.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, List<String>> setupPersonTraditions() {
        final Map<String, List<String>> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("person-traditions.csv");

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] lineInArray;
            while ((lineInArray = reader.readNext()) != null) {
                res.put(lineInArray[0], Arrays.asList(lineInArray[1].split(",")));
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, Boolean> setupRICRID() {
        final Map<String,Boolean> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("rid-ric.txt");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String line = reader.readLine();
                 res.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static void setPrefixes(Model m) {
        setPrefixes(m, false);
    }

    public static void setPrefixes(Model m, String type) {
        setPrefixes(m, type.equals("place"));
        if (type.equals("subscriber")) {
            m.setNsPrefix("aut", AUT);
            m.setNsPrefix("adr", ADR);
        }
    }
    
    public static void setPrefixes(Model m, boolean addVcard) {
        m.setNsPrefix("", BDO);
        m.setNsPrefix("adm", ADM);
        m.setNsPrefix("bdr", BDR);
        m.setNsPrefix("bda", BDA);
        m.setNsPrefix("bdg", BDG);
        m.setNsPrefix("bdu", BDU);
        m.setNsPrefix("owl", OWL.getURI());
        m.setNsPrefix("bf", BF);
        m.setNsPrefix("edtf", LOCDT);
        m.setNsPrefix("rdf", RDF.getURI());
        m.setNsPrefix("rdfs", RDFS.getURI());
        m.setNsPrefix("skos", SKOS.getURI());
        m.setNsPrefix("xsd", XSD.getURI());
        m.setNsPrefix("rkts", "http://purl.rkts.eu/resource/");
        if (addVcard)
            m.setNsPrefix("vcard", VCARD4.getURI());

    }
    
    public static Map<String, Boolean> setupSimpleList(final String filename) {
        final Map<String,Boolean> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream(filename);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String rid = reader.readLine().trim();
                 res.put(rid, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, String> setupTol() {
        final Map<String,String> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("tol.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String[] line = reader.readLine().split(",");
                 res.put(line[1], line[2]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, String> setupOclcW() {
        final Map<String,String> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("oclc-ia.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String[] line = reader.readLine().split(",");
                 res.put(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, String> setupRIDReplacements() {
        final Map<String,String> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("ridReplacements.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String[] line = reader.readLine().split(",");
                 res.put(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public static Map<String, Boolean> setupRemoveW() {
        final Map<String,Boolean> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("femc-removeW.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                 String line = reader.readLine();
                 res.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static Map<String, String> setupInstanceClusters() {
        final Map<String,String> res = new HashMap<>();
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("instance-clusters.csv");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        try {
            while(reader.ready()) {
                String[] line = reader.readLine().split(",");
                res.put(line[0], line[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }
    
    public static void writeRIDReplacements(String filename) {
        PrintWriter writer;
        try {
            writer = new PrintWriter(filename, "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        for (Entry<String,String> e : ridReplacements.entrySet()) {
            writer.println(e.getKey()+","+e.getValue());
        }
        writer.close();
    }
    
    public static class ResourceInfo {
        public Map<String,String> links = null;
        public String status = "absent";
        
        public ResourceInfo() {
            this.links = null;
            this.status = "absent";
        }
    }
    
    public static Map<String,ResourceInfo> resourceInfos = new HashMap<>();
    
    public static void resourceHasStatus(String res, String status) {
        if (res.startsWith("G9GBX") || res.contains("TLM")) return;
        ResourceInfo ri = resourceInfos.computeIfAbsent(res, x -> new ResourceInfo());
        ri.status = status;
        if (status.equals("released"))
            ri.links = null;
    }

    public static void resourceReplacedWith(String res, String rid) {
        ridReplacements.put(res, rid);
    }
    
    public static void recordLinkTo(String orig, String prop, String to) {
        if (orig == null || orig.startsWith("G9GBX") || orig.contains("TLM")) return;
        if (isDisconnected(to)) return;
        ResourceInfo ri = resourceInfos.computeIfAbsent(to, x -> new ResourceInfo());
        if (!ri.status.equals("released")) {
            if (ri.links == null)
                ri.links = new HashMap<String,String>();
            ri.links.put(orig, prop);
        }
    }

    public static String sanitizeRID(String orig, String prop, String to) {
        String repl = ridReplacements.get(to);
        if (repl != null) {
            to = repl;
        }
        recordLinkTo(orig, prop, to);
        return to;
    }
    
    public static void reportMissing() {
        for (Entry<String,ResourceInfo> e : resourceInfos.entrySet()) {
            final ResourceInfo ri = e.getValue();
            if (ri.status.equals("released") || ri.links == null)
                continue;
            StringBuilder sb = new StringBuilder();
            sb.append("resource in status `"+ri.status+"` linked from ");
            for (Entry<String,String> e2 : ri.links.entrySet()) {
                sb.append("`"+e2.getKey()+"` (prop. `"+e2.getValue()+"`) ");
            }
            ExceptionHelper.logException(ExceptionHelper.ET_MISSING, e.getKey(), e.getKey(), e.getKey(), sb.toString());
        }
        resourceInfos = null;
    }
    
    
    
    public static PrefixMap getPrefixMap() {
        PrefixMap pm = PrefixMapFactory.create();
        pm.add("",      BDO);
        pm.add("adm",   ADM);
        pm.add("bda",   BDA);
        pm.add("bdg",   BDG);
        pm.add("bdr",   BDR);
        pm.add("bdu",   BDU);
        pm.add("owl",   OWL.getURI());
        pm.add("rdf",   RDF.getURI());
        pm.add("rdfs",  RDFS.getURI()); ;
        pm.add("skos",  SKOS.getURI());
        pm.add("vcard", VCARD4.getURI());
        pm.add("xsd",   XSD.getURI());
        return pm;
    }
    
    public static PrefixMap getSubPrefixMap() {
        PrefixMap pm = PrefixMapFactory.create();
        pm.add("",      BDO);
        pm.add("adm",   ADM);
        pm.add("bda",   BDA);
        pm.add("bdg",   BDG);
        pm.add("bdr",   BDR);
        pm.add("bdu",   BDU);
        pm.add("aut",   AUT);
        pm.add("adr",   ADR);
        pm.add("owl",   OWL.getURI());
        pm.add("rdf",   RDF.getURI());
        pm.add("rdfs",  RDFS.getURI()); ;
        pm.add("skos",  SKOS.getURI());
        pm.add("vcard", VCARD4.getURI());
        pm.add("xsd",   XSD.getURI());
        return pm;
    }
    
    public static PrefixMap stdPrefixMap = getPrefixMap();
    public static PrefixMap subPrefixMap = getSubPrefixMap();

    public static PrefixMap getPrefixMap(String type) {
        if ("subscriber".equals(type)) {
            return subPrefixMap;
        }
        return stdPrefixMap;
    }
    
    public static void setupSTTL() {
        sttl = STTLWriter.registerWriter();
        strig = STriGWriter.registerWriter();
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put(ADM, 5);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add(ADM+"logDate");
        predicatesPrio.add(BDO+"seqNum");
        predicatesPrio.add(BDO+"eventWhen");
        predicatesPrio.add(BDO+"noteText");
        predicatesPrio.add(BDO+"noteSource");
        predicatesPrio.add(BDO+"contentLocationStatement");
        predicatesPrio.add(BDO+"volumeNumber");
        predicatesPrio.add(BDO+"eventWho");
        predicatesPrio.add(BDO+"eventWhere");
        ctx = new Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 4);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "predicateBaseWidth"), 18);
    }
    
	public static void writeLogsTo(PrintWriter w) {
	    logWriter = w;
	}

	public static void writeLog(String s) {
        try {
            logWriter.write(s+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static String inputStreamToString(InputStream s) {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        try {
            while ((length = s.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            return null;
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        try {
            return result.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void modelToOutputStream(Model m, String type, int outputType, String fname) {
        OutputStream out = null;
        try {
            if (m == null) 
                throw new IllegalArgumentException("modelToOutputStream called with null model");
            if (fname == null || fname.isEmpty()) 
                return;
            if (fname.contains(".ttl") || fname.contains(".trig")) {
                out = new FileOutputStream(fname);
            } else {
                if (outputType == OUTPUT_TRIG) {
                    out = new FileOutputStream(fname + ".trig");
                } else if (outputType == OUTPUT_STTL) {
                    out = new FileOutputStream(fname + ".ttl");
                }
            }
            modelToOutputStream(m, out, type, outputType, fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IllegalArgumentException e) {
            writeLog("error writing "+fname+": "+e.getMessage());
        }
        
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void modelToOutputStream(Model m, OutputStream out, String type, int outputType, String fname) 
            throws FileNotFoundException 
    {
        if (outputType == OUTPUT_STTL) {
            RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build().output(out);
            return;
        }
        if (outputType == OUTPUT_TRIG) {
            // compute graph uri from fname; if fname == null then testing so use a dummy graph URI
            String foo = (fname != null && !fname.isEmpty()) ? fname.substring(fname.lastIndexOf("/")+1) : "GraphForTesting";
            foo = foo.replace(".trig",  "").replace(".ttl",  "");
            String uriStr = BDG+foo;
            Node graphUri = NodeFactory.createURI(uriStr);
            DatasetGraph dsg = DatasetGraphFactory.createGeneral();
            try {
                dsg.addGraph(graphUri, m.getGraph());
                new STriGWriter().write(out, dsg, getPrefixMap(type), null, ctx);
            } catch (BrokenException e) {
                System.err.println("can't write "+foo+": "+e.getMessage());
                m.write(System.err, "TTL");
            }
            
        }
    }

    public static boolean isSimilarTo(Model src, Model dst) {
        return src.isIsomorphicWith(dst);
    }
	
	public static Document documentFromFileName(String fname) {
		if (documentFactory == null) {
			documentFactory = DocumentBuilderFactory.newInstance();
		}
		// working around http://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8175245
		documentFactory.setNamespaceAware(true);
		Document document = null;
		// create a new builder at each document to parse in parallel
		try {
		    final DocumentBuilder builder = documentFactory.newDocumentBuilder();   
		    document = builder.parse(new File(fname));
		}
		catch (final ParserConfigurationException | SAXException | IOException e) {
		    e.printStackTrace();
		}
		return document;
	}

	public static Model modelFromFileName(String fname) {
	    Model model = ModelFactory.createDefaultModel();
	    Graph g = model.getGraph();

	    if (fname.endsWith(".ttl")) {
	        try {
	            // workaround for https://github.com/jsonld-java/jsonld-java/issues/199
	            RDFParser.create()
    	            .source(fname)
    	            .lang(RDFLanguages.TTL)
    	            .parse(StreamRDFLib.graph(g));
	        } catch (RiotException e) {
	            writeLog("error reading "+fname);
	            return null;
	        }
	    } else if (fname.endsWith(".trig")) {
	        try {
	            Dataset dataset = RDFDataMgr.loadDataset(fname);
	            Iterator<String> iter = dataset.listNames();
	            if (iter.hasNext()) {
	                String graphUri = iter.next();
	                if (iter.hasNext())
	                    writeLog("modelFromFileName " + fname + " getting named model: " + graphUri + ". Has more graphs! ");
	                model = dataset.getNamedModel(graphUri);
	            }
	        } catch (RiotException e) {
	            writeLog("error reading "+fname);
	            return null;
	        }
	    }

	    setPrefixes(model);
	    return model;
	}

	public static Model xmlToRdf(Document d, String type) {
		Model m = null;
		switch (type) {
		case CORPORATION:
			m = CorporationMigration.MigrateCorporation(d);
			break;
		case PERSON:
			m = PersonMigration.MigratePerson(d);
			break;
		case PLACE:
			m = PlaceMigration.MigratePlace(d);
			break;
		case IMAGEGROUP:
            m = ImagegroupMigration.MigrateImagegroup(d);
            break;
		case LINEAGE:
			m = LineageMigration.MigrateLineage(d);
			break;
        case OFFICE:
            m = OfficeMigration.MigrateOffice(d);
            break;
//        case OUTLINE:
//            m = OutlineMigration.MigrateOutline(d);
//            break;
        case SCANREQUEST:
            m = ScanrequestMigration.MigrateScanrequest(d);
            break;
        case TOPIC:
            m = TopicMigration.MigrateTopic(d);
            break;
        case TAXONOMY:
            m = TaxonomyMigration.MigrateTaxonomy(d);
            break;
//	    case WORK:
//	        m = WorkMigration.MigrateWork(d);
//	        break;
		default:
			// arg
			return m;
		}
		return m;
	}
	
	public static boolean mustBeMigrated(Element root, String type, String status) {
	    boolean res = (!status.isEmpty() && !status.equals("withdrawn") && !status.equals("onHold"));
	    if (res == false) return false;
	    if (type.equals("outline")) {
	        res = res && !OutlineMigration.ridsToIgnore.containsKey(root.getAttribute("RID"));   
	    }
	    if (type.equals("place") && root.getAttribute("RID").startsWith("G9GBX") && !status.equals("released")) {
	        return false;
	    }
	    return res;
	}
	
	// model from XML
	public static Model getModelFromFile(String src, String type, String fileName) {
	    Document d = documentFromFileName(src);
	    if (checkagainstXsd) {
	        Validator v = getValidatorFor(type);
	        CommonMigration.documentValidates(d, v, src);
	    }
        Element root = d.getDocumentElement();
        final String status = root.getAttribute("status");
        MigrationHelpers.resourceHasStatus(root.getAttribute("RID"), status);
        Model m = null;
        if (status.equals("withdrawn") || status.equals("onHold")) {
            return migrateWithdrawn(d, type);      
        }
        if (!mustBeMigrated(root, type, status)) {
            return null;
        }
        try {
            m = xmlToRdf(d, type);
        } catch (IllegalArgumentException e) {
            writeLog("error in "+fileName+" "+e.getMessage());
        }
        return m;
	}
	
	private static final Pattern withdrawnPattern = Pattern.compile("(?i:withdrawn in ?favou?re? of) +([a-zA-Z]+[0-9]+[a-zA-Z0-9]+).*");
	
	public static Model migrateWithdrawn(Document xmlDocument, final String type) {
	    if (type.equals(PUBINFO) || type.equals(SCANREQUEST)) {
	        return null;
	    }
        
	    Element root = xmlDocument.getDocumentElement();        
        Model m = xmlToRdf(xmlDocument, type);
        Resource admMain = getAdminRoot(m);
        Resource main = admMain.getPropertyResourceValue(m.createProperty(ADM+"adminAbout"));
        String thisRID = root.getAttribute("RID");
                
        final String XsdPrefix = typeToXsdPrefix.get(type);
        NodeList nodeList = root.getElementsByTagNameNS(XsdPrefix, "log");
        String withdrawnmsg = null;
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element log = (Element) nodeList.item(i);
            NodeList logEntriesList = log.getElementsByTagNameNS(XsdPrefix, "entry");
            for (int j = 0; j < logEntriesList.getLength(); j++) {
                Element logEntry = (Element) logEntriesList.item(j);
                final String msg = logEntry.getTextContent();
                if (msg.toLowerCase().contains("withdrawn in "))
                    withdrawnmsg = msg.trim();
            }
            logEntriesList = log.getElementsByTagName("entry");
            for (int k = 0; k < logEntriesList.getLength(); k++) {
                Element logEntry = (Element) logEntriesList.item(k);
                final String msg = logEntry.getTextContent();
                if (msg.toLowerCase().contains("withdrawn in "))
                    withdrawnmsg = msg.trim();
            }
        }
        
        
        if (ridReplacements.containsKey(thisRID)) {
            final String rid = ridReplacements.get(thisRID);
            admMain.addProperty(m.createProperty(ADM, "replaceWith"), m.createResource(BDR+rid));
        } else if (withdrawnmsg != null) {
            Matcher matcher = withdrawnPattern.matcher(withdrawnmsg);
            if (!matcher.matches()) {
                System.out.println("possible typo in withdrawing log message in "+main.getLocalName()+": "+withdrawnmsg);
            } else {
                final String rid = matcher.group(1).toUpperCase();
                admMain.addProperty(m.createProperty(ADM, "replaceWith"), m.createResource(BDR+rid));
                resourceReplacedWith(thisRID, rid);
            }
        }
        
        return m;
    }

    public static void outputOneModel(Model m, String mainId, String dst, String type) {
	    if (m == null) return;
        if (writefiles) {
            modelToOutputStream(m, type, OUTPUT_TRIG, dst);
        }
	}
	
	public static void removeIndividuals(OntModel o) {
		ExtendedIterator<Individual> it = o.listIndividuals();
	    while(it.hasNext()) {
			Individual i = it.next();
			if (i.getLocalName().equals("UNKNOWN")) continue;
			i.remove();
	    }
	}

	
	@SuppressWarnings("unused")
    private static OntModel getInferredModel(OntModel m) {
	    OntModel ontoModelInferred = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, m);
	    ontoModelInferred.setStrictMode(false);
	    return ontoModelInferred;
	}
	
	public static Map<String,Validator> validators = new HashMap<String,Validator>();
	
	private static final Pattern basicP = Pattern.compile("[^|]+");
	public static Map<String,Integer> getImgmapForImggrp(String imagegrouplname) {
	    Map<String,Integer> res = new HashMap<>();
	    if (!imagegrouplname.startsWith("I")) {
	        imagegrouplname = "I"+imagegrouplname;
	    }
	    String fname = MigrationApp.XML_DIR+"tbrc-imagegroups/"+imagegrouplname+".xml";
	    File f = new File(fname);
	    if (!f.exists()) {
	        System.err.println("can't find "+fname);
	        return res;
	    }
	    Document d = MigrationHelpers.documentFromFileName(fname);
        Element root = d.getDocumentElement();
        String imglist="";
        NodeList nodeList = root.getElementsByTagNameNS(ImagegroupMigration.IGXSDNS, "description");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            String type = current.getAttribute("type").trim();
            if (!type.equals("ondisk") && !type.equals("onDisk")) continue;
            imglist = current.getTextContent();
        }
	    Matcher basicM = basicP.matcher(imglist);
	    int i = 1;
	    while (basicM.find()) {
	        final String imgfname = basicM.group(0).toLowerCase();
	        int dotidx = imgfname.lastIndexOf('.');
	        if (dotidx == -1) {
	            System.err.println("strange image name: "+imgfname);
	            continue;
	        }
	        res.put(imgfname.substring(0,dotidx), i);
	        i += 1;
	    }
	    return res;
	}
	
	public static Validator getValidatorFor(String type) {
	    Validator res = validators.get("type");
	    if (res != null) return res;
		String xsdFullName = "src/main/resources/xsd/"+type+".xsd";
		SchemaFactory factory = 
	            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema;
		try {
			schema = factory.newSchema(new File(xsdFullName));
		}
		catch (SAXException ex) {
			System.err.println("xsd file looks invalid...");
			return null;
		}
		res = schema.newValidator();
		validators.put(type, res);
		return res;
	}
}
