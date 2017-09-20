package io.bdrc.xmltoldmigration.xml2files;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EtextBodyMigration {

    public static boolean oneLongString = true;
    private static final XPath xPath = EtextMigration.initXpath();
    private static final String TEI_PREFIX = EtextMigration.TEI_PREFIX;
    //private static final Charset charset = Charset.forName("UTF-8");
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    
    public static String normalizeString(final String src, final String page, final String lineNum) {
        String res = CommonMigration.normalizeTibetan(src);
        // I don't think we want non-breakable spaces, just normal spaces
        res = res.replace('\u00A0', ' ');
        return res;
    }
    
    public static void MigrateBody(Document d, OutputStream strOutput, Model m, String eTextId) {
        final PrintStream ps = new PrintStream(strOutput);
        Element body;
        Resource etextR = m.getResource(BDR+eTextId);
        try {
            body = (Element) ((NodeList)xPath.evaluate("/tei:TEI/tei:text/tei:body/tei:div",
                    d.getDocumentElement(), XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            return;
        }
        final NodeList pars = body.getElementsByTagNameNS(TEI_PREFIX, "p");
        int currentTotalPoints = 0;
        for (int i = 0; i < pars.getLength(); i++) {
            Element par = (Element) pars.item(i);
            String pageNum = par.getAttribute("n");
            Resource pageR = m.createResource();
            etextR.addProperty(m.createProperty(BDO, "eTextHasPage"), pageR);
            pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createLiteral(pageNum)); // TODO: convert to int, but what about image ref?
            pageR.addProperty(m.createProperty(BDO, "sliceStart"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
            final NodeList children = par.getChildNodes();
            int linenum = 0;
            for (int j = 0; j < children.getLength(); j++) {
              final Node child = children.item(j);
              if (child instanceof Element) {
                  final Element milestone = (Element) child;
                  try {
                      linenum = Integer.parseInt(milestone.getAttribute("n"));
                  } catch (NumberFormatException ex) {
                      System.out.println("cannot parse line number string: "+milestone.getAttribute("n"));
                  }
              } else {
                final String s = normalizeString(child.getTextContent(), Integer.toString(linenum), pageNum);
                Resource lineR = m.createResource();
                pageR.addProperty(m.createProperty(BDO, "pageHasLine"), lineR);
                lineR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(linenum, XSDDatatype.XSDinteger));
                lineR.addProperty(m.createProperty(BDO, "sliceStart"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
                currentTotalPoints += s.codePointCount(0, s.length());
                lineR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
                // web annotations refer to code points, not UTF-16 code units:
                ps.print(s);
              }
           }
           pageR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
        }
    }
    
}
