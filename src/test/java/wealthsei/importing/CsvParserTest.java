package wealthsei.importing;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvParserTest {

    @Test
    void readsHeaderAndRecords() {
        CsvParser p = CsvParser.parse("Date,Description,Amount\n2026-10-01,Coffee,-3.50\n2026-10-02,Pay,100\n");
        assertEquals(List.of("Date", "Description", "Amount"), p.header());
        assertEquals(2, p.records().size());
        assertEquals("Coffee", p.records().get(0).get(1));
        assertEquals("100", p.records().get(1).get(2));
    }

    @Test
    void quotedFieldsKeepCommasAndDoubledQuotes() {
        CsvParser p = CsvParser.parse("a,b\n\"Smith, John\",\"He said \"\"hi\"\"\"\n");
        assertEquals("Smith, John", p.records().get(0).get(0));
        assertEquals("He said \"hi\"", p.records().get(0).get(1));
    }

    @Test
    void lineBreakInsideQuotesStaysInTheField() {
        CsvParser p = CsvParser.parse("a,b\n\"line1\nline2\",x\nnext,y\n");
        assertEquals("line1\nline2", p.records().get(0).get(0));
        assertEquals(2, p.records().get(0).lineNumber());
        assertEquals(4, p.records().get(1).lineNumber());
    }

    @Test
    void handlesCrLfBomAndBlankLines() {
        CsvParser p = CsvParser.parse("﻿a,b\r\n\r\n1,2\r\n\r\n3,4");
        assertEquals(List.of("a", "b"), p.header());
        assertEquals(2, p.records().size());
        assertEquals("4", p.records().get(1).get(1));
    }

    @Test
    void shortRowsReadAsEmptyStrings() {
        CsvParser p = CsvParser.parse("a,b,c\n1,2\n");
        assertEquals("2", p.records().get(0).get(1));
        assertEquals("", p.records().get(0).get(2));
        assertEquals("", p.records().get(0).get(9));
    }

    @Test
    void valuesAreTrimmed() {
        CsvParser p = CsvParser.parse(" a , b \n  x  ,  y  \n");
        assertEquals(List.of("a", "b"), p.header());
        assertEquals("x", p.records().get(0).get(0));
    }

    @Test
    void emptyTextHasNoHeader() {
        assertTrue(CsvParser.parse("").header().isEmpty());
        assertTrue(CsvParser.parse("\n\n  \n").header().isEmpty());
    }

    @Test
    void unterminatedQuoteIsAnError() {
        CsvFormatException e = assertThrows(CsvFormatException.class, () -> CsvParser.parse("a,b\n\"oops,1\n"));
        assertTrue(e.getMessage().contains("line 2"));
    }

    @Test
    void lastRecordWithoutTrailingNewlineIsKept() {
        assertEquals(1, CsvParser.parse("a\nx").records().size());
    }
}
