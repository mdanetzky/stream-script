package com.mdanetzky.streamscript.parser.js;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class JsExecutorTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

//    @Test
//    public void returnsString() {
//        Map<String, Object> context = HashMap.empty();
//        String val = (String) JsExecutor.execute(context, "'test_value';");
//        Assert.assertEquals("test_value", val);
//    }

    @Test
    public void throwsExceptionWithScriptInMessage() {
        expectedException.expectMessage(CoreMatchers.containsString("return 'test_value';"));
        Map<String, Object> context = HashMap.empty();
        JsExecutor.execute(context, "return 'test_value';");
    }

    @Test
    public void setsVariableInContext() {
        Map<String, Object> context = HashMap.empty();
        context = JsExecutor.execute(context, "variable = 'test_value';");
        Assert.assertEquals("test_value", context.get("variable").get());
    }

    @Test
    public void throwsExceptionWithContextInMessage() {
        expectedException.expectMessage(CoreMatchers.containsString("variable_value"));
        Map<String, Object> context = HashMap.empty();
        context = context.put("variable", "variable_value");
        JsExecutor.execute(context, "return 'test_value';");
    }

//    @Test
//    public void getsBoolean() {
//        Map<String, Object> context = HashMap.empty();
//        Boolean trueVal = (Boolean) JsExecutor.execute(context, "if(true)" +
//                " true; " +
//                " else " +
//                " false;");
//        Assert.assertTrue(trueVal);
//    }

    @Test
    public void throwsExceptionOnWrongJsSyntax() {
        expectedException.expectMessage("for (;");
        JsExecutor.execute(HashMap.empty(), "for (;");
    }
}
