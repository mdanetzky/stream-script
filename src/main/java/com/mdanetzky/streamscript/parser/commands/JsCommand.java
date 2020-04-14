package com.mdanetzky.streamscript.parser.commands;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import com.mdanetzky.streamscript.parser.annotations.StreamSourceFromChildren;
import com.mdanetzky.streamscript.parser.js.JsExecutor;
import io.vavr.collection.Map;

import java.util.function.Function;

@SuppressWarnings("unused")
@Command("js")
public class JsCommand {

    private String code;
    private Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource;

    @CommandParameters
    public void setCode(String code) {
        this.code = code;
    }

    @StreamSourceFromChildren
    public void setChildrenSource(Function<Map<String, Object>, Source<ByteString, NotUsed>> childrenSource) {
        this.childrenSource = childrenSource;
    }

    @StreamSource
    public Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        context = JsExecutor.execute(context, code);
        return childrenSource.apply(context);
    }
}
