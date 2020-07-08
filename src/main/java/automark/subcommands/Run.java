package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;
import automark.stages.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Run {
    public static void run(File workingDir) throws UserFriendlyException {
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
            return;
        }

        Metadata.getDataDir(workingDir).mkdirs();
        Metadata.getMetadataDir(workingDir).mkdirs();

        for (int i = loadingResult.stageIndex; i < Stage.values().length; i++) {
            Stage stage = Stage.values()[i];
            System.out.println("Running stage " + stage.name());
            System.out.println();

            submissions = runStage(stage, workingDir, config, submissions);
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
            }
        }
    }

    private static List<Submission> runStage(
            Stage stage,
            File workingDir,
            Properties config,
            List<Submission> submissions
    ) throws UserFriendlyException {
        switch (stage) {
            case DOWNLOAD:
                return DownloadStage.run(workingDir, config, submissions);
            default:
                throw new RuntimeException("not implemented");
        }
    }
}
