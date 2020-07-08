package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class MarkResolved {
    public static void run(File workingDir, String submissionSlug, String problemIdentifier, boolean requalify) throws UserFriendlyException {
        Metadata.MetadataLoadingResult loadingResult = Metadata.loadLatestMetadata(workingDir);
        Stage lastStage = loadingResult.lastStage;
        List<Submission> submissions = loadingResult.submissions;
        List<Submission> submissionsToWorkOn = submissions;

        if(lastStage == null)
            throw new UserFriendlyException("Nothing to resolve - no stage has been run yet");

        if(!submissionSlug.equals("_")) {
            submissionsToWorkOn = submissions.stream()
                    .filter(submission -> submission.getSlug().equals(submissionSlug))
                    .collect(Collectors.toList());
        }

        for (Submission submission : submissionsToWorkOn) {
            if(problemIdentifier.matches("^\\d+$")) {
                int problemIndex = Integer.parseInt(problemIdentifier) - 1;
                submission.getProblems().remove(problemIndex);
            } else {
                List<Problem> newProblems = submission.getProblems().stream()
                        .filter(problem -> !problem.type.name().equalsIgnoreCase(problemIdentifier))
                        .collect(Collectors.toList());
                submission.setProblems(newProblems);
            }

            if(requalify)
                submission.setDisqualified(false);
        }

        Metadata.saveSubmissions(submissions, Metadata.getMetadataFile(workingDir, lastStage));
    }
}
