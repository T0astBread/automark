package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.util.*;

public class Status {
    public static void run(File workingDir) throws UserFriendlyException {
        Metadata.MetadataLoadingResult loadingResult = Metadata.loadLatestMetadata(workingDir);
        List<Submission> submissions = loadingResult.submissions;

        if (submissions == null)
            System.out.println("Nothing to show");
        else
            Summaries.printDetailedSummary(System.out, submissions, loadingResult.lastStage);
    }
}
