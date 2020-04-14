package com.mdanetzky.streamscript.integration.xmlgenerator;

import java.util.Map;

public interface XmlFileGenerator {
    void setDbConnection(XmlGGeneratorDbConnection dbConnection);

    void setXsd(String xsd);

    void validateScript(String script);

    XmlFileGeneratorResponse runScript(String script, Map<String, Object> context);

}
