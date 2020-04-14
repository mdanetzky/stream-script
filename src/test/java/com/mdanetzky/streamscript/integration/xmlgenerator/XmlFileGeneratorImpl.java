package com.mdanetzky.streamscript.integration.xmlgenerator;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.japi.Pair;
import akka.stream.*;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.*;
import akka.util.ByteString;
import com.mdanetzky.streamscript.ScriptExecutorException;
import com.mdanetzky.streamscript.ScriptParserException;
import com.mdanetzky.streamscript.parser.ScriptParser;
import com.mdanetzky.streamscript.parser.commands.QueryCommand;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import scala.util.Failure;
import scala.util.Try;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


@SuppressWarnings("unused")
public class XmlFileGeneratorImpl implements XmlFileGenerator {

    public final static String TIMEOUT_PARAMETER = "akka.stream.timeout";
    public final static String ON_COMPLETE_PARAMETER = "akka.stream.complete";
    private final static ByteString END_OF_STREAM_MARKER = ByteString.fromString("####END_OF_STREAM_MARKER####");
    private static ActorSystem actorSystem;
    private static Materializer materializer;
    private XmlGGeneratorDbConnection dbConnection;
    private Map<String, Object> context;
    private String xsd;

    private static String getNameFromClass() {
        return XmlFileGeneratorImpl.class.getName().replaceAll("\\.", "_");
    }

    public static Config getConfig(XmlGGeneratorDbConnection dbConnection) {
        Map<String, String> dbConfigMap = new HashMap<>();
        dbConfigMap.put("profile", getProfileFor(dbConnection.getDatabase()));
        dbConfigMap.put("db.driver", dbConnection.getDriver());
        dbConfigMap.put("db.url", dbConnection.getUrl());
        dbConfigMap.put("db.user", dbConnection.getUser());
        dbConfigMap.put("db.password", dbConnection.getPassword());
        return ConfigFactory.parseMap(dbConfigMap);
    }

    private static String getProfileFor(XmlGGeneratorDbConnection.Database database) {
        switch (database) {
            case H2:
                return "slick.jdbc.H2Profile$";
            case DB2:
                return "slick.jdbc.DB2Profile$";
            case MYSQL:
                return "slick.jdbc.MySQLProfile$";
            case ORACLE:
                return "slick.jdbc.OracleProfile$";
            case POSTGRES:
                return "slick.jdbc.PostgresProfile$";
            case SQL_SERVER:
                return "slick.jdbc.SQLServerProfile$";
        }
        return null;
    }

    public static ActorSystem getActorSystem() {
        if (actorSystem == null) {
            Config cancelOnTimeoutConf = ConfigFactory
                    .parseString("akka.stream.materializer.subscription-timeout.mode = cancel");
            actorSystem = ActorSystem.create(getNameFromClass(), ConfigFactory.load(cancelOnTimeoutConf));
        }
        return actorSystem;
    }

    public static Materializer getMaterializer() {
        if (materializer == null) {
            materializer = Materializer.createMaterializer(getActorSystem());
        }
        return materializer;
    }

    @Override
    public XmlFileGeneratorResponse runScript(String script, Map<String, Object> context) {
        try {
            this.context = context;
            addSlickSessionToContext(context);
            Source<ByteString, NotUsed> source = ScriptParser.run(script, context);
            final MultiChunkStream multiChunkReader = new MultiChunkStream();
            ExceptionHolder exceptionHolder = new ExceptionHolder();
            Future<?> xsdValidationFinished =
                    XsdValidator.startValidation(xsd, multiChunkReader, exceptionHolder);
            final Pair<UniqueKillSwitch, InputStream> stream = startAkkaStream(source,
                    multiChunkReader, exceptionHolder, xsdValidationFinished);
            return new XmlFileGeneratorResponseImpl(stream.second(),
                    (cause) -> stream.first().abort(new ScriptExecutorException("Abort! " + cause)));
        } catch (Throwable t) {
            onComplete(new Failure<>(t));
            throw t;
        }
    }

    private Attributes timeoutFromContext(Map<String, Object> context) {
        if (context.containsKey(TIMEOUT_PARAMETER)) {
            long timeoutInSeconds = Long.decode((String) context.get(TIMEOUT_PARAMETER));
            FiniteDuration timeout = FiniteDuration.create(timeoutInSeconds, TimeUnit.SECONDS);
            return ActorAttributes
                    .streamSubscriptionTimeout(timeout, StreamSubscriptionTimeoutTerminationMode.cancel());
        }
        return Attributes.none();
    }

    @Override
    public void validateScript(String script) throws ScriptParserException {
        ScriptParser.verify(script);
    }

    private Pair<UniqueKillSwitch, InputStream> startAkkaStream(Source<ByteString, NotUsed> source,
                                                                MultiChunkStream multiChunkReader,
                                                                ExceptionHolder exceptionHolder,
                                                                Future<?> xsdValidatorFinished) {
        final Sink<ByteString, InputStream> outputSink = StreamConverters.asInputStream();
        final Flow<ByteString, ByteString, NotUsed> xsdValidatorFlow = Flow.fromFunction(
                (dataChunk) -> this.validateXml(dataChunk, multiChunkReader, exceptionHolder, xsdValidatorFinished));
        return source
                .intersperse(ByteString.emptyByteString(), ByteString.emptyByteString(), END_OF_STREAM_MARKER)
                .via(xsdValidatorFlow)
                .viaMat(KillSwitches.single(), Keep.right())
                .alsoTo(Sink.onComplete(this::onComplete))
                .toMat(outputSink, Keep.both())
                .addAttributes(timeoutFromContext(context))
                .run(getMaterializer());
    }

    @SuppressWarnings("unchecked")

    private void onComplete(Try<Done> doneTry) {
        closeSlickSession(doneTry);
        Object onStreamCompleted = context.get(ON_COMPLETE_PARAMETER);
        if (onStreamCompleted instanceof BiConsumer) {
            ((BiConsumer<Try<Done>, Map<String, Object>>) onStreamCompleted)
                    .accept(doneTry, context);
        }
    }

    private void closeSlickSession(Try<Done> doneTry) {
        Object slickSession = context.get(QueryCommand.SLICK_SESSION);
        if (slickSession instanceof SlickSession) {
            ((SlickSession) slickSession).close();
        }
    }

    private ByteString validateXml(ByteString input, MultiChunkStream multiChunkReader,
                                   ExceptionHolder exceptionHolder, Future<?> xsdValidatorFinished)
            throws Exception {
        if (input.equals(END_OF_STREAM_MARKER)) {
            multiChunkReader.finish();
            xsdValidatorFinished.get();
            input = ByteString.emptyByteString();
        } else {
            multiChunkReader.addChunk(input.utf8String());
        }
        exceptionHolder.throwExceptionFromQueue();
        return input;
    }

    private void addSlickSessionToContext(Map<String, Object> context) {
        Config dbConfig = getConfig(dbConnection);
        SlickSession slickSession = SlickSession.forConfig(dbConfig);
        context.put(QueryCommand.SLICK_SESSION, slickSession);
    }

    @Override
    public void setDbConnection(XmlGGeneratorDbConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void setXsd(String xsd) {
        this.xsd = xsd;
    }
}
