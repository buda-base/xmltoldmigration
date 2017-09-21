package io.bdrc.xmltoldmigration.xml2files;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int meanChunkSizeAim = 300;
    private static final int maxChunkSizeAim = 500;
    private static final int minChunkSize = 30;
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    
    public static String normalizeString(final String src, final String page, final String lineNum) {
        String res = CommonMigration.normalizeTibetan(src);
        // I don't think we want non-breakable spaces, just normal spaces
        res = res.replace('\u00A0', ' ');
        //res = res.replaceAll("\\s+", " "); // not sure it's necessary
        // TODO: replace \r, \n, etc. with space ?
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
        final StringBuilder totalStr = new StringBuilder();
        final Map<Resource,int[]> resourcesCoords = new HashMap<Resource,int[]>();
        int pageBeginChar = 0;
        for (int i = 0; i < pars.getLength(); i++) {
            Element par = (Element) pars.item(i);
            String pageNum = par.getAttribute("n");
            Resource pageR = m.createResource();
            etextR.addProperty(m.createProperty(BDO, "eTextHasPage"), pageR);
            pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createLiteral(pageNum)); // TODO: convert to int, but what about image ref?
            if (oneLongString) {
                pageR.addProperty(m.createProperty(BDO, "sliceStart"), m.createLiteral("1-"+currentTotalPoints));
            } else {
                pageBeginChar = currentTotalPoints;
            }
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
                final int strLen = s.codePointCount(0, s.length()); 
                if (oneLongString) {
                    lineR.addProperty(m.createProperty(BDO, "sliceStart"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
                    lineR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createTypedLiteral(currentTotalPoints+strLen, XSDDatatype.XSDinteger));
                } else {
                    resourcesCoords.put(lineR, new int[] {currentTotalPoints, currentTotalPoints+strLen});
                }
                currentTotalPoints += strLen;
                
                // web annotations refer to code points, not UTF-16 code units:
                if (oneLongString)
                    ps.print(s);
                else
                    totalStr.append(s);
              }
           }
           if (oneLongString) {
               pageR.addProperty(m.createProperty(BDO, "sliceEnd"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
           } else {
               resourcesCoords.put(pageR, new int[] {pageBeginChar, currentTotalPoints});
           }
        }
        // at that point the processing is done if we're in one long string mode
        if (!oneLongString) {
            chunkString(totalStr.toString(), resourcesCoords, ps, m, eTextId, currentTotalPoints);
        }
    }
    
    public static void chunkString(final String totalStr, final Map<Resource,int[]> resourcesCoords, final PrintStream out, final Model m, final String eTextId, final int totalPoints) {
        final List<Integer> charbreaks = getBreakingCharsIndexes(totalStr, totalPoints);
    }
    
    public static List<Integer> getBreakingCharsIndexes(final String totalStr, final int totalPoints) {
        final List<Integer> res = new ArrayList<Integer>();
        int lastDecidedIndex = -1;
        int lastSpaceIndex = -1;
        int curCharIndex = minChunkSize;
        List<Integer> candidates = new ArrayList<Integer>();
        while (curCharIndex < totalPoints-minChunkSize) {
            // only break on spaces or \n:
            final int curChar = totalStr.codePointAt(curCharIndex);
            if (curChar == '\n') {
                res.add(curCharIndex);
                lastDecidedIndex = curCharIndex;
                lastSpaceIndex = -1;
                curCharIndex += minChunkSize;
                candidates = new ArrayList<Integer>();
                continue;
            }
            if (curChar != ' ') {
                curCharIndex += 1;
                continue;
            }
            if (totalStr.codePointAt(curCharIndex+1) == '\u0F04' || totalStr.codePointAt(curCharIndex+2) == '\u0F04') {
                res.add(curCharIndex);
                lastDecidedIndex = curCharIndex;
                lastSpaceIndex = -1;
                curCharIndex += minChunkSize; // 20 is a bit random here...
                candidates = new ArrayList<Integer>();
                continue;
            }
            if (totalStr.codePointAt(curCharIndex-2) == '\u0F05') {
                curCharIndex += minChunkSize;
                continue;
            }
            if (curCharIndex - lastDecidedIndex > maxChunkSizeAim) {
                res.add(curCharIndex);
                lastDecidedIndex = curCharIndex;
                lastSpaceIndex = -1;
                curCharIndex += minChunkSize; // 20 is a bit random here...
                candidates = new ArrayList<Integer>();
                continue;
            }
            if (curCharIndex - lastDecidedIndex > maxChunkSizeAim) {
                res.add(curCharIndex);
                lastDecidedIndex = curCharIndex;
                lastSpaceIndex = -1;
                curCharIndex += minChunkSize; // 20 is a bit random here...
                candidates = new ArrayList<Integer>();
                continue;
            }
            if (lastSpaceIndex != -1 && curCharIndex - lastSpaceIndex < minChunkSize) {
                curCharIndex += 2; // not +minChunkSize so that we're sure not to cut trails of small chunks
                lastSpaceIndex = curCharIndex;
                continue;
            }
            if (curCharIndex - lastDecidedIndex >= meanChunkSizeAim) {
                res.add(curCharIndex);
                lastDecidedIndex = curCharIndex;
                lastSpaceIndex = -1;
                curCharIndex += minChunkSize; // 20 is a bit random here...
                candidates = new ArrayList<Integer>();
                continue;
            }
            
        }
        return res;
    }
   
    
}
