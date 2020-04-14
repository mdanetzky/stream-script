package com.mdanetzky.streamscript.integration.xmlgenerator;

import java.io.InputStream;

public interface XmlFileGeneratorResponse {
    InputStream getInputStream();

    void abort(String cause);

}
