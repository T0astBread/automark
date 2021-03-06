package automark;

import automark.gui.*;
import automark.io.*;
import automark.subcommands.*;

public class Main {
    private static final int E_USER_ERROR = 1;

    public static void main(String[] args) {
        // Improves Swing font rendering
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

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
                Run.run(commandLineArgs.workingDir, commandLineArgs.enableInsecureDebugMechanisms);
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
            case MARK_PLAGIARIZED:
                MarkPlagiarized.run(commandLineArgs.workingDir, commandLineArgs.markPlagiarizedSlugs);
                break;
            case GUI:
                UI.setRunningInGUIEnvironment(true);
                GUI.start(commandLineArgs);
                break;
            case MANUAL:
                UI.setRunningInGUIEnvironment(true);
                GUI.startManual(commandLineArgs);
                break;
        }
    }
}
