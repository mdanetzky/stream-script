package com.mdanetzky.streamscript.parser;

import com.mdanetzky.streamscript.parser.commands.PlainText;
import com.mdanetzky.streamscript.parser.commands.VarCommand;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandParserTest {

    @Test
    public void parsesOneElementFromScriptParser() {
        String script = "{var:variableName/}";
        CommandContainer command = ScriptParser.parse(script);
        assertTrue(command.getSubCommands().get(0).getCommand() instanceof VarCommand);
    }

    @Test
    public void parsesTwoElementsFromScriptParser() {
        String script = "someText{var:variableName/}";
        CommandContainer command = ScriptParser.parse(script);
        assertTrue(command.getSubCommands().get(0).getCommand() instanceof PlainText);
        assertTrue(command.getSubCommands().get(1).getCommand() instanceof VarCommand);
    }

    @Test
    public void throwsExceptionOnUnknownDataTypeInQueryResults() {
        String script = "{query:select;field|unknown_Data_Type/}";
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                ScriptParser.parse(script));
        Assert.assertTrue(e.getMessage().contains("'unknown_data_type'"));
    }

    @Test
    public void throwsOnMissingCommandParameters() {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                CommandParser.scanCommands("test_commands.missing_command_code"));
        Assert.assertTrue(e.getMessage().contains("is missing @CommandParameters annotated method"));
    }

    @Test
    public void throwsOnTwoCommandsForTheSameName() {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                CommandParser.scanCommands("test_commands.two_with_same_name"));
        Assert.assertTrue(e.getMessage().contains(" implement the same command: 'sameCommand'"));
    }

    @Test
    public void throwsOnMissingSourceInCommand() {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                CommandParser.scanCommands("test_commands.missing_source"));
        Assert.assertTrue(e.getMessage().contains("missing @StreamSource annotated method"));
    }

    @Test
    public void throwsOnMissingDefaultConstructorInCommand() {
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                CommandParser.scanCommands("test_commands.no_default_constructor"));
        Assert.assertTrue(e.getMessage().contains("is missing default constructor"));
    }

    @Test
    public void returnsStringIfNotParsed() throws Exception {
        io.vavr.collection.Map<String, Object> context = io.vavr.collection.HashMap.empty();
        String commandWithParams = "Regular String";
        CommandContainer command = CommandParser.parseCommand(commandWithParams);
        String output = ScriptTestTools.materializeSource(command.toStream(context));
        assertEquals(commandWithParams, output);
    }

    @Test
    public void throwsExceptionOnUnknownCommand() {
        String commandWithParams = "{unknown_command:params}";
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                CommandParser.parseCommand(commandWithParams));
        Assert.assertTrue(e.getMessage().contains("Unknown command"));
    }

    @Test
    public void parsesContextVariable() throws Exception {
        io.vavr.collection.Map<String, Object> context = io.vavr.collection.HashMap.empty();
        context = context.put("variable", "variable_value");
        String commandWithParams = "{var:variable}";
        CommandContainer command = CommandParser.parseCommand(commandWithParams);
        String output = ScriptTestTools.materializeSource(command.toStream(context));
        assertEquals("variable_value", output);
    }
}
