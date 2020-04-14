package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StreamScriptTest {

    @Test
    public void createsPureTextSource() throws Exception {
        String script = "text";
        Map<String, Object> context = HashMap.empty();
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        assertEquals("text", output);
    }

    @Test
    public void trimsStaticContentIfSet() throws Exception {
        String script = "  untrimmed static content  \n";
        Map<String, Object> context = HashMap.empty();
        context = context.put("trim.static.xml", "true");
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("untrimmed static content", output);
    }

    @Test
    public void doesntTrimStaticContentByDefault() throws Exception {
        String script = "  untrimmed static content  ";
        Map<String, Object> context = HashMap.empty();
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("  untrimmed static content  ", output);
    }

    @Test
    public void materializesSimpleSource() throws Exception {
        Source<ByteString, NotUsed> source = Source.single(ByteString.fromString("test"));
        String output = ScriptTestTools.materializeSource(source);
        assertEquals("test", output);
    }

    @Test
    public void materializesConcatenatedSource() throws Exception {
        Source<ByteString, NotUsed> source = Source.empty();
        source = source.concat(Source.single(ByteString.fromString("test")));
        String output = ScriptTestTools.materializeSource(source);
        assertEquals("test", output);
    }
}
