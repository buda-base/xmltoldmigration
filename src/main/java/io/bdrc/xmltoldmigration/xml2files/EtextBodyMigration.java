package io.bdrc.xmltoldmigration.xml2files;

import static io.bdrc.libraries.LangStrings.normalizeTibetan;
import static io.bdrc.libraries.Models.BDO;
import static io.bdrc.libraries.Models.BDR;
import static io.bdrc.libraries.Models.getFacetNode;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
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

import io.bdrc.libraries.Models.FacetType;
import io.bdrc.xmltoldmigration.helpers.ExceptionHelper;

public class EtextBodyMigration {

    public static boolean oneLongString = false;
    private static final XPath xPath = EtextMigration.initXpath();
    private static final String TEI_PREFIX = EtextMigration.TEI_PREFIX;
    public static final String PAGE_INSERT = "\n\n";
    public static final int PAGE_INSERT_codelen = 2;
    public static final String LINE_INSERT = "\n";
    public static final int LINE_INSERT_codelen = 1;
    
    public static final Pattern rtfP = Pattern.compile("(\\s*\\d*(PAGE|\\$)[\u0000-\u0127]+)+");
    public static String normalizeString(final String src, final String page, final String lineNum, final boolean fromRTF, final String eTextId) {
        String res = normalizeTibetan(src);
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
    public static void addChunkLocation(Model m, Resource r, int charNum, boolean start) {
        final String startEndString = (start ? "Start" : "End");
        r.addProperty(m.getProperty(BDO, "slice"+startEndString+"Char"), m.createTypedLiteral(charNum, XSDDatatype.XSDinteger));
    }
    
    public static void MigrateBody(final Document d, final OutputStream strOutput, final Model m, final String eTextId, final Map<String,Integer> imageNumPageNum, final boolean needsPageNameTranslation, final boolean keepPages) {
        final int pageShift;
        if (!needsPageNameTranslation && keepPages) // for eKangyur, we need to shift page numbers by 2
            pageShift = 2;
        else
            pageShift = 0;
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
        boolean firstPage = true;
        for (int i = 0; i < pars.getLength(); i++) {
            final Element par = (Element) pars.item(i);
            if (!par.hasChildNodes()) {
                continue;
            }
            Resource pageR = null;
            if (keepPages) {
                pageR = getFacetNode(FacetType.ETEXT_PAGE,  etextR);
                etextR.addProperty(m.createProperty(BDO, "eTextHasPage"), pageR);
            }
            final String pageNum = par.getAttribute("n");
            if (!pageNum.isEmpty() && keepPages) {
                if (imageNumPageNum != null) {
                    int dotidx = pageNum.lastIndexOf('.');
                    if (dotidx == -1) {
                        System.err.println("strange image name in etext xml: "+pageNum);
                        continue;
                    }
                    Integer pageNumI = imageNumPageNum.get(pageNum.substring(0, dotidx).toLowerCase());
                    if (pageNumI == null) {
                        ExceptionHelper.logException(ExceptionHelper.ET_GEN, eTextId, eTextId, "cannot find image "+pageNum);
                    } else {
                        pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(pageNumI, XSDDatatype.XSDinteger));
                    }
                } else {
                    try {
                        final Integer pageNumI = Integer.valueOf(pageNum)+pageShift;
                        pageR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(pageNumI, XSDDatatype.XSDinteger));
                    } catch (NumberFormatException e) {
                        ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "cannot convert image to int "+pageNum);
                    }
                }
            }
            int pageBeginPointIndex = currentTotalPoints;
            final NodeList children = par.getChildNodes();
            int linenum = 0;
            boolean firstLine = true;
            for (int j = 0; j < children.getLength(); j++) {
              final Node child = children.item(j);
              if (child instanceof Element) {
                  // milestones inbetween text chuncks
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
                // text chunks
                String s = child.getTextContent();
                if (s.isEmpty())
                    continue;
                if (!firstLine) {
                    ps.print(LINE_INSERT);
                    currentTotalPoints += LINE_INSERT_codelen;
                } else if (!firstPage) {
                    ps.print(PAGE_INSERT);
                    currentTotalPoints += PAGE_INSERT_codelen;
                    pageBeginPointIndex += PAGE_INSERT_codelen;
                }
                s = normalizeString(s, Integer.toString(linenum), pageNum, !needsPageNameTranslation, eTextId);
                final int strLen = s.codePointCount(0, s.length()); 
                if (keepPages && linenum != 0) {
                    Resource lineR = getFacetNode(FacetType.ETEXT_LINE,  pageR);
                    pageR.addProperty(m.createProperty(BDO, "pageHasLine"), lineR);
                    lineR.addProperty(m.createProperty(BDO, "seqNum"), m.createTypedLiteral(linenum, XSDDatatype.XSDinteger));
                    lineR.addProperty(m.getProperty(BDO, "sliceStartChar"), m.createTypedLiteral(currentTotalPoints, XSDDatatype.XSDinteger));
                    lineR.addProperty(m.getProperty(BDO, "sliceEndChar"), m.createTypedLiteral(currentTotalPoints+strLen, XSDDatatype.XSDinteger));
                }
                currentTotalPoints += strLen;

                ps.print(s);
                firstPage = false;
                firstLine = false;
              }
           }
           final int pageEndPointIndex = currentTotalPoints;
           if (keepPages) {
               pageR.addProperty(m.getProperty(BDO, "sliceStartChar"), m.createTypedLiteral(pageBeginPointIndex, XSDDatatype.XSDinteger));
               pageR.addProperty(m.getProperty(BDO, "sliceEndChar"), m.createTypedLiteral(pageEndPointIndex, XSDDatatype.XSDinteger));
           }
        }
        if (totalStr.length() == 0)
            ExceptionHelper.logException(ExceptionHelper.ET_ETEXT, eTextId, eTextId, "is empty");
    }

}
