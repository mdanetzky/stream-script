package com.mdanetzky.streamscript.parser.js;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Assert;
import org.junit.Test;

public class JsExecutorTest {

//    @Test
//    public void returnsString() {
//        Map<String, Object> context = HashMap.empty();
//        String val = (String) JsExecutor.execute(context, "'test_value';");
//        Assert.assertEquals("test_value", val);
//    }

    @Test
    public void throwsExceptionWithScriptInMessage() {
        Map<String, Object> context = HashMap.empty();
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                JsExecutor.execute(context, "return 'test_value';"));
        Assert.assertTrue(e.getMessage().contains("return 'test_value';"));
    }

    @Test
    public void setsVariableInContext() {
        Map<String, Object> context = HashMap.empty();
        context = JsExecutor.execute(context, "variable = 'test_value';");
        Assert.assertEquals("test_value", context.get("variable").get());
    }

    @Test
    public void throwsExceptionWithContextInMessage() {
        Map<String, Object> context = HashMap.empty();
        final Map<String, Object> final_context = context.put("variable", "variable_value");
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                JsExecutor.execute(final_context, "return 'test_value';"));
        Assert.assertTrue(e.getMessage().contains("variable_value"));
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
        RuntimeException e = Assert.assertThrows(RuntimeException.class, () ->
                JsExecutor.execute(HashMap.empty(), "for (;"));
        Assert.assertTrue(e.getMessage().contains("for (;"));
    }
}
