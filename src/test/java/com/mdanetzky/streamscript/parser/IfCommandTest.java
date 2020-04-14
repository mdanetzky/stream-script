package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IfCommandTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void producesContentIfTrue() throws Exception {
        String script = "{if:true;}text{/if}";
        Source<ByteString, NotUsed> source =
                ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("text", output);
    }

    @Test
    public void doesNotProduceContentIfFalse() throws Exception {
        String script = "{if:false;}text{/if}";
        Source<ByteString, NotUsed> source =
                ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("", output);
    }

    @Test
    public void withoutSemicolon() throws Exception {
        String script = "{if:true}text{/if}";
        Source<ByteString, NotUsed> source =
                ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("text", output);
    }

    @Test
    public void stringComparison() throws Exception {
        Map<String, Object> context = HashMap.empty();
        context = context.put("variable", "test_value");
        String script = "{if: variable != 'test_value1'}text{/if}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("text", output);
    }

    @Test
    public void ifVariableExists() throws Exception {
        Map<String, Object> context = HashMap.empty();
        context = context.put("variable", "test_value");
        String script = "{if:!!typeof(variable) !== undefined}text{/if}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("text", output);
    }

    @Test
    public void ifVariableDoesNotExist() throws Exception {
        String script = "{if:typeof(variable) === undefined}text{/if}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("", output);
    }

    @Test
    public void ifVariableIsNull() throws Exception {
        Map<String, Object> context = HashMap.empty();
        context = context.put("variable", null);
        String script = "{if: variable != null}text{/if}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("", output);
    }

    @Test
    public void throwExceptionIfOutcomeIsNotBoolean() throws Exception {
        expectedException.expectMessage("instead of a boolean value");
        String script = "{if:'some_string';}text{/if}";
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, HashMap.empty());
        ScriptTestTools.materializeSource(source);
    }
}
