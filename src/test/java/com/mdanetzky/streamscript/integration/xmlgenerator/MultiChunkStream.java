package com.mdanetzky.streamscript.integration.xmlgenerator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MultiChunkStream extends InputStream {

    private static final String END_OF_QUEUE = "########END_OF_QUEUE######";
    private static final int EOF = -1;
    private final BlockingQueue<String> stringQueue = new LinkedBlockingQueue<>(100);
    private ByteArrayInputStream currentStream;
    private boolean finished = false;

    public void addChunk(String string) {
        if (string == null || string.isEmpty()) {
            return;
        }
        putToQueue(string);
    }

    private void putToQueue(String string) {
        try {
            stringQueue.put(string);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void finish() {
        putToQueue(END_OF_QUEUE);
    }

    @Override
    public int read() {
        if (finished) {
            return EOF;
        }
        int read = (currentStream == null) ? EOF : currentStream.read();
        while (read == EOF) {
            String nextString = takeFromQueue();
            if (nextString.equals(END_OF_QUEUE)) {
                finished = true;
                return EOF;
            }
            currentStream = new ByteArrayInputStream(nextString.getBytes());
            read = currentStream.read();
        }
        return read;
    }

    private String takeFromQueue() {
        try {
            return stringQueue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
