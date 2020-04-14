package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import io.vavr.collection.HashMap;
import org.junit.Assert;
import org.junit.Test;

public class JsCommandTest {

    @Test
    public void setsAndOutputsVariable() throws Exception {
        String script = "{js: variable = 'test';}{var:variable/}{/js}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("test", output);
    }
}
