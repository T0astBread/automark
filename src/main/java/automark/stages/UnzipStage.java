package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import net.lingala.zip4j.*;
import net.lingala.zip4j.exception.*;

import java.io.*;
import java.util.*;

public class UnzipStage {

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File downloadDir = new File(Metadata.getDataDir(workingDir), "download");
        File unzipDir = Metadata.mkStageDir(Stage.UNZIP, workingDir);

        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            try {
                unzip(downloadDir, unzipDir, submission);
            } catch (ZipException e) {
                submission.addProblem(Problem.createException(Stage.UNZIP, e));
                submission.setDisqualified(true);
            }
        }

        return submissions;
    }

    private static File unzip(File downloadDir, File unzipDir, Submission submission) throws ZipException {
        File submissionFile = new File(downloadDir, submission.getSlug() + ".zip");
        if (!submissionFile.exists()) {
            submission.addProblem(Problem.createInvalidSubmissionFile(Stage.UNZIP, "File was not a ZIP-file"));
        }
        File submissionFolder = new File(unzipDir, submission.getSlug());

        ZipFile submissionZip = new ZipFile(submissionFile);
        submissionZip.extractAll(submissionFolder.getAbsolutePath());

        return submissionFolder;
    }
}
