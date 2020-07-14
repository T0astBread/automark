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
    public void closed(Session session, int statusCode, String reason) {
        System.setOut(originalSystemOut);
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
        originalSystemOut = System.out;
        System.setOut(new PrintStream(new WebSocketOutputStream(session)));

        try {
            while (Run.run(workingDir, true)) {
                session.getRemote().sendString("s");
            }
        } catch (UserFriendlyException e) {
            e.printStackTrace();
            System.out.println();
            e.printStackTrace(System.out);
            System.out.println();
            System.out.println(e.getMessage());
            System.out.flush();
            session.close(new CloseStatus(400, e.getMessage()));
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println();
            e.printStackTrace(System.out);
            System.out.flush();
            session.close(new CloseStatus(500, e.getMessage()));
        }
        session.close();
    }
}
