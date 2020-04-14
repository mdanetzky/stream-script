package com.mdanetzky.streamscript.parser;

import io.vavr.collection.Map;

import java.util.stream.Collectors;

public class CommandUtil {

    public static String mapToString(Map<String, Object> map) {
        return map.toJavaMap().keySet().stream()
                .map(key -> key + " = " + Context.get(map, key))
                .collect(Collectors.joining("\n"));
    }
}
