package io.bdrc.xmltoldmigration.helpers;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExceptionHelper {
    // Exception types
    public static final int ET_EWTS = 0;
    public static final int ET_LANG = 1;
    public static final int ET_DESC = 2;
    public static final int ET_GEN = 3;
    public static final int ET_OUTLINE = 4;
    public static final int ET_MISSING = 5;
    public static final int ET_IMAGEGROUP = 6;
    public static final int ET_ETEXT = 7;
    private static final int ET_EWTS_CSV = 100;
    
    public static final Map<Integer, String> logNames = new HashMap<Integer, String>();
    static {
        logNames.put(ET_EWTS, "errors-ewts.log");
        logNames.put(ET_EWTS_CSV, "errors-ewts.csv");
        logNames.put(ET_LANG, "errors-lang.log");
        logNames.put(ET_DESC, "errors-desc.log");
        logNames.put(ET_GEN, "errors-gen.log");
        logNames.put(ET_OUTLINE, "errors-outline.log");
        logNames.put(ET_MISSING, "errors-missing.log");
        logNames.put(ET_IMAGEGROUP, "errors-imagegroup.log");
        logNames.put(ET_ETEXT, "errors-etexts.log");
    }
    
    public static final Map<Integer, FileWriter> writers = new HashMap<Integer, FileWriter>();
    
    public static FileWriter getFileWriter(int type) {
        FileWriter res = writers.get(type);
        if (res == null) {
            String fileName = logNames.get(type);
            try {
                res = new FileWriter(fileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            writers.put(type, res);
        }
        return res;
    }
    
    public static void closeAll() {
        for (FileWriter fw : writers.values()) {
            try {
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static String getUri(int type, String RID, String subRID) {
        if (type == ET_OUTLINE) {
            return "https://www.tbrc.org/#library_work_ViewByOutline-"+subRID+"|"+RID;
        }
        return "https://www.tbrc.org/#!rid="+RID;
    }
    
    public static void logException(int type, String RID, String subRID, String propIndication, String error) {
        //System.out.println(error);
        FileWriter f = getFileWriter(type);
        try {
            f.write("- [ ] ["+subRID+"]("+getUri(type, RID, subRID)+") ");
            f.write("on property `"+propIndication+"`: "+error+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logException(int type, String RID, String subRID, String rawError) {
        //System.out.println(error);
        FileWriter f = getFileWriter(type);
        try {
            f.write("- [ ] ["+subRID+"]("+getUri(type, RID, subRID)+") "+rawError+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logOutlineException(int type, String workRID, String outlineRID, String nodeRID, String rawError) {
        //System.out.println(error);
        FileWriter f = getFileWriter(type);
        try {
            f.write("- [ ] ["+nodeRID+"]("+getUri(type, workRID, nodeRID)+") in "+outlineRID+" "+rawError+"\n");
            //f.write("- [ ] ["+outlineRID+"]("+getUri(ET_GEN, workRID, workRID)+") "+rawError+"\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void logEwtsException(String RID, String subRID, String propIndication, String original, List<String> warnings) {
        FileWriter f = getFileWriter(ET_EWTS);
        try {
            f.write("- [ ] ["+RID+"](https://www.tbrc.org/#!rid="+RID+") ");
            f.write("has EWTS conversion problems on property `"+propIndication+"`");
            f.write(", original string: `"+original+"`:\n");
            for (String warning : warnings) {
                f.write("  - "+warning.replace('"', '`').replace("line1: ", "")+"\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        f = getFileWriter(ET_EWTS_CSV);
//        try {
//            f.write("\""+RID+"\",");
//            f.write("\""+original+"\",,\"");
//            for (String warning : warnings) {
//                f.write(warning.replace('"', '`').replace("line1: ", "")+",");
//            }
//            f.write("\"\n");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
