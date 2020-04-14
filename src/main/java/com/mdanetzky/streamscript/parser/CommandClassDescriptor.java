package com.mdanetzky.streamscript.parser;

import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import com.mdanetzky.streamscript.parser.annotations.StreamSourceFromChildren;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class CommandClassDescriptor {

    private final Class<?> commandClass;
    private final Method commandParametersMethod;
    private final Method sourceMethod;
    private final Method sourceFromChildrenMethod;

    CommandClassDescriptor(Class<?> commandClass) {
        this.commandClass = commandClass;
        this.commandParametersMethod = getAnnotatedMethod(CommandParameters.class);
        this.sourceMethod = getAnnotatedMethod(StreamSource.class);
        this.sourceFromChildrenMethod = getAnnotatedMethod(StreamSourceFromChildren.class);
    }

    String getCommandName() {
        Command commandAnnotation = this.commandClass.getAnnotation(Command.class);
        if (commandAnnotation == null) {
            return null;
        }
        return commandAnnotation.value();
    }

    private Method getAnnotatedMethod(Class<? extends Annotation> annotation) {
        for (final Method method : this.commandClass.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                return method;
            }
        }
        return null;
    }

    Object createCommand(String code)
            throws InstantiationException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        Object command = this.getCommandClass().getConstructor().newInstance();
        this.getCommandParametersMethod().invoke(command, code);
        return command;
    }

    Class<?> getCommandClass() {
        return commandClass;
    }

    Method getCommandParametersMethod() {
        return commandParametersMethod;
    }

    Method getSourceMethod() {
        return sourceMethod;
    }

    Method getSourceFromChildrenMethod() {
        return sourceFromChildrenMethod;
    }
}
