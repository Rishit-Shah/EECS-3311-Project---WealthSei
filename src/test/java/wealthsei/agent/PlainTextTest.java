package wealthsei.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlainTextTest {

    @Test
    void boldMarksAreRemovedButTheWordsStay() {
        assertEquals("You spent $435.14 on dining.", PlainText.of("You spent **$435.14** on **dining**."));
        assertEquals("Spent a lot.", PlainText.of("__Spent__ a lot."));
    }

    @Test
    void headingsBackticksAndStarBulletsAreTidied() {
        assertEquals("What happened:\n- one\n- two", PlainText.of("## What happened:\n* one\n* two"));
        assertEquals("Use the forecast tool.", PlainText.of("Use the `forecast` tool."));
    }

    @Test
    void plainTextIsLeftAlone() {
        assertEquals("Weekend days averaged 36.55 (1.7 times). 59% saved.", PlainText.of("Weekend days averaged 36.55 (1.7 times). 59% saved."));
    }
}
