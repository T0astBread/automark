package automark;

import automark.io.*;
import automark.subcommands.*;

public class Main {
    private static final int E_USER_ERROR = 1;

    public static void main(String[] args) {
        try {
            main0(args);
        } catch (UserFriendlyException e) {
            e.printStackTrace();
            System.err.println();
            System.err.println(e.getMessage());
            System.exit(E_USER_ERROR);
        }
    }

    private static void main0(String[] args) throws UserFriendlyException {
        CommandLineArgs commandLineArgs = CommandLineArgs.parse(args);

        switch (commandLineArgs.subcommand) {
            case RUN:
                Run.run(commandLineArgs.workingDir);
                break;
            case STATUS:
                Status.run(commandLineArgs.workingDir);
                break;
            case ROLLBACK:
                Rollback.run(commandLineArgs.workingDir, commandLineArgs.rollbackStage);
                break;
            case MARK_RESOLVED:
                MarkResolved.run(commandLineArgs.workingDir,
                        commandLineArgs.markResolvedSubmissionSlug,
                        commandLineArgs.markResolvedProblemIdentifier,
                        commandLineArgs.markResolvedRequalify);
                break;
        }
    }
}
