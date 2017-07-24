package io.bdrc.xmltoldmigration;

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
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.Chars;
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
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormat.JSONLDVariant;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.http.HttpClient;
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

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;

import org.apache.jena.vocabulary.OWL2;


public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	protected static Map<String,Object> typeToFrameObject = new HashMap<String,Object>();
	
	public static Writer logWriter;
	
	public static HttpClient httpClient;
	public static CouchDbInstance dbInstance;
//	public static CouchDbConnector db = null;
	
	public static boolean usecouchdb = false;
	public static boolean writefiles = false;
	public static boolean checkagainstOwl = false;
	public static boolean checkagainstXsd = true;
	
	public static OntModel ontologymodel = null;
	
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
    public static final String WORK = "work";
    // types in source DB and not in target DB
    public static final String IMAGEGROUP = "imagegroup";
    public static final String PRODUCT = "product";
    public static final String PUBINFO = "pubinfo";
    public static final String SCANREQUEST = "scanrequest";
    public static final String VOLUME = "volume";
	
    public static Hashtable<String, CouchDbConnector> dbs = new Hashtable<>();
    
    public static void putDB(String type) {
        dbs.put(type, new StdCouchDbConnector(DB_PREFIX + type, dbInstance));
    }

    static {
        if (checkagainstOwl) {
            ontologymodel = MigrationHelpers.getOntologyModel();
        }
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
            putDB(TOPIC);
            putDB(VOLUMES);
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
		typeToRootShortUri.put(PERSON, ":Person");
		typeToRootShortUri.put(WORK, "wor:Work");
		typeToRootShortUri.put(OUTLINE, "out:Outline");
		typeToRootShortUri.put(PLACE, ":Place");
		typeToRootShortUri.put(TOPIC, ":Topic");
		typeToRootShortUri.put(LINEAGE, "lin:Lineage");
		typeToRootShortUri.put(CORPORATION, ":Corporation");
		typeToRootShortUri.put(PRODUCT, "prd:Product");
		typeToRootShortUri.put(VOLUMES, "vol:Volumes");
		typeToRootShortUri.put(VOLUME, "vol:Volume");
		typeToRootShortUri.put(OFFICE, ":Role");
    }
	
	public static Object getFrameObject(String type) {
		Object jsonObject = typeToFrameObject.get(type);
		if (jsonObject != null) {
			return jsonObject;
		}
		String rootShortUri = typeToRootShortUri.get(type); 
		String jsonString = "{\"@type\": \""+rootShortUri+"\", \"@context\": "+CommonMigration.getJsonLDContext()+"}";
		try {
			jsonObject = JsonUtils.fromString(jsonString);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
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
	                throw new IllegalArgumentException("empty graph, shouldn't happen!");
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
    
    public static void jsonObjectToCouch(Object jsonObject, String type) {
        CouchDbConnector db = dbs.get(type);
        if (db == null) return;
        TreeMap<String,Object> obj = (TreeMap<String,Object>) jsonObject;
        Object graph = null; // = ((TreeMap<String,Object>) jsonObject).get("@graph"); this doesn't work for unknown reasons
        for(Map.Entry<String, Object> entry : obj.entrySet()) {
            if (entry.getKey().equals("@graph")) {
                graph = entry.getValue();
                break;
            }
        }
        if (graph == null || ((ArrayList<Map<String,Object>>) graph).size() == 0) {
            System.out.println("cannot extract graph");
            return;
        }
        Map<String,Object> resource = ((ArrayList<Map<String,Object>>) graph).get(0);
        String Id = null;//resource.get("@id").toString(); again, this doesn't work... programming in Java is so sad...
        for(Map.Entry<String, Object> entry : resource.entrySet()) {
            if (entry.getKey().equals("@id")) {
                Id = entry.getValue().toString();
                break;
            }
        }
        if (Id == null) {
            System.out.println("cannot extract @id");
            System.out.println(resource.toString());
            return;
        }
        HashMap<String,Object> finalObject = new HashMap<String,Object>();
        finalObject.put("@graph", graph);
        finalObject.put("_id", Id);
        if (!db.contains(Id)) {
            db.create(Id, finalObject);
        }
    }
	
	// these annotations don't work, for some reason
	@SuppressWarnings("unchecked")
    public static void modelToOutputStream (Model m, OutputStream out, String type, boolean frame) throws IllegalArgumentException {
	    if (m==null) 
	        throw new IllegalArgumentException("null model returned");
	    if (out == null && !usecouchdb) return;
		JsonLDWriteContext ctx = new JsonLDWriteContext();
		JSONLDVariant variant;
		if (frame) {
			Object frameObj = getFrameObject(type);
			ctx.setFrame(frameObj);
			variant = (RDFFormat.JSONLDVariant) RDFFormat.JSONLD_FRAME_PRETTY.getVariant();
		} else {
		    variant = (RDFFormat.JSONLDVariant) RDFFormat.JSONLD_COMPACT_PRETTY.getVariant();
		}
		// https://issues.apache.org/jira/browse/JENA-1292
		ctx.setJsonLDContext(CommonMigration.getJsonLDContext());
        JsonLdOptions opts = new JsonLdOptions();
        ctx.setOptions(opts);
        DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        PrefixMap pm = RiotLib.prefixMap(g);
        String base = null;
        Object jsonObject;
        Writer wr = null;
        if (out != null) wr = new OutputStreamWriter(out, Chars.charsetUTF8) ;
        try {
            jsonObject = JsonLDWriter.toJsonLDJavaAPI(variant, g, pm, base, ctx);
            jsonObject = orderEntries((Map<String,Object>) jsonObject);
            if (usecouchdb)
                jsonObjectToCouch(jsonObject, type);
            if (wr != null) {
                JsonUtils.writePrettyPrint(wr, jsonObject) ;
                wr.write("\n");
                IO.flush(wr) ;
            }
        } catch (JsonLdError | IOException e) {
            e.printStackTrace();
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
		catch (final ParserConfigurationException e) {
		    e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return document;
	}
	
	public static Model modelFromFileName(String fname) {
		Model model = ModelFactory.createDefaultModel();
		try {
		    model.read(fname, "JSON-LD") ;
		} catch (RiotException e) {
		    writeLog("error reading "+fname);
		    return null;
		}
		return model;
	}
	
	public static void modelToFileName(Model m, String fname, String type, boolean frame) {
	    FileOutputStream s = null;
	    try {
		    s = new FileOutputStream(fname);
		    modelToOutputStream(m, s, type, frame);
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
	
	public static void convertOneFile(String src, String dst, String type, boolean frame) {
		convertOneFile(src, dst, type, frame, "");
	}
	
	public static boolean mustBeMigrated(Element root) {
	    return (!root.getAttribute("status").equals("withdrawn") && !root.getAttribute("status").equals("onHold")); 
	}
	
	public static Model getModelFromFile(String src, String type, String fileName) {
	    Document d = documentFromFileName(src);
	    if (checkagainstXsd) {
	        Validator v = getValidatorFor(type);
	        CommonMigration.documentValidates(d, v, src);
	    }
        Element root = d.getDocumentElement();
        if (!mustBeMigrated(root)) return null;
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
	
	public static void convertOneFile(String src, String dst, String type, boolean frame, String fileName) {
        Model m = getModelFromFile(src, type, fileName);
        if (m == null) return;
        modelToFileName(m, dst, type, frame);
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
	    // then we fix it by removing the individuals and converting rdf10 to rdf11
	    //removeIndividuals(ontoModel);
	    rdf10tordf11(ontoModel);
	    // then we change the reasoner to Openllet:
	    OntModel ontoModelInferred = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, ontoModel);
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
