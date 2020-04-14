package com.mdanetzky.streamscript.integration.xmlgenerator;

import java.io.InputStream;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class XmlFileGeneratorResponseImpl implements XmlFileGeneratorResponse {

    private final InputStream inputStream;
    private final Consumer<String> abort;

    @SuppressWarnings("WeakerAccess")
    public XmlFileGeneratorResponseImpl(InputStream inputStream, Consumer<String> abort) {
        this.inputStream = inputStream;
        this.abort = abort;
    }

    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void abort(String cause) {
        abort.accept(cause);
    }
}
