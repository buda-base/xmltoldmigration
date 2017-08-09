package io.bdrc.xmltoldmigration;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

// takes the BDRC owl file and generates one context per document type

public class GenerateContext {
    
    public static boolean datatypeNeedsContext(String datatype) {
        if (datatype.equals(XSDDatatype.XSDstring.getURI())
                || datatype.equals(RDF.langString.getURI()))
                return false;
        return true;
    }
    
    public static Map<String, Object> generateCompleteContext(OntModel m, PrefixMap prefixMap) {
        SortedMap<String, Object> context = new TreeMap<>();
        context.putAll(prefixMap.getMappingCopyStr());
        Map<String,String> typemap = new HashMap<>();
        typemap.put("@type", "@id");
        // we iterate on all properties and figure out the type
        // with the range, as some annotation properties 
        ExtendedIterator<OntProperty> ppi = m.listAllOntProperties();
        while (ppi.hasNext()) {
            OntProperty p = ppi.next();
            System.out.println(p.toString());
            String abbr = prefixMap.abbreviate(p.getURI());
            if (abbr == null || abbr.startsWith("owl:"))
                continue;
            if (p.isObjectProperty()) {
                context.put(abbr, typemap);
                continue;
            }
            OntResource range = p.getRange();
            if (range != null && range.isURIResource()) {
                Resource r = range.getPropertyResourceValue(RDF.type);
                if (r != null) {
                    if (r.equals(OWL.Class)) {
                        context.put(abbr, typemap);
                    } else {
                        if (datatypeNeedsContext(r.getURI())) {
                            Map<String,String> pm = new HashMap<>();
                            pm.put("@type", range.getURI());
                            context.put(abbr, pm);
                        }
                    }
                }
            }
        }
        return context;
    }
}
