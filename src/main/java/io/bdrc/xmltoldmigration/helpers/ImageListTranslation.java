package io.bdrc.xmltoldmigration.helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import io.bdrc.xmltoldmigration.xml2files.CommonMigration;

public class ImageListTranslation {
    
    public static final boolean startWith0 = false;
    public static boolean considerMissingPages = true;
    private static final String BDO = CommonMigration.ONTOLOGY_PREFIX;
    
    private static final Pattern imageP = Pattern.compile("^(.+)(\\d{4})( ?\\..+)$");
    private static final Pattern basicP = Pattern.compile("[^|]+");
    public static void addImageList(String src, String mainId, Integer volNum, Model model, Resource main) {
        Matcher basicM = basicP.matcher(src);
        String prefix = "";
        String suffix = "";
        int i = -1;
        int total = 0;
        boolean first = true;
        StringBuilder dst = new StringBuilder();
        int firstOkInSeq = -1;
        int lastOkInSeq = -1;
        List<String> missingPages = new ArrayList<>();
        boolean hasSlash = false;
        String mixedCase = null;
        String notSorted = null;
        String previous = null;
        while (basicM.find()) {
            if (basicM.group(0).indexOf('/') != -1)
                hasSlash = true;
            if (notSorted == null && previous != null && previous.compareTo(basicM.group(0)) > 0) {
                notSorted = previous+"|"+basicM.group(0);
            }
            previous = basicM.group(0);
            total = total +1;
            Matcher m = imageP.matcher(basicM.group(0));
            if (!m.find()) {
                ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, mainId, "cannot understand image string "+basicM.group(0));
                if (lastOkInSeq != -1)
                    dst.append(":"+(lastOkInSeq-firstOkInSeq+1));
                if (!first)
                    dst.append("|");
                dst.append(basicM.group(0));
                prefix = "";
                i = -1;
                suffix = "";
                lastOkInSeq = -1;
                firstOkInSeq = -1;
                first = false;
                continue;
            }
            final int newInt = Integer.parseInt(m.group(2));
            if (firstOkInSeq == -1)
                firstOkInSeq = newInt;
            if (i != -1 && newInt > i+1) {
                int rangeB = i+1;
                int rangeE = newInt-1;
                if (rangeB == rangeE)
                    missingPages.add(Integer.toString(rangeB));
                else
                    missingPages.add(rangeB+"-"+rangeE);
            }
            final String newSuffix = m.group(3);
            if (mixedCase == null && !newSuffix.equals(suffix) && newSuffix.toLowerCase().equals(suffix.toLowerCase())) {
                mixedCase = suffix+" and "+newSuffix;
            }
            if (!m.group(1).equals(prefix) || !newSuffix.equals(suffix) || newInt != i+1) {
                if (lastOkInSeq != -1)
                    dst.append(":"+(lastOkInSeq-firstOkInSeq+1));
                if (!first)
                    dst.append("|");
                dst.append(m.group(0));
                prefix = m.group(1);
                i = newInt;
                suffix = newSuffix;
                lastOkInSeq = -1;
                firstOkInSeq = newInt;
            } else {
                i = i +1;
                lastOkInSeq = newInt;
            }
            first = false;
        }
        if (lastOkInSeq != -1)
            dst.append(":"+(lastOkInSeq-firstOkInSeq+1));
        if (hasSlash)
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, mainId, "image list contains invalid character `/`");
        if (mixedCase != null)
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, mainId, "image list contains a mix of upper and lower case extensions: "+mixedCase);
        if (notSorted != null)
            ExceptionHelper.logException(ExceptionHelper.ET_GEN, mainId, mainId, "image list is not sorted alphabetically, for example: "+notSorted);
        Literal value = model.createLiteral(dst.toString());
        model.add(main, model.getProperty(BDO+"imageList"), value);
        model.add(main, model.getProperty(BDO+"imageCount"), model.createTypedLiteral(total, XSDDatatype.XSDinteger));
        String missingImages = String.join(",", missingPages);
        if (!missingImages.isEmpty())
            model.add(main, model.getProperty(BDO+"imagesMissing"), model.createLiteral(missingImages));
    }
    
    public static Resource getVolumeResource(Model m, int volumeNumber) {
        final Literal num = m.createTypedLiteral(volumeNumber, XSDDatatype.XSDinteger);
        final Property volumeNumberP = m.getProperty(CommonMigration.BDO, "volumeNumber");
        final List<Statement> sl = m.listStatements(null, volumeNumberP, num).toList();
        if (sl.size() == 0)
            return null;
        if (sl.size() > 1)
            System.err.println("two volumes have the same number!");
        return sl.get(0).getSubject().asResource();
    }
    
    private static final Pattern imageListGeneralP = Pattern.compile("([^|:]+):?(\\d+)?");
    private static final Pattern imageListWellFormattedP = Pattern.compile("^(.+)(\\d{4})( ?\\..+)$");
    private static final Pattern imagesMissingP = Pattern.compile("(\\d+)-?(\\d+)?");
    
    public static Map<String,Integer> getImageNums(String imageList, String imagesMissing) {
        Map<Integer,Boolean> isMissing = new HashMap<>();
        if (considerMissingPages && imagesMissing != null && !imagesMissing.isEmpty()) {
            Matcher imagesMissingM = imagesMissingP.matcher(imagesMissing);
            while (imagesMissingM.find()) {
                final int first = Integer.valueOf(imagesMissingM.group(1));
                if (imagesMissingM.group(2) != null) {
                    final int second = Integer.valueOf(imagesMissingM.group(2));
                    for (int i = first ; i <= second ; i++) {
                        isMissing.put(i, true);
                    }
                } else {
                    isMissing.put(first, true);
                }
            }
        }
        int curPageNum = 1; // TODO: startWith0
        final Matcher imageListM = imageListGeneralP.matcher(imageList);
        final Map<String,Integer> res = new HashMap<>();
        while (imageListM.find()) {
            while (considerMissingPages && isMissing.containsKey(curPageNum)) {
                curPageNum++;
            }
            if (imageListM.group(2) != null && !imageListM.group(2).equals("1")) {
                final Matcher imageNumM = imageListWellFormattedP.matcher(imageListM.group(1));
                if (!imageNumM.find()) {
                    System.err.println("You should take a look at this");
                    return res;
                }
                final int times = Integer.valueOf(imageListM.group(2));
                final String firstPart = imageNumM.group(1).toLowerCase();
                final String lastPart = imageNumM.group(3).toLowerCase();
                final int initialNum = Integer.valueOf(imageNumM.group(2));
                for (int i = 0 ; i < times ; i++) {
                    final int thisNum = i+initialNum;
                    res.put(firstPart+String.format("%04d", thisNum)+lastPart, curPageNum);
                    curPageNum += 1;
                }
            } else {
                res.put(imageListM.group(1).toLowerCase(), curPageNum);
                curPageNum += 1;
            }
        }
        return res;
    }
    
    public static Map<String,Integer> getImageNums(Model m, int volumeNumber) {
        Resource vol = getVolumeResource(m, volumeNumber);
        if (vol == null)
            return null;
        final Property imageListP = m.getProperty(CommonMigration.BDO, "imageList");
        final Property imagesMissingP = m.getProperty(CommonMigration.BDO, "imagesMissing");
        final Statement imageListS = vol.getProperty(imageListP);
        if (imageListS == null)
            return null;
        final String imageList = imageListS.getString();
        final Statement imagesMissingS = vol.getProperty(imagesMissingP);
        String imagesMissing = null;
        if (imagesMissingS != null)
            imagesMissing = imagesMissingS.getString();
        return getImageNums(imageList, imagesMissing);
    }
}
