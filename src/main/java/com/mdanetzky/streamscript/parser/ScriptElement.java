package com.mdanetzky.streamscript.parser;

import java.util.ArrayList;
import java.util.List;

class ScriptElement {

    private final List<ScriptElement> elements = new ArrayList<>();
    private String content = "";

    String getContent() {
        return content;
    }

    void setContent(String content) {
        this.content = content;
    }

    List<ScriptElement> getElements() {
        return elements;
    }

}
