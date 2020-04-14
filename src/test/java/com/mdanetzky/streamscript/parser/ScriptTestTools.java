package com.mdanetzky.streamscript.parser;

import akka.Done;
import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import akka.stream.alpakka.slick.javadsl.Slick;
import akka.stream.alpakka.slick.javadsl.SlickSession;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ScriptTestTools {

    private static final ActorSystem system = ActorSystem.create();
    private static final Materializer materializer = Materializer.createMaterializer(system);

    public static void executeSql(SlickSession slickSession, String[] sql) throws InterruptedException, ExecutionException {
        RunnableGraph<CompletionStage<Done>> createDbGraph =
                Source.from(Arrays.asList(sql))
                        .toMat(Slick.sink(slickSession, 1), Keep.right());
        CompletionStage<Done> createDbFuture = createDbGraph.run(materializer);
        createDbFuture.toCompletableFuture().get();
    }

    static String materializeSource(Source<ByteString, NotUsed> source) throws Exception {
        final CompletionStage<List<ByteString>> future = source
                .runWith(Sink.seq(), materializer);
        final List<ByteString> result = future.toCompletableFuture().get(3, TimeUnit.SECONDS);
        return result.stream().map(ByteString::utf8String).collect(Collectors.joining());
    }

    static Source<ByteString, NotUsed> createSourceFromScript(String script, io.vavr.collection.Map<String, Object> context) {
        CommandContainer command = ScriptParser.parse(script);
        return command.toStream(context);
    }
}
