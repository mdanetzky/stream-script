package com.mdanetzky.streamscript.integration.xmlgenerator;

import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class XsdValidator {

    static Future<?> startValidation(String xsd, MultiChunkStream reader, ExceptionHolder exceptionHolder) {
        return Executors.newSingleThreadExecutor().submit(
                () -> runValidation(xsd, reader, exceptionHolder));
    }

    private static void runValidation(String xsd, MultiChunkStream reader, ExceptionHolder exceptionHolder) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
            Validator validator = schema.newValidator();
            Source xmlSource = new StreamSource(reader);
            validator.validate(xmlSource);
        } catch (SAXException | IOException e) {
            exceptionHolder.setException(e);
        }
    }
}
