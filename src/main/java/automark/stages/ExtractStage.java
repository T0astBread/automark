package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ExtractStage {

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        // Set up files & config
        File unzipDir = new File(Metadata.getDataDir(workingDir), "unzip");
        File extractDir = Metadata.mkStageDir(Stage.EXTRACT, workingDir);
        List<String> sourceFileNames = Config.asList(config.getProperty(Config.SOURCE_FILES));

        // Iterate over submissions
        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            File submissionInUnzip = new File(unzipDir, submission.getSlug());
            File submissionInExtract = new File(extractDir, submission.getSlug());

            try {
                // Filter wanted files from unzip dir tree
                List<File> filesToCopy = Files.walk(submissionInUnzip.toPath())
                        .map(Path::toFile)
                        .filter(file -> file.isFile() && sourceFileNames.contains(file.getName()))
                        .collect(Collectors.toList());

                if (filesToCopy.size() > 0) {
                    // Copy wanted files from unzip dir to extract dir
                    for (File file : filesToCopy) {
                        submissionInExtract.mkdirs();
                        File destFile = new File(submissionInExtract, file.getName());
                        Files.copy(file.toPath(), destFile.toPath());
                    }
                } else {
                    submission.addProblem(Problem.createInvalidSubmissionFile(Stage.EXTRACT, "None of the wanted source files found"));
                    submission.setDisqualified(true);
                }
            } catch (IOException e) {
                submission.addProblem(Problem.createException(Stage.EXTRACT, e));
            }
        }

        return submissions;
    }
}
