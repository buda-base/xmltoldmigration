package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.Chars;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RDFFormat.JSONLDVariant;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.util.iterator.ExtendedIterator;

import openllet.jena.PelletReasonerFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.utils.JsonUtils;



public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	protected static Map<String,Object> typeToFrameObject = new HashMap<String,Object>();
	
	public static Writer logWriter;
	
	static {
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
		typeToRootShortUri.put("person", "per:Person");
		typeToRootShortUri.put("work", "wor:Work");
		typeToRootShortUri.put("outline", "out:Outline");
		typeToRootShortUri.put("place", "plc:Place");
		typeToRootShortUri.put("topic", "top:Topic");
		typeToRootShortUri.put("lineage", "lin:Lineage");
		typeToRootShortUri.put("corporation", "crp:Corporation");
		typeToRootShortUri.put("product", "prd:Product");
		typeToRootShortUri.put("volumes", "vol:Volumes");
		typeToRootShortUri.put("volume", "vol:Volume");
		typeToRootShortUri.put("office", "ofc:Office");
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
	         if(s1.equals("@context")) return 1;
	         if(s1.equals("@graph")) return -1;
	         if(s1.equals("rdfs:label")) return -1;
	         if(s1.equals("status")) return -1;
	         return s1.compareTo(s2);
	     }
	 }
	
	@SuppressWarnings("unchecked")
    protected static void insertRec(String k, Object v, SortedMap<String,Object> tm) {
	    if (k.equals("@graph")) {
	        if (v instanceof ArrayList) {
	            if (((ArrayList<Object>) v).size() == 0) {
	                tm.put(k,v);
	                writeLog("empty graph, shouldn't happen!");
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
    protected static Map<String,Object> orderEntries(Map<String,Object> input)
    {
        SortedMap<String,Object> res = new TreeMap<String,Object>(new MigrationComparator());
        input.forEach( (k,v) -> insertRec(k, v, res) );
        return res;
    }
	
	// these annotations don't work, for some reason
	//@JsonPropertyOrder(alphabetic=true)
	//@JsonPropertyOrder({ "rdfs:label" })
	@SuppressWarnings("unchecked")
    public static void modelToOutputStream (Model m, OutputStream out, String type, boolean frame) {
	    if (m==null) return; // not sure why but seems needed
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
        Writer wr = new OutputStreamWriter(out, Chars.charsetUTF8) ;
        try {
            jsonObject = JsonLDWriter.toJsonLDJavaAPI(variant, g, pm, base, ctx);
            jsonObject = orderEntries((Map<String,Object>) jsonObject);
            JsonUtils.writePrettyPrint(wr, jsonObject) ;
            wr.write("\n");
        } catch (JsonLdError | IOException e) {
            e.printStackTrace();
            return;
        }
        IO.flush(wr) ;
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
		    System.err.println("error reading "+fname);
		    return null;
		}
		return model;
	}
	
	public static void modelToFileName(Model m, String fname, String type, boolean frame) {
	    FileOutputStream s;
	    try {
		    s = new FileOutputStream(fname);
			modelToOutputStream(m, s, type, frame);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
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
		case "corporation":
			m = CorporationMigration.MigrateCorporation(d);
			break;
		case "person":
			m = PersonMigration.MigratePerson(d);
			break;
		case "place":
			m = PlaceMigration.MigratePlace(d);
			break;
		case "product":
			m = ProductMigration.MigrateProduct(d);
			break;
		case "pubinfo":
            m = PubinfoMigration.MigratePubinfo(d);
            break;
		case "imagegroup":
            m = ImagegroupMigration.MigrateImagegroup(d);
            break;
		case "lineage":
			m = LineageMigration.MigrateLineage(d);
			break;
        case "office":
            m = OfficeMigration.MigrateOffice(d);
            break;
        case "outline":
            m = OutlineMigration.MigrateOutline(d);
            break;
        case "scanrequest":
            m = ScanrequestMigration.MigrateScanrequest(d);
            break;
        case "topic":
            m = TopicMigration.MigrateTopic(d);
            break;
	    case "work":
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
        Element root = d.getDocumentElement();
        if (!mustBeMigrated(root)) return null;
        Model m = null;
        try {
            m = xmlToRdf(d, type);
        } catch (IllegalArgumentException e) {
            System.err.println("error in "+fileName+" "+e.getMessage());
        }
        return m;
	}
	
	public static void convertOneFile(String src, String dst, String type, boolean frame, String fileName) {
        Model m = getModelFromFile(src, type, fileName);
        if (m == null) return;
        modelToFileName(m, dst, type, frame);
    }
	
	// change Range Datatypes from rdf:PlainLitteral to rdf:langString
	// Warning: only works for 
	public static void rdf10tordf11(OntModel o) {
		Resource RDFPL = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral");
		Resource RDFLS = o.getResource("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString");
		ExtendedIterator<DatatypeProperty> it = o.listDatatypeProperties();
	    while(it.hasNext()) {
			DatatypeProperty p = it.next();
			Resource r = p.getRange();
			if (r != null && r.equals(RDFPL)) {
				p.setRange(RDFLS);
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
	        InputStream inputStream = new FileInputStream("src/main/resources/owl/bdrc.owl");
	        ontoModel.read(inputStream, "", "RDF/XML");
	        inputStream.close();
	    } catch (Exception e) {
	        System.err.println(e.getMessage());
	    }
	    // then we fix it by removing the individuals and converting rdf10 to rdf11
	    removeIndividuals(ontoModel);
	    rdf10tordf11(ontoModel);
	    // then we change the reasoner to Openllet:
	    OntModel ontoModelInferred = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC, ontoModel);
	    return ontoModelInferred;
	}
	
	public static Validator getValidatorFor(String type) {
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
		return schema.newValidator();
	}
	
}
