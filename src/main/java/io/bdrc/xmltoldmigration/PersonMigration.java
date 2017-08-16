package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PersonMigration {

    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    private static final String BDR = CommonMigration.RESOURCE_PREFIX;
	private static final String PXSDNS = "http://www.tbrc.org/models/person#";
	
	private static String getUriFromTypeSubtype(String type, String subtype) {
	    switch (type) {
	    case "name":
	        return BDO+"Person"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "gender":
            return BDR+"Gender"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "event":
            return BDO+"PersonEvent"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "incarnationOf":
            return BDO+"incarnation"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
	    default:
	           return "";
	    }
	}
	
	private static Resource createFromType(Map<String,Resource> typeNodes, Model m, Resource main, Property p, String type, String subtype) {
	    Resource typeIndividual = m.getResource(getUriFromTypeSubtype(type, subtype));
	    Resource r = m.createResource();
	    r.addProperty(RDF.type, typeIndividual);
	    main.addProperty(p, r);
	    return r;
	}
	
	private static Resource getResourceForType(Map<String,Resource> typeNodes, Model m, Resource main, Property p, String type, String subtype) {
	    return typeNodes.computeIfAbsent(subtype, (t) -> createFromType(typeNodes, m, main, p, type, t));
	}
	
	public static Model MigratePerson(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		CommonMigration.setPrefixes(m);
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String RID = root.getAttribute("RID");
		Resource main = m.createResource(BDR + RID);
		m.add(main, RDF.type, m.createResource(BDO + "Person"));
		CommonMigration.addStatus(m, main, root.getAttribute("status"));
		
		Map<String,Resource> typeNodes = new HashMap<>();
		
		// names
		
		NodeList nodeList = root.getElementsByTagNameNS(PXSDNS, "name");
		Property nameProp = m.getProperty(CommonMigration.GENLABEL_URI);
		Property prop = m.getProperty(BDO+"personName");
		Map<String,Boolean> labelDoneForLang = new HashMap<>();
		String typeUsedForLabel = null;
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			if (current.getTextContent().trim().isEmpty()) continue;
			String type = current.getAttribute("type").trim();
			if (type.isEmpty())
			    type = "primaryName";
			Resource r = getResourceForType(typeNodes, m, main, prop, "name", type);
			Literal l = CommonMigration.getLiteral(current, CommonMigration.EWTS_TAG, m, type, RID, null);
			if (l == null) continue;
			r.addProperty(nameProp, l);
			String lang = l.getLanguage().substring(0, 2);
			if (!labelDoneForLang.containsKey(lang) && (typeUsedForLabel == null || typeUsedForLabel.equals(type))) {
			    main.addProperty(m.getProperty(CommonMigration.PREFLABEL_URI), l);
			    labelDoneForLang.put(lang, true);
			    typeUsedForLabel = type;
			}
		}
		
		// gender
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String gender = current.getAttribute("gender");
            if (!gender.isEmpty()) {
                prop = m.getProperty(BDO, "personGender");
                m.add(main, prop, m.getResource(getUriFromTypeSubtype("gender", gender)));
            }
        }
		
		// events
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "event");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addEvent(m, main, current, i);
		}
		
		// seat
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "seat");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addSeat(m, main, current);
		}
		
		// relations
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "teacherOf");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			prop = m.getProperty(BDO, "personTeacherOf");
			String val = current.getAttribute("pid");
			if (val.isEmpty()) continue;
			if (val.contains(" ")) {
			    String [] parts = val.split(" ");
			    for (String part: parts) {
			        if (part.startsWith("#")) {
			            ExceptionHelper.logException(ExceptionHelper.ET_GEN, RID, RID, "teacherOf", "value contains unparsed strings: `"+part+"`");
                        continue;
                    }
			        if (!part.startsWith("P")) {
			            ExceptionHelper.logException(ExceptionHelper.ET_GEN, RID, RID, "teacherOf", "cannot parse `"+val+"` correctly");
			            continue;
			        }
			        m.add(main, prop, m.createResource(BDR+part));
			    }
			} else {
			    m.add(main, prop, m.createResource(BDR+val)); 
			}
		}
        
		nodeList = root.getElementsByTagNameNS(PXSDNS, "studentOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            prop = m.getProperty(BDO, "studentOf");
            String val = current.getAttribute("pid");
            if (val.isEmpty()) continue;
            if (val.contains(" ")) {
                String [] parts = val.split(" ");
                for (String part: parts) {           
                    if (part.startsWith("#")) {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, RID, RID, "studentOf", "value contains unparsed strings: `"+part+"`");
                        continue;
                    }
                    if (!part.startsWith("P")) {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, RID, RID, "studentOf", "cannot parse `"+val+"` correctly");
                        continue;
                    }
                    m.add(main, prop, m.createResource(BDR+part));
                }
            } else {
                m.add(main, prop, m.createResource(BDR+val)); 
            }
        }
		
		// kinship
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "kinship");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addKinship(m, main, current);
		}
		
		// ofSect
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "ofSect");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			prop = m.getProperty(BDO, "personOfSect");
			m.add(main, prop, m.createResource(BDR+current.getAttribute("sect")));
		}
		
		// incarnationOf
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "incarnationOf");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addIncarnation(m, main, current, i);
		}
		
		CommonMigration.addNotes(m, root, main, PXSDNS);
		
		CommonMigration.addExternals(m, root, main, PXSDNS);
		
		CommonMigration.addLog(m, root, main, PXSDNS);
		
		return m;
	}
	
	public static void addIncarnation(Model m, Resource r, Element e, int i) {
		String value = e.getAttribute("being").trim();
		if (value.isEmpty()) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "incarnationOf", "no RID for incarnation, text reads: `"+e.getTextContent()+"`");
		    return;
		} else {
		    value = BDR+value;
		}
        Resource being = m.createResource(value);
        
		value = e.getAttribute("relation");
		if (value != null && !value.isEmpty()) {
		    if (value.equals("yangsi")) value = "yangtse";
			String uri = getUriFromTypeSubtype("incarnationOf", value);
			r.addProperty(m.getProperty(uri), being);
		} else {
		    String uri = getUriFromTypeSubtype("incarnationOf", "general");
		    r.addProperty(m.getProperty(uri), being);
		}
		value = e.getAttribute("secondary");
		if (value != null && !value.isEmpty()) {
		    if (value.equals("yangsi")) value = "yangtse";
            String uri = getUriFromTypeSubtype("incarnationOf", value);
            r.addProperty(m.getProperty(uri), being);
		}
	}
	
	public static void addEvent(Model m, Resource person, Element e, int i) {
		Resource subResource = m.createResource();
		String typeValue = e.getAttribute("type");
		if (typeValue.isEmpty()) {
		    typeValue = "NotSpecified";
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, person.getLocalName(), person.getLocalName(), "event", "missing type");
		}
		m.add(subResource, RDF.type, 
		        m.createProperty(getUriFromTypeSubtype("event", typeValue)));
		String circa = CommonMigration.normalizeString(e.getAttribute("circa"));
		if (!circa.isEmpty()) {
		    Literal value = m.createLiteral(circa);
	        Property prop = m.getProperty(BDO, "onOrAbout");
	        m.add(subResource, prop, value);		    
		}
        NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "personEventPlace");
            Resource r = m.createResource(BDR+current.getAttribute("pid").trim());
            m.add(subResource, prop, r);
        }
        nodeList = e.getElementsByTagNameNS(PXSDNS, "office");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "personEventRole");
            Resource r = m.createResource(BDR+current.getAttribute("pid").trim());
            m.add(subResource, prop, r);
        }
        nodeList = e.getElementsByTagNameNS(PXSDNS, "corporation");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "personEventCorporation");
            Resource r = m.createResource(BDR+current.getAttribute("pid").trim());
            m.add(subResource, prop, r);
        }
		m.add(person, m.getProperty(BDO+"personEvent"), subResource);
	}
	
	public static void addKinship(Model m, Resource person, Element e) {
		String relation = e.getAttribute("relation");
		if (relation.isEmpty()) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, person.getLocalName(), person.getLocalName(), "kinship", "missing kinship type");
		    relation = "hasUnknownKinship";
		}
		String with = e.getAttribute("person");
		if (!with.isEmpty()) {
		    Resource r = m.createResource(BDR+with);
		    person.addProperty(m.getProperty(BDO, relation), r);
		}
	}
	
	public static void addSeat(Model m, Resource person, Element e) {
        Resource subResource = m.createResource();
        m.add(person, m.getProperty(BDO+"personEvent"), subResource);
        m.add(subResource, RDF.type, 
                m.createProperty(getUriFromTypeSubtype("event", "occupiesSeat")));
        String circa = CommonMigration.normalizeString(e.getAttribute("circa"));
        if (circa != null && !circa.isEmpty()) {
            m.add(subResource, m.getProperty(BDO, "onOrAbout"), 
                    m.createLiteral(circa));
        }
		NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Property prop = m.getProperty(BDO, "personEventPlace");
			Resource r = m.createResource(BDR+current.getAttribute("pid"));
			m.add(subResource, prop, r);
		}
	}

}
