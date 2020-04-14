package com.mdanetzky.streamscript.parser.commands;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.Context;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import io.vavr.collection.Map;

@SuppressWarnings("unused")
@Command("var")
public class VarCommand {

    private String code;

    @CommandParameters
    public void setCode(String code) {
        this.code = code;
    }

    @StreamSource
    public Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        return Source.single(ByteString.fromString(Context.get(context, code)));
    }

    public Object value(Map<String, Object> context) {
        return context.get(code).get();
    }

}
