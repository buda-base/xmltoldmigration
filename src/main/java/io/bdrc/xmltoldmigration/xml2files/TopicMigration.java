package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.ADM;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDA;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDO;
import static io.bdrc.xmltoldmigration.xml2files.CommonMigration.BDR;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class TopicMigration {

	public static final String TXSDNS = "http://www.tbrc.org/models/topic#";

	public static Model MigrateTopic(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "topic");
		Element root = xmlDocument.getDocumentElement();
		Element current;
        Resource main = CommonMigration.createRoot(m, BDR+root.getAttribute("RID"), BDO+"Topic");
        Resource admMain = CommonMigration.createAdminRoot(main);
		CommonMigration.addStatus(m, admMain, root.getAttribute("status"));
		admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));

		CommonMigration.addNames(m, root, main, TXSDNS);

		CommonMigration.addNotes(m, root, main, TXSDNS);

		CommonMigration.addExternals(m, root, main, TXSDNS);

		CommonMigration.addLog(m, root, admMain, TXSDNS);

		CommonMigration.addDescriptions(m, root, main, TXSDNS);

        NodeList nodeList = root.getElementsByTagNameNS(TXSDNS, "seeAlso");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String value = current.getAttribute("rid").trim();
            m.add(main, m.getProperty(RDFS.getURI(), "seeAlso"), m.createProperty(BDR+value));
        }
		
		return m;
	}
	
	
}
