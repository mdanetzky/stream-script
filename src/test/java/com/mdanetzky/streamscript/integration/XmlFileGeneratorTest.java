package com.mdanetzky.streamscript.integration;

import akka.Done;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import com.mdanetzky.streamscript.Resources;
import com.mdanetzky.streamscript.ScriptParserException;
import com.mdanetzky.streamscript.TestUtil;
import com.mdanetzky.streamscript.integration.xmlgenerator.XmlFileGenerator;
import com.mdanetzky.streamscript.integration.xmlgenerator.XmlFileGeneratorImpl;
import com.mdanetzky.streamscript.integration.xmlgenerator.XmlFileGeneratorResponse;
import com.mdanetzky.streamscript.parser.ScriptTestTools;
import com.typesafe.config.Config;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import scala.util.Try;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class XmlFileGeneratorTest {

    private static final String[] DB_SETUP = {
            "create table DATA (ID integer NOT NULL, TEXT varchar(50) NOT NULL);",
            "INSERT INTO DATA VALUES(1, 'first');",
            "INSERT INTO DATA VALUES(2, 'second');",
            "INSERT INTO DATA VALUES(3, 'third');"
    };

    @BeforeClass
    public static void initDb() throws ExecutionException, InterruptedException {
        Config config = XmlFileGeneratorImpl.getConfig(TestUtil.getH2());
        SlickSession slickSession = SlickSession.forConfig(config);
        ScriptTestTools.executeSql(slickSession, DB_SETUP);
        slickSession.close();
    }

    @Test
    public void runsSimpleScript() throws Exception {
        String script = Resources.read("/simpleXmlScript");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        Map<String, Object> context = new HashMap<>();
        XmlFileGeneratorResponse response = generator.runScript(script, context);
        String output = TestUtil.getStringFromStream(response.getInputStream());
        Assert.assertTrue(output.contains("</Dataset>"));
    }

    @Test
    public void validatesGoodScript() {
        String script = Resources.read("/simpleXmlScript");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.validateScript(script);
    }

    @Test
    public void failsOnErroneousScript() {
        String script = "{noSuchCommand:/}";
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        ScriptParserException e = Assert.assertThrows(ScriptParserException.class, () ->
                generator.validateScript(script));
        Assert.assertTrue(e.getMessage().contains("Unknown command: 'noSuchCommand:'"));
    }

    @Test
    public void runsSimpleDbScript() throws Exception {
        String script = Resources.read("/simpleXmlFromDb");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        Map<String, Object> context = new HashMap<>();
        context.put("trim.static.xml", "true");
        XmlFileGeneratorResponse response = generator.runScript(script, context);
        String output = TestUtil.getStringFromStream(response.getInputStream());
        Assert.assertTrue(output.contains("<Text>third</Text>"));
    }

    @Test
    public void abortsScript() {
        String script = Resources.read("/simpleXmlFromDb");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        XmlFileGeneratorResponse response = generator.runScript(script, new HashMap<>());
        response.abort("test cause");
        IOException e = Assert.assertThrows(IOException.class, () ->
                TestUtil.getStringFromStream(response.getInputStream()));
        Assert.assertTrue(e.getMessage().contains("test cause"));
    }

    @Test
    public void runsDbExceptionScript() {
        String script = Resources.read("/exceptionFromDb");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        XmlFileGeneratorResponse response = generator.runScript(script, new HashMap<>());
        IOException e = Assert.assertThrows(IOException.class, () ->
                TestUtil.getStringFromStream(response.getInputStream()));
        Assert.assertTrue(e.getMessage().contains("wrong_sql_query"));
    }

    @Test
    public void runsXsdExceptionScript() {
        String script = Resources.read("/exceptionFromXsdValidator");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        XmlFileGeneratorResponse response = generator.runScript(script, new HashMap<>());
        IOException e = Assert.assertThrows(IOException.class, () ->
                TestUtil.getStringFromStream(response.getInputStream()));
        Assert.assertTrue(e.getMessage().contains("UnexpectedTag"));
    }

    @Test
    public void compressesXml() throws Exception {
        String script = Resources.read("/simpleXmlScript");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        Map<String, Object> context = new HashMap<>();
        context.put("trim.static.xml", "true");
        XmlFileGeneratorResponse response = generator.runScript(script, context);
        String output = TestUtil.getStringFromStream(response.getInputStream());
        Assert.assertFalse(output.contains("\n"));
    }

    @Test
    public void onCompleteIsCalledOnErrorInCompilePhase() throws Exception {
        CountDownLatch streamComplete = new CountDownLatch(1);
        BiConsumer<Try<Done>, Map<String, Object>> markStreamComplete =
                (Try<Done> done, Map<String, Object> ctx) -> streamComplete.countDown();
        Map<String, Object> context = new HashMap<>();
        context.put(XmlFileGeneratorImpl.ON_COMPLETE_PARAMETER, markStreamComplete);
        try {
            String script = "{bad script}";
            String xsd = Resources.read("/simpleXml.xsd");
            XmlFileGenerator generator = new XmlFileGeneratorImpl();
            generator.setXsd(xsd);
            generator.setDbConnection(TestUtil.getH2());
            XmlFileGeneratorResponse response = generator.runScript(script, context);
            TestUtil.getStringFromStream(response.getInputStream());
        } catch (Throwable t) {
            // finally is where the test takes place
        } finally {
            Assert.assertTrue(streamComplete.await(1, TimeUnit.SECONDS));
        }
    }

    @Test
    public void onCompleteIsCalledOnTimeoutInStream() throws Exception {
        CountDownLatch streamComplete = new CountDownLatch(1);
        BiConsumer<Try<Done>, Map<String, Object>> onStreamComplete =
                (Try<Done> done, Map<String, Object> ctx) -> streamComplete.countDown();
        Map<String, Object> context = new HashMap<>();
        context.put(XmlFileGeneratorImpl.ON_COMPLETE_PARAMETER, onStreamComplete);
        context.put(XmlFileGeneratorImpl.TIMEOUT_PARAMETER, "1");
        String script = Resources.read("/waitXmlScript");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        XmlFileGeneratorResponse response = generator.runScript(script, context);
        try {
            TestUtil.getStringFromStream(response.getInputStream());
        } catch (IOException e) {
            // do nothing, because finally is where the test takes place
        } finally {
            Assert.assertTrue(streamComplete.await(13, TimeUnit.SECONDS));
        }
    }

    @Test
    public void throwsExceptionOnTimeout() {
        Map<String, Object> context = new HashMap<>();
        context.put(XmlFileGeneratorImpl.TIMEOUT_PARAMETER, "1");
        String script = Resources.read("/waitXmlScript");
        String xsd = Resources.read("/simpleXml.xsd");
        XmlFileGenerator generator = new XmlFileGeneratorImpl();
        generator.setXsd(xsd);
        generator.setDbConnection(TestUtil.getH2());
        XmlFileGeneratorResponse response = generator.runScript(script, context);
        IOException e = Assert.assertThrows(IOException.class, () ->
                TestUtil.getStringFromStream(response.getInputStream()));
        Assert.assertTrue(e.getMessage().contains("Timeout"));
    }
}
