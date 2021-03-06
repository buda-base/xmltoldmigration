package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.setPrefixes;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class ScanrequestMigration {

	public static final String SRXSDNS = "http://www.tbrc.org/models/scanrequest#";
	
	// used for testing only
	public static Model MigrateScanrequest(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
	    MigrationHelpers.setPrefixes(m, "item");
        Element root = xmlDocument.getDocumentElement();
        String value = root.getAttribute("work");
        String rid=root.getAttribute("RID");
        if (value.isEmpty()) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "work", "missing work id");
            return m;
        }

        Resource volumes = m.createResource(BDR+"TestVolumes");
        m.add(volumes, RDF.type, m.getResource(BDO+"Item"));
        MigrateScanrequest(xmlDocument, m, volumes);
        
        return m;
	}
	
	// use this giving an Item as main argument to fill the work data
	public static Model MigrateScanrequest(Document xmlDocument, Model m, Resource item) {
		
		Element root = xmlDocument.getDocumentElement();
		
		//String value = root.getAttribute("venue").trim();
        //if (!value.isEmpty()) {
        //    Resource admR = getAdminData(item);
        //    m.add(admR, m.getProperty(ADM+"volumeScanVenue"), m.createLiteral(value));
        //}
		
		return m;
	}
	
	   // use this giving a vol:Volumes as main argument to fill the work data
    public static String getWork(Document xmlDocument) {
        
        Element root = xmlDocument.getDocumentElement();
        
        String value = root.getAttribute("work");
        if (value.isEmpty()) {
            System.err.println("No work ID for scanrequest "+root.getAttribute("RID")+"!");
            return "";
        }
        return value;
    }
	
}
