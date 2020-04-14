package com.mdanetzky.streamscript.knowlege;

import com.mdanetzky.streamscript.Resources;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;

public class XsdValidationTest {

    private static final String TEST_XML =
            "<?xml version=\"1.0\"?><Data><Dataset><Id>1</Id><Text>text</Text>\n</Dataset></Data>";
    private static final String WRONG_ID_FORMAT_XML =
            "<?xml version=\"1.0\"?><Data><Dataset><Id>Wrong ID</Id><Text>text</Text></Dataset></Data>";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void validatesAgainstXsd() throws SAXException, IOException {
        String xsd = Resources.read("/simpleXml.xsd");
        Source xmlFile = new StreamSource(new StringReader(TEST_XML));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

    @Test
    public void failsAgainstMalformedXsd() throws SAXException, IOException {
        expectedException.expectMessage("'Wrong ID' is not a valid value for 'integer'");
        String xsd = Resources.read("/simpleXml.xsd");
        Source xmlFile = new StreamSource(new StringReader(WRONG_ID_FORMAT_XML));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(new StreamSource(new StringReader(xsd)));
        Validator validator = schema.newValidator();
        validator.validate(xmlFile);
    }

}
