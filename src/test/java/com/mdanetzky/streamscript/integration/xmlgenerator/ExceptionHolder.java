package com.mdanetzky.streamscript.integration.xmlgenerator;

import java.util.concurrent.atomic.AtomicReference;

class ExceptionHolder {

    private final AtomicReference<Exception> error = new AtomicReference<>();

    public void setException(Exception e) {
        this.error.set(e);
    }

    void throwExceptionFromQueue() throws Exception {
        if (error.get() != null) {
            throw error.get();
        }
    }
}
