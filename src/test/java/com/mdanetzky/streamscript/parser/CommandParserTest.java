package com.mdanetzky.streamscript.parser;

import com.mdanetzky.streamscript.parser.commands.PlainText;
import com.mdanetzky.streamscript.parser.commands.VarCommand;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandParserTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        expectedException.expectMessage("'unknown_data_type'");
        String script = "{query:select;field|unknown_Data_Type/}";
        ScriptParser.parse(script);
    }

    @Test
    public void throwsOnMissingCommandParameters() {
        expectedException.expectMessage("is missing @CommandParameters annotated method");
        CommandParser.scanCommands("test_commands.missing_command_code");
    }

    @Test
    public void throwsOnTwoCommandsForTheSameName() {
        expectedException.expectMessage(" implement the same command: 'sameCommand'");
        CommandParser.scanCommands("test_commands.two_with_same_name");
    }

    @Test
    public void throwsOnMissingSourceInCommand() {
        expectedException.expectMessage("missing @StreamSource annotated method");
        CommandParser.scanCommands("test_commands.missing_source");
    }

    @Test
    public void throwsOnMissingDefaultConstructorInCommand() {
        expectedException.expectMessage("is missing default constructor");
        CommandParser.scanCommands("test_commands.no_default_constructor");
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
        expectedException.expectMessage("Unknown command");
        String commandWithParams = "{unknown_command:params}";
        CommandParser.parseCommand(commandWithParams);
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
