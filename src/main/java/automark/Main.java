package automark;

import automark.config.*;
import automark.errors.*;

public class Main {

    public static void main(String[] args) {
        UI.setup();

        CommandLineArgs commandLineArgs;
        try {
            commandLineArgs = CommandLineArgs.parse(args);
        } catch (AutomarkException e) {
            System.err.println(e.getMessage());
            System.exit(ErrorCodes.USER_ERROR);
            return;  // should be unreachable
        }

        Config config = new Config(commandLineArgs);
        System.out.println(config.get("moodleBaseURL"));
    }
}
