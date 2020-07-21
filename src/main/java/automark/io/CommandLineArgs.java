package automark.io;

import automark.*;
import automark.models.*;

import java.io.*;
import java.util.*;

public class CommandLineArgs {
    public final File workingDir;
    public final Subcommand subcommand;
    public final Stage rollbackStage;
    public final String markResolvedSubmissionSlug;
    public final String markResolvedProblemIdentifier;
    public final boolean markResolvedRequalify;
    public final List<String> markPlagiarizedSlugs;
    public final boolean enableInsecureDebugMechanisms;
    public final boolean openBrowser;

    public static CommandLineArgs parse(String[] args) throws UserFriendlyException {
        ListIterator<String> iterator = List.of(args).listIterator();

        File workingDir = new File(System.getProperty("user.dir"));
        Subcommand subcommand = null;
        Stage rollbackStage = null;
        String markResolvedSubmissionSlug = null;
        String markResolvedProblemIdentifier = null;
        boolean markResolvedRequalify = false;
        List<String> markPlagiarizedSlugs = null;
        boolean enableInsecureDebugMechanisms = false;
        boolean openBrowser = true;

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

            } else if ("--requalify".equals(token)) {
                markResolvedRequalify = true;

            } else if ("--problem".equals(token)) {
                if (!iterator.hasNext())
                    throw new UserFriendlyException("--problem must by followd by <problem_type|problem_index>");
                markResolvedProblemIdentifier = iterator.next();

            } else if ("--enable-insecure-debug-mechanisms".equals(token)) {
                enableInsecureDebugMechanisms = true;

            } else if ("--no-open-browser".equals(token)) {
                openBrowser = false;

            } else if ("--help".equals(token) || "-h".equals(token)) {
                subcommand = Subcommand.MANUAL;

            } else if (subcommand != null) {
                throw new UserFriendlyException("Subcommand has already been specified as " + subcommand.name() + "(tried to specify again as " + token + ")");

            } else {
                // Match a subcommand
                try {
                    subcommand = Subcommand.valueOf(token.toUpperCase().replaceAll("-", "_"));
                } catch (IllegalArgumentException e) {
                    throw new UserFriendlyException("Unknown subcommand: " + token);
                }

                if (subcommand == Subcommand.ROLLBACK) {
                    if (!iterator.hasNext())
                        throw new UserFriendlyException("rollback subcommand must be followed by the stage to roll back");

                    String rollbackStageName = iterator.next();
                    try {
                        rollbackStage = Stage.valueOf(rollbackStageName.toUpperCase().replaceAll("-", "_"));
                    } catch (IllegalArgumentException e) {
                        throw new UserFriendlyException("Unknown stage in rollback subcommand " + rollbackStageName);
                    }

                } else if (subcommand == Subcommand.MARK_RESOLVED) {
                    if (!iterator.hasNext())
                        throw new UserFriendlyException("mark-resolved must be followed by: <submission_slug|_>");
                    markResolvedSubmissionSlug = iterator.next();

                } else if (subcommand == Subcommand.MARK_PLAGIARIZED) {
                    markPlagiarizedSlugs = new ArrayList<>();

                    while (iterator.hasNext()) {
                        String subToken = iterator.next();
                        if (subToken.startsWith("-")) {
                            iterator.previous();
                            break;
                        }
                        markPlagiarizedSlugs.add(subToken);
                    }
                }
            }
        }

        if (subcommand == null)
            subcommand = Subcommand.RUN;

        if (subcommand != Subcommand.MARK_RESOLVED && (markResolvedProblemIdentifier != null || markResolvedRequalify)) {
            System.out.println("Info: --problem and --requalify are only effective on the mark-resolve subcommand");
        }

        if (subcommand != Subcommand.GUI && !openBrowser) {
            System.out.println("Info: --no-open-browser is only effective on the gui subcommand");
        }

        return new CommandLineArgs(
                workingDir,
                subcommand,
                rollbackStage,
                markResolvedSubmissionSlug,
                markResolvedProblemIdentifier,
                markResolvedRequalify,
                markPlagiarizedSlugs,
                enableInsecureDebugMechanisms,
                openBrowser);
    }

    private CommandLineArgs(
            File workingDir,
            Subcommand subcommand,
            Stage rollbackStage,
            String markResolvedSubmissionSlug,
            String markResolvedProblemIdentifier,
            boolean markResolvedRequalify,
            List<String> markPlagiarizedSlugs,
            boolean enableInsecureDebugMechanisms,
            boolean openBrowser) {
        this.workingDir = workingDir;
        this.subcommand = subcommand;
        this.rollbackStage = rollbackStage;
        this.markResolvedSubmissionSlug = markResolvedSubmissionSlug;
        this.markResolvedProblemIdentifier = markResolvedProblemIdentifier;
        this.markResolvedRequalify = markResolvedRequalify;
        this.markPlagiarizedSlugs = markPlagiarizedSlugs;
        this.enableInsecureDebugMechanisms = enableInsecureDebugMechanisms;
        this.openBrowser = openBrowser;
    }

    public enum Subcommand {
        RUN,
        ROLLBACK,
        STATUS,
        MARK_RESOLVED,
        MARK_PLAGIARIZED,
        BYPASS_DOWNLOAD,
        GUI,
        MANUAL,
    }
}
