package io.bdrc.xmltoldmigration.xml2files;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class LineageMigration {

    private static final String BDA = CommonMigration.BDA;
    private static final String BDO = CommonMigration.BDO;
    private static final String BDR = CommonMigration.BDR;
    private static final String ADM = CommonMigration.ADM;
   
	public static final String LXSDNS = "http://www.tbrc.org/models/lineage#";
	
	public static Model MigrateLineage(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m, "lineage");
		Element root = xmlDocument.getDocumentElement();
		String value = getTypeStr(root);
		String rid = root.getAttribute("RID");
		if (value.equals("NotSpecified")) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, rid, rid, "event", "missing lineage type");
        }
		value = BDR+"Lineage"+value.substring(0, 1).toUpperCase() + value.substring(1);
        Resource main = m.createResource(BDR + rid);
        Resource admMain = CommonMigration.getAdmResource(main);
		m.add(main, RDF.type, m.createResource(BDO + "Lineage"));
		m.add(main, m.getProperty(BDO, "lineageType"), m.createResource(value));
		
		CommonMigration.addStatus(m, admMain, root.getAttribute("status"));
		admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_Open"));
		
    	CommonMigration.addNames(m, root, main, LXSDNS);
    	CommonMigration.addNotes(m, root, main, LXSDNS);
    	CommonMigration.addExternals(m, root, main, LXSDNS);
    	CommonMigration.addDescriptions(m, root, main, LXSDNS);
    	CommonMigration.addLog(m, root, admMain, LXSDNS);
    	CommonMigration.addLocations(m, main, root, LXSDNS, "", null, null, null);
		
        NodeList nodeList = root.getElementsByTagNameNS(LXSDNS, "object");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("RID").trim();
            if (!value.isEmpty()) {
                m.add(main, m.getProperty(BDO, "lineageObject"), m.getResource(BDR+value));
            }
        }
        
        nodeList = root.getElementsByTagNameNS(LXSDNS, "lineageRef");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            value = current.getAttribute("RID").trim();
            if (!value.isEmpty())
                m.add(main, m.getProperty(BDO, "lineageRef"), m.getResource(BDR+value));
        }
		
        List<Element> elList = CommonMigration.getChildrenByTagName(root, LXSDNS, "holder");
        for (int i = 0; i < elList.size(); i++) {
            Element current = (Element) elList.get(i);
            addHolder(m, main, current, i);
        }
        
        nodeList = root.getElementsByTagNameNS(LXSDNS, "alternative");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element current = (Element) nodeList.item(i);
            elList = CommonMigration.getChildrenByTagName(current, LXSDNS, "holder");
            if (elList.isEmpty()) continue;
//            Resource alternative = m.createResource();
//            //m.add(alternative, RDF.type, m.getResource(BDO+"LineageAlternative"));
//            m.add(main, m.getProperty(BDO, "lineageAlternative"), alternative);
            for (int j = 0; j < elList.size(); j++) {
                Element holderElement = (Element) elList.get(j);
                addHolder(m, main, holderElement, j);
            }
        }
		
		return m;
	}
	
	public static void addHolder(Model m, Resource r, Element e, int i) {
	    //String value = CommonMigration.getSubResourceName(r, LP, "Holder", i+1);
	    Resource holder = m.createResource();
	    m.add(holder, RDF.type, m.getResource(BDO+"LineageHolder"));
	    m.add(r, m.getProperty(BDO, "lineageHolder"), holder);
	    
       CommonMigration.addNotes(m, e, holder, LXSDNS);
       CommonMigration.addDescriptions(m, e, holder, LXSDNS);
       String value;
	    
	    NodeList nodeList = e.getElementsByTagNameNS(LXSDNS, "who");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty()) {
                m.add(holder, m.getProperty(BDO, "lineageWho"), m.getResource(BDR+value));
            }
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "downTo");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(BDO, "lineageDownTo"), m.getResource(BDR+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "downFrom");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(BDO, "lineageDownFrom"), m.getResource(BDR+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "work");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            value = current.getAttribute("RID");
            if (!value.isEmpty())
                m.add(holder, m.getProperty(BDO, "lineageWork"), m.getResource(BDR+value));
        }
        
        nodeList = e.getElementsByTagNameNS(LXSDNS, "received");
        for (int j = 0; j < nodeList.getLength(); j++) {
            Element current = (Element) nodeList.item(j);
            //value = CommonMigration.getSubResourceName(r, LP, "Received", j+1);
            Resource received = m.createResource();
            //m.add(received, RDF.type, m.getResource(BDO+"LineageEvent"));
            m.add(holder, m.getProperty(BDO, "lineageReceived"), received);
            value = current.getAttribute("RID");
            if (!value.isEmpty()) {
                if (value.contains(" ")) {
                    String [] parts = value.split(" ");
                    for (String part: parts) {
                        if (part.startsWith("#")) {
                            ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "received", "received value contains unparsed strings: `"+part+"`");
                            continue;
                        }
                        m.add(received, m.getProperty(BDO, "lineageFrom"), m.createResource(BDR+part));
                    }
                } else {
                    m.add(received, m.getProperty(BDO, "lineageFrom"), m.getResource(BDR+value));
                }
            }

            value = current.getAttribute("site");
            if (!value.isEmpty())
                m.add(received, m.getProperty(BDO, "eventWhere"), m.getResource(BDR+value));
            CommonMigration.addDates(current.getAttribute("circa"), received);
        }
	}

	public static String getTypeStr(Element root) {
		NodeList nodeList = root.getElementsByTagNameNS(LXSDNS, "info");
		String value = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			value = current.getAttribute("type");
			if (value.isEmpty()) {
			    value = "lineageTypes:NotSpecified";
			}
			if (value.equals("lineageTypes:rlung")) value = "lineageTypes:lung";
			value = value.substring(13);
			value = CommonMigration.normalizePropName(value, "Class");
		}
		if (value == null) {
		    value = "NotSpecified";
		}
		return value;
	}
	
}
