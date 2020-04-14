package com.mdanetzky.streamscript.parser.js;

import io.vavr.collection.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Set;
import java.util.stream.Collectors;

public class JsExecutor {

    private final static ScriptEngineManager MANAGER = new ScriptEngineManager();

    public static Map<String, Object> execute(Map<String, Object> context, String script) {
        ScriptEngine engine = MANAGER.getEngineByName("nashorn");
        try (GlobalVavrMap globals = new GlobalVavrMap(context)) {
            engine.eval(script, globals);
            globals.close();
            return copy(globals.entrySet(), context);
        } catch (ScriptException e) {
            throw wrapException(context, script, e);
        }
    }

    private static Map<String, Object> copy(Set<java.util.Map.Entry<String, Object>> entries,
                                            Map<String, Object> context) {
        for (java.util.Map.Entry<String, Object> entry : entries) {
            context = context.put(entry.getKey(), entry.getValue());
        }
        return context;
    }

    private static String map2String(Map<String, Object> map) {
        return map.toJavaMap().keySet().stream()
                .map(key -> key + " = " + map.get(key))
                .collect(Collectors.joining("\n"));
    }

    public static Object executeWithReturn(Map<String, Object> context, String code) {
        ScriptEngine engine = MANAGER.getEngineByName("nashorn");
        try (GlobalVavrMap globals = new GlobalVavrMap(context)) {
            return engine.eval(code, globals);
        } catch (ScriptException e) {
            throw wrapException(context, code, e);
        }
    }

    private static RuntimeException wrapException(Map<String, Object> context, String code, ScriptException e) {
        String message = e.getMessage() +
                "\n\nJS Exception in:\n" + code + "\nContext:\n" + map2String(context) + "\n";
        return new RuntimeException(message, e);
    }
}
