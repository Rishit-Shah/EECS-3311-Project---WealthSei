package wealthsei.llm;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesObjectsArraysAndScalars() {
        Map<String, Object> o = Json.parseObject(" {\"a\": 1.50, \"b\": [true, false, null, \"x\"], \"c\": {\"d\": -2}} ");
        assertEquals(new BigDecimal("1.50"), o.get("a"));
        assertEquals(List.of(true, false), ((List<?>) o.get("b")).subList(0, 2));
        assertNull(((List<?>) o.get("b")).get(2));
        assertEquals(new BigDecimal("-2"), ((Map<?, ?>) o.get("c")).get("d"));
    }

    @Test
    void parsesEscapes() {
        Map<String, Object> o = Json.parseObject("{\"t\": \"line\\nbreak \\\"q\\\" \\u00e9 \\\\ \\/\"}");
        assertEquals("line\nbreak \"q\" \u00e9 \\ /", o.get("t"));
    }

    @Test
    void writesWhatItReads() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("name", "a \"b\"\n");
        o.put("n", new BigDecimal("12.30"));
        o.put("list", List.of(1, true));
        o.put("nothing", null);
        String text = Json.write(o);
        assertEquals("{\"name\":\"a \\\"b\\\"\\n\",\"n\":12.30,\"list\":[1,true],\"nothing\":null}", text);
        Map<String, Object> back = Json.parseObject(text);
        assertEquals("a \"b\"\n", back.get("name"));
    }

    @Test
    void controlCharactersAreEscaped() {
        assertEquals("\"\\u0001\"", Json.write("\u0001"));
    }

    @Test
    void badInputIsRejectedWithAPosition() {
        for (String bad : new String[] {"", "{", "{\"a\" 1}", "[1,]", "{\"a\":1} x", "\"open", "tru", "{\"a\":\"\\q\"}", "{1:2}"}) {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Json.parse(bad));
            assertTrue(e.getMessage().startsWith("Invalid JSON"));
        }
    }

    @Test
    void parseObjectInsistsOnAnObject() {
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1]"));
    }

    @Test
    void emptyContainers() {
        assertTrue(Json.parseObject("{}").isEmpty());
        assertEquals(List.of(), Json.parse("[ ]"));
    }

    @Test
    void unsupportedTypesCannotBeWritten() {
        assertThrows(IllegalArgumentException.class, () -> Json.write(new Object()));
    }
}
