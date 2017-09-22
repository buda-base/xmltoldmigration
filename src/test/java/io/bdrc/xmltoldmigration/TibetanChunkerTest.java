package io.bdrc.xmltoldmigration;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.Matchers.*;
import io.bdrc.xmltoldmigration.xml2files.TibetanStringChunker;

public class TibetanChunkerTest {

    @Test
    public void test1() {
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
        res = TibetanStringChunker.getAllBreakingCharsIndexes("xxx།། །། ༆ ། །xxx");
        assertThat(res[0], contains(9));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("xxx༎ ༎༆ ༎xxx");
        assertThat(res[0], contains(6));
        res = TibetanStringChunker.getAllBreakingCharsIndexes("སྤྱི་ལོ་༢༠༡༧ ཟླ་༡ ཚེས་༡༤ ཉིན་ལ་བྲིས་པ་དགེ");
        assertTrue(res[0].isEmpty());
        res = TibetanStringChunker.getAllBreakingCharsIndexes("བཀྲིས་ ༼བཀྲ་ཤིས༽ ངའི་གྲོགས་པོ་རེད།");
        assertTrue(res[0].isEmpty());
        res = TibetanStringChunker.getAllBreakingCharsIndexes("ག གི གྲ ཀ ཤ པ མ");
        System.out.println(Arrays.toString(res[0].toArray()));
        assertThat(res[0], contains(2, 5, 10, 12));
    }
}
