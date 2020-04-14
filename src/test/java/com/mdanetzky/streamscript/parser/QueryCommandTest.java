package com.mdanetzky.streamscript.parser;

import akka.NotUsed;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.mdanetzky.streamscript.parser.commands.QueryCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class QueryCommandTest {

    private static final String[] DB_SETUP = {
            "create table DATA (ID integer NOT NULL, NAME varchar(50) NOT NULL);",
            "INSERT INTO DATA VALUES(1, 'first');",
            "INSERT INTO DATA VALUES(2, 'second');",
            "INSERT INTO DATA VALUES(3, 'third');",
            "create table DATES (ID integer NOT NULL, TIMESTAMP datetime NOT NULL);",
            "INSERT INTO DATES VALUES(1, '2000-12-31 15:45:21');",
            "INSERT INTO DATES VALUES(2, '2010-12-31 15:45:21');",
            "INSERT INTO DATES VALUES(3, '2020-12-31 15:45:21');"
    };

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @BeforeClass
    public static void initDb() throws ExecutionException, InterruptedException {
        Config config = createH2Config();
        SlickSession slickSession = SlickSession.forConfig("h2", config);
        ScriptTestTools.executeSql(slickSession, DB_SETUP);
        slickSession.close();
    }

    private static Config createH2Config() {
        Map<String, String> dbConfigMap = HashMap.empty();
        dbConfigMap = dbConfigMap.put("h2.profile", "slick.jdbc.H2Profile$");
        dbConfigMap = dbConfigMap.put("h2.db.dataSourceClass", "slick.jdbc.DriverDataSource");
        dbConfigMap = dbConfigMap.put("h2.db.properties.driver", "org.h2.Driver");
        dbConfigMap = dbConfigMap.put("h2.db.properties.url", "jdbc:h2:mem:test-stream-script;DB_CLOSE_DELAY=-1");
        return ConfigFactory.parseMap(dbConfigMap.toJavaMap());
    }

    @Test
    public void readsFromDb() throws Exception {
        Config config = createH2Config();
        final SlickSession slickSession = SlickSession.forConfig("h2", config);
        String script = "{query:select id, name from data;id|int:name|string}" +
                "{var:id/},{var:name/}\n" +
                "{/query}";
        Map<String, Object> context = HashMap.empty();
        context = context.put("slick.session", slickSession);
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("1,first\n2,second\n3,third\n", output);
    }

    @Test
    public void performsNestedDependentQuery() throws Exception {
        Config config = createH2Config();
        final SlickSession slickSession = SlickSession.forConfig("h2", config);
        String script = "{query:select id from data;id|int}" +
                "{query:select name from data where id = (:id);name|string}" +
                "{var:id/},{var:name/}\n" +
                "{/query}" +
                "{/query}";
        Map<String, Object> context = HashMap.empty();
        context = context.put("slick.session", slickSession);
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("1,first\n2,second\n3,third\n", output);
    }

    @Test
    public void performsQueryByDate() throws Exception {
        Config config = createH2Config();
        final SlickSession slickSession = SlickSession.forConfig("h2", config);
        String script = "{query:select id from dates where timestamp > '(:dateFrom)';id|int}" +
                "{var:id/}\n" +
                "{/query}";
        Map<String, Object> context = HashMap.empty();
        context = context.put("slick.session", slickSession);
        Calendar cal = Calendar.getInstance();
        cal.set(2005, Calendar.JANUARY, 1);
        context = context.put("dateFrom", cal.getTime());
        Source<ByteString, NotUsed> source = ScriptTestTools.createSourceFromScript(script, context);
        String output = ScriptTestTools.materializeSource(source);
        Assert.assertEquals("2\n3\n", output);
    }

    @Test
    public void throwsOnMalformedParameters() {
        expectedException.expectMessage("malformedParam");
        QueryCommand queryCommand = new QueryCommand();
        queryCommand.setCode("select query;malformedParam");
    }
}
