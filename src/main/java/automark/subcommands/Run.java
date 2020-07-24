package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.stages.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Run {
    public static void run(File workingDir, boolean enableInsecureDebugMechanisms) throws UserFriendlyException {
        runAndStop(workingDir, enableInsecureDebugMechanisms, false);
    }

    /**
     * @return should call again?
     */
    public static boolean runAndStop(File workingDir, boolean enableInsecureDebugMechanisms, boolean stopAfterOneStage) throws UserFriendlyException {
        Properties config = Config.loadConfig(workingDir);

        List<String> missingRequiredProperties = Config.getMissingRequiredProperties(config);
        if (missingRequiredProperties.size() > 0) {
            System.err.println("Missing some required properties in config file:");
            missingRequiredProperties.forEach(prop -> System.out.println("\t" + prop));
            throw new UserFriendlyException("Configuration invalid");
        }

        Metadata.MetadataLoadingResult loadingResult = Metadata.loadLatestMetadata(workingDir);
        List<Submission> submissions = loadingResult.submissions;

        if (loadingResult.hasGoneThroughAllStages) {
            System.out.println("All stages done!");
            System.out.println("If this is an error, try rolling back with `rollback <targetStage>`");
            return false;
        }

        Metadata.getDataDir(workingDir).mkdirs();
        Metadata.getMetadataDir(workingDir).mkdirs();

        for (int i = loadingResult.nextStageIndex; i < Stage.values().length; i++) {
            Stage stage = Stage.values()[i];
            System.out.println("Running stage " + stage.name());
            System.out.println();

            submissions = runStage(stage, workingDir, config, submissions, enableInsecureDebugMechanisms);
            Metadata.saveSubmissions(submissions, Metadata.getMetadataFile(workingDir, stage));

            List<Submission> submissionsWithNewProblems = submissions.stream()
                    .filter(submission -> submission.getProblems().stream()
                            .anyMatch(problem -> problem.stage == stage))
                    .collect(Collectors.toList());

            if (submissionsWithNewProblems.size() > 0) {
                System.out.println();
                System.out.println("Some submissions have new problems after this stage:");
                System.out.println();
                Summaries.printShortSummary(System.out, submissionsWithNewProblems);
                System.out.println();
                System.out.println("In addition to any instructions printed above, you can now:");
                System.out.println("  - Roll back the stage using \"rollback " + stage.name().toLowerCase() + "\"");
                System.out.println("  - Resolve issues manually and run \"mark-resolved\" (see mark-resolved --help)");
                System.out.println("  - Ignore the problems (\"Problems\" don't always mean something is broken)");
                return false;
            }

            if (stage == Stage.JPLAG || stage == Stage.SUMMARY)
                return false;

            if (stopAfterOneStage)
                return true;
        }

        return false;
    }

    private static List<Submission> runStage(
            Stage stage,
            File workingDir,
            Properties config,
            List<Submission> submissions,
            boolean enableInsecureDebugMechanisms
    ) throws UserFriendlyException {
        switch (stage) {
            case DOWNLOAD:
                if ("bypass".equals(config.get(Config.DOWNLOAD_STAGE)))
                    return BypassDownloadStage.run(workingDir, config);
                else
                    return MoodleScraperStage.run(workingDir, config);
            case UNZIP:
                return UnzipStage.run(workingDir, config, submissions);
            case EXTRACT:
                return ExtractStage.run(workingDir, config, submissions);
            case JPLAG:
                return JPlagStage.run(workingDir, config, submissions);
            case PREPARE_COMPILE:
                return PrepareCompileStage.run(workingDir, config, submissions);
            case COMPILE:
                return CompileStage.run(workingDir, config, submissions);
            case TEST:
                return TestStage.run(workingDir, config, submissions);
            case SUMMARY:
                return SummaryStage.run(workingDir, config, submissions);
            default:
                throw new UserFriendlyException("Invalid state: Tried to execute a stage that isn't implemented: " + stage.name());
        }
    }
}
