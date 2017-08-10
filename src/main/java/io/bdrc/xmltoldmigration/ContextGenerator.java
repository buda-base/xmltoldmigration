package io.bdrc.xmltoldmigration;

import java.util.Comparator;
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

/**
 * This self-contained class (which should probably have its own repository)
 * generates a complete JSON-LD context file from a Jena OntModel.
 * 
* @author Elie Roux
* @author Buddhist Digital Resource Center (BDRC)
* @version 0.1.0
 *
 */
public class ContextGenerator {
    
    /**
     * 
     * @param datatype
     * the URI of the data type
     * @return
     * true if a property with this data type as range will
     * benefit from having its type specified in the context.
     * We return false for types that convert to json native types,
     * this could be parametrized.
     */
    public static boolean datatypeNeedsContext(String datatype) {
        if (datatype.equals(XSDDatatype.XSDstring.getURI())
                || datatype.equals(RDF.langString.getURI())
                // for legacy RDF 1.0
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
    
    /**
     * A very simple comparator built from a prefix map list, simply
     * keeping the prefix keys before the property ones.
     */
    private static final class ContextKeyComparator implements Comparator<String> {

        Map<String,String> prefixMapList = null;
        
        public ContextKeyComparator(Map<String,String> prefixMapList) {
            super();
            this.prefixMapList = prefixMapList;
        }
        
        // first prefixes, then predicates with no prefix, then predicates with a prefix
        private Integer getPriority(String arg0) {
            if (prefixMapList != null && prefixMapList.containsKey(arg0))
                return 0;
            if (arg0.indexOf(':') != -1) {
                return 2;
            }
            return 1;
        }
        
        @Override
        public int compare(String arg0, String arg1) {
            Integer prio0 = getPriority(arg0);
            Integer prio1 = getPriority(arg1);
            if (prio0 != prio1) {
                return prio0.compareTo(prio1);
            }
            return arg0.compareTo(arg1);
        }
    }
    
    /**
     * Builds a map to be used in @context. If the prefixMap contains
     * an empty prefix, we made the choice to convert it into @vocab
     * when @vocab can be used or into an alternate prefix passed
     * as an argument for IRI compaction where @vocab cannot be used.
     * 
     * @param m
     * The model corresponding to the ontology you want to convert
     * @param prefixMap
     * A Jena prefixMap that will be transferred to the jsonld context
     * @param defaultPrefixRenaming
     * A prefix that will replace the empty prefix if present
     * @return
     * A Map that can be used as the value for @context in a standalone context
     * file, a general document or a frame document
     */
    public static Map<String, Object> generateContextObject(final OntModel m, final PrefixMap prefixMap, final String defaultPrefixRenaming) {
        Map<String,String> prefixMapStr = prefixMap.getMappingCopyStr();
        if (prefixMapStr.containsKey("")) {
            prefixMapStr.put(defaultPrefixRenaming, prefixMapStr.get(""));
            prefixMapStr.put("@vocab", prefixMapStr.get(""));
            prefixMapStr.remove("");
        }
        final SortedMap<String, Object> context = new TreeMap<>(new ContextKeyComparator(prefixMapStr));
        context.putAll(prefixMapStr);
        final Map<String,String> typemap = new HashMap<>();
        typemap.put("@type", "@id");
        // we iterate on all properties and figure out the type
        // with the range, as some annotation properties 
        final ExtendedIterator<OntProperty> ppi = m.listAllOntProperties();
        while (ppi.hasNext()) {
            final OntProperty p = ppi.next();
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
            final OntResource range = p.getRange();
            if (range != null && range.isURIResource()) {
                final Resource r = range.getPropertyResourceValue(RDF.type);
                if (r != null && r.equals(OWL.Class)) {
                    context.put(abbr, typemap);
                    continue;
                }
                final String typeUri = range.getURI();
                if (datatypeNeedsContext(typeUri)) {
                    String abbrType = prefixMap.abbreviate(typeUri);
                    if (abbrType == null || abbrType.isEmpty())
                        abbrType = typeUri;
                    if (abbrType.charAt(0) == ':') {
                        // types should not be compacted with @vocab, so we use defaultPrefixRenaming:
                        if (defaultPrefixRenaming != null && !defaultPrefixRenaming.isEmpty()) {
                            abbrType = defaultPrefixRenaming+abbrType;
                        } else {
                            abbrType = typeUri;
                        }
                    }
                    final Map<String,String> pm = new HashMap<>();
                    pm.put("@type", abbrType);
                    context.put(abbr, pm);
                }
            }
        }
        return context;
    }

}
