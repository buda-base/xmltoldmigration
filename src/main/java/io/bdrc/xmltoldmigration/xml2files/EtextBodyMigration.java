package io.bdrc.xmltoldmigration.xml2files;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;

import org.apache.jena.rdf.model.Model;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class EtextBodyMigration {

    public static boolean oneLongString = true;
    private static final XPath xPath = EtextMigration.initXpath();
    private static final String TEI_PREFIX = EtextMigration.TEI_PREFIX;
    private static final Charset charset = Charset.forName("UTF-8");
    
    public static String normalizeString(final String src, final String page, final String lineNum) {
        return CommonMigration.normalizeTibetan(src);
    }
    
    public static void MigrateBody(Document d, OutputStream strOutput, Model m, String eTextId) {
        final PrintStream ps = new PrintStream(strOutput);
        Element body;
        try {
            body = (Element) ((NodeList)xPath.evaluate("/tei:TEI/tei:text/tei:body/tei:div",
                    d.getDocumentElement(), XPathConstants.NODESET)).item(0);
        } catch (XPathExpressionException ex) {
            ex.printStackTrace();
            return;
        }
        final NodeList pars = body.getElementsByTagNameNS(TEI_PREFIX, "p");
        for (int i = 0; i < pars.getLength(); i++) {
            Element par = (Element) pars.item(i);
            String pageNum = par.getAttribute("n");
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
                final int len = s.codePointCount(0, s.length());
                // web annotations refer to code points, not UTF-16 code units:
                ps.print(s);
              }
           }
        }
    }
    
}
