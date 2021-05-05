package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.AUT;
import static io.bdrc.libraries.Models.ADR;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.getFacetNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.MigrationApp;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;


public class ProductMigration {
    
	public static final String PRXSDNS = "http://www.tbrc.org/models/product#";
	
	public static List<String> replPR88CT000129 = new ArrayList<>();
	static {
	    replPR88CT000129.add("PRA99BRL01");
	    replPR88CT000129.add("PRA99BUL01");
	    replPR88CT000129.add("PRA99CUHK01");
	    replPR88CT000129.add("PRA99COL01");
	    replPR88CT000129.add("PRA99DDBC01");
	    replPR88CT000129.add("PRA99DRA01");
	    replPR88CT000129.add("PRA99EMR01");
	    replPR88CT000129.add("PRA99GTW01");
	    replPR88CT000129.add("PRA99GOODMAN01");
	    replPR88CT000129.add("PRA99HRVD01");
	    replPR88CT000129.add("PRA99IUL01");
	    replPR88CT000129.add("PRA99KOMU01");
	    replPR88CT000129.add("PRA99LOC01");
	    replPR88CT000129.add("PRA99LMU01");
	    replPR88CT000129.add("PRA99NWU01");
	    replPR88CT000129.add("PRA99OTANU01");
	    replPR88CT000129.add("PRA99OXB01");
	    replPR88CT000129.add("PRA99PRIU01");
	    replPR88CT000129.add("PRA99RICE01");
	    replPR88CT000129.add("PRA99RMA01");
	    replPR88CT000129.add("PRA99SBB01");
	    replPR88CT000129.add("PRA99STU01");
	    replPR88CT000129.add("PRA99TUFS01");
	    replPR88CT000129.add("PRA99TORU01");
	    replPR88CT000129.add("PRA99UCS01");
	    replPR88CT000129.add("PRA99UCOB01");
	    replPR88CT000129.add("PRA99UHAM01");
	    replPR88CT000129.add("PRA99ULAU01");
	    replPR88CT000129.add("PRA99UMI01");
	    replPR88CT000129.add("PRA99UVA01");
	    replPR88CT000129.add("PRA99VIU01");
	    replPR88CT000129.add("PRA99YALE01");
	}
	
	public static Map<String,List<String>> subscriptions = new HashMap<>();
	
	public static String getType(Document xmlDocument) {
	 // products are of three types:
        // - pure collections (no <product:access>), migrated into bdo:Collection
        // - pure IP addresses of institutions (<product:access><product:org>), migrated into aut:Subscriber
        // - collections + access control (<product:access><product:include>), migrated into bdo:Collection,
        //                   with some tweaks to the subscriber models
        Element root = xmlDocument.getDocumentElement();
        Element current;
        NodeList nodeList = root.getElementsByTagNameNS(PRXSDNS, "access");
        if (nodeList.getLength() == 0) {
            return "collection";
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            NodeList subNodeList = current.getElementsByTagNameNS(PRXSDNS, "include");
            if (subNodeList.getLength() > 0) {
                return "collection";
            }
            subNodeList = current.getElementsByTagNameNS(PRXSDNS, "org");
            if (subNodeList.getLength() > 0) {
                return "subscriber";
            }
        }
        System.err.println("can't determine type of "+root.getAttribute("RID"));
        return "subscriber";
	}
	
	public static void addSubscription(final String collectionRid, String subscriberRid) {
	    if (subscriberRid.equals("PR88CT000129")) {
	        for (String sub : replPR88CT000129) {
	            List<String> subsForSubscriber = subscriptions.get(sub);
	            if (subsForSubscriber == null) {
	                subsForSubscriber =  new ArrayList<String>();
	                subscriptions.put(sub, subsForSubscriber);
	            }
	            subsForSubscriber.add(collectionRid);
	        }
	    } else {
	        subscriberRid = "PRA"+subscriberRid.substring(2);
	        List<String> subsForSubscriber = subscriptions.get(subscriberRid);
            if (subsForSubscriber == null) {
                subsForSubscriber =  new ArrayList<String>();
                subscriptions.put(subscriberRid, subsForSubscriber);
            }
	        subsForSubscriber.add(collectionRid);
	    }
	}
	
	public static void finishProductMigration() {
	    for (Entry<String,List<String>> subs : subscriptions.entrySet()) {
	        final String subscriber = subs.getKey();
	        final String subscriberPath = MigrationApp.getDstFileName("subscriber", subscriber, ".trig");
	        final Model subscriberModel = MigrationHelpers.modelFromFileName(subscriberPath);
	        if (subscriberModel == null) {
	            ExceptionHelper.logException(ExceptionHelper.ET_GEN, "", "", "cannot read subscriber model for image name translation on "+subscriberPath);
	            return;
	        }
	        final Resource subscriberR = subscriberModel.getResource(ADR+subscriber);
	        Property p = subscriberModel.getProperty(AUT, "subscribedTo");
	        for (String collection : subs.getValue()) {
	            subscriberR.addProperty(p, subscriberModel.createResource(BDR+collection));	            
	        }
	        MigrationHelpers.outputOneModel(subscriberModel, subscriber, subscriberPath, "subscriber");
	    }
	}
	    
	public static Model MigrateCollection(Document xmlDocument) {
	    Model m = ModelFactory.createDefaultModel();
        MigrationHelpers.setPrefixes(m, "collection");
        Element root = xmlDocument.getDocumentElement();
        Resource main = m.createResource(BDR+root.getAttribute("RID"));
        main.addProperty(RDF.type, m.createResource(BDO + "Collection"));
        Resource admMain = createAdminRoot(main);
        if (MigrationHelpers.ricrid.containsKey(root.getAttribute("RID"))) {
            admMain.addLiteral(admMain.getModel().createProperty(ADM, "isRestrictedInChina"), true);
        }
        
        addStatus(m, admMain, root.getAttribute("status"));

        CommonMigration.addNotes(m, root, main, PRXSDNS);

        CommonMigration.addExternals(m, root, main, PRXSDNS);
        
        CommonMigration.addLog(m, root, admMain, PRXSDNS, false);
        
        CommonMigration.addDescriptions(m, root, main, PRXSDNS, true);
        
        // access (contains everything
        Element current;
        NodeList nodeList = root.getElementsByTagNameNS(PRXSDNS, "access");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            NodeList subNodeList = current.getElementsByTagNameNS(PRXSDNS, "include");
            for (int j = 0; j < subNodeList.getLength(); j++) {
                Element subCurrent = (Element) subNodeList.item(j);
                String value = subCurrent.getAttribute("RID");
                addSubscription(root.getAttribute("RID"), value);
            }
        }

        List<String> workRIDs = WorkMigration.productWorks.get(main.getLocalName());
        if (workRIDs != null) {
            for (final String workRID : workRIDs) {
                m.add(main, m.createProperty(BDO, "collectionMember"), m.createProperty(BDR+workRID));
            }
        }

        return m;
	}
	
	public static Model MigrateSubscriber(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		MigrationHelpers.setPrefixes(m, "subscriber");
		Element root = xmlDocument.getDocumentElement();
		Resource main = m.createResource(ADR+"PRA"+root.getAttribute("RID").substring(2));
		main.addProperty(RDF.type, m.createResource(AUT + "Subscriber"));
		Resource admMain = createAdminRoot(main);
		
		addStatus(m, admMain, root.getAttribute("status"));

		CommonMigration.addNotes(m, root, main, PRXSDNS);

		CommonMigration.addExternals(m, root, main, PRXSDNS);
		
		CommonMigration.addLog(m, root, admMain, PRXSDNS, false);
		
		CommonMigration.addDescriptions(m, root, main, PRXSDNS, true);
		
		// access (contains everything
        Element current;
		NodeList nodeList = root.getElementsByTagNameNS(PRXSDNS, "access");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addAllows(m, main, current);
			addOrgs(m, main, current);
		}

		return m;
	}
	
	public static void addOrgs(Model m, Resource r, Element e) {
		List<Element> nodeList = CommonMigration.getChildrenByTagName(e, PRXSDNS, "org");
		boolean hasMultipleOrgz = nodeList.size() > 1;
		for (int i = 0; i < nodeList.size(); i++) {
			Element current = (Element) nodeList.get(i);
		    addOrg(m, r, current, i, hasMultipleOrgz);
		}
	}
	
	public static void addOrg(Model m, Resource rez, Element orgElement, int i, boolean hasMultipleOrgz) {
	    Resource org = rez;
	    String nameValue = CommonMigration.normalizeString(orgElement.getAttribute("name"));
	    if (hasMultipleOrgz) {
	        org = getFacetNode(FacetType.SUBSCRIBER_ORG, ADR, rez);
	        if (!nameValue.isEmpty()) {
	            m.add(org, SKOS.prefLabel, m.createLiteral(nameValue, "en"));
	        }
	        m.add(rez, m.getProperty(AUT+"subscriberHasOrganization"), org);
	    } else {
	        if (!nameValue.isEmpty()) {
                m.add(org, SKOS.altLabel, m.createLiteral(nameValue, "en"));
            }
	    }
		addAllows(m, org, orgElement);
		// sub orgs
		addOrgs(m, org, orgElement);
	}
	
	public static void addAllows(Model m, Resource r, Element e) {
		List<Element> nodeList = CommonMigration.getChildrenByTagName(e, PRXSDNS, "allow");
		for (int j = 0; j < nodeList.size(); j++) {
			Element current = nodeList.get(j);
			String value = current.getTextContent().trim();
			m.add(r, m.getProperty(AUT+"hasIPAddress"), m.createLiteral(value));
		}
	}
}
