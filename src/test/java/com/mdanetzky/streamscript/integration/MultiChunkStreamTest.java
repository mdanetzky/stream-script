package com.mdanetzky.streamscript.integration;

import com.mdanetzky.streamscript.TestUtil;
import com.mdanetzky.streamscript.integration.xmlgenerator.MultiChunkStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.concurrent.Executors;

public class MultiChunkStreamTest {

    @Rule
    public Timeout globalTimeout = Timeout.seconds(2);

    @Test
    public void finishesEmptyStream() {
        MultiChunkStream stream = new MultiChunkStream();
        stream.finish();
        Assert.assertEquals(-1, stream.read());
    }

    @Test
    public void readsSingleString() throws IOException {
        MultiChunkStream stream = new MultiChunkStream();
        Executors.newSingleThreadExecutor().submit(() -> {
            stream.addChunk("test");
            stream.finish();
        });
        String output = TestUtil.getStringFromStream(stream);
        Assert.assertEquals("test", output);
    }

    @Test
    public void readsTwoStrings() throws IOException {
        MultiChunkStream stream = new MultiChunkStream();
        Executors.newSingleThreadExecutor().submit(() -> {
            stream.addChunk("test");
            stream.addChunk("test");
            stream.finish();
        });
        String output = TestUtil.getStringFromStream(stream);
        Assert.assertEquals("testtest", output);
    }

    @Test
    public void readsEmptyString() throws IOException {
        MultiChunkStream stream = new MultiChunkStream();
        Executors.newSingleThreadExecutor().submit(() -> {
            stream.addChunk("");
            stream.finish();
        });
        String output = TestUtil.getStringFromStream(stream);
        Assert.assertEquals("", output);
    }

    @Test
    public void readsNullString() throws IOException {
        MultiChunkStream stream = new MultiChunkStream();
        Executors.newSingleThreadExecutor().submit(() -> {
            stream.addChunk(null);
            stream.finish();
        });
        String output = TestUtil.getStringFromStream(stream);
        Assert.assertEquals("", output);
    }
}
