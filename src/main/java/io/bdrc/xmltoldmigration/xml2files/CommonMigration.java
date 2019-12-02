package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.LangStrings.getBCP47;
import static io.bdrc.libraries.LangStrings.isDeva;
import static io.bdrc.libraries.LangStrings.isLikelyEnglish;
import static io.bdrc.libraries.LangStrings.normalizeTibetan;
import static io.bdrc.libraries.Models.ADM;
import static io.bdrc.libraries.Models.BDA;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.addReleased;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.getAdminData;
import static io.bdrc.libraries.Models.getEvent;
import static io.bdrc.libraries.Models.getFacetNode;
import static io.bdrc.libraries.Models.setPrefixes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Validator;

import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.atlascopco.hunspell.Hunspell;

import io.bdrc.ewtsconverter.EwtsConverter;
import io.bdrc.libraries.Models.FacetType;
//import io.bdrc.libraries.BdrcDateType;
import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.helpers.EwtsFixer;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;
import openllet.core.exceptions.InternalReasonerException;

public class CommonMigration  {

    public static final String FPL_LIBRARY_ID = "G1TLMFPL000001";

    public static final String IMAGE_ITEM_SUFFIX = "";

    public static final int ET_LANG = ExceptionHelper.ET_LANG;

    public static final EwtsConverter converter = new EwtsConverter();
    public static final EwtsConverter converterAlalc = new EwtsConverter(true, true, false, false, EwtsConverter.Mode.ALALC);
    public static final String hunspellBoPath = "src/main/resources/hunspell-bo/";
    public static final Hunspell speller = new Hunspell(hunspellBoPath+"bo.dic", hunspellBoPath+"bo.aff");

    public static final Map<String, String> logWhoToUri = new HashMap<>();
    public static final Map<String, Boolean> genreTopics = new HashMap<>();
    public static final Map<Integer, Boolean> isTraditional = new HashMap<>();
    public static final Map<String, String> creatorMigrations = new HashMap<>();

    static {
        fillLogWhoToUri();
        fillGenreTopics();
        getTcList();
        initCreatorMigrations();
    }

    private static void initCreatorMigrations() {
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("creator-migrations.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                String[] key_val = line.split(",");
                creatorMigrations.put(key_val[0], key_val[1]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void getTcList() {
        final ClassLoader classLoader = MigrationHelpers.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("tclist.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                isTraditional.put(line.codePointAt(0), true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static final String userNumFormat = "%05d";

    private static void fillGenreTopics() {
        final ClassLoader classLoader = CommonMigration.class.getClassLoader();
        final InputStream inputStream = classLoader.getResourceAsStream("topics-genres.txt");
        final BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        try {
            while((line = in.readLine()) != null) {
                genreTopics.put(line, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getCreatorRoleUri(String type) {
        if (type.startsWith("has"))
            type = type.substring(3);
        return BDR+creatorMigrations.get(type);
    }

    
    public static final List<String> creatorForInstance = Arrays.asList(new String[]{"hasCalligrapher", "hasScribe", "hasEditor"});
    
    /**
     * Creates a new named AgentAsCreator node and adds bdo:creator node to the supplied work.
     * 
     * @param work that the AgentAsCreator is a creator for
     * @param person that is the creating agent
     * @param roleKey the name of the type pof role of the creator
     * @param rootAdmWork the root AdminData that contains the adm:facetIndex
     */
    public static void addAgentAsCreator(Resource work, Resource person, String roleKey, Resource workA) {
        if (person.getLocalName().equals("P7326")) {
            // this makes the data inconsistent
            return;
        }
        Model m = work.getModel();
        Resource agentAsCreator;
        if (workA != null && !creatorForInstance.contains(roleKey)) {
            agentAsCreator = getFacetNode(FacetType.CREATOR, workA);
            workA.addProperty(m.createProperty(BDO+"creator"), agentAsCreator);
        } else {
            agentAsCreator = getFacetNode(FacetType.CREATOR, work); 
            work.addProperty(m.createProperty(BDO+"creator"), agentAsCreator);
        }
        agentAsCreator.addProperty(m.createProperty(BDO+"agent"), person);
        Resource role = m.createResource(getCreatorRoleUri(roleKey));
        agentAsCreator.addProperty(m.createProperty(BDO+"role"), role);
    }

    public static void addDatesToEvent(String dateStr, Resource r, String eventProp, String eventType) {
        Resource event = getEvent(r, eventType, eventProp);
        addDates(dateStr, event, r);
    }

    public static void addDates(String dateStr, final Resource event) {
        addDates(dateStr, event, null);
    }

    public static Literal yearLit(Model m, String dateStr) throws NumberFormatException {
        int yr = Integer.parseInt(dateStr);
        return yearLit(m, yr);
    }

    public static Literal yearLit(Model m, int yr) {
        String padded = String.format("%04d" , yr);
        return m.createTypedLiteral(padded, XSDDatatype.XSDgYear);
    }
    
    public static void addDates(String dateStr, final Resource event, final Resource mainResource) {
        if (dateStr == null || dateStr.isEmpty())
            return;
        dateStr = normalizeString(dateStr);
        dateStr = dateStr.replaceAll(" ", "");
        dateStr = dateStr.replaceAll("\\[", "");
        dateStr = dateStr.replaceAll("\\]", "");
        if (dateStr.length() < 3)
            return;
        final Model m = event.getModel();
        if (dateStr.endsWith("?")) {
            dateStr = dateStr.substring(0, dateStr.length()-1);
        }
        if (dateStr.charAt(1) == '.') { // for b., d. and c. 
            dateStr = dateStr.substring(2);
        }
        if (dateStr.endsWith(".000000")) {
            dateStr = dateStr.substring(0, dateStr.length()-7);
        }
        try {
            m.add(event, m.getProperty(BDO, "onYear"), yearLit(m, dateStr));    
            return;
        } catch (NumberFormatException e) {}
        int slashidx = dateStr.indexOf('/');
        if (slashidx == -1) {
            slashidx = dateStr.indexOf('-');
            // if the date starts with -, it's not a separator
            if (slashidx == 0)
                slashidx = -1;
        }
        if (slashidx != -1) {
            String firstDate = dateStr.substring(0, slashidx);
            String secondDate = dateStr.substring(slashidx+1, dateStr.length());
            firstDate = firstDate.replace('u', '0');
            secondDate = secondDate.replace('u', '9');
            try {
                m.add(event, m.getProperty(BDO, "notBefore"), yearLit(m, firstDate));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            try {
                m.add(event, m.getProperty(BDO, "notAfter"), yearLit(m, secondDate));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            return;
        }
        if (dateStr.indexOf('u') != -1) {
            String firstDate = dateStr.replace('u', '0');
            String secondDate = dateStr.replace('u', '9');
            try {
                m.add(event, m.getProperty(BDO, "notBefore"), yearLit(m, firstDate));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            try {
                m.add(event, m.getProperty(BDO, "notAfter"), yearLit(m, secondDate));    
            } catch (NumberFormatException e) { 
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
            }
            return;
        }
        m.add(event, m.getProperty(BDO, "onOrAbout"), m.createLiteral(dateStr));
        if (mainResource != null) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainResource.getLocalName(), mainResource.getLocalName(), "couldn't parse date "+dateStr);
        }
    }

    public static void fillLogWhoToUri() {
        String prefix = BDR+"U"; // ?
        logWhoToUri.put("Gene Smith", prefix+String.format(userNumFormat, 1));
        logWhoToUri.put("agardner@sdrubin.org", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Alex Gardner", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Alexander Gardner", prefix+String.format(userNumFormat, 2));
        logWhoToUri.put("Bumu Dega", prefix+String.format(userNumFormat, 3));
        logWhoToUri.put("Dega Bumu", prefix+String.format(userNumFormat, 3));
        logWhoToUri.put("Catherine Tsuji", prefix+String.format(userNumFormat, 4));
        logWhoToUri.put("Chris Tomlinson", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("ct", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Code Ferret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Code Feret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("chris", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("CodeFerret", prefix+String.format(userNumFormat, 6));
        logWhoToUri.put("Chungdak Nangpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("Chungdak Ngagpa", prefix+String.format(userNumFormat, 7));// is it the same name?
        logWhoToUri.put("Chungdak Ngakpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("cnakpa", prefix+String.format(userNumFormat, 7));
        logWhoToUri.put("Cuilan Liu", prefix+String.format(userNumFormat, 8));
        logWhoToUri.put("Gabi Coura", prefix+String.format(userNumFormat, 9));
        logWhoToUri.put("Harry Einhorn", prefix+String.format(userNumFormat, 10));
        logWhoToUri.put("Jann Ronis", prefix+String.format(userNumFormat, 11));
        logWhoToUri.put("jann ronis", prefix+String.format(userNumFormat, 11));
        logWhoToUri.put("Jeff Wallman", prefix+String.format(userNumFormat, 12));
        logWhoToUri.put("Jigme Namgyal", prefix+String.format(userNumFormat, 13));
        logWhoToUri.put("jm", prefix+String.format(userNumFormat, 14)); // ?
        logWhoToUri.put("Joe McClellan", prefix+String.format(userNumFormat, 15));
        logWhoToUri.put("Joseph McClellan", prefix+String.format(userNumFormat, 15));
        logWhoToUri.put("Karma Gongde", prefix+String.format(userNumFormat, 16));
        logWhoToUri.put("kgongde", prefix+String.format(userNumFormat, 16));
        logWhoToUri.put("Chozin", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("chozin", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Kelsang Lhamo", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("kelsang", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Kelsang", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("klhamo", prefix+String.format(userNumFormat, 17));
        logWhoToUri.put("Konchok Tsering", prefix+String.format(userNumFormat, 18));
        logWhoToUri.put("Lobsang Shastri", prefix+String.format(userNumFormat, 19));
        logWhoToUri.put("lshastri", prefix+String.format(userNumFormat, 19));
        logWhoToUri.put("Michael R. Sheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("Michael Sheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("msheehy", prefix+String.format(userNumFormat, 20));
        logWhoToUri.put("paldor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("Paldor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("pal dor", prefix+String.format(userNumFormat, 21));
        logWhoToUri.put("Penghao Sun", prefix+String.format(userNumFormat, 22));
        logWhoToUri.put("Ralf Kramer", prefix+String.format(userNumFormat, 23));
        logWhoToUri.put("Ramon Prats", prefix+String.format(userNumFormat, 24));
        logWhoToUri.put("Rory Lindsay", prefix+String.format(userNumFormat, 25));
        logWhoToUri.put("Tendzin Parsons", prefix+String.format(userNumFormat, 26));
        logWhoToUri.put("Tenzin Dickyi", prefix+String.format(userNumFormat, 27));
        logWhoToUri.put("Arya Moallem", prefix+String.format(userNumFormat, 28));
        logWhoToUri.put("Awang Ciren", prefix+String.format(userNumFormat, 29));
        logWhoToUri.put("Chen Lai", prefix+String.format(userNumFormat, 30));
        logWhoToUri.put("Dennis Johnson", prefix+String.format(userNumFormat, 31));
        logWhoToUri.put("Dorjee Choden", prefix+String.format(userNumFormat, 32));
        logWhoToUri.put("dorjee choden", prefix+String.format(userNumFormat, 32));
        logWhoToUri.put("dzongsarlibrary", prefix+String.format(userNumFormat, 33));
        logWhoToUri.put("Erdene Baatar", prefix+String.format(userNumFormat, 34));
        logWhoToUri.put("Gyurmed Chodrak", prefix+String.format(userNumFormat, 35));
        logWhoToUri.put("Gyurme Chograg", prefix+String.format(userNumFormat, 35));
        logWhoToUri.put("Hachuluu", prefix+String.format(userNumFormat, 36));
        logWhoToUri.put("Haschuluu", prefix+String.format(userNumFormat, 36));
        logWhoToUri.put("Jamyang Lodoe", prefix+String.format(userNumFormat, 37));
        logWhoToUri.put("Jamyang.Lodoe", prefix+String.format(userNumFormat, 37));
        logWhoToUri.put("Jigme Tashi", prefix+String.format(userNumFormat, 38));
        logWhoToUri.put("John Canti", prefix+String.format(userNumFormat, 39));
        logWhoToUri.put("Khedup Gyatso", prefix+String.format(userNumFormat, 40));
        logWhoToUri.put("Legacy Converter", prefix+String.format(userNumFormat, 41));
        logWhoToUri.put("mangaram", prefix+String.format(userNumFormat, 42));
        logWhoToUri.put("Mara Canizzaro", prefix+String.format(userNumFormat, 43));
        logWhoToUri.put("mara canizzaro", prefix+String.format(userNumFormat, 43));
        logWhoToUri.put("Morris Hopkins", prefix+String.format(userNumFormat, 44));
        logWhoToUri.put("Ngawang Trinley", prefix+String.format(userNumFormat, 45));
        logWhoToUri.put("tenzang", prefix+String.format(userNumFormat, 45));// to be checked
        logWhoToUri.put("pbaduo", prefix+String.format(userNumFormat, 46));
        logWhoToUri.put("topic reclassify", prefix+String.format(userNumFormat, 47));
        logWhoToUri.put("zhangning", prefix+String.format(userNumFormat, 48));
        logWhoToUri.put("Arthur McKeown", prefix+String.format(userNumFormat, 49));
        logWhoToUri.put("Bruno Laine", prefix+String.format(userNumFormat, 50));
        logWhoToUri.put("chengdu", prefix+String.format(userNumFormat, 51));
        logWhoToUri.put("Chengdu", prefix+String.format(userNumFormat, 51));
        logWhoToUri.put("Chojor Radha", prefix+String.format(userNumFormat, 52));
        logWhoToUri.put("Elie Roux", prefix+String.format(userNumFormat, 53));
        logWhoToUri.put("Gelek Gyatso", prefix+String.format(userNumFormat, 54));
        logWhoToUri.put("Gelek.Gyatso", prefix+String.format(userNumFormat, 54));
        logWhoToUri.put("Georgia Kashnig", prefix+String.format(userNumFormat, 55));
        logWhoToUri.put("jw", prefix+String.format(userNumFormat, 56));
        logWhoToUri.put("monastery import", prefix+String.format(userNumFormat, 57));
        logWhoToUri.put("mongol import", prefix+String.format(userNumFormat, 58));
        logWhoToUri.put("Palri", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Nepal", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Parkhang", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palri Parkhang Nepal", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("Palris", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("palris", prefix+String.format(userNumFormat, 59));
        logWhoToUri.put("places-ensure-contains-has-name", prefix+String.format(userNumFormat, 60));
        logWhoToUri.put("Shallima Dellefant", prefix+String.format(userNumFormat, 61));
        logWhoToUri.put("sherabling", prefix+String.format(userNumFormat, 62)); // maybe NT?
        logWhoToUri.put("Shoko Mekata", prefix+String.format(userNumFormat, 63));
        logWhoToUri.put("Stacey Van Vleet", prefix+String.format(userNumFormat, 64));
        logWhoToUri.put("Tsering Dhondup", prefix+String.format(userNumFormat, 65));
        logWhoToUri.put("Tsering Dondrup", prefix+String.format(userNumFormat, 65));
        logWhoToUri.put("Tserings Wangdag and Dhondup", prefix+String.format(userNumFormat, 65)); // same ?
        logWhoToUri.put("Travis DeTour", prefix+String.format(userNumFormat, 66)); // same ?
    }

    public static Literal getLitFromUri(Model m, String uri) {
        //return m.createLiteral(m.shortForm(uri));
        return m.createLiteral(uri);
    }

    public static String getSubResourceName(Resource main, String prefix, String type, String index) {
        String mainName = main.getLocalName();
        return prefix+mainName+"_"+type+index;
    }

    public static String getSubResourceName(Resource main, String prefix, String type, int index) {
        return getSubResourceName(main, prefix, type, String.valueOf(index));
    }

    public static String getSubResourceName(Resource main, String prefix, String type) {
        return getSubResourceName(main, prefix, type, "");
    }

    public static String getDescriptionUriFromType(String type) {
        String res = normalizePropName(type, "description");
        switch (res) {
        case "noType":                return RDFS.getURI()+"comment";
        case "status":                return RDFS.getURI()+"comment";
        case "authorship":            return BDO+"workAuthorshipStatement";
        case "incipit":               return BDO+"workIncipit";
        case "note":                  return BDO+"note";
        case "notes":                 return BDO+"note";
        case "chapter":               return BDO+"work_desc_chapters";
        case "chapters":              return BDO+"work_desc_chapters";
        case "content":               return RDFS.getURI()+"comment";
        case "contents":              return RDFS.getURI()+"comment";
        case "completionDate":        return BDO+"work_desc_completionDate"; // this one and the next one are handled separately
        case "date":                  return ADM+"work_desc_date";
        case "errata":                return BDO+"workErrata";
        case "extent":                return BDO+"workExtentStatement";
        case "id":                    return "__fpl";
        case "libraryOfCongress":     return BDO+"work_desc_libraryOfCongress";
        case "location":              return BDO+"workLocationStatement";
        case "remarks":               return "__fpl";
        case "room":                  return "__fpl";
        case "summary":               return RDFS.getURI()+"comment";
        case "snar_bstan_number":     return BDO+"workKaTenSiglaN";
        case "snr_thang_number":      return BDO+"workKaTenSiglaN";
        case "snar_thang_number":     return BDO+"workKaTenSiglaN"; 
        case "gser_bris_numbr":       return BDO+"workKaTenSiglaG";
        case "gser_birs_number":      return BDO+"workKaTenSiglaG";
        case "gse_bris_number":       return BDO+"workKaTenSiglaG";
        case "sger_bris_number":      return BDO+"workKaTenSiglaG";
        case "gser_bri_numer":        return BDO+"workKaTenSiglaG";
        case "gser_dris_number":      return BDO+"workKaTenSiglaG";
        case "gser_bri_number":       return BDO+"workKaTenSiglaG";
        case "gser_bris_nimber":      return BDO+"workKaTenSiglaG";
        case "gser_bris_number":      return BDO+"workKaTenSiglaG";
        case "colopho":               return BDO+"workColophon";
        case "colophon":              return BDO+"workColophon";
        case "colophn":               return BDO+"workColophon";
        case "colophone":             return BDO+"workColophon";
        case "sde_gde_number":        return BDO+"workKaTenSiglaD";
        case "de_dge_number":         return BDO+"workKaTenSiglaD";
        case "sdg_dge_number":        return BDO+"workKaTenSiglaD";
        case "sdr_dge_number":        return BDO+"workKaTenSiglaD";
        case "sde_dge_number":        return BDO+"workKaTenSiglaD";
        case "lhasa_number":          return BDO+"workKaTenSiglaH";
        case "stog_numbe":            return BDO+"workKaTenSiglaS";
        case "stog_unmber":           return BDO+"workKaTenSiglaS";
        case "stog_number":           return BDO+"workKaTenSiglaS";
        case "stogNumber":            return BDO+"workKaTenSiglaS";
        case "toh_number":            return BDO+"workKaTenSiglaD";
        case "toh":                   return BDO+"workKaTenSiglaD";
        case "otani_number":          return BDO+"workKaTenSiglaQ";
        case "otani":                 return BDO+"workKaTenSiglaQ";
        case "otani_beijing":         return BDO+"workKaTenSiglaQ";
        case "sheyNumber":            return BDO+"workKaTenSiglaZ";
        case "shey_number":           return BDO+"workKaTenSiglaZ";
        case "rKTsReference":         return BDO+"workKaTenRefrKTs";
        case "bon_bka_gyur_number":   return BDO+"workKaTenSiglaBon";
        case "urga_number":           return BDO+"workKaTenSiglaU";
        case "isIAO":                 return BDO+"workRefIsIAO";
        case "catalogue_number":      return BDO+"workRefChokLing";
        case "gonpaPerEcumen":        return BDO+"placeGonpaPerEcumen";
        case "nameLex":
        case "nameKR":
        case "gbdist":
        case "town_syl":
        case "town_py":
        case "town_ch":
        case "prov_py":
        case "gonpaPer1000":
        case "dist_py":
        case "ondisk":
        case "onDisk":
        case "dld":
        case "icon":
        case "text": // TODO: migrate? how? https://github.com/BuddhistDigitalResourceCenter/xmltoldmigration/issues/22
        case "dpl480":
        case "featured":
            return "__ignore";
        default:
            return null;
        }
    }

    public static String normalizePropName(String toNormalize, String targetType) {
        String res = toNormalize.trim().replace("'", "").replace(" ", "_");
        if (targetType == "Class") {
            res = res.substring(0,1).toUpperCase() + res.substring(1);
        } else {
            res = res.substring(0,1).toLowerCase() + res.substring(1);
        }
        return res;
    }

    public static Pattern whiteSpacePattern = Pattern.compile("[\u180E\\s\\p{Cntrl}]+", Pattern.UNICODE_CHARACTER_CLASS);

    public static String normalizeString(String toNormalize, boolean keepSpaces) {
        if (keepSpaces)
            return toNormalize.trim();
        return whiteSpacePattern.matcher(toNormalize).replaceAll(" ").trim();
    }

    public static String normalizeString(String toNormalize) {
        return normalizeString(toNormalize, false);
    }

    /**
     * Adds a new named Note node and adds bdo:note node to the supplied rez.
     * 
     * @param rez respource the note is attached to
     * @param noteText String note text content
     * @param lang language of the noteText. If null set to "en"
     * @param loc the location statement for the note
     * @param ref Work resource from which the note is taken
     */
    public static void addNote(Resource rez, String noteText, String lang,  String loc, Resource ref) {
        Literal noteLit = null;
        if (noteText != null && !noteText.isEmpty()) {
            noteLit = rez.getModel().createLiteral(noteText, (lang != null ? lang : "en"));
        }
        addNote(rez, noteLit, loc, ref);
    }

    /**
     *Adds a new named Note node and adds bdo:note node to the supplied rez.
     * 
     * @param rez respource the note is attached to
     * @param noteText Literal note text content
     * @param loc the location statement for the note
     * @param ref Work resource from which the note is taken
     */
    private static void addNote(Resource rez, Literal noteText, String loc, Resource ref) {
        Model m = rez.getModel();
        Resource noteR = getFacetNode(FacetType.NOTE, rez);
        Property prop = m.getProperty(BDO, "note");
        rez.addProperty(prop, noteR);

        if (noteText != null) {
            noteR.addProperty(m.createProperty(BDO+"noteText"), noteText);
        }
        if (loc != null && !loc.isEmpty()) {
            noteR.addProperty(m.createProperty(BDO+"noteLocationStatement"), loc);
        }
        if (ref != null) {
            noteR.addProperty(m.getProperty(BDO, "noteWork"), ref);
        }
    }

    private static void addNote(Model m, Element e, Resource rez, int i, Property p, Literal l) {
        // some empty <note/> appear sometimes
        if (e.getAttribute("work").isEmpty() && e.getAttribute("location").isEmpty() && e.getTextContent().trim().isEmpty()) {
            return;
        }

        Literal noteText = getLiteral(e, "en", m, "note", rez.getLocalName(), rez.getLocalName(), false);	    
        String noteLoc = e.getAttribute("location").trim();
        String workRid = e.getAttribute("work").trim();
        Resource noteWork = null;
        if (!workRid.isEmpty()) {
            workRid = MigrationHelpers.sanitizeRID(rez.getLocalName(), "noteWork", workRid);
            if (!MigrationHelpers.isDisconnected(workRid))
                noteWork = m.createResource(BDR+workRid);
        }
        addNote(rez, noteText, noteLoc, noteWork);
    }

    public static void addNotes(Model m, Element e, Resource r, String XsdPrefix) {
        List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "note");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            addNote(m, current, r, i, null, null);
        }
    }

    public static String normalizeToLUrl(String toLUrl) {
        String res = toLUrl.replace("http", "https");
        res = res.replace("//treasuryoflives.org", "//www.treasuryoflives.org");
        res = res.replace("//beta.treasuryoflives.org", "//www.treasuryoflives.org");
        return res;
    }

    public static String getRIDFromTbrcUrl(String tbrcUrl) {
        int i = tbrcUrl.indexOf("RID=");
        String res = tbrcUrl;
        if (i > 0) {
            res = res.substring(i+4);
        } else {
            // case of http://tbrc.org/#library_topic_Object-T151
            // and http://tbrc.org/?locale=bo#library_work_Object-W1PD107999
            i = res.indexOf("-");
            return res.substring(i+1);
        }
        i = res.indexOf("$");
        if (i > 0) {
            res = res.substring(0, i);
        }
        i = res.indexOf("#");
        if (i > 0) {
            res = res.substring(0, i);
        }
        i = res.indexOf("|");
        if (i > 0) {
            res = res.substring(i+1);
        }
        String newRID = OutlineMigration.ridsToConvert.get(res);
        if (newRID != null) return newRID;
        return res;
    }

    public static void addExternal(Model m, Element e, Resource rez, int i) {
        Resource admR = getAdminData(rez);
        String value = e.getAttribute("data").trim();
        if (value.isEmpty()) return;
        if (value.contains("treasuryoflives.org")) {
            value = normalizeToLUrl(value);
            admR.addProperty(m.createProperty(ADM, "seeOtherToL"), m.createTypedLiteral(value, XSDDatatype.XSDanyURI));
            return;
        }
        if (value.contains("blog.tbrc.org")) return;
        if (value.contains("tbrc.org")) {
            value = getRIDFromTbrcUrl(value);
            // TODO: map outline nodes to new ones
            admR.addProperty(m.createProperty(RDFS.getURI(), "seeAlso"), m.createResource(BDR+value));
        }
    }

    public static void addExternals(Model m, Element e, Resource r, String XsdPrefix) {
        List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "external");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            addExternal(m, current, r, i);
        }
    }

    public static Literal literalFromXsdDate(Model m, String s) {
        // was quite difficult to find...
        XSDDateTime dateTime = (XSDDateTime)XSDDatatype.XSDdateTime.parse(s);
        return m.createTypedLiteral(dateTime);
    }

    public static void addLogEntry(Model m, Element e, Resource rez) {
        if (e == null) return;
        Resource logEntry = getFacetNode(FacetType.LOG_ENTRY, BDA, rez);
        Property prop = m.getProperty(ADM, "logEntry");
        m.add(rez, prop, logEntry);
        String value = e.getAttribute("when");
        if (!value.isEmpty()) {
            prop = m.createProperty(ADM+"logDate");
            try {
                m.add(logEntry, prop, literalFromXsdDate(m, value));
            } catch (DatatypeFormatException ex) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, rez.getLocalName(), rez.getLocalName(), "log_entry", "cannot convert log date properly, original date: `"+value+"`");
            }
        }
        value = normalizeString(e.getAttribute("who"));
        if (!value.isEmpty() && !value.equals("unspecified")) {
            prop = m.createProperty(ADM+"logWho");
            String uri = logWhoToUri.get(value);
            if (uri == null) {
                m.add(logEntry, m.createProperty(ADM+"logWhoStr"), value);
            } else {
                m.add(logEntry, prop, m.createResource(uri));
            }
        }
        value = normalizeString(e.getTextContent(), true);
        if (!value.isEmpty()) {
            prop = m.createProperty(ADM+"logMessage");
            m.add(logEntry, prop, m.createLiteral(value, "en"));
        }

    }

    public static void addLog(Model m, Element e, Resource rez, String XsdPrefix) {
        NodeList nodeList = e.getElementsByTagNameNS(XsdPrefix, "log");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element log = (Element) nodeList.item(i);
            NodeList logEntriesList = log.getElementsByTagNameNS(XsdPrefix, "entry");
            for (int j = 0; j < logEntriesList.getLength(); j++) {
                Element logEntry = (Element) logEntriesList.item(j);
                addLogEntry(m, logEntry, rez);
            }
            logEntriesList = log.getElementsByTagName("entry");
            for (int k = 0; k < logEntriesList.getLength(); k++) {
                Element logEntry = (Element) logEntriesList.item(k);
                addLogEntry(m, logEntry, rez);
            }
        }
    }

    // returns true if a PREFLABEL was added
    public static boolean addNames(Model m, Element e, Resource r, String XsdPrefix, boolean guessLabel, String additionalNameProp) {
        List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "name");
        Map<String,Boolean> labelDoneForLang = new HashMap<>();
        boolean res = false;
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            if (current.getTextContent().trim().isEmpty()) continue;
            // not sure about the second one in case of an outline
            Literal l = getLiteral(current, EWTS_TAG, m, "name", r.getLocalName(), r.getLocalName());
            if (l != null) {
                if (guessLabel) {
                    String lang = l.getLanguage().substring(0, 2);
                    if (!labelDoneForLang.containsKey(lang)) {
                        r.addProperty(SKOS.prefLabel, l);
                        labelDoneForLang.put(lang, true);
                    } else {
                        r.addProperty(SKOS.altLabel, l);
                    }
                } else {
                    r.addProperty(RDFS.label, l);
                }
                if (additionalNameProp != null) {
                    r.addProperty(m.getProperty(additionalNameProp), l);
                }
                res = true;
            }
        }
        return res;
    }

    public static boolean addNames(Model m, Element e, Resource r, String XsdPrefix) {
        return addNames(m, e, r, XsdPrefix, true, null);
    }

    public static String descriptionTypeNeedsLang(String type) {
        switch (type) {
        case "incipit":
        case "colophon":
        case "colopho":
        case "colophn":
        case "colophone":
            return EWTS_TAG;
        case "authorship":
        case "summary":
        case "content":
        case "contents": // for office, corporation, etc., maybe not for works
            return "en";
        default:
            return null;
        }
    }
    
    private static Resource getEra(String type) {
        switch (type) {
        case "beDate": return ResourceFactory.createResource(BDR+"EraBE");
        case "ceDate": return ResourceFactory.createResource(BDR+"EraCE");
        case "csDate": return ResourceFactory.createResource(BDR+"EraCS");
        }
        
        return null;
    }
    
    private static int getEraYearOffset(String type) {
        switch (type) {
        case "beDate": return -543;
        case "ceDate": return 0;
        case "csDate": return 638;
        }
        
        return 0;
    }
    
    private static boolean doFEMCDesc(Resource rez, String type, String value) {
        if ( ! rez.getLocalName().contains("FEMC")) {
            return false;
        }

        // a Khmer work from FEMC        
        Model m = rez.getModel();
        if (type.equals("beDate") || type.equals("ceDate") || type.equals("csDate")) {
            Resource event = getEvent(rez, "CompletedEvent", "workEvent");
            try {
                int yrInEra = Integer.parseInt(value);
                // add DateIndication
                Resource dateInd = getFacetNode(FacetType.DATE_INDICATION, rez);
                Resource era = getEra(type);
                event.addProperty(m.getProperty(BDO, "dateIndication"), dateInd);
                dateInd.addProperty(m.getProperty(BDO, "era"), era);
                // the following is used to avoid the automatic generation of an xsd:long literal instead of an xsd:integer
                Literal yrInEraLit = ResourceFactory.createTypedLiteral(value, XSDDatatype.XSDinteger);
                dateInd.addLiteral(m.getProperty(BDO, "yearInEra"), yrInEraLit);
                // add notBefore/notAfter or onYear
                int yrOff = getEraYearOffset(type);
                if (yrOff == 0) {
                    event.addProperty(m.getProperty(BDO, "onYear"), yearLit(m, yrInEra));
                } else {
                    int notBefore = yrOff > 0 ? yrInEra+yrOff-1 : yrInEra+yrOff;
                    int notAfter = yrOff > 0 ? yrInEra+yrOff : yrInEra+yrOff+1;
                    event.addProperty(m.getProperty(BDO, "notBefore"), yearLit(m, notBefore));
                    event.addProperty(m.getProperty(BDO, "notAfter"), yearLit(m, notAfter));
                }
                
            } catch (DatatypeFormatException ex) {}
            return true;
        } else if (type.equals("oldCodes")) {
            rez.addProperty(m.getProperty(BDO, "workKDPPOldId"), value);
            return true;
        } else if (type.equals("femcManuscriptCode")) {
            rez.addProperty(m.getProperty(BDO, "workFEMCManuscriptCode"), value);
            String[] pieces = value.split("\\.");
            int len = pieces.length;
            String subjCode = pieces[len-1];
            if (pieces[len-1].matches("\\d")) {
                subjCode = pieces[len-2]+"_"+pieces[len-1];
            }
            String subjId = "FEMC_Scheme_"+subjCode;
            rez.addProperty(m.getProperty(BDO, "workIsAbout"), m.createResource(BDR+subjId));
            return true;
        } else if (type.equals("filmCanister")) {
            Property mfp = m.getProperty(BDO+"workMicrofilm");
            Resource mf = rez.getPropertyResourceValue(mfp);
            if (mf == null) {
                mf = getFacetNode(FacetType.MICROFILM, rez);
                rez.addProperty(mfp, mf);
            }
            mf.addProperty(m.getProperty(BDO+"microfilmCanister"), value);
            return true;
        } else if (type.equals("filmStrip")) {
            Property mfp = m.getProperty(BDO+"workMicrofilm");
            Resource mf = rez.getPropertyResourceValue(mfp);
            if (mf == null) {
                mf = getFacetNode(FacetType.MICROFILM, rez);
                rez.addProperty(mfp, mf);
            }
            mf.addProperty(m.getProperty(BDO+"microfilmStrip"), value);
            return true;
        } else if (type.equals("catalogPage")) {
            Property notep = m.getProperty(BDO+"note");
            Resource note = null;
            StmtIterator notes = rez.listProperties(notep);
            while (notes.hasNext()) {
                Statement noteStmt = notes.next();
                Statement noteText = noteStmt.getResource().getProperty(m.getProperty(BDO+"noteText"));
                String noteTextStr = noteText.getString();
                if (noteTextStr.startsWith("Catalog")) {
                    note = noteStmt.getResource();
                    m.remove(noteText);
                    break;
                }
            }
            if (note == null) {
                note = getFacetNode(FacetType.NOTE, rez);
                note.addProperty(m.getProperty(BDO+"noteText"), "Catalog");
                rez.addProperty(notep, note);
            }
            note.addProperty(m.getProperty(BDO+"noteLocationStatement"), "pg. "+value);
            return true;
        } else if (type.equals("complete")) {
            if (value.equals("false")) {
                rez.addLiteral(m.getProperty(BDO, "workComplete"), false);
            } else {
                rez.addLiteral(m.getProperty(BDO, "workComplete"), true);
            }
            return true;
        } else if (type.equals("fascicles")) {
            rez.addProperty(m.getProperty(BDO+"hasFascicles"), value);
        } else if (type.equals("workNum")) {
            rez.addProperty(m.getProperty(BDO+"hasWorkNum"), value);
        }

        return false;
    }

    public static Map<String,Model> addDescriptions(Model m, Element e, Resource r, String XsdPrefix) {
        return addDescriptions(m, e, r, XsdPrefix, false);
    }

    public static Map<String,Model> addDescriptions(Model m, Element e, Resource rez, String XsdPrefix, boolean guessLabel) {
        List<Element> nodeList = getChildrenByTagName(e, XsdPrefix, "description");
        Map<String,Boolean> labelDoneForLang = new HashMap<>();
        Resource fplItem = null;
        Resource admFplItem = null;
        Model resModel = null;
        String fplId = null;
        String fplRoom = null;
        String fplDescription = null;
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            String value = current.getTextContent().trim();
            if (value.isEmpty()) continue;
            String type = current.getAttribute("type").trim();
            if (type.isEmpty())
                type = "noType";
            
            if (doFEMCDesc(rez,  type,  value)) continue;
            
            Literal lit;
            // we add some spaghettis for the case of R8LS13081 which has no description type
            // but needs to be added as label
            String lang = descriptionTypeNeedsLang(type);
            if (lang != null || (guessLabel && type.equals("noType"))) {
                if (lang == null)
                    lang = "en";
                lit = getLiteral(current, lang, m, "description", rez.getLocalName(), rez.getLocalName());
                if (lit == null) continue;
            } else {
                lit = m.createLiteral(normalizeString(value));
            }
            if (type.equals("nameLex")) {
                String placeId = rez.getLocalName();
                current.setTextContent(current.getTextContent().replace(placeId, ""));
            }
            if (type.equals("note")) {
                Resource note = getFacetNode(FacetType.NOTE, rez);
                m.add(rez, m.getProperty(BDO+"note"), note);
                m.add(note, m.getProperty(BDO+"noteText"), lit);
                continue;
            }
            if (type.equals("completionDate") || type.equals("date")) {
                Resource event = getEvent(rez, "CompletedEvent", "workEvent");
                addDates(value, event, rez);
                continue;
            }
            String propUri = getDescriptionUriFromType(type);
            if (propUri != null && propUri.equals("__ignore")) 
                continue;
            if (propUri == null) {
                ExceptionHelper.logException(ExceptionHelper.ET_DESC, rez.getLocalName(), rez.getLocalName(), "description", "unhandled description type: "+type);
                if (!guessLabel)
                    continue;
            }
            //			if (!guessLabel && type.equals("noType"))
            //			    l = m.createLiteral(l.getString()+" - was description with no type", l.getLanguage());
            if (propUri != null && propUri.equals("__fpl")) {
                if (fplItem == null) {
                    resModel = ModelFactory.createDefaultModel();
                    setPrefixes(resModel, "item");
                    String workId = rez.getLocalName();
                    fplItem = resModel.createResource(BDR+"I"+workId.substring(1)+"_P001");
                    admFplItem = createAdminRoot(fplItem);
                    if (WorkMigration.addItemForWork) {
                        fplItem.addProperty(resModel.getProperty(BDO, "itemForWork"), rez);
                    }
                    addReleased(resModel, admFplItem);
                    fplItem.addProperty(RDF.type, resModel.getResource(BDO+"ItemPhysicalAsset"));
                    fplItem.addProperty(resModel.getProperty(BDO, "itemLibrary"), resModel.getResource(BDR+FPL_LIBRARY_ID));
                    if (WorkMigration.addWorkHasItem) {
                        rez.addProperty(resModel.getProperty(BDO+"workHasItem"), fplItem);
                    }
                }
                switch(type) {
                case "id":
                    fplId = value;
                    if (fplRoom != null) {
                        fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"+fplId));
                    }
                    break;
                case "room":
                    fplRoom = value;
                    if (fplId != null) {
                        fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"+fplId));
                    }
                    break;
                case "remarks":
                    fplDescription = (fplDescription == null) ? value : fplDescription+"\n"+value;
                    break;
                }
                continue;
            }
            // for product and office the name is the first description type="contents", and we don't want to keep it in a description
            if (guessLabel && (type.equals("contents") || type.equals("noType"))) {
                lang = lit.getLanguage().substring(0, 2);
                if (!labelDoneForLang.containsKey(lang)) {
                    rez.addProperty(SKOS.prefLabel, lit);
                    labelDoneForLang.put(lang, true);
                } else {
                    rez.addProperty(SKOS.altLabel, lit);
                }
                continue;
            }
            rez.addProperty(m.getProperty(propUri), lit);
        }
        if ((fplId == null && fplRoom != null) ||
                (fplId != null && fplRoom == null)) {
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, rez.getLocalName(), rez.getLocalName(), "description", "types `id` and `room` should both be present");
            if (fplId == null)
                fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral(fplRoom+"|"));
            else
                fplItem.addProperty(resModel.getProperty(BDO, "itemShelf"), resModel.createLiteral("|"+fplId));
        }
        if (fplDescription != null) {
            Resource fplVolume = getFacetNode(FacetType.VOLUME, rez, resModel.getResource(BDO+"VolumePhysicalAsset"));
            fplItem.addProperty(resModel.getProperty(BDO, "itemHasVolume"), fplVolume);
            fplVolume.addProperty(resModel.getProperty(BDO, "volumeNumber"), resModel.createTypedLiteral(1, XSDDatatype.XSDinteger));
            fplVolume.addProperty(resModel.getProperty(BDO, "volumePhysicalDescription"), resModel.createLiteral(fplDescription, "en"));
        }
        if (resModel != null) {
            Map<String,Model> itemModels = new HashMap<>();
            itemModels.put(fplItem.getLocalName(), resModel);
            return itemModels;
        } else {
            return null;
        }
    }

    private static Resource getNodeType(String type, boolean outlineMode, Resource work) {
        Model m = work.getModel();

        switch (type) {
        case "titlePageTitle":
        case "fullTitle":
        case "subtitle":
        case "runningTitle":
        case "dkarChagTitle":
        case "colophonTitle":
        case "coverTitle":
        case "incipitTitle":
        case "halfTitle":
        case "otherTitle":
        case "spineTitle":
        case "copyrightPageTitle":
            return m.createResource(BDO+"Work"+type.substring(0, 1).toUpperCase() + type.substring(1));
        case "sectionTitle":
        case "captionTitle":
            if (outlineMode)
                return m.createResource(BDO+"WorkRunningTitle");
            else
                return m.createResource(BDO+"WorkOtherTitle");
        case "portion":
            return m.createResource(BDO+"WorkTitlePortion");
        case "incipit":
            return m.createResource(BDO+"WorkIncipitTitle");
        case "bibliographicalTitle":
            return m.createResource(BDO+"WorkTitle");
        default:
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, work.getLocalName(), work.getLocalName(), "title", "unknown title type `"+type+"`");
            return m.createResource(BDO+"WorkTitle");
        }
    }

    private static Literal getFEMCLit(Element title, Model m) {
        String value = title.getTextContent();
        String lang = title.getAttribute("lang");
        String type = title.getAttribute("type");
        boolean prem = type.equals("khmerOriginal") || type.equals("romanOriginal");
        boolean roman = type.contains("roman");
        String tag = "km-x-unspec";
        
        if (lang.equals("khmer")) {
            if (roman) {
                tag = prem ? "km-x-kmpre20c-kmfemc" : "km-x-kmfemc" ;
            } else {
                tag = prem ? "km-x-kmpre20c" : "km" ;
            }
        } else if (lang.equals("pāli")) {
            if (roman) {
                tag = prem ? "pi-x-kmpre20c-kmfemc" : "pi-x-kmfemc" ;
            } else {
                tag = prem ? "pi-khmr-x-kmpre20c" : "pi-khmr" ;
            }
        }
        
        return m.createLiteral(value, tag);
    }
    private static Resource addFEMCTitle(Resource main, Element title, String type, boolean addPref) {
        Literal lit = getFEMCLit(title, main.getModel());
        
        if (lit == null) return null;
        
        Resource nodeType = getNodeType(type, false, main);
        Resource titleNode = getFacetNode(FacetType.TITLE, main, nodeType);
        titleNode.addProperty(RDFS.label, lit);
        main.addProperty(main.getModel().getProperty(BDO, "workTitle"), titleNode);
        if (addPref) {
            main.addProperty(SKOS.prefLabel, lit);
        }
        
        return titleNode;
    }
    
    private static boolean addFEMCTitles(Resource main, List<Element> nodeList) {
        String rid = main.getLocalName();
        if (!rid.contains("FEMC") || rid.equals("W1FEMC01") || rid.equals("W1FEMC02")) {
            return false;
        }
        boolean biblioKhmer = false;
        boolean biblioRoman = false;
        
        Element khmerStd = null;
        Element romanStd = null;
        Element khmerCor = null;
        Element romanCor = null;
        Element khmerOrg = null;
        Element romanOrg = null;
        
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            String type = current.getAttribute("type");
            if (type.isEmpty()) continue;
            if (type.equals("khmerStandard"))
                khmerStd = current;
            if (type.equals("romanStandard"))
                romanStd = current;
            if (type.equals("khmerCorrectedOriginal"))
                khmerCor = current;
            if (type.equals("romanCorrectedOriginal"))
                romanCor = current;
            if (type.equals("khmerOriginal"))
                khmerOrg = current;
            if (type.equals("romanOriginal"))
                romanOrg = current;
        }

        Resource stdTitleKhm = null, 
                stdTitleRom = null, 
                corTitleKhm = null, 
                corTitleRom = null, 
                orgTitleKhm = null, 
                orgTitleRom = null;
        
        if (khmerStd != null) {
            if (romanStd.getTextContent().contentEquals("das jātak")) {
                stdTitleKhm = addFEMCTitle(main, khmerStd, "otherTitle", false);
            } else {
                biblioKhmer = true;
                stdTitleKhm = addFEMCTitle(main, khmerStd, "bibliographicalTitle", true);
            }
        }
        if (romanStd != null) {
            if (romanStd.getTextContent().contentEquals("das jātak")) {
                stdTitleRom = addFEMCTitle(main, khmerStd, "otherTitle", false);
            } else {
                biblioRoman = true;
                stdTitleRom = addFEMCTitle(main, romanStd, "bibliographicalTitle", true);
            }
        }
        
        if (khmerCor != null) {
            if (biblioKhmer) {
                corTitleKhm = addFEMCTitle(main, khmerCor, "coverTitle", false);
            } else {
                biblioKhmer = true;
                corTitleKhm = addFEMCTitle(main, khmerCor, "bibliographicalTitle", true);
            }
        }        
        if (romanCor != null) {
            if (biblioRoman) {
                corTitleRom = addFEMCTitle(main, romanCor, "coverTitle", false);
            } else {
                biblioRoman = true;
                corTitleRom = addFEMCTitle(main, romanCor, "bibliographicalTitle", true);
            }
        }
        
        if (khmerOrg != null) {
            if (biblioKhmer) {
                orgTitleKhm = addFEMCTitle(main, khmerOrg, "coverTitle", false);
            } else {
                biblioKhmer = true;
                orgTitleKhm = addFEMCTitle(main, khmerOrg, "bibliographicalTitle", true);
            }
        }
        if (romanOrg != null) {
            if (biblioRoman) {
                orgTitleRom = addFEMCTitle(main, romanOrg, "coverTitle", false);
            } else {
                biblioRoman = true;
                orgTitleRom = addFEMCTitle(main, romanOrg, "bibliographicalTitle", true);
            }
        }

        if (corTitleKhm != null && orgTitleKhm != null) {
            corTitleKhm.addProperty(main.getModel().getProperty(BDO, "femcConversionOf"), orgTitleKhm);
        }
        if (corTitleRom != null && orgTitleRom != null) {
            corTitleRom.addProperty(main.getModel().getProperty(BDO, "femcConversionOf"), orgTitleRom);
        }
        
        return true;
    }

    public static Literal abstractTitle(Literal l, Model m, String rid) {
        if (l.getLanguage().equals("bo-x-ewts")) {
            // remove first parenthesis, as in:
            // "(ya) yang bzlog 'phyong /"
            // "(ya)_gsang ba sbas ston las/_sman gyi gzhung shes yig chung /（phyi/_kha/_85）"
            // "(ya)bla ma'i rnal 'byor zab mo nyams len gnad kyi snying po/"
            String s = l.getString().strip();
            s = s.replaceAll("_? ?[\\(（][^\\)）༽]+[\\)）༽]", "");
            s = s.replaceAll("^[^ ]+\\)_?", "");
            s = s.replaceAll(" *\" *", "");
            return m.createLiteral(s, l.getLanguage());
        }
        return l;
    }
    
    public static void addTitles(Model m, Resource main, Element root, String XsdPrefix, boolean guessLabel, boolean outlineMode, final Resource mainA) {
        List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "title");
        if (addFEMCTitles(main, nodeList)) {
            return;
        }
        Map<String,Boolean> labelDoneForLang = new HashMap<>();
        Map<String,Boolean> titleSeen = new HashMap<>();
        String typeUsedForLabel = null;
        
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            Literal lit = getLiteral(current, EWTS_TAG, m, "title", main.getLocalName(), main.getLocalName());
            String nextTitle = null;
            if (lit == null) continue;
            if (main.getLocalName().contains("FPL") && lit.getLanguage().equals("pi-x-iast") && lit.getString().contains("--")) {
                String[] split = lit.getString().split("--");
                if (!split[1].isEmpty()) {
                    nextTitle = split[1];
                    lit = m.createLiteral(split[0], "pi-x-iast");
                }
            }
            final String litStr = lit.getString()+"@"+lit.getLanguage();
            if (titleSeen.containsKey(litStr))
                continue;
            titleSeen.put(litStr, true);
            String type = current.getAttribute("type");
            if (type.isEmpty()) {
                type = "bibliographicalTitle";
            }
            // warning: incipit inconsistently represents the incipit title or the incipit...
            if (type.equals("incipit")) {
                main.addProperty(m.getProperty(BDO, "workIncipit"), lit);
                continue;
            }
            /* here we split the titles between the work and instance as follows:
             * if we have bibliographicalTitle + another title, we promote the
             * bibliographicalTitle as label for the abstract work and we don't consider it
             * for the instance. If there's just one labe
             */
            if (mainA != null && type.equals("bibliographicalTitle")) {
                if (i != 0) {
                    //System.out.println("biblio in second position for RID "+main.getLocalName());
                }
                if (nodeList.size() > 1) {
                    if (guessLabel) {
                        String lang = lit.getLanguage().substring(0, 2);
                        if (!labelDoneForLang.containsKey(lang) && (typeUsedForLabel == null || typeUsedForLabel.equals(type))) {
                            main.addProperty(SKOS.prefLabel, lit);
                            Literal abstractLit = abstractTitle(lit, m, main.getLocalName());
                            //if (lit.getLanguage().equals("bo-x-ewts")) {
                            //    ExceptionHelper.logException(ExceptionHelper.ET_GEN, "", "", "title: "+main.getLocalName()+":"+abstractLit.getString());
                            //}
                            // this shouldn't add the same label twice because of the nature of triples
                            mainA.addProperty(SKOS.prefLabel, abstractLit);
                            labelDoneForLang.put(lang, true);
                            typeUsedForLabel = type;
                        } else {
                            mainA.addProperty(SKOS.altLabel, lit);
                        }
                    }
                    continue;
                }
            }

            Resource nodeType = getNodeType(type, outlineMode, main);
            Resource titleNode = getFacetNode(FacetType.TITLE, main, nodeType);        
            titleNode.addProperty(RDFS.label, lit);
            main.addProperty(m.getProperty(BDO, "workTitle"), titleNode);

            if (nextTitle != null) {
                titleNode = getFacetNode(FacetType.TITLE, main, nodeType);
                titleNode.addProperty(RDFS.label, m.createLiteral(nextTitle, "pi-x-iast"));
                main.addProperty(m.getProperty(BDO, "workTitle"), titleNode);
            }

            if (guessLabel) {
                String lang = lit.getLanguage().substring(0, 2);
                if (!labelDoneForLang.containsKey(lang) && (typeUsedForLabel == null || typeUsedForLabel.equals(type))) {
                    main.addProperty(SKOS.prefLabel, lit);
                    // this shouldn't add the same label twice because of the nature of triples
                    if (mainA != null) {
                        Literal abstractLit = abstractTitle(lit, m, main.getLocalName());
                        //if (lit.getLanguage().equals("bo-x-ewts")) {
                        //    ExceptionHelper.logException(ExceptionHelper.ET_GEN, "", "", "title: "+main.getLocalName()+":"+abstractLit.getString());
                        //}
                        mainA.addProperty(SKOS.prefLabel, abstractLit);
                    }
                    labelDoneForLang.put(lang, true);
                    typeUsedForLabel = type;
                } else if (mainA != null) {
                    mainA.addProperty(SKOS.altLabel, abstractTitle(lit, m, main.getLocalName()));
                }
                continue;
            }
        }
    }

    public static boolean isCommentaryTopic(String rid) {
        switch (rid) {
        case "T304":
        case "T3JT5054":
        case "T61":
        case "T4JW5424":
        case "T10MS12837":
        case "T132":
        case "T1488":
        case "T1491":
        case "T2397":
            return true;
        }
        return false;
    }

    // list of topics associated with languages. We simply remove them as the
    // the data already contains a language indication for the very large majority
    public static final Map<String,Boolean> langTopics = new HashMap<>();
    static {
        langTopics.put("T3CN1331", true); // Chinese
        langTopics.put("T2411", true); // Sanskrit
        langTopics.put("T3CN2027", true); // Mongolian
    }

    public static void addSubjects(Model m, Resource main, Element root, String XsdPrefix) {
        List<Element> nodeList = getChildrenByTagName(root, XsdPrefix, "subject");
        boolean needsCommentaryTopic = false;
        boolean hasCommentaryTopic = false;
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            String rid = current.getAttribute("class").trim();
            if (rid.isEmpty())
                continue;
            if (isCommentaryTopic(rid))
                hasCommentaryTopic = true;
            if (langTopics.containsKey(rid))
                continue;
            String value = current.getAttribute("type").trim();
            String prop = null;
            switch (value) {
            case "isAboutPerson":
            case "isAboutCorporation":
            case "isAboutMeeting":
            case "isAboutPlace":
            case "isAboutClan":
            case "isAboutSect":
            case "isAboutText":
                prop = BDO+"workIsAbout";
                break;
            case "isAboutControlled":
            case "isAboutUncontrolled":
                prop = BDO+"workIsAbout";
                break;
            case "isInstanceOfGenre":
            case "isInstanceOf":
                prop = BDO+"workGenre";
                break;
            case "isCommentaryOn":
                prop = BDO+"workIsAbout";
                needsCommentaryTopic = true;
                break;
            default:
                prop = BDO+"workIsAbout";
                break;
            }
            if (genreTopics.containsKey(rid)) {
                prop = BDO+"workGenre"; 
            }
            rid = MigrationHelpers.sanitizeRID(main.getLocalName(), value, rid);
            if (!MigrationHelpers.isDisconnected(rid))
                m.add(main, m.getProperty(prop), m.createResource(BDR+rid));
        }
        if (needsCommentaryTopic && !hasCommentaryTopic) {
            m.add(main, m.getProperty(BDO, "workGenre"), m.createResource(BDR+"T132"));
        }
    }

    private static int addLocationIntOrString(Model m, Resource main, Resource loc, Element current, String attributeName, String propname, Integer doNotAddIfEquals) {
        String value = current.getAttribute(attributeName).replaceAll(",$", "").trim();
        int res = -1;
        if (!value.isEmpty()) {
            try {
                int intval = Integer.parseInt(value);
                if (intval < 1) {
                    ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                    m.add(loc, m.getProperty(BDO, propname), m.createLiteral(value));
                } else {
                    if (doNotAddIfEquals == null || intval != doNotAddIfEquals)
                        m.add(loc, m.getProperty(BDO, propname), m.createTypedLiteral(intval, XSDDatatype.XSDinteger));
                    res = intval;
                }
            } catch (NumberFormatException e) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, main.getLocalName(), main.getLocalName(), "location", "`"+propname+"` must be a positive integer, got `"+value+"`");
                m.add(loc, m.getProperty(BDO, propname), m.createLiteral(value));
            }
        }
        return res;
    }

    public static class LocationVolPage {
        public Integer endVolNum;
        public int endPageNum;
        public Integer beginVolNum;
        public int beginPageNum;
        public String RID;

        public LocationVolPage(Integer beginVolNum, int beginPageNum, Integer endVolNum, int endPageNum, String RID) {
            this.endVolNum = endVolNum;
            this.endPageNum = endPageNum;
            this.beginVolNum = beginVolNum;
            this.beginPageNum = beginPageNum;
            this.RID = RID;
        }

        public String toString() {
            return "encVolNum: "+endVolNum+", endPageNum: "+endPageNum+", beginVolNum: "+beginVolNum+", beginPageNum: "+beginPageNum+", RID: "+RID;
        }
    }

    public static LocationVolPage addLocations(Model m, Resource main, Element root, String XsdPrefix, String workId, String outlineId, String outlineNode, String outlineNodeTitle) {

        List<Element> nodeList = CommonMigration.getChildrenByTagName(root, XsdPrefix, "location");
        if (nodeList.size() == 0) 
            return null;

        int i;
        int volume1 = -1;
        int page1 = -1;
        int page2 = -1;

        Resource loc = getFacetNode(FacetType.WORK_LOC, main);
        LocationVolPage res = null;
        for (i = 0; i < nodeList.size(); i++) {
            if (i > 1) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\" too many locations, it should only have 2");
                break;
            }
            Element current = (Element) nodeList.get(i);

            if (i == 0) {
                //String value = getSubResourceName(main, WORK_PREFIX, "Location", i+1);
                String value = current.getAttribute("type");
                if (value.equals("folio")) {
                    loc.addProperty(m.getProperty(BDO, "workLocationByFolio"), m.createTypedLiteral(true));
                }    
            }

            String value = current.getAttribute("work").trim();
            if (workId.isEmpty()) {
                if (!value.isEmpty())
                    loc.addProperty(m.getProperty(BDO, "workLocationWork"), m.createResource(BDR+value));
            } else if (!value.isEmpty() && !value.equals(workId)) {
                String error = "title: \""+outlineNodeTitle+"\" has locations in work "+value+" instead of "+workId;
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, error);
            }

            String endString = (i == 0) ? "" : "End";
            int volume = addLocationIntOrString(m, main, loc, current, "vol", "workLocation"+endString+"Volume", volume1);
            if (i == 0) volume1 = volume;
            if (i == 1 && volume != -1 && volume1 != -1 && volume < volume1) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", end location volume is before beginning location volume");
            }
            int page = addLocationIntOrString(m, main, loc, current, "page", "workLocation"+endString+"Page", null);
            if (i == 0) {
                page1 = page;
            } else {
                page2 = page;
            }
            if (i == 1 && page != -1 && page1 != -1 && page < page1 && volume == volume1) {
                ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", end location page is before beginning location");
            }
            addLocationIntOrString(m, main, loc, current, "phrase", "workLocation"+endString+"Phrase", null);
            addLocationIntOrString(m, main, loc, current, "line", "workLocation"+endString+"Line", null);

            if (i == 1 && page != -1) {
                res = new LocationVolPage(volume1, page1, volume, page, null);
            }

            value = current.getAttribute("side");
            if (!value.isEmpty())
                m.add(loc, m.getProperty(BDO, "workLocation"+endString+"Side"), m.createLiteral(value));

        }

        // only add locations with statements
        StmtIterator locProps = loc.listProperties();
        if (locProps.hasNext()) {
            m.add(main, m.getProperty(BDO, "workLocation"), loc);
            // comment to remove workLocationWork in outline nodes
            if (!workId.isEmpty())
                m.add(loc, m.getProperty(BDO, "workLocationWork"), m.createResource(BDR+workId));
        } else {
            m.removeAll(loc, null, null);
        }

        if (volume1 == -1 && (page1 == -1 || page2 == -1)) {
            ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", missing volume, beginpage or endpage");
        } else if (volume1 != -1 && (page1 == -1 || page2 == -1)) {
            ExceptionHelper.logOutlineException(ExceptionHelper.ET_OUTLINE, workId, outlineId, outlineNode, "title: \""+outlineNodeTitle+"\", vol. "+volume1+", missing beginpage or endpage");
        }

        return res;
    }

    public static Literal getLiteral(Element e, String dflt, Model m, String propertyHint, String RID, String subRID) {
        return getLiteral(e, dflt, m, propertyHint, RID, subRID, true);
    }

    // TODO: use the bdrclib version once there's a new release
    public static String addEwtsShad(String s) {
        // we suppose that there is no space at the end
        if (s == null)
            return s;
        s = s.replaceAll("[ _/]+$", "");
        final int sLen = s.length();
        if (sLen < 2)
            return s;
        int last = s.codePointAt(sLen-1);
        int finalidx = sLen-1;
        if (last == 'a' || last == 'i' || last == 'e' || last == 'o') {
            last = s.codePointAt(sLen-2);
            finalidx = sLen-2;
        }
        if (sLen > 2 && last == 'g' && s.codePointAt(finalidx-1) == 'n')
            return s+" /";
        if (last == 'g' || last == 'k' || (sLen == 3 && last == 'h' && s.codePointAt(finalidx-1) == 's') || (sLen > 3 && last == 'h' && s.codePointAt(finalidx-1) == 's' && s.codePointAt(finalidx-2) != 't'))
            return s;
        if (last < 'A' || last > 'z' || (last > 'Z' && last < 'a'))  // string doesn't end with tibetan letter
            return s;
        return s+"/";
    }

    public static String normalizeEwts(final String s) {
        return addEwtsShad(s.replace((char)0x2019, (char)0x27));
    }
    
    public static Literal getLiteral(Element elem, String dflt, Model m, String propertyHint, String RID, String subRID, boolean normalize) {
        String value = elem.getTextContent();
        value = normalize ? normalizeString(value) : value.trim();
        if (value.isEmpty()) return null;
        if (value.indexOf('\ufffd') != -1)
            ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "string contains invalid replacement character: `"+value+"`");
        String tag = getBCP47(elem, dflt);
        if (tag.equals("bo")) {
            value = normalizeTibetan(value);
            if (EwtsConverter.isCombining(value.charAt(0))) {
                ExceptionHelper.logException(ET_LANG, RID, subRID, propertyHint, "Unicode string `"+value+"` starts with combining character");
            }
        }
        if (tag.equals("sa")) {
            if (value.contains("+"))
                tag = "sa-x-ewts";
            else if (isDeva(value))
                tag = "sa-Deva";
            else
                tag = "sa-x-ndia";
        }
        if (tag.equals(EWTS_TAG)) {
            if (RID.startsWith("W1FPL")) {
                tag = "en";
            } else {
                List<String> conversionWarnings = new ArrayList<String>();
                converter.toUnicode(value, conversionWarnings, true);
                if (conversionWarnings.size() > 0) {
                    String fixed = EwtsFixer.getFixedStr(RID, value);
                    if (fixed == null) {
                        if (isLikelyEnglish(value)) {
                            tag = "en";
                        } else {
                            value = normalizeEwts(value);	                        
                        }
                        ExceptionHelper.logEwtsException(RID, subRID, propertyHint, value, conversionWarnings);
                    } else if ("LNG".equals(fixed))
                        tag = EwtsFixer.guessLang(value);
                    else
                        value = fixed;
                } else {
                    value = normalizeEwts(value);
                }
            }
        }
        if (tag.equals("bo-alalc97")) {
            List<String> conversionWarnings = new ArrayList<String>();
            converterAlalc.toUnicode(value, conversionWarnings, true);
            if (conversionWarnings.size() > 0)
                ExceptionHelper.logEwtsException(RID, subRID, propertyHint, value, conversionWarnings);
        }
        return m.createLiteral(value, tag);
    }

    public static boolean documentValidates(Document document, Validator validator) {
        return documentValidates(document, validator, "");
    }

    public static boolean documentValidates(Document document, Validator validator, String fileName) {
        Source xmlSource = new DOMSource(document);
        try {
            validator.validate(xmlSource);
        }
        catch (SAXException ex) {
            MigrationHelpers.writeLog("Document "+fileName+" is not valid because:");
            MigrationHelpers.writeLog(ex.getMessage());
            //ex.printStackTrace();
            return false;
        } catch (IOException e) {
            MigrationHelpers.writeLog("IO problem:");
            MigrationHelpers.writeLog(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean rdfOkInOntology(Model m, OntModel o) {
        return rdfOkInOntology(m, o, "");
    }

    public static boolean rdfOkInOntology(Model m, OntModel o, String fileName) {
        o.addSubModel(m);
        ValidityReport vr;
        try {
            vr = o.validate();
        }
        catch(InternalReasonerException e) {
            MigrationHelpers.writeLog(e.getMessage());
            return false;
        }
        if (vr == null) return true;
        if (!vr.isValid()) {
            MigrationHelpers.writeLog("Model "+fileName+" not OK in ontology because:");
            Iterator<ValidityReport.Report> itr = vr.getReports();
            while(itr.hasNext()) {
                ValidityReport.Report report = itr.next();
                MigrationHelpers.writeLog(report.toString());
            }
        }
        return vr.isValid();
    }

    // like getElementsByTagNameNS but not recursive (strange DOM doesn't have that)
    public static List<Element> getChildrenByTagName(Element parent, String xsdPrefix, String name) {
        List<Element> nodeList = new ArrayList<Element>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getLocalName())) {
                nodeList.add((Element) child);
            }
        }
        return nodeList;
    }

}
