package com.mdanetzky.streamscript.parser.commands;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.Context;
import com.mdanetzky.streamscript.parser.annotations.Command;
import com.mdanetzky.streamscript.parser.annotations.CommandParameters;
import com.mdanetzky.streamscript.parser.annotations.StreamSource;
import io.vavr.collection.Map;

import java.util.Arrays;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Command("")
public class PlainText {

    private static final String TRIM_STATIC_XML_PARAMETER = "trim.static.xml";
    private String value;
    private String formattedValue;

    private static String trimStaticXml(String xml) {
        return Arrays.stream(xml.split("\n"))
                .map(String::trim)
                .collect(Collectors.joining());
    }

    @CommandParameters
    public void setCode(String value) {
        this.value = value;
    }

    @StreamSource
    public Source<ByteString, NotUsed> toStream(Map<String, Object> context) {
        ByteString output = ByteString.fromString(getValue(context));
        return Source.single(output);
    }

    private String getValue(Map<String, Object> context) {
        if (formattedValue == null) {
            if (Context.get(context, TRIM_STATIC_XML_PARAMETER).equalsIgnoreCase("true")) {
                formattedValue = trimStaticXml(value);
            } else {
                formattedValue = value;
            }
        }
        return formattedValue;
    }
}
