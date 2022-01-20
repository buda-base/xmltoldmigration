package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.EWTS_TAG;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.createAdminRoot;
import static io.bdrc.libraries.Models.createRoot;
import static io.bdrc.libraries.Models.setPrefixes;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import io.bdrc.xmltoldmigration.MigrationHelpers;
import io.bdrc.xmltoldmigration.xml2files.OutlineMigration.CurNodeInt;

public class TaxonomyMigration {

    private static final String OXSDNS = "http://www.tbrc.org/models/outline#";
    
    public static Model MigrateTaxonomy(Document d) {
        Element root = d.getDocumentElement();
        
        Model m = ModelFactory.createDefaultModel();
        MigrationHelpers.setPrefixes(m, "outline");

        CurNodeInt curNodeInt = new CurNodeInt();
        curNodeInt.i = 0;
        
        String legacyOutlineRID = root.getAttribute("RID");
        Resource mainTaxonomy = createRoot(m, BDR+legacyOutlineRID, null);
        Resource admTaxonomy = createAdminRoot(mainTaxonomy);

        CommonMigration.addLog(m, root, admTaxonomy, OXSDNS, false);
        
        addNodes(m, mainTaxonomy, root, mainTaxonomy.getLocalName(), curNodeInt, null, legacyOutlineRID, "");
        
        return m;
    }

    public static void addNames(Model m, Element e, Resource r) {
        if (r.getLocalName().startsWith("T")) {
            // we don't want topic labels in the taxonomy graph, these should
            // already be in the topics graph
            return;
        }
        List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "name");
        for (int i = 0; i < nodeList.size(); i++) {
            Element current = (Element) nodeList.get(i);
            if (current.getTextContent().trim().isEmpty()) continue;
            // not sure about the second one in case of an outline
            Literal l = CommonMigration.getLiteral(current, EWTS_TAG, m, "name", r.getLocalName(), r.getLocalName());
            if (l != null) {
                if (!l.getLanguage().equals("bo") && !l.getLanguage().equals("zh-latn-pinyin-x-ndia"))
                    r.addProperty(SKOS.prefLabel, l);
            }
        }
    }

    public static void addNode(Model m, Resource r, Element e, int i, String workId, CurNodeInt curNode, String legacyOutlineRID, int partIndex, String thisPartTreeIndex) {
        curNode.i = curNode.i+1;
        String clazz = e.getAttribute("class");
        String rid = e.getAttribute("RID");
        Resource node;
        if (clazz.isEmpty()) {
            // internal node of a taxonomy
            String value = workId+"_"+String.format("%04d", curNode.i);
            if (!rid.isEmpty()) {
                value = rid;
            }
            node = m.createResource(BDR+value);
            node.addProperty(RDF.type, m.createResource(BDO+"Taxonomy"));
        } else {
            // external node, often a Topic, create node of the form bdr:Txxx
            node = m.createResource(BDR+clazz.trim());
        }
        String nodeRID = e.getAttribute("RID").trim();
        //node.addProperty(m.createProperty(BDO, "workPartTreeIndex"), thisPartTreeIndex);
//        if (!value.isEmpty()) {
//            node.addProperty(m.getProperty(ADM, "workLegacyNode"), RID);
//        }

        addNames(m, e, node);
        //m.add(node, m.createProperty(BDO, "taxSubClassOf"), r);
        m.add(r, m.createProperty(BDO, "taxHasSubClass"), node);

        // sub nodes
        addNodes(m, node, e, workId, curNode, nodeRID, legacyOutlineRID, thisPartTreeIndex);
    }
    
    static String getPartTreeIndexStr(final int index, final int nbSiblings) {
        if (nbSiblings < 10)
            return Integer.toString(index);
        if (nbSiblings < 100)
            return String.format("%02d", index);
        return String.format("%03d", index);
    }
    
    public static boolean addNodes(Model m, Resource r, Element e, String workId, CurNodeInt curNode, String parentRID, String legacyOutlineRID, String curPartTreeIndex) {
        boolean res = false;
        final List<Element> nodeList = CommonMigration.getChildrenByTagName(e, OXSDNS, "node");
        final int nbChildren = nodeList.size();
        for (int i = 0; i < nbChildren; i++) {
            res = true;
            Element current = (Element) nodeList.get(i);
            final String thisPartTreeIndex;
            if (curPartTreeIndex.isEmpty()) {
                thisPartTreeIndex = getPartTreeIndexStr(i+1, nbChildren);
            } else {
                thisPartTreeIndex = curPartTreeIndex+"."+getPartTreeIndexStr(i+1, nbChildren);
            }
            addNode(m, r, current, i, workId, curNode, legacyOutlineRID, i+1, thisPartTreeIndex);
        }
        return res;
    }
    
}
