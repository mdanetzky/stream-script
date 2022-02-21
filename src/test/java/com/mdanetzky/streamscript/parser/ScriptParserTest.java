package com.mdanetzky.streamscript.parser;

import com.mdanetzky.streamscript.ScriptParserException;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptParserTest {

    @Test
    public void returnsSameStringIfNotParsed() {
        String script = "lfwfkowfoa\nadfsafasf\nfdsafaf<>:sdfafd";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals(script, firstElement.getContent());
    }

    @Test
    public void parsesSingleEntry() {
        String script = "{var:variableName/}";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals("{var:variableName}", firstElement.getContent());
        assertTrue(firstElement.getElements().isEmpty());
    }

    @Test
    public void parsesEmptyEnclosingEntry() {
        String script = "{var:variableName}{/var}";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals("{var:variableName}", firstElement.getContent());
        assertTrue(firstElement.getElements().isEmpty());
    }

    @Test
    public void parsesEnclosingEntryWithText() {
        String script = "{var:variableName}some_text{/var}";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals("{var:variableName}", firstElement.getContent());
        String parsedText = firstElement.getElements().get(0).getContent();
        assertEquals("some_text", parsedText);
    }

    @Test
    public void throwsUnmatchedTagException() {
        String script = "{var:variableName}{/ver}";
        ScriptParserException e = Assert.assertThrows(ScriptParserException.class, () ->
                ScriptParser.parseToElements(script));
        Assert.assertTrue(e.getMessage().contains(
                "Closing tag:\"{/ver}\" does not match opening tag:\"{var:variableName}\""));
    }

    @Test
    public void parsesSubTag() {
        String script = "{var:variableName}{js/}{/var}";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals("{js}", firstElement.getElements().get(0).getContent());
    }

    @Test
    public void parsesSubTagSurroundedByText() {
        String script = "{var:variableName}text{js/}text{/var}";
        ScriptElement firstElement = ScriptParser.parseToElements(script).getElements().get(0);
        assertEquals("{js}", firstElement.getElements().get(1).getContent());
        assertEquals(3, firstElement.getElements().size());
    }
}
