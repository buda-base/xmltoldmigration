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
        // building fake entries
        List<Integer>[] allIndexes = new List[3];
        allIndexes[0] = Arrays.asList(1, 2, 3, 4, 5, 6);
        allIndexes[1] = Arrays.asList(1, 2, 3, 4, 5, 6);
        allIndexes[2] = Arrays.asList(1, 9, 9, 9, 9, 6, 7);
        Map<Integer,Boolean> breakIndexes = new HashMap<Integer,Boolean>();
        for (int i = 0; i < 6; i++) {
            breakIndexes.put(i, true);
        }
        TibetanStringChunker.filterQuatrains(breakIndexes, allIndexes);
        assertThat(breakIndexes.values(), contains(true, false, false, false, true, true));
    }
}
