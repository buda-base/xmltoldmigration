package io.bdrc.xmltoldmigration.helpers;

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
                final String firstPart = imageNumM.group(1);
                final String lastPart = imageNumM.group(3);
                final int initialNum = Integer.valueOf(imageNumM.group(2));
                for (int i = initialNum ; i <= times ; i++) {
                    final int thisNum = i;
                    res.put(firstPart+String.format("%04d", thisNum)+lastPart, curPageNum);
                    curPageNum += 1;
                }
            } else {
                res.put(imageListM.group(1), curPageNum);
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
