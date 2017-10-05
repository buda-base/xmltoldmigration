package io.bdrc.xmltoldmigration.xml2files;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;

public class EtextBodyMigration {

    public static boolean oneLongString = false;
    private static final XPath xPath = EtextMigration.initXpath();
    private static final String TEI_PREFIX = EtextMigration.TEI_PREFIX;
    public static final String BDR = CommonMigration.BDR;
    public static final String BDO = CommonMigration.BDO;
    public static final String ADM = CommonMigration.ADM;
    
    public static final Pattern rtfP = Pattern.compile("(\\s*\\d*(PAGE|\\$)[\u0000-\u0127]+)+");
    public static String normalizeString(final String src, final String page, final String lineNum, final boolean fromRTF, final String eTextId) {
        String res = CommonMigration.normalizeTibetan(src);
        // I don't think we want non-breakable spaces, just normal spaces
        res = res.replace('\u00A0', ' ');
        if (fromRTF) {
            Matcher rtfM = rtfP.matcher(res);
            while(rtfM.find()) {
                ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "removed RTF string `"+rtfM.group(0)+"`");
            }
            rtfM.replaceAll("");
        }
        //res = res.replaceAll("\\s+", " "); // not sure it's necessary
        // TODO: replace \r, \n, etc. with space ?
        return res;
    }
    
    public static final boolean LOC_START = true;
    public static final boolean LOC_END = false;
    public static void addChunkLocation(Model m, Resource r, int chunkNum, int charNum, boolean start) {
        final String startEndString = (start ? "Start" : "End");
        r.addProperty(m.getProperty(BDO, "slice"+startEndString+"Chunk"), m.createTypedLiteral(chunkNum, XSDDatatype.XSDinteger));
        r.addProperty(m.getProperty(BDO, "slice"+startEndString+"Char"), m.createTypedLiteral(charNum, XSDDatatype.XSDinteger));
    }
    
    public static void MigrateBody(final Document d, final OutputStream strOutput, final Model m, final String eTextId, final Map<String,Integer> imageNumPageNum, final boolean needsPageNameTranslation, final boolean keepPages) {
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
        boolean first = true;
        for (int i = 0; i < pars.getLength(); i++) {
            final Element par = (Element) pars.item(i);
            if (!par.hasChildNodes())
                continue;
            Resource pageR = null;
            if (keepPages) {
                pageR = m.createResource();
                etextR.addProperty(m.createProperty(BDO, "eTextHasPage"), pageR);
            }
            final String pageNum = par.getAttribute("n");
            if (!pageNum.isEmpty() && keepPages) {
                if (imageNumPageNum != null) {
                    Integer pageNumI = imageNumPageNum.get(pageNum.toLowerCase());
                    if (pageNumI == null) { // TODO: are there some cases in which this breaks?
                        pageNumI = imageNumPageNum.get(pageNum.replace(".tif", ".jpg").toLowerCase());
                    }
                    if (pageNumI == null) {
                        //System.out.println(imageNumPageNum);
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot find image "+pageNum);
                    } else {
                        pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(pageNumI, XSDDatatype.XSDinteger));
                    }
                } else {
                    try {
                        final Integer pageNumI = Integer.valueOf(pageNum);
                        pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(pageNumI, XSDDatatype.XSDinteger));
                    } catch (NumberFormatException e) {
                        ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "cannot convert image to int "+pageNum);
                    }
                }
            }
            final int pageBeginPointIndex = currentTotalPoints+1;
            final NodeList children = par.getChildNodes();
            int linenum = 0;
            for (int j = 0; j < children.getLength(); j++) {
              final Node child = children.item(j);
              if (child instanceof Element) {
                  if (!keepPages)
                      continue;
                  final Element milestone = (Element) child;
                  try {
                      linenum = Integer.parseInt(milestone.getAttribute("n"));
                  } catch (NumberFormatException ex) {
                      System.out.println("cannot parse line number string: "+milestone.getAttribute("n"));
                      linenum = 0;
                  }
              } else {
                String s = child.getTextContent();
                if (s.isEmpty())
                    continue;
                s = normalizeString(s, Integer.toString(linenum), pageNum, !needsPageNameTranslation, eTextId);
                if (!first && !keepPages)
                    s = ' '+s;
                final int strLen = s.codePointCount(0, s.length()); 
                if (keepPages && linenum != 0) {
                    Resource lineR = m.createResource();
                    pageR.addProperty(m.createProperty(BDO, "pageHasLine"), lineR);
                    lineR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(linenum, XSDDatatype.XSDinteger));
                    if (oneLongString) {
                        addChunkLocation(m, lineR, 1, currentTotalPoints+1, LOC_START);
                        addChunkLocation(m, lineR, 1, currentTotalPoints+strLen, LOC_END);
                    } else {
                        resourcesCoords.put(lineR, new int[] {currentTotalPoints+1, currentTotalPoints+strLen});
                    }
                }
                currentTotalPoints += strLen;
                
                // web annotations refer to code points, not UTF-16 code units:
                if (oneLongString)
                    ps.print(s);
                else
                    totalStr.append(s);
              }
              first = false;
           }
           final int pageEndPointIndex = currentTotalPoints;
           if (oneLongString && keepPages) {
               addChunkLocation(m, pageR, 1, pageBeginPointIndex, true);
               addChunkLocation(m, pageR, 1, pageEndPointIndex, false);
           } else if (keepPages) {
               resourcesCoords.put(pageR, new int[] {pageBeginPointIndex, pageEndPointIndex});
           }
        }
        if (totalStr.length() == 0)
            ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "is empty");
        // at that point the processing is done if we're in one long string mode
        if (!oneLongString)
            chunkString(totalStr.toString(), resourcesCoords, ps, m, eTextId, currentTotalPoints);
    }
    
    public static int[] translatePoint(final List<Integer> pointBreaks, final int pointIndex, final boolean isStart) {
        // pointIndex depends on the context, 
        // if it's about the starting point (isStart == true):
        //     it's the index of the starting char: ab|cd -> pointIndex 2 for the beginning of the second segment (cd)
        // else 
        //     it's the index after the end char: ab|cd -> pointIndex 2 for the end of the first segment (ab)
        int curLine = 1;
        int toSubstract = 0;
        for (final int pointBreak : pointBreaks) {
            // pointBreak is the index of the char after which the break occurs, for instance
            // a|bc|d will have pointBreaks of 1 and 3
            if (pointBreak >= pointIndex) {
                break;
            }
            toSubstract = pointBreak;
            curLine += 1;
        }
        return new int[] {curLine, pointIndex-toSubstract};
    }
    
    public static void chunkString(final String totalStr, final Map<Resource,int[]> resourcesCoords, final PrintStream out, final Model m, final String eTextId, final int totalPoints) {
        final List<Integer>[] breaks = TibetanStringChunker.getAllBreakingCharsIndexes(totalStr);
        
        final List<Integer> charBreaks = breaks[0];
        final List<Integer> pointBreaks = breaks[1];
        int previousIndex = 0;
        //final int nbBreaks = charBreaks.size();
        for (final int charBreakIndex : charBreaks) { 
            out.print(totalStr.substring(previousIndex, charBreakIndex)+'\n');
            previousIndex = charBreakIndex;
        }
        if (previousIndex != totalStr.length()) {
            out.print(totalStr.substring(previousIndex));
        }
        for (final Entry<Resource,int[]> e : resourcesCoords.entrySet()) {
            final int[] oldSet = e.getValue();
            final int[] start = translatePoint(pointBreaks, oldSet[0], LOC_START);
            final int[] end = translatePoint(pointBreaks, oldSet[1], LOC_END);
            addChunkLocation(m, e.getKey(), start[0], start[1], LOC_START);
            addChunkLocation(m, e.getKey(), end[0], end[1], LOC_END);
        }
    }

}
