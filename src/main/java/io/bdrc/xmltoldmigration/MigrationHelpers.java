package io.bdrc.xmltoldmigration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.graph.Graph;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormat.JSONLDVariant;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserBuilder;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DocumentNotFoundException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.HttpResponse;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbConnector;
import org.ektorp.impl.StdCouchDbInstance;

import openllet.jena.PelletReasonerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;

import io.bdrc.jena.sttl.CompareComplex;
import io.bdrc.jena.sttl.ComparePredicates;
import io.bdrc.jena.sttl.STTLWriter;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.SKOS;


public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	protected static Map<String,Object> typeToFrameObject = new HashMap<>();
	
	public static Writer logWriter;
	
	public static HttpClient httpClient;
	public static CouchDbInstance dbInstance;
//	public static CouchDbConnector db = null;
	
	public static boolean usecouchdb = false;
	public static boolean writefiles = false;
	public static boolean checkagainstOwl = false;
	public static boolean checkagainstXsd = true;
	
	private static ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeReference<HashMap<String,Object>> hashMapTypeRef = new TypeReference<HashMap<String,Object>>() {};
	
	public static OntModel ontologymodelSimple = MigrationHelpers.getOntologyModel();
	public static OntModel ontologymodel = MigrationHelpers.getInferredModel(ontologymodelSimple);
	public static PrefixMap prefixMap = getPrefixMap();
	public static Map<String,Object> jsonldcontext = ContextGenerator.generateContextObject(ontologymodelSimple, prefixMap, "bdo");
	
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
    
    public static final int OUTPUT_STTL = 0;
    public static final int OUTPUT_JSONLD = 1;
    
    public static Lang sttl;
    public static Context ctx;
	
    public static Hashtable<String, CouchDbConnector> dbs = new Hashtable<>();
    
    public static void putDB(String type) {
        dbs.put(type, new StdCouchDbConnector(DB_PREFIX + type, dbInstance));
    }

    static {
        ontologymodel = MigrationHelpers.getOntologyModel();
    	try {
            httpClient = new StdHttpClient.Builder()
                    .url("http://localhost:13598")
                    .build();
            dbInstance = new StdCouchDbInstance(httpClient);

            putDB(CORPORATION);
            putDB(LINEAGE);
            putDB(OFFICE);
            putDB(OUTLINE);
            putDB(PERSON);
            putDB(PLACE);
            putDB(PRODUCT);
            putDB(TOPIC);
            putDB(ITEM);
            putDB(WORK);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

	    try {
            logWriter = new PrintWriter(new OutputStreamWriter(System.err, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // Well, that's a stupid try/catch...
            e.printStackTrace();
        }
	    
	    setupSTTL();
	}
	
    public static PrefixMap getPrefixMap() {
        PrefixMap pm = PrefixMapFactory.create();
        pm.add("", CommonMigration.ONTOLOGY_PREFIX);
        pm.add("adm", CommonMigration.ADMIN_PREFIX);
        pm.add("bdd", CommonMigration.DATA_PREFIX);
        pm.add("bdr", CommonMigration.RESOURCE_PREFIX);
        pm.add("tbr", CommonMigration.TBR_PREFIX);
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
        predicatesPrio.add(CommonMigration.BDO+"onOrAbout");
        predicatesPrio.add(CommonMigration.BDO+"noteText");
        predicatesPrio.add(CommonMigration.BDO+"noteWork");
        predicatesPrio.add(CommonMigration.BDO+"noteLocationStatement");
        ctx = new Context();
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsPriorities"), nsPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "nsDefaultPriority"), 2);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "complexPredicatesPriorities"), predicatesPrio);
        ctx.set(Symbol.create(STTLWriter.SYMBOLS_NS + "indentBase"), 3);
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

	public static Map<String,String> typeToRootShortUri = new HashMap<String,String>();
	static {
		typeToRootShortUri.put(PERSON, "Person");
		typeToRootShortUri.put(WORK, "Work");
		typeToRootShortUri.put(OUTLINE, "Work");
		typeToRootShortUri.put(PLACE, "Place");
		typeToRootShortUri.put(TOPIC, "Topic");
		typeToRootShortUri.put(LINEAGE, "Lineage");
		typeToRootShortUri.put(CORPORATION, "Corporation");
		typeToRootShortUri.put(PRODUCT, "adm:Product");
		typeToRootShortUri.put(ITEM, "ItemImageAsset");
		typeToRootShortUri.put(OFFICE, "Role");
    }
	
	public static Object getFrameObject(String type) {
	    if (typeToFrameObject.containsKey(type))
	        return typeToFrameObject.get(type);
		Map<String,Object> jsonObject = new HashMap<String,Object>();
		jsonObject.put("@type", typeToRootShortUri.get(type));
		jsonObject.put("@context", jsonldcontext);
		typeToFrameObject.put(type, jsonObject);
		return jsonObject;
	}
	
	 static class MigrationComparator implements Comparator<String>
	 {
	     public int compare(String s1, String s2)
	     {
	         if(s1.equals("log_entry")) return 1;
	         if(s2.equals("log_entry")) return -1;
	         if(s1.equals("@context")) return 1;
	         if(s1.equals("@graph")) return -1;
	         if(s1.equals("rdfs:label")) return -1;
	         if(s1.equals("skos:prefLabel")) return -1;
	         if(s1.equals("skos:altLabel")) return -1;
	         if(s1.equals("status")) return -1;
	         return s1.compareTo(s2);
	     }
	 }
	
	@SuppressWarnings("unchecked")
    protected static void insertRec(String k, Object v, SortedMap<String,Object> tm) throws IllegalArgumentException {
	    if (k.equals("@graph")) {
	        if (v instanceof ArrayList) {
	            if (((ArrayList<Object>) v).size() == 0) {
	                tm.put(k,v);
	                //throw new IllegalArgumentException("empty graph, shouldn't happen!");
	                return;
	            }
	            Object o = ((ArrayList<Object>) v).get(0);
	            if (o instanceof Map) {
                    Map<String,Object> orderedo = orderEntries((Map<String,Object>) o);
                    ((ArrayList<Object>) v).set(0, orderedo);
	            }
	            tm.put(k, v);
	        } else {// supposing v instance of Map
	            tm.put(k, orderEntries( (Map<String,Object>) v));
	        }
	    } else {
	        tm.put(k, v);
	    }
	}
	
	// reorder list
    protected static Map<String,Object> orderEntries(Map<String,Object> input) throws IllegalArgumentException
    {
        SortedMap<String,Object> res = new TreeMap<String,Object>(new MigrationComparator());
        input.forEach( (k,v) ->  insertRec(k, v, res) );
        return res;
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
    
    public static void couchUpdateOrCreate(Map<String,Object> jsonObject, String documentName, String type) {
        CouchDbConnector db = dbs.get(type);
        if (db == null) {
            System.err.println("cannot get couch connector for type "+type);
            return;
        }
        jsonObject.put("_id", documentName);
        try {
            final InputStream oldDocStream = db.getAsStream(documentName);
            final Map<String, Object> res = objectMapper.readValue(oldDocStream, hashMapTypeRef);
            String oldVersion = (String) res.get("_rev");
            jsonObject.put("_rev", oldVersion);
            String uri = "/bdrc_"+type+"/_design/jsonld/_show/revOnly/" + documentName;
            HttpResponse r = db.getConnection().get(uri);
            InputStream stuff = r.getContent();
            String result = inputStreamToString(stuff);
            if (result.charAt(0) == '{') {
                System.err.println("cannot transfer document, _design document not updated with latest revision");
                return;
            }
            result = result.substring(1, result.length()-1);
            jsonObject.put("_rev", result);
            db.update(jsonObject);
        } catch (DocumentNotFoundException e) {
            
        } catch (IOException e) {
            System.err.println(e);
        }
    }
    
    public static void jsonObjectToCouch(Map<String,Object> jsonObject, String mainId, String type) {
        String documentName = "bdr:"+mainId;
        couchUpdateOrCreate(jsonObject, documentName, type);
    }
    
    public static Map<String,Object> modelToJsonObject(Model m, String type, boolean isOutline) {
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        JSONLDVariant variant;
        if (!isOutline) {
            Object frameObj = getFrameObject(type);
            ctx.setFrame(frameObj);
            variant = (RDFFormat.JSONLDVariant) RDFFormat.JSONLD_FRAME_PRETTY.getVariant();
        } else {
            variant = (RDFFormat.JSONLDVariant) RDFFormat.JSONLD_COMPACT_PRETTY.getVariant();
        }
        // https://issues.apache.org/jira/browse/JENA-1292
        ctx.setJsonLDContext(jsonldcontext);
        JsonLdOptions opts = new JsonLdOptions();
        opts.setUseNativeTypes(true);
        opts.setPruneBlankNodeIdentifiers(true);
        //opts.setProcessingMode("json-ld-1.1");
        ctx.setOptions(opts);
        DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        PrefixMap pm = RiotLib.prefixMap(g);
        String base = null;
        Map<String,Object> tm;
        try {
            tm = (Map<String,Object>) JsonLDWriter.toJsonLDJavaAPI(variant, g, pm, base, ctx);
            tm.replace("@context", "http://purl.bdrc.io/context.jsonld");
            tm = orderEntries(tm);
        } catch (JsonLdError | IOException e) {
            e.printStackTrace();
            return null;
        }
        return tm;
    }
    
    public static void jsonObjectToOutputStream(Object jsonObject, OutputStream out) {
        Writer wr = new OutputStreamWriter(out, Chars.charsetUTF8) ;
        try {
            JsonUtils.writePrettyPrint(wr, jsonObject) ;
            wr.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        IO.flush(wr) ;
    }
    
	// these annotations don't work, for some reason
    public static void modelToOutputStream (Model m, OutputStream out, String type, int outputType) throws IllegalArgumentException {
	    if (m==null) 
	        throw new IllegalArgumentException("null model returned");
	    if (out == null && !usecouchdb) return;
	    if (outputType == OUTPUT_STTL) {
	        RDFWriter.create().source(m.getGraph()).context(ctx).lang(sttl).build().output(out);
	        return;
	    }
	    Map<String,Object> jsonObject = modelToJsonObject(m, type, type.equals("outline"));
	    jsonObjectToOutputStream(jsonObject, out);
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
		             .lang(RDFLanguages.JSONLD)
		             .canonicalLiterals(true);
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
		    modelToOutputStream(m, s, type, outputType);
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
	    case WORK:
	        m = WorkMigration.MigrateWork(d);
	        break;
		default:
			// arg
			return m;
		}
		return m;
	}
	
	public static boolean mustBeMigrated(Element root, String type) {
	    boolean res = (!root.getAttribute("status").equals("withdrawn") && !root.getAttribute("status").equals("onHold"));
	    if (res == false) return false;
	    if (type.equals("outline")) {
	        res = res && !OutlineMigration.ridsToIgnore.containsKey(root.getAttribute("RID"));   
	    }
	    return res;
	}
	
	public static Model getModelFromFile(String src, String type, String fileName) {
	    Document d = documentFromFileName(src);
	    if (checkagainstXsd) {
	        Validator v = getValidatorFor(type);
	        CommonMigration.documentValidates(d, v, src);
	    }
        Element root = d.getDocumentElement();
        if (!mustBeMigrated(root, type)) return null;
        Model m = null;
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
	
	public static void convertOneFile(String src, String mainId, String dst, String type, int outputType, String fileName, boolean isOutline) {
        Model m = getModelFromFile(src, type, fileName);
        if (m == null) return;
        if (usecouchdb) {
            Map<String,Object> o = modelToJsonObject(m, type, isOutline);
            if (o != null) {
                jsonObjectToCouch(o, mainId, type);
            }
        }
        if (writefiles) {
            modelToFileName(m, dst, type, outputType);
        }
    }
	
	public static void outputOneModel(Model m, String mainId, String dst, String type, boolean isOutline) {
	    if (m == null) return;
        if (usecouchdb) {
            Map<String,Object> o = modelToJsonObject(m, type, isOutline);
            if (o != null) {
                jsonObjectToCouch(o, mainId, type);
            }
        }
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
	    return ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, m);
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
