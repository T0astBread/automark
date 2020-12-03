package automark.gui;

import org.eclipse.jetty.websocket.api.*;

import java.io.*;
import java.nio.*;
import java.util.*;

public class WebSocketOutputStream extends OutputStream {
    private final Session session;
    private final Thread flushThread;
    private final StringBuilder buffer = new StringBuilder();

    public WebSocketOutputStream(Session session) {
        this.session = session;

        this.flushThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(100);
                    flushBuffer();
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }, WebSocketOutputStream.class.getSimpleName() + "::flushThread");
        this.flushThread.start();
    }

    @Override
    public void write(int i) {
        try {
            synchronized (this.buffer) {
                this.buffer.appendCodePoint(i);
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        super.close();
        this.flushThread.interrupt();
    }

    public void flushBuffer() throws IOException {
        if (this.session.isOpen()) {
            synchronized (this.buffer) {
                if (this.buffer.length() > 0) {
                    this.session.getRemote().sendString("l" + buffer.toString());
                    this.buffer.delete(0, this.buffer.length());
                }
            }
        }
    }
}
