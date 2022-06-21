package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addStatus;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.FacetType.EVENT;
import static io.bdrc.libraries.Models.FacetType.NAME;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import io.bdrc.xmltoldmigration.helpers.SymetricNormalization;

public class PersonMigration {

	public static final String PXSDNS = "http://www.tbrc.org/models/person#";
	
	public static final class FoundingEvent {
	    public String person;
	    public String circa;
	    
	    public FoundingEvent(String person, String circa) {
	        this.person = person;
	        this.circa = circa;
	    }
	    
	    public String toString() {
	        return "person: "+this.person+", circa: "+this.circa;
	    }
	}
	
	public static Map<String,FoundingEvent> placeEvents = new HashMap<>();
	
	private static String getUriFromTypeSubtype(String type, String subtype) {
	    switch (type) {
	    case "name":
	        switch(subtype) {
	        case "chineseName":
	        case "sanskrit":
	            return BDO+"PersonOtherName";
	        default:
	            return BDO+"Person"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
	        }
        case "gender":
            return BDR+"Gender"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "event":
            if (subtype.equals("NotSpecified"))
                return BDO+"PersonEventNotSpecified"; 
            return BDO+"Person"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
        case "incarnationOf":
            return BDO+"incarnation"+subtype.substring(0, 1).toUpperCase() + subtype.substring(1);
	    default:
	           return "";
	    }
	}
    
    private static Resource getNameForType(Resource personR, String subtype) {
        Model m = personR.getModel();
        Resource typeIndividual = m.getResource(getUriFromTypeSubtype("name", subtype));
        Resource nameR = getFacetNode(NAME, personR, typeIndividual);
        personR.addProperty(m.getProperty(BDO+"personName"), nameR);
        return nameR;
    }
	
	public static Model MigratePerson(Document xmlDocument) {
		Model m = ModelFactory.createDefaultModel();
		MigrationHelpers.setPrefixes(m, "person");
		Element root = xmlDocument.getDocumentElement();
		Element current;
		String RID = root.getAttribute("RID");
        Resource main = createRoot(m, BDR+RID, BDO+"Person");
        Resource admMain = createAdminRoot(main);
        if (MigrationHelpers.ricrid.containsKey(RID)) {
            admMain.addLiteral(admMain.getModel().createProperty(ADM, "restrictedInChina"), true);
        }
        if (MigrationHelpers.tol.containsKey(RID)) {
            admMain.addProperty(m.createProperty(ADM, "seeOtherToL"), m.createTypedLiteral(MigrationHelpers.tol.get(RID), XSDDatatype.XSDanyURI));
        }
        
        final List<String> traditions = MigrationHelpers.personTraditions.get(RID);
        if (traditions != null) {
            for (final String tlname : traditions) {
                main.addProperty(m.createProperty(BDO+"associatedTradition"), m.createResource(BDR+"Tradition"+tlname));
            }
        }
        
		addStatus(m, admMain, root.getAttribute("status"));
		admMain.addProperty(m.getProperty(ADM, "metadataLegal"), m.createResource(BDA+"LD_BDRC_CC0"));
		int gender = SymetricNormalization.GENDER_U;

		// names
		
		NodeList nodeList = root.getElementsByTagNameNS(PXSDNS, "name");
		Map<String,Literal> labelForLang = new HashMap<>();
		Map<String,String> labelTypeForLang = new HashMap<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			if (current.getTextContent().trim().isEmpty()) continue;
			String subtype = current.getAttribute("type").trim();
			if (subtype.isEmpty())
			    subtype = "primaryName";
            Resource nameR = getNameForType(main, subtype);
			Literal lit = CommonMigration.getLiteral(current, EWTS_TAG, m, subtype, RID, null);
			if (lit == null) continue;
			nameR.addProperty(RDFS.label, lit);
			String lang = lit.getLanguage().substring(0, 2);
			if (!labelForLang.containsKey(lang) || (subtype.equals("primaryTitle") && !labelTypeForLang.get(lang).equals("primaryTitle"))) {
			    labelForLang.put(lang, lit);
			    labelTypeForLang.put(lang, subtype);
			}
		}
		
		for (Literal lit : labelForLang.values()) {
		    main.addProperty(SKOS.prefLabel, lit);
		}
		
		// gender
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "info");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
            String genderval = current.getAttribute("gender");
            if (!genderval.isEmpty()) {
                Property prop = m.getProperty(BDO, "personGender");
                m.add(main, prop, m.getResource(getUriFromTypeSubtype("gender", genderval)));
                if (genderval.equals("male"))
                    gender = SymetricNormalization.GENDER_M;
                else if (genderval.equals("female"))
                    gender = SymetricNormalization.GENDER_F;
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
			        part = MigrationHelpers.sanitizeRID(main.getLocalName(), "teacherOf", part);
			        if (!MigrationHelpers.isDisconnected(part))
			            SymetricNormalization.addSymetricProperty(m, "personTeacherOf", main.getLocalName(), part, null); 
			    }
			} else {
			    val = MigrationHelpers.sanitizeRID(main.getLocalName(), "teacherOf", val);
			    if (!MigrationHelpers.isDisconnected(val)) {
			        SymetricNormalization.addSymetricProperty(m, "personTeacherOf", main.getLocalName(), val, null);
			    } else {
			        System.out.println("ignoring seemingly disconnected "+val);
			    }
			}
		}
        
		nodeList = root.getElementsByTagNameNS(PXSDNS, "studentOf");
        for (int i = 0; i < nodeList.getLength(); i++) {
            current = (Element) nodeList.item(i);
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
                    part = MigrationHelpers.sanitizeRID(main.getLocalName(), "studentOf", part);
                    if (!MigrationHelpers.isDisconnected(part))
                        SymetricNormalization.addSymetricProperty(m, "personStudentOf", main.getLocalName(), part, null);
                }
            } else {
                val = MigrationHelpers.sanitizeRID(main.getLocalName(), "studentOf", val);
                if (!MigrationHelpers.isDisconnected(val))
                    SymetricNormalization.addSymetricProperty(m, "personStudentOf", main.getLocalName(), val, null);
            }
        }
		
		// kinship
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "kinship");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			addKinship(m, main, current, gender);
		}
		
		// ofSect
		
		nodeList = root.getElementsByTagNameNS(PXSDNS, "ofSect");
		for (int i = 0; i < nodeList.getLength(); i++) {
			current = (Element) nodeList.item(i);
			Property prop = m.getProperty(BDO, "personOfSect");
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
		
		CommonMigration.addLog(m, root, admMain, PXSDNS, false);
		
		SymetricNormalization.insertMissingTriplesInModel(m, root.getAttribute("RID"));
		
		return m;
	}
	
	public static void addIncarnation(Model m, Resource r, Element e, int i) {
		String value = e.getAttribute("being").trim();
		if (value.isEmpty()) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, r.getLocalName(), r.getLocalName(), "incarnationOf", "no RID for incarnation, text reads: `"+e.getTextContent()+"`");
		    return;
		}
		value = MigrationHelpers.sanitizeRID(r.getLocalName(), "incarnationOf", value);
		value = BDR+value;
        Resource being = m.createResource(value);
        
		value = e.getAttribute("relation");
		// Not handling symetry in incarnationOf as I'm not sure how it would work
		if (value != null && !value.isEmpty()) {
		    if (value.equals("yangsi") || value.equals("yangtse")) value = "general";
			String uri = getUriFromTypeSubtype("incarnationOf", value);
			r.addProperty(m.getProperty(uri), being);
		} else {
		    String uri = getUriFromTypeSubtype("incarnationOf", "general");
		    r.addProperty(m.getProperty(uri), being);
		}
		// TODO: inspect: in the case of a secondary incarnation, wouldn't we have two triples instead of one?
		String beingAttr = e.getAttribute("being").trim();
		beingAttr = MigrationHelpers.sanitizeRID(r.getLocalName(), "incarnationOf", beingAttr);
		value = e.getAttribute("secondary");
		if (value != null && !value.isEmpty()) {
		    if (value.equals("yangsi") || value.equals("yangtse")) value = "general";
            String uri = getUriFromTypeSubtype("incarnationOf", value);
            if (!MigrationHelpers.isDisconnected(beingAttr))
                r.addProperty(m.getProperty(uri), being);
		}
	}
    
    public static Resource getEventForType(Resource rez, Property prop, String subtype) {
        Model m = rez.getModel();
        Resource typeIndividual = m.getResource(getUriFromTypeSubtype("event", subtype));
        Resource eventR = getFacetNode(EVENT, rez, typeIndividual);
        rez.addProperty(prop, eventR);
        return eventR;
    }
	
	private static void addEvent(Model m, Resource person, Element e, int i) {
		String typeValue = e.getAttribute("type");
		if (typeValue.isEmpty()) {
		    typeValue = "NotSpecified";
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, person.getLocalName(), person.getLocalName(), "event", "missing type");
		}
		if (typeValue.equals("assumeOffice")) // an interesting typo
		    typeValue = "assumesOffice";
		if (typeValue.equals("residence")) // to clarify a bit
            typeValue = "inResidence";
		if (typeValue.equals("foundsMonastery")) {
		    // for foundsMonastery, if the event has an office, we transform it into
		    // assumesOffice, otherwise we just record it into placeEvents
	        String circa = CommonMigration.normalizeString(e.getAttribute("circa"));
	        String place = null;
	        NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
	        for (int i1 = 0; i1 < nodeList.getLength(); ) {
	            Element current = (Element) nodeList.item(i1);
	            place = current.getAttribute("pid").trim();
	            place = MigrationHelpers.sanitizeRID(person.getLocalName(), "event/place", place);
	            break;
	        }
	        if (place != null && !circa.isEmpty()) {
	            placeEvents.put(place, new FoundingEvent(person.getLocalName(), circa));
	        }
	        // if no office, just return
            typeValue = "assumesOffice";
            if (e.getElementsByTagNameNS(PXSDNS, "office").getLength() == 0)
                return;
		}
		Resource subResource = getEventForType(person, m.getProperty(BDO+"personEvent"), typeValue);
	    CommonMigration.addDates(e.getAttribute("circa"), subResource, person);
        NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "eventWhere");
            String pid = current.getAttribute("pid").trim();
            pid = MigrationHelpers.sanitizeRID(person.getLocalName(), "event/place", pid);
            Resource r = m.createResource(BDR+pid);
            if (!MigrationHelpers.isDisconnected(pid))
                m.add(subResource, prop, r);
        }
        nodeList = e.getElementsByTagNameNS(PXSDNS, "office");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "role");
            String pid = current.getAttribute("pid").trim();
            pid = MigrationHelpers.sanitizeRID(person.getLocalName(), "event/office", pid);
            Resource r = m.createResource(BDR+pid);
            if (!MigrationHelpers.isDisconnected(pid))
                m.add(subResource, prop, r);
        }
        nodeList = e.getElementsByTagNameNS(PXSDNS, "corp");
        for (int i1 = 0; i1 < nodeList.getLength(); i1++) {
            Element current = (Element) nodeList.item(i1);
            Property prop = m.getProperty(BDO, "corporation");
            String pid = current.getAttribute("pid").trim();
            pid = MigrationHelpers.sanitizeRID(person.getLocalName(), "event/corporation", pid);
            Resource r = m.createResource(BDR+pid);
            if (!MigrationHelpers.isDisconnected(pid))
                m.add(subResource, prop, r);
        }
	}
	
	public static void addKinship(Model m, Resource person, Element e, int gender) {
		String relation = e.getAttribute("relation");
		if (relation.isEmpty()) {
		    ExceptionHelper.logException(ExceptionHelper.ET_GEN, person.getLocalName(), person.getLocalName(), "kinship", "missing kinship type");
		    relation = "hasUnknownKinship";
		}
		if (relation.equals("hasConsort"))
		    relation = "personHasConsort";
		String with = e.getAttribute("person");
		if (!with.isEmpty()) {
		    with = MigrationHelpers.sanitizeRID(person.getLocalName(), relation, with);
		    if (!MigrationHelpers.isDisconnected(with))
		        SymetricNormalization.addSymetricProperty(m, relation, person.getLocalName(), with, gender); 
		}
	}
	
	public static void addSeat(Model m, Resource person, Element e) {
	    Resource subResource = getEventForType(person, m.getProperty(BDO+"personEvent"), "occupiesSeat");
        CommonMigration.addDates(e.getAttribute("circa"), subResource, person);
		NodeList nodeList = e.getElementsByTagNameNS(PXSDNS, "place");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element current = (Element) nodeList.item(i);
			Property prop = m.getProperty(BDO, "eventWhere");
			String pid = current.getAttribute("pid").trim();
			pid = MigrationHelpers.sanitizeRID(person.getLocalName(), "seat", pid);
			Resource r = m.createResource(BDR+pid);
			if (!MigrationHelpers.isDisconnected(pid))
			    m.add(subResource, prop, r);
		}
	}

}
