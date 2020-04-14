package com.mdanetzky.streamscript.parser;

import com.mdanetzky.streamscript.ScriptParserException;
import com.mdanetzky.streamscript.parser.annotations.Command;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class CommandParser {

    private static final char COMMAND_BODY_DELIMITER = ':';
    private static final String COMMANDS_PACKAGE_ROOT = "com.mdanetzky";

    private static Map<String, CommandClassDescriptor> commands;

    private static Map<String, CommandClassDescriptor> getCommands() {
        if (commands == null) {
            commands = scanCommands(COMMANDS_PACKAGE_ROOT);
        }
        return commands;
    }

    static Map<String, CommandClassDescriptor> scanCommands(String packageRoot) {
        Map<String, CommandClassDescriptor> commands = new HashMap<>();
        Reflections reflections = new Reflections(packageRoot, new TypeAnnotationsScanner());
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(Command.class, true);
        for (Class<?> commandClass : commandClasses) {
            CommandClassDescriptor commandDescriptor = new CommandClassDescriptor(commandClass);
            assertOneClassPerCommand(commands, commandDescriptor);
            assertCommandParametersMethod(commandDescriptor);
            assertDefaultConstructor(commandClass);
            assertStreamSourceMethod(commandDescriptor);
            commands.put(commandDescriptor.getCommandName(), commandDescriptor);
        }
        return commands;
    }

    private static void assertOneClassPerCommand(Map<String, CommandClassDescriptor> commands, CommandClassDescriptor commandDescriptor) {
        CommandClassDescriptor existingCommand = commands.get(commandDescriptor.getCommandName());
        if (existingCommand != null) {
            throw new ScriptParserException("Classes: " + commandDescriptor.getCommandClass().getCanonicalName() +
                    " and " + commandDescriptor.getCommandClass().getCanonicalName()
                    + " implement the same command: '" + commandDescriptor.getCommandName() + "'");
        }
    }

    private static void assertStreamSourceMethod(CommandClassDescriptor commandClassDescriptor) {
        if (commandClassDescriptor.getSourceMethod() == null) {
            throw new ScriptParserException("Class " + commandClassDescriptor.getCommandClass().getCanonicalName() +
                    " is missing @StreamSource annotated method");
        }
    }

    private static void assertDefaultConstructor(Class<?> commandClass) {
        try {
            commandClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new ScriptParserException("Class " + commandClass.getCanonicalName() +
                    " is missing default constructor");
        }
    }

    private static void assertCommandParametersMethod(CommandClassDescriptor commandClassDescriptor) {
        if (commandClassDescriptor.getCommandParametersMethod() == null) {
            throw new ScriptParserException("Class " + commandClassDescriptor.getCommandClass().getCanonicalName() +
                    " is missing @CommandParameters annotated method");
        }
    }

    static CommandContainer parseScript(ScriptElement scriptElement) {
        CommandContainer command = parseCommand(scriptElement.getContent());
        command.createCommandObject();
        for (ScriptElement subElement : scriptElement.getElements()) {
            command.getSubCommands().add(parseScript(subElement));
        }
        return command;
    }

    static CommandContainer parseCommand(String commandText) {
        if (commandText.startsWith(ScriptParser.TOKEN_START) && commandText.endsWith(ScriptParser.TOKEN_END)) {
            commandText = removeCommandMarker(commandText);
            int delimiterLocation = commandText.indexOf(COMMAND_BODY_DELIMITER);
            if (delimiterLocation == -1) {
                throw new ScriptParserException("Malformed command - missing '"
                        + COMMAND_BODY_DELIMITER + "'\n" + commandText);
            }
            String prefix = commandText.substring(0, delimiterLocation);
            String code = commandText.substring(delimiterLocation + 1);
            return createCommandByPrefix(prefix, code);
        }
        return createCommandByPrefix("", commandText);
    }

    private static String removeCommandMarker(String command) {
        return command.substring(ScriptParser.TOKEN_START.length(),
                command.length() - ScriptParser.TOKEN_END.length());
    }

    private static CommandContainer createCommandByPrefix(String prefix, String code) {
        CommandClassDescriptor commandClassDescriptor = getCommands().get(prefix);
        if (commandClassDescriptor == null) {
            throw new ScriptParserException("Unknown command: '"
                    + prefix + COMMAND_BODY_DELIMITER + code + "'");
        }
        return new CommandContainer(commandClassDescriptor, code);
    }
}
