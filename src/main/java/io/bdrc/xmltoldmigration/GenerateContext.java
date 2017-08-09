package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

// takes the BDRC owl file and generates one context per document type

public class GenerateContext {
    
    public static boolean datatypeNeedsContext(String datatype) {
        if (datatype.equals(XSDDatatype.XSDstring.getURI())
                || datatype.equals(RDF.langString.getURI())
                || datatype.equals(RDF.getURI()+"PlainLiteral")
                // messes with the useNativeTypes if these are not here, 
                // certainly a bug in jsonld-java, but cannot test before
                // https://github.com/jsonld-java/jsonld-java/issues/208
                // gets fixed
                || datatype.equals(XSDDatatype.XSDboolean.getURI())
                || datatype.equals(XSDDatatype.XSDinteger.getURI())
                || datatype.equals(XSDDatatype.XSDfloat.getURI()))
                return false;
        return true;
    }
    
    public static Map<String, Object> generateCompleteContext(OntModel m, PrefixMap prefixMap) {
        SortedMap<String, Object> context = new TreeMap<>();
        context.putAll(prefixMap.getMappingCopyStr());
        context.put("@version", "1.1");
        context.put("@vocab", CommonMigration.ONTOLOGY_PREFIX);
        context.remove("");
        Map<String,String> typemap = new HashMap<>();
        typemap.put("@type", "@id");
        // we iterate on all properties and figure out the type
        // with the range, as some annotation properties 
        ExtendedIterator<OntProperty> ppi = m.listAllOntProperties();
        while (ppi.hasNext()) {
            OntProperty p = ppi.next();
            String abbr = prefixMap.abbreviate(p.getURI());
            if (abbr == null || abbr.isEmpty() || abbr.startsWith("owl:"))
                continue;
            // we replace ':property' by 'property'
            if (abbr.charAt(0) == ':') {
                abbr = abbr.substring(1);
            }
            if (p.isObjectProperty()) {
                context.put(abbr, typemap);
                continue;
            }
            OntResource range = p.getRange();
            if (range != null && range.isURIResource()) {
                Resource r = range.getPropertyResourceValue(RDF.type);
                if (r != null && r.equals(OWL.Class)) {
                    context.put(abbr, typemap);
                    continue;
                }
                String typeUri = range.getURI();
                if (datatypeNeedsContext(typeUri)) {
                    Map<String,String> pm = new HashMap<>();
                    pm.put("@type", typeUri);
                    context.put(abbr, pm);
                }
            }
        }
        return context;
    }
}
