package io.bdrc.xmltoldmigration;

import java.util.Comparator;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.rulesys.Util;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class BDRCNodeComparator implements Comparator<RDFNode> {

    public int compareLiterals(Literal l0, Literal l1) {
        final String lang0 = l0.getLanguage();
        int res;
        if (!lang0.isEmpty()) {
            res = lang0.compareTo(l1.getLanguage());
            if (res != 0) return res;
        }
        Object o0 = l0.getValue();
        Object o1 = l1.getValue();
        if (o0 instanceof String) {
            return ((String) o0).compareTo(o1.toString());
        }
        // compares dates and numbers
        return Util.compareTypedLiterals(l0.asNode(), l1.asNode());
    }
    
    public int compareProperties(Resource r0, Resource r1, Property p) {
        final Statement s0 = r0.getProperty(p);
        if (s0 == null) return 0;
        final Statement s1 = r1.getProperty(p);
        if (s1 == null) return 1;
        final RDFNode o0 = s0.getObject();
        final RDFNode o1 = s1.getObject();
        if (o0.isURIResource()) { 
            if (s1.getObject().isURIResource()) {
                return o0.asResource().getURI().compareTo(o1.asResource().getURI());
            }
            return 1;
        }
        // can't compare blank nodes
        if (o0.isAnon()) return 0;
        return compareLiterals(o0.asLiteral(), o1.asLiteral());
    }
    
    @Override
    public int compare(RDFNode arg0, RDFNode arg1) {
        Resource r0 = arg0.asResource();
        Resource r1 = arg1.asResource();
        // sort named nodes by uri
        if (!arg0.isAnon()) {
            return r0.getURI().compareTo(r1.getURI());
        }
        // sort blank nodes by type:
        int res = compareProperties(r0, r1, RDF.type);
        if (res != 0) return res;
        // if same type, sort by label
        res = compareProperties(r0, r1, RDFS.label);
        if (res != 0) return res;
        // if no label, the only case is log entries, sorted by logWhen
        return compareProperties(r0, r1, r0.getModel().getProperty(CommonMigration.ADM, "logWhen"));
    }

}
