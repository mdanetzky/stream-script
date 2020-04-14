package com.mdanetzky.streamscript;

import com.mdanetzky.streamscript.integration.xmlgenerator.XmlGGeneratorDbConnection;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class TestUtil {

    public static String getStringFromStream(InputStream stream) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
        return writer.toString();
    }

    public static XmlGGeneratorDbConnection getH2() {
        XmlGGeneratorDbConnection dbConnection = new XmlGGeneratorDbConnection();
        dbConnection.setDatabase(XmlGGeneratorDbConnection.Database.H2);
        dbConnection.setUrl("jdbc:h2:mem:test-generator;DB_CLOSE_DELAY=-1");
        return dbConnection;
    }
}
