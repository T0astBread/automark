package automark.gui;

import org.eclipse.jetty.websocket.api.*;

import java.io.*;

public class WebSocketOutputStream extends OutputStream {
    private final Session session;
    private String buffer = "";

    public WebSocketOutputStream(Session session) {
        this.session = session;
    }

    @Override
    public void write(int i) throws IOException {
        try {
            String character = Character.toString(i);
            buffer += character;
            if ("\n".equals(character)) {
                session.getRemote().sendString("l" + buffer);
                buffer = "";
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}
