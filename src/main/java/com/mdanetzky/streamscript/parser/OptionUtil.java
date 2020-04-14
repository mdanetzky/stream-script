package com.mdanetzky.streamscript.parser;

import io.vavr.control.Option;

public class OptionUtil {
    public static <T> T orNull(Option<T> option) {
        if (option instanceof Option.None) {
            return null;
        }
        return option.get();
    }
}
