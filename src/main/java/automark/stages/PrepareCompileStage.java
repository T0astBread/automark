package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class PrepareCompileStage {

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        File extractDir = new File(Metadata.getDataDir(workingDir), "extract");
        File prepareCompileDir = Metadata.mkStageDir(Stage.PREPARE_COMPILE, workingDir);
        List<File> testFiles = SubmissionUtils.getTestFiles(workingDir, config);

        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            // Copy sources from extract stage
            File submissionInExtract = new File(extractDir, submission.getSlug());
            File submissionInPrepareCompile = new File(prepareCompileDir, submission.getSlug());

            try {
                FileIO.cpDir(submissionInExtract.toPath(), submissionInPrepareCompile.toPath());
            } catch (IOException e) {
                submission.addProblem(Problem.createException(Stage.PREPARE_COMPILE,
                        new UnexpectedException("Failed to copy submission files from extract stage", e)));
                submission.setDisqualified(true);
                continue;
            }

            // Copy test files
            for (File testFile : testFiles) {
                File testFileInPrepareCompile = new File(submissionInPrepareCompile, testFile.getName());
                try {
                    Files.copy(testFile.toPath(), testFileInPrepareCompile.toPath());
                } catch (IOException e) {
                    submission.addProblem(Problem.createException(Stage.PREPARE_COMPILE,
                            new UnexpectedException("Failed to copy test file " + testFile.getName() + " into submission files", e)));
                }
            }

            try {
                List<String> sourceFiles = List.of(Objects.requireNonNull(submissionInPrepareCompile.list()));
                patchPackage(submissionInPrepareCompile, sourceFiles, SubmissionUtils.getPackageNameForSubmission(submission));
            } catch (Exception e) {
                submission.addProblem(Problem.createException(Stage.PREPARE_COMPILE,
                        new UnexpectedException("Failed to patch package headers in submission " + submission.getSlug(), e)));
                submission.setDisqualified(true);
            }
        }

        return submissions;
    }

    private static List<File> patchPackage(File submissionFolder, List<String> sourceAndResourceFiles, String wantedPackage) throws IOException {
        String packagePath = wantedPackage.replaceAll("\\.", FileIO.getEscapedFileSeperator()) + File.separator;
        List<File> sourceFilesForCompilation = new ArrayList<>();

        for (String sourceFileName : sourceAndResourceFiles) {
            File sourceFile = new File(submissionFolder, sourceFileName);
            File destFile = new File(submissionFolder, packagePath + sourceFile.getName());

            if (sourceFile.exists()) {
                destFile.getParentFile().mkdirs();

                if (sourceFileName.endsWith(".java")) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
                         FileWriter writer = new FileWriter(destFile)) {
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) break;

                            line = line.replaceFirst("^package [^;]+;", "package " + wantedPackage + ";");
                            writer.write(line);
                            writer.write("\n");
                        }
                    }
                    sourceFilesForCompilation.add(destFile);
                } else {
                    Files.copy(sourceFile.toPath(), destFile.toPath());
                }
                sourceFile.delete();
            }
        }
        return sourceFilesForCompilation;
    }
}
