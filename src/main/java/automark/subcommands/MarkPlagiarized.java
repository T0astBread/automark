package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.util.*;

public class MarkPlagiarized {
    public static void run(File workingDir, List<String> submissionsToMark) throws UserFriendlyException {
        Metadata.MetadataLoadingResult loadingResult = Metadata.loadLatestMetadata(workingDir);
        Stage lastStage = loadingResult.lastStage;
        List<Submission> submissions = loadingResult.submissions;

        for (Submission submission : submissions) {
            if (submissionsToMark.contains(submission.getSlug()) && !hasBeenMarked(submission))
                submission.addProblem(Problem.createdPlagiarized());
        }

        Metadata.saveSubmissions(submissions, Metadata.getMetadataFile(workingDir, lastStage));
    }

    private static boolean hasBeenMarked(Submission submission) {
        return submission.getProblems().stream().anyMatch(problem -> problem.type == Problem.Type.PLAGIARIZED);
    }
}
