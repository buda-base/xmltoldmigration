package io.bdrc.xmltoldmigration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.*;
import io.bdrc.xmltoldmigration.xml2files.TibetanStringChunker;

public class TibetanChunkerTest {

    @Test
    public void testAllIndexes() {
        List<Integer>[]res = TibetanStringChunker.getAllBreakingCharsIndexes("x། xx");
        assertThat(res[0], contains(3));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x། །xx");
        assertThat(res[0], contains(4));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x༑ x");
        assertThat(res[0], contains(3));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x ༑ x");
        assertThat(res[0], contains(2));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("༑ x");
        assertTrue(res[0].isEmpty());
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x ༑x");
        assertThat(res[0], contains(3));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x\u0f14x");
        assertThat(res[0], contains(2));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("x\u0f7f x");
        assertThat(res[0], contains(3));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("xxx།། །། ༆ ། །xxx");
        assertThat(res[0], contains(9));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("xxx༎ ༎༆ ༎xxx");
        assertThat(res[0], contains(6));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("སྤྱི་ལོ་༢༠༡༧ ཟླ་༡ ཚེས་༡༤ ཉིན་ལ་བྲིས་པ་དགེ");
        assertTrue(res[0].isEmpty());
        res = TibetanStringChunker.getAllBreakingCharsIndexes("བཀྲིས་ ༼བཀྲ་ཤིས༽ ངའི་གྲོགས་པོ་རེད།");
        assertTrue(res[0].isEmpty());
        res = TibetanStringChunker.getAllBreakingCharsIndexes("ག གི གྲ ཀ ཤ པ མ");
        assertThat(res[0], contains(2, 5, 10, 12));
    }
    
    @Test
    public void testNbSylls() {
        List<Integer>[]res = TibetanStringChunker.getAllBreakingCharsIndexes("༄༅། ཀ༌ཀོ་ཀཿཀ࿒ཀ་ཀ ཀ་རང་ཀ།་");
        assertThat(res[2], contains(6, 3));
    }
    
    @Test
    public void testSelection() {
        // test quatrain grouping
        List<Integer>[] allIndexes = new List[3];
        allIndexes[0] = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25);
        allIndexes[1] = allIndexes[0];
        allIndexes[2] = Arrays.asList(7,7,7,7, 1, 9,9,9,9, 1, 7,7,7, 6, 7,7,7,7, 8,8,8,8,8, 7,7,7,7);
        Map<Integer,Boolean> breakIndexes = new HashMap<Integer,Boolean>();
        for (int i = 0; i < allIndexes[0].size(); i++) {
            breakIndexes.put(i, true);
        }
        TibetanStringChunker.filterQuatrains(breakIndexes, allIndexes);
        assertThat(breakIndexes.values(), contains(false, false, false, true, true, false, false, false, true, true, true, true, true, true, false, false, false, true, false, false, false, true, true, false, false, false));
        // test small grouping
        allIndexes = new List[3];
        allIndexes[0] = Arrays.asList(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17);
        allIndexes[1] = allIndexes[0];
        allIndexes[2] = Arrays.asList(2,2, 3, 2,1,2, 4, 4, 2,2,2,2,2,2,2,2, 2,2,2);
        breakIndexes = new HashMap<Integer,Boolean>();
        for (int i = 0; i < allIndexes[0].size(); i++) {
            breakIndexes.put(i, true);
        }
        TibetanStringChunker.filterSmalls(breakIndexes, allIndexes, 3);
        assertThat(breakIndexes.values(), contains(false, true, true, false, false, true, true, true, false, false, false, false, false, false, false, true, false, false));
    }
}
