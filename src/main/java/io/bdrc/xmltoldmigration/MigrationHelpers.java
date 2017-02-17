package io.bdrc.xmltoldmigration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.WriterDatasetRIOT;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.RiotLib;
import org.apache.jena.sparql.core.DatasetGraph;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.github.jsonldjava.core.JsonLdOptions;

public class MigrationHelpers {

	protected static DocumentBuilderFactory documentFactory = null;
	
	public static void modelToOutputStream (Model m, OutputStream out) {
		//RDFDataMgr.write(System.out, m, RDFFormat.JSONLD_PRETTY);
		WriterDatasetRIOT w = RDFDataMgr.createDatasetWriter(RDFFormat.JSONLD_COMPACT_PRETTY);
		JsonLDWriteContext ctx = new JsonLDWriteContext();
        JsonLdOptions opts = new JsonLdOptions();
        ctx.setOptions(opts);
        DatasetGraph g = DatasetFactory.create(m).asDatasetGraph();
        PrefixMap pm = RiotLib.prefixMap(g);
        String base = null;
        w.write(System.out, g, pm, base, ctx) ;
	}
	
	public static boolean isSimilarTo(Model src, Model dst) {
		return src.isIsomorphicWith(dst);
	}
	
	public static Document documentFromFileName(String fname) {
		if (documentFactory == null) {
			documentFactory = DocumentBuilderFactory.newInstance();
		}
		documentFactory.setNamespaceAware(true);
		Document document = null;
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
		model.read(fname, "JSON-LD") ;
		return model;
	}
	
	public static void modelToFileName(Model m, String fname) {
		try {
			modelToOutputStream(m, new FileOutputStream(fname));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static Model xmlToRdf(Document d, String type) {
		Model m = null;
		switch (type) {
		case "person":
			m = PersonMigration.MigratePerson(d);
			break;
		default:
			// arg
			return m;
		}
		return m;
	}
	
	public static void convertOneFile(String src, String dst, String type) {
		Document d = documentFromFileName(src);
		Model m = xmlToRdf(d, type);
		modelToFileName(m, dst);
	}
	
}
