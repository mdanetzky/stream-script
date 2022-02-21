package com.mdanetzky.streamscript.knowlege;

import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.function.Function;
import akka.stream.Materializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickRow;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class SlickTest {

    private final ActorSystem system = ActorSystem.create();
    private final Materializer materializer = Materializer.createMaterializer(system);

    private final String[] setup = {
            "create table DATA (ID integer NOT NULL, NAME varchar(50) NOT NULL);"
    };

    @Test
    public void writesToAndReadsFromDb() throws ExecutionException, InterruptedException {
        Config config = createH2Config();
        final SlickSession slickSession = SlickSession.forConfig("test", config);
        RunnableGraph<CompletionStage<Done>> createDbGraph =
                Source.from(Arrays.asList(setup))
                        .toMat(Slick.sink(slickSession, 1), Keep.right());
        CompletionStage<Done> createDbFuture = createDbGraph.run(materializer);
        createDbFuture.toCompletableFuture().get();
        final Function<Integer, String> insertData =
                (i) -> "INSERT INTO DATA VALUES(" + i + ", 'data_" + i + "');";
        RunnableGraph<CompletionStage<Done>> storeGraph =
                Source.range(1, 100).map(insertData)
                        .toMat(Slick.sink(slickSession, 4), Keep.right());
        CompletionStage<Done> saveFuture = storeGraph.run(materializer);
        saveFuture.toCompletableFuture().get();
        RunnableGraph<CompletionStage<Done>> readGraph =
                Slick.source(slickSession, "SELECT * FROM DATA;",
                                (SlickRow row) -> new Data(row.nextInt(), row.nextString()))
                        .toMat(Sink.foreach(System.out::println), Keep.right());
        CompletionStage<Done> printFuture = readGraph.run(materializer);
        printFuture.toCompletableFuture().get();
        slickSession.close();
        System.out.println();
    }

    private Config createH2Config() {
        Map<String, String> dbConfigMap = new HashMap<>();
        dbConfigMap.put("test.profile", "slick.jdbc.H2Profile$");
        dbConfigMap.put("test.db.dataSourceClass", "slick.jdbc.DriverDataSource");
        dbConfigMap.put("test.db.properties.driver", "org.h2.Driver");
        dbConfigMap.put("test.db.properties.url", "jdbc:h2:mem:test-slick;DB_CLOSE_DELAY=-1");
        return ConfigFactory.parseMap(dbConfigMap);
    }

    @AllArgsConstructor
    @ToString
    private static class Data {
        @Getter
        private int id;
        @Getter
        private String name;
    }
}
