package com.mdanetzky.streamscript.parser;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ScriptParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        expectedException.expectMessage(
                "Closing tag:\"{/ver}\" does not match opening tag:\"{var:variableName}\"");
        String script = "{var:variableName}{/ver}";
        ScriptParser.parseToElements(script);
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
