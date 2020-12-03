package automark.gui;

import automark.*;
import automark.io.*;
import automark.subcommands.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.*;

@WebSocket
public class RunWebSocketHandler {
    private final String secret;
    private final CommandLineArgs commandLineArgs;
    private File workingDir;
    private boolean isRunning = false;
    private PrintStream originalSystemOut;

    public RunWebSocketHandler(File workingDir, String secret, CommandLineArgs commandLineArgs) {
        this.workingDir = workingDir;
        this.secret = secret;
        this.commandLineArgs = commandLineArgs;
    }

    @OnWebSocketConnect
    public void connected(Session session) {
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) throws IOException {
        final PrintStream wsOut = System.out;
        System.setOut(originalSystemOut);
        wsOut.close();
    }

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        if (!isRunning) {
            if (secret.equals(message) || commandLineArgs.enableInsecureDebugMechanisms) {
                startNewThread(session);
            } else {
                session.close(new CloseStatus(401, "Not allowed"));
            }
        }
    }

    public void setWorkingDir(File workingDir) {
        this.workingDir = workingDir;
    }

    private void startNewThread(Session session) {
        this.originalSystemOut = System.out;
        WebSocketOutputStream wsOut = new WebSocketOutputStream(session);
        System.setOut(new PrintStream(wsOut));

        try {
            while (Run.runAndStop(workingDir, commandLineArgs.enableInsecureDebugMechanisms, true)) {
                session.getRemote().sendString("s");
            }
        } catch (UserFriendlyException e) {
            e.printStackTrace();
            System.out.println();
            e.printStackTrace(System.out);
            System.out.println();
            System.out.println(e.getMessage());
            System.out.flush();
            closeSession(session, wsOut, new CloseStatus(400, e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println();
            e.printStackTrace(System.out);
            System.out.flush();
            closeSession(session, wsOut, new CloseStatus(500, e.getMessage()));
        }

        try {
            wsOut.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        session.close();
    }

    private void closeSession(Session session, WebSocketOutputStream wsOut, CloseStatus closeStatus) {
        try {
            wsOut.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
        session.close(closeStatus);
    }
}
