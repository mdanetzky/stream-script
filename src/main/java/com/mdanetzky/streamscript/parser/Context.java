package com.mdanetzky.streamscript.parser;

import io.vavr.collection.Map;
import io.vavr.control.Option;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Context {

    public static String get(Map<String, Object> context, String key) {
        Option<Object> value = context.get(key);
        return convertToString(value);
    }

    public static String convertToString(Option<Object> object) {
        if (object instanceof Option.None) {
            return "null";
        }
        return convertToString(object.get());
    }

    public static String convertToString(Object object) {
        if (object instanceof Date) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            return dateFormat.format((Date) object);
        }
        return String.valueOf(object);
    }
}
