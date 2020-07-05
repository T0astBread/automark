package automark.config;

import automark.errors.*;

import java.io.*;
import java.util.*;

public class CommandLineArgs {
    public final Mode mode;
    public final File workingDir;
    public final Map<String, String> config;

    private CommandLineArgs(Mode mode, File workingDir, Map<String, String> config) {
        this.mode = mode;
        this.workingDir = workingDir;
        this.config = config;
    }

    public static CommandLineArgs parse(String[] args) throws AutomarkException {
        Iterator<String> iterator = List.of(args).iterator();

        Mode mode = Mode.START;
        File workingDir = new File(System.getProperty("user.dir"));
        Map<String, String> config = new HashMap<>();

        for(int i = 0; iterator.hasNext(); i++) {
            String arg = iterator.next();

            if(arg.equals("-o")) {
                if(!iterator.hasNext())
                    throw new AutomarkException("Command line arg \"-o\" must be followed by a value");

                String propStr = iterator.next();
                int splitPoint = propStr.indexOf("=");
                if(splitPoint == -1)
                    throw new AutomarkException("Command line properties must be in the format \"key=value\"");

                String key = propStr.substring(0, splitPoint);
                String value = propStr.substring(splitPoint + 1);
                config.put(key, value);
            } else if(arg.equals("-d")) {
                if (!iterator.hasNext())
                    throw new AutomarkException("Command line arg \"-o\" must be followed by a value");

                String workingDirStr = iterator.next();
                workingDir = new File(workingDirStr);

                if (!workingDir.exists() || !workingDir.isDirectory())
                    throw new AutomarkException("Working dir " + workingDirStr + " does not exist");
            } else if(i == 0) {
                try {
                    mode = Mode.valueOf(arg.toUpperCase());
                } catch(IllegalArgumentException e) {
                    throw new AutomarkException("Invalid subcommand " + arg + " (must be get/set/start)");
                }
            } else {
                throw new AutomarkException("Illegal argument " + arg);
            }
        }
        return new CommandLineArgs(mode, workingDir, config);
    }
}
