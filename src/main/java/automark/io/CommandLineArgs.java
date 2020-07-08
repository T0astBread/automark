package automark.io;

import automark.*;
import automark.models.*;

import java.io.*;
import java.util.*;

public class CommandLineArgs {
    public final File workingDir;
    public final Subcommand subcommand;
    public final Stage rollbackStage;

    public static CommandLineArgs parse(String[] args) throws UserFriendlyException {
        ListIterator<String> iterator = List.of(args).listIterator();

        File workingDir = new File(System.getProperty("user.dir"));
        Subcommand subcommand = Subcommand.RUN;
        Stage rollbackStage = null;

        // Format: [options] subcommand [subcommand positional args] [options]
        while (iterator.hasNext()) {
            String token = iterator.next();

            if ("--workingDir".equals(token) || "-d".equals(token)) {
                // Match a workingDir arg
                if (!iterator.hasNext())
                    throw new UserFriendlyException("--workingDir (or -d) must be followed by a path to the working directory");

                String workingDirPath = iterator.next();
                workingDir = new File(workingDirPath);
                if (!(workingDir.exists() && workingDir.isDirectory()))
                    throw new UserFriendlyException("workingDir must exist");

            } else {
                // Match a subcommand
                try {
                    subcommand = Subcommand.valueOf(token.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new UserFriendlyException("Unknown subcommand: " + token);
                }

                if (subcommand == Subcommand.ROLLBACK) {
                    if (!iterator.hasNext())
                        throw new UserFriendlyException("rollback subcommand must be followed by the stage to roll back");

                    String rollbackStageName = iterator.next();
                    try {
                        rollbackStage = Stage.valueOf(rollbackStageName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new UserFriendlyException("Unknown stage in rollback subcommand " + rollbackStageName);
                    }
                }
            }
        }

        return new CommandLineArgs(workingDir, subcommand, rollbackStage);
    }

    private CommandLineArgs(File workingDir, Subcommand subcommand, Stage rollbackStage) {
        this.workingDir = workingDir;
        this.subcommand = subcommand;
        this.rollbackStage = rollbackStage;
    }

    public enum Subcommand {
        RUN, ROLLBACK, STATUS;
    }
}
