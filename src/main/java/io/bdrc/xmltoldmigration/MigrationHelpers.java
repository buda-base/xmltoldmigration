package io.bdrc.xmltoldmigration;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.util.iterator.ExtendedIterator;
import openllet.jena.PelletReasonerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;
import io.bdrc.xmltoldmigration.helpers.ContextGenerator;
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

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;


public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	protected static Map<String,Object> typeToFrameObject = new HashMap<>();
	
	public static Writer logWriter;
	
	public static boolean writefiles = true;
	public static boolean checkagainstOwl = false;
	public static boolean checkagainstXsd = false;
	public static boolean deleteDbBeforeInsert = true;
	
	public static OntModel ontologymodelSimple = MigrationHelpers.getOntologyModel();
	public static OntModel ontologymodel = MigrationHelpers.getInferredModel(ontologymodelSimple);
	public static PrefixMap prefixMap = getPrefixMap();
	public static final Map<String,Object> jsonldcontext = ContextGenerator.generateContextObject(ontologymodelSimple, prefixMap, "bdo");
	
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
    
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
    private static final String ADM = CommonMigration.ADMIN_PREFIX;
    
    public static Lang sttl;
    public static Context ctx;
    
    public static final Map<String,String> typeToXsdPrefix = new HashMap<>();
    
    public static final Map<String, Boolean> disconnectedRIds;

    static {
        ontologymodel = MigrationHelpers.getOntologyModel();
        try {
            logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Well, that's a stupid try/catch...
            e.printStackTrace();
        }
        disconnectedRIds = setupDisconnectedRIDs();
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
        pm.add("", CommonMigration.ONTOLOGY_PREFIX);
        pm.add("adm", CommonMigration.ADMIN_PREFIX);
        pm.add("bdd", CommonMigration.DATA_PREFIX);
        pm.add("bdr", CommonMigration.RESOURCE_PREFIX);
        pm.add("owl", CommonMigration.OWL_PREFIX);
        pm.add("rdf", CommonMigration.RDF_PREFIX);
        pm.add("rdfs", CommonMigration.RDFS_PREFIX);
        pm.add("skos", CommonMigration.SKOS_PREFIX);
        pm.add("vcard", CommonMigration.VCARD_PREFIX);
        pm.add("xsd", CommonMigration.XSD_PREFIX);
        return pm;
    }
    
    public static void setupSTTL() {
        sttl = STTLWriter.registerWriter();
        SortedMap<String, Integer> nsPrio = ComparePredicates.getDefaultNSPriorities();
        nsPrio.put(SKOS.getURI(), 1);
        nsPrio.put("http://purl.bdrc.io/ontology/admin/", 5);
        nsPrio.put("http://purl.bdrc.io/ontology/toberemoved/", 6);
        List<String> predicatesPrio = CompareComplex.getDefaultPropUris();
        predicatesPrio.add(CommonMigration.ADM+"logDate");
        predicatesPrio.add(CommonMigration.BDO+"seqNum");
        predicatesPrio.add(CommonMigration.BDO+"onYear");
        predicatesPrio.add(CommonMigration.BDO+"notBefore");
        predicatesPrio.add(CommonMigration.BDO+"notAfter");
        predicatesPrio.add(CommonMigration.BDO+"noteText");
        predicatesPrio.add(CommonMigration.BDO+"noteWork");
        predicatesPrio.add(CommonMigration.BDO+"noteLocationStatement");
        predicatesPrio.add(CommonMigration.BDO+"volumeNumber");
        predicatesPrio.add(CommonMigration.BDO+"eventWho");
        predicatesPrio.add(CommonMigration.BDO+"eventWhere");
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

    public static void modelToOutputStream (Model m, OutputStream out, String type, int outputType, String mainResourceName) throws IllegalArgumentException {
	    if (m==null) 
	        throw new IllegalArgumentException("null model returned");
	    if (out == null) return;
	    if (outputType == OUTPUT_STTL) {
	        RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build().output(out);
	        return;
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
		try {
		    // workaround for https://github.com/jsonld-java/jsonld-java/issues/199
		    RDFParserBuilder pb = RDFParser.create()
		             .source(fname)
		             .lang(RDFLanguages.TTL);
		             //.canonicalLiterals(true);
		    pb.parse(StreamRDFLib.graph(g));
		} catch (RiotException e) {
		    writeLog("error reading "+fname);
		    return null;
		}
		CommonMigration.setPrefixes(model);
		return model;
	}
	
	public static void modelToFileName(Model m, String fname, String type, int outputType) {
	    FileOutputStream s = null;
	    try {
		    s = new FileOutputStream(fname);
		    modelToOutputStream(m, s, type, outputType, fname);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (IllegalArgumentException e) {
		    writeLog("error writing "+fname+": "+e.getMessage());
		}
		try {
            s.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
		case PRODUCT:
			m = ProductMigration.MigrateProduct(d);
			break;
		case PUBINFO:
            m = PubinfoMigration.MigratePubinfo(d);
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
        case OUTLINE:
            m = OutlineMigration.MigrateOutline(d);
            break;
        case SCANREQUEST:
            m = ScanrequestMigration.MigrateScanrequest(d);
            break;
        case TOPIC:
            m = TopicMigration.MigrateTopic(d);
            break;
        case TAXONOMY:
            m = TaxonomyMigration.MigrateTaxonomy(d);
            break;
	    case WORK:
	        m = WorkMigration.MigrateWork(d);
	        break;
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
        if (!mustBeMigrated(root, type, status)) {
            return null;
        }
        Model m = null;
        if (status.equals("withdrawn")) {
            return migrateWithdrawn(d, type);      
        }
        try {
            m = xmlToRdf(d, type);
        } catch (IllegalArgumentException e) {
            writeLog("error in "+fileName+" "+e.getMessage());
        }
        if (checkagainstOwl) {
            CommonMigration.rdfOkInOntology(m, ontologymodel);
        }
        return m;
	}
	
	public static Model migrateWithdrawn(Document xmlDocument, final String type) {
	    if (type.equals(PUBINFO) || type.equals(SCANREQUEST)) {
	        return null;
	    }
        Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m, type);
        Element root = xmlDocument.getDocumentElement();
        Resource main = m.createResource(CommonMigration.BDR + root.getAttribute("RID"));
        CommonMigration.addStatus(m, main, root.getAttribute("status"));
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
        if (withdrawnmsg != null) {
            final String prefix = "withdrawn in favor of ";
            if (withdrawnmsg.toLowerCase().startsWith(prefix)) {
                String rid = withdrawnmsg.substring(prefix.length());
                if (rid.matches("[A-Z0-9]+")) {
                    main.addProperty(m.createProperty(ADM, "replaceWithIndividual"), m.createResource(BDR+rid));
                    // TODO
                    //MigrationHelpers.resourceHasStatus(root.getAttribute("RID"), status);
                } else {
                    System.out.println("possible typo in withdrawing log message in "+main.getLocalName()+": "+withdrawnmsg);
                }
            } else {
                System.out.println("possible typo in withdrawing log message in "+main.getLocalName()+": "+withdrawnmsg);
            }
        }
        return m;
    }

    public static void outputOneModel(Model m, String mainId, String dst, String type) {
	    if (m == null) return;
        if (writefiles) {
            modelToFileName(m, dst, type, OUTPUT_STTL);
        }
	}
	
	// change Range Datatypes from rdf:PlainLitteral to rdf:langString
	public static void rdf10tordf11(OntModel o) {
		Resource RDFPL = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
		Resource RDFLS = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
		ExtendedIterator<DatatypeProperty> it = o.listDatatypeProperties();
	    while(it.hasNext()) {
			DatatypeProperty p = it.next();
			if (p.hasRange(RDFPL)) {
			    p.removeRange(RDFPL);
			    p.addRange(RDFLS);
			}
	    }
	    ExtendedIterator<Restriction> it2 = o.listRestrictions();
	    while(it2.hasNext()) {
            Restriction r = it2.next();
            Statement s = r.getProperty(OWL2.onDataRange); // is that code obvious? no
            if (s != null && s.getObject().asResource().equals(RDFPL)) {
                s.changeObject(RDFLS);
            }
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
	
	public static OntModel getOntologyModel()
	{   
		// the initial model from Protege is not considered valid by Openllet because
		// it's RDF1.0, so we first open it with no reasoner:
		OntModel ontoModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);
	    try {
            ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream("owl-file/bdrc.owl");
	        ontoModel.read(inputStream, "", "RDF/XML");
	        inputStream.close();
	    } catch (Exception e) {
	        System.err.println(e.getMessage());
	        System.exit(1);
	    }
	    // then we fix it by converting RDF-1.0 to RDF-1.1
	    rdf10tordf11(ontoModel);
	    return ontoModel;
	}
	
	public static OntModel getInferredModel(OntModel m) {
	    OntModel ontoModelInferred = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, m);
	    ontoModelInferred.setStrictMode(false);
	    return ontoModelInferred;
	}
	
	public static Map<String,Validator> validators = new HashMap<String,Validator>();
	
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
