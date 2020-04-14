package com.mdanetzky.streamscript.parser.commands;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.ScriptExecutorException;
import com.mdanetzky.streamscript.parser.CommandUtil;
import com.mdanetzky.streamscript.parser.Context;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import com.mdanetzky.streamscript.parser.annotations.StreamSourceFromChildren;
import com.mdanetzky.streamscript.parser.js.JsExecutor;
import io.vavr.collection.Map;

import java.util.function.Function;

@SuppressWarnings("unused")
@Command("if")
public class IfCommand {

    private String code;
    private Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource;

    @CommandParameters
    public void setCode(String code) {
        this.code = code;
    }

    @StreamSourceFromChildren
    public void setChildrenSource(
            Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource) {
        this.childrenSource = childrenSource;
    }

    @StreamSource
    public Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        Object outcome = JsExecutor.executeWithReturn(context, code);
        if (outcome instanceof Boolean) {
            if ((Boolean) outcome) {
                return childrenSource.apply(context);
            } else {
                return Source.empty();
            }
        }
        throw new ScriptExecutorException("If command: Following code produces: ("
                + outcome.getClass().getName() + "): '" + Context.convertToString(outcome)
                + "' instead of a boolean value:\n"
                + code + "\nContext:\n" + CommandUtil.mapToString(context));
    }
}
