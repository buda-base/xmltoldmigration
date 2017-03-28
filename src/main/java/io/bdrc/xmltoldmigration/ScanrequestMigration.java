package io.bdrc.xmltoldmigration;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class ScanrequestMigration {

	private static final String WP = CommonMigration.WORK_PREFIX;
	private static final String VP = CommonMigration.VOLUMES_PREFIX;
	private static final String SRXSDNS = "http://www.tbrc.org/models/scanrequest#";

	
	// used for testing only
	public static Model MigrateScanrequest(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        CommonMigration.setPrefixes(m);
        Element root = xmlDocument.getDocumentElement();
        String value = root.getAttribute("work");
        if (value.isEmpty()) {
            System.err.println("No work ID for scanrequest "+root.getAttribute("RID")+"!");
            return m;
        }
        Resource main = m.createResource(WP+value);

        Resource volumes = m.createResource(VP+"TestVolumes");
        m.add(volumes, RDF.type, m.getResource(VP+"Volumes"));
        MigrateScanrequest(xmlDocument, m, volumes);
        
        return m;
	}
	
	// use this giving a vol:Volumes as main argument to fill the work data
	public static Model MigrateScanrequest(Document xmlDocument, Model m, Resource volumes) {
		
		Element root = xmlDocument.getDocumentElement();
		
		String value = root.getAttribute("venue");
        if (!value.isEmpty())
            m.add(volumes, m.getProperty(VP+"scan_venue"), m.createLiteral(value));
		
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
