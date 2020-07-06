package automark;

import automark.config.*;
import automark.errors.*;
import automark.execution.*;

import java.io.*;
import java.util.*;

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

        if(commandLineArgs.mode == Mode.GET) {
            Properties props = commandLineArgs.global ? config.getGlobalConfig() : config.getLocalConfig();
            if(props != null) {
                props.forEach((key, value) -> {
                    System.out.println(key + "=" + value);
                });
            }

        } else if(commandLineArgs.mode == Mode.SET) {
            try {
                config.set(commandLineArgs.config, commandLineArgs.global);
                System.out.println("Successfully wrote to config file");
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("\nFailed to write config file");
                System.exit(ErrorCodes.UNEXPECTED_ERROR);
                return;  // should be unreachable
            }

        } else if(commandLineArgs.mode == Mode.START) {
            Executor executor = new Executor(config);
            try {
                executor.run();
            } catch (AutomarkException e) {
                e.printStackTrace();
                System.err.println("\nFailed to complete execution");
                System.exit(ErrorCodes.UNEXPECTED_ERROR);
                return;  // should be unreachable
            }
        }
    }
}
