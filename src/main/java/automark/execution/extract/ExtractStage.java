package automark.execution.extract;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.execution.download.*;
import automark.models.*;
import net.lingala.zip4j.*;
import net.lingala.zip4j.exception.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class ExtractStage implements Stage {

    public static final String NAME = "EXTRACT";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException {
        File downloadDir = new File(config.getWorkingDir(), DownloadStage.NAME.toLowerCase());
        if (!(downloadDir.exists() && downloadDir.isDirectory()))
            throw new AutomarkException("Folder from previous stage doesn't exist - maybe try re-running the " + DownloadStage.NAME + " stage? (i.e. set nextStage to 0)");

        File stageDir = Utils.cleanAndMakeStageDir(new File(config.getWorkingDir(), getName().toLowerCase()));
        String[] sourceFileNames = config.getList(ConfigConstants.SOURCE_FILES);
        config.trySaveBack(ConfigConstants.SOURCE_FILES, String.join(" ", sourceFileNames));

        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            File submissionFolder = null;
            try {
                submissionFolder = unzip(downloadDir, stageDir, submission);
                extractSources(submissionFolder, sourceFileNames);
            } catch (Exception e) {
                submission.addProblem(Problem.createException(getName(), e));
                submission.setDisqualified(true);
            }
            if (submissionFolder != null) {
                try {
                    deleteIfNoSources(submissionFolder, submission);
                } catch (Exception e) {
                    submission.addProblem(Problem.createException(getName(), e));
                    submission.setDisqualified(true);
                }
            }
        }

        return submissions;
    }

    private File unzip(File downloadDir, File stageDir, Submission submission) throws ZipException {
        File submissionFile = new File(downloadDir, submission.getSlug() + ".zip");
        if (!submissionFile.exists()) {
            submission.addProblem(Problem.createInvalidSubmissionFile(getName(), "File was not a ZIP-file"));
        }
        File submissionFolder = new File(stageDir, submission.getSlug());

        ZipFile submissionZip = new ZipFile(submissionFile);
        submissionZip.extractAll(submissionFolder.getAbsolutePath());

        return submissionFolder;
    }

    private void extractSources(File submissionFolder, String[] sourceFileNames) throws IOException {
        // Move source files to top and delete other files
        List<String> _sourceFilesNames = List.of(sourceFileNames);

        List<File> s = Files.walk(submissionFolder.toPath())
                .map(Path::toFile)
                .filter(file -> file.exists() && file.isFile())
                .collect(Collectors.toList());

        for (File file : s) {
            if (_sourceFilesNames.contains(file.getName())) {
                Files.move(file.toPath(), new File(submissionFolder, file.getName()).toPath());
            } else {
                file.delete();
            }
        }

        // Delete directory structures
        File[] contents = submissionFolder.listFiles();
        if (contents != null) {
            for (File file : contents) {
                if (file.exists() && file.isDirectory())
                    Utils.deleteFolder(file.toPath());
            }
        }
    }

    private void deleteIfNoSources(File submissionFolder, Submission submission) {
        if (Utils.isEmpty(submissionFolder)) {
            submission.addProblem(Problem.createInvalidSubmissionFile(getName(), "No source files found"));
            submission.setDisqualified(true);
            submissionFolder.delete();
        }
    }
}
