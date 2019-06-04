package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.ADM;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDA;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDO;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDR;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OfficeMigration {
	
	public static final String OXSDNS = "http://www.tbrc.org/models/office#";
	
	public static Model MigrateOffice(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "office");
		Element root = xmlDocument.getDocumentElement();
        Resource main = CommonMigration.createRoot(m, BDR+root.getAttribute("RID"), BDO+"Role");
        Resource admMain = CommonMigration.createAdminRoot(main);
		CommonMigration.addStatus(m, admMain, root.getAttribute("status"));
		admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
		
		CommonMigration.addNotes(m, root, main, OXSDNS);
		
		CommonMigration.addExternals(m, root, main, OXSDNS);
		
		CommonMigration.addLog(m, root, admMain, OXSDNS);
		
		CommonMigration.addDescriptions(m, root, main, OXSDNS, true);
		
		return m;
	}

}
