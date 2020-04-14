package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.ScriptExecutorException;
import com.mdanetzky.streamscript.ScriptParserException;
import io.vavr.collection.Map;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class CommandContainer {

    private final CommandClassDescriptor commandClassDescriptor;
    private final String code;
    private final List<CommandContainer> subCommands = new ArrayList<>();
    private Object command;

    CommandContainer(CommandClassDescriptor commandClassDescriptor, String code) {
        this.commandClassDescriptor = commandClassDescriptor;
        this.code = code;
    }

    private static void rethrowExceptionOfTypeIfPresent(Exception e,
                                                        Class<? extends RuntimeException> exceptionClass) {
        if (exceptionClass.isInstance(e.getCause())) {
            throw exceptionClass.cast(e.getCause());
        }
    }

    private static Source<ByteString, NotUsed> sourceFrom(List<CommandContainer> commands,
                                                          Map<String, Object> context) {
        Source<ByteString, NotUsed> source = Source.empty();
        for (CommandContainer command : commands) {
            Source<ByteString, NotUsed> subSource = command.toStream(context);
            source = source.concat(subSource);
        }
        return source;
    }

    List<CommandContainer> getSubCommands() {
        return subCommands;
    }

    public Object getCommand() {
        if (command == null) {
            createCommandObject();
        }
        return command;
    }

    void createCommandObject() {
        try {
            command = commandClassDescriptor.createCommand(code);
            setSourceFromChildren();
        } catch (InvocationTargetException e) {
            rethrowExceptionOfTypeIfPresent(e, ScriptParserException.class);
            throw new RuntimeException(e);
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void setSourceFromChildren() throws IllegalAccessException, InvocationTargetException {
        if (commandClassDescriptor.getSourceFromChildrenMethod() != null) {
            commandClassDescriptor.getSourceFromChildrenMethod().invoke(getCommand(),
                    (Function<Map<String, Object>, Source<ByteString, NotUsed>>)
                            (ctx) -> sourceFrom(subCommands, ctx));
        }
    }

    @SuppressWarnings("unchecked")
    Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        try {
            Object sourceObject = commandClassDescriptor.getSourceMethod().invoke(getCommand(), context);
            Source<ByteString, NotUsed> source = (Source<ByteString, NotUsed>) sourceObject;
            if (commandClassDescriptor.getSourceFromChildrenMethod() == null) {
                source = Source.from(Arrays.asList(source, Source.lazySource(() -> sourceFrom(subCommands, context))))
                        .flatMapConcat((src) -> src);
            }
            return source;
        } catch (IllegalAccessException | InvocationTargetException e) {
            rethrowExceptionOfTypeIfPresent(e, ScriptExecutorException.class);
            throw new RuntimeException(e);
        }
    }
}
