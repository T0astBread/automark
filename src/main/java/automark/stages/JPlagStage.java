package automark.stages;

import automark.*;
import automark.io.*;
import automark.models.*;
import com.google.gson.internal.bind.util.*;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.util.*;
import java.util.List;

public class JPlagStage {

    private static final String JPLAG_JAR_NAME = "jplag-2.12.1-SNAPSHOT-jar-with-dependencies.jar";

    public static List<Submission> run(File workingDir, Properties config, List<Submission> submissions) throws UserFriendlyException {
        // Set up files & config
        String assignmentName = config.getProperty(Config.ASSIGNMENT_NAME);
        String language = config.getProperty(Config.JPLAG_LANGUAGE);

        File repoDir = Config.asFile(config.getProperty(Config.JPLAG_REPOSITORY));
        if (!(repoDir.exists() && repoDir.isDirectory()))
            throw new UserFriendlyException("JPlag repository directory doesn't exist (looked for " + repoDir.getAbsolutePath() + ")");

        File jplagDir = Metadata.mkStageDir(Stage.JPLAG, workingDir);
        File submissionsDir = new File(jplagDir, "submissions");
        File resultsDir = new File(jplagDir, "results");
        submissionsDir.mkdirs();
        resultsDir.mkdirs();

        System.out.println("Copying submissions from JPlag repository...");
        File repoAssignmentDir = new File(repoDir, assignmentName);
        if (repoAssignmentDir.exists()) {
            try {
                FileIO.cpDir(repoAssignmentDir.toPath(), submissionsDir.toPath());
            } catch (IOException e) {
                throw new UserFriendlyException("Failed to read previous submissions from JPlag repository", e);
            }
        }

        System.out.println("Copying submissions from previous stage...");
        File extractDir = new File(Metadata.getDataDir(workingDir), "extract");
        int currentYear = LocalDate.now().getYear();
        List<File> submissionFolders = new ArrayList<>();

        for (Submission submission : submissions) {
            if (submission.isDisqualified())
                continue;

            File submissionInExtract = new File(extractDir, submission.getSlug());
            File submissionInJPlag = new File(submissionsDir, currentYear + "_" + submission.getSlug());

            try {
                if(submissionInJPlag.exists()) {
                    System.out.println("Warning: Overriding existing submission folder in JPlag working dir. Was it already in the repo?");
                    FileIO.rm(submissionInJPlag);
                }

                FileIO.cpDir(submissionInExtract.toPath(), submissionInJPlag.toPath());
            } catch (IOException e) {
                submission.addProblem(Problem.createException(Stage.JPLAG,
                        new UnexpectedException("Failed to copy submission from extract stage to JPlag stage", e)));
            }

            submissionFolders.add(submissionInJPlag);
        }

        System.out.println("Writing JPlag JAR to disk...");
        File jplagJar = new File(jplagDir, JPLAG_JAR_NAME);
        try (InputStream resStream = JPlagStage.class.getResourceAsStream(JPLAG_JAR_NAME);
             FileOutputStream outputStream = new FileOutputStream(jplagJar)) {
            FileIO.pipe(resStream, outputStream);
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to write JPlag jar", e);
        }

        System.out.println("Running JPlag...");
        try {
            File parseLog = new File(jplagDir, "jplag-log.txt");
            int exitCode = new ProcessBuilder()
                    .command("java", "-jar", JPLAG_JAR_NAME,
                            "-vl", "-s", "-l", language,
                            "-r", resultsDir.getAbsolutePath(),
                            "-o", parseLog.getAbsolutePath(),
                            submissionsDir.getAbsolutePath())
                    .directory(jplagDir)
                    .inheritIO()
                    .start()
                    .waitFor();

            if (exitCode != 0) {
                throw new UserFriendlyException("JPlag exited with non-zero exit status " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            throw new UserFriendlyException("Failed to run JPlag", e);
        }

        System.out.println("Copying submissions to JPlag repo...");
        for (File submissionFolder : submissionFolders) {
            File submissionFolderInRepo = new File(repoAssignmentDir, submissionFolder.getName());
            try {
                FileIO.cpDir(submissionFolder.toPath(), submissionFolderInRepo.toPath());
                System.out.println("Copied " + submissionFolder.getName());
            } catch (IOException e) {
                System.out.println("Failed to copy " + submissionFolder.getPath() + " to " + submissionFolderInRepo.getPath());
            }
        }

        URI resultsURI = new File(resultsDir, "index.html").toURI();
        Desktop desktop = Desktop.getDesktop();
        if (desktop != null) {
            try {
                desktop.browse(resultsURI);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Unable to open JPLag results in browser");
                System.out.println();
                System.out.println(resultsURI);
            }
        }

        System.out.println();
        System.out.println("What to do next:");
        System.out.println("  - To delete an entry from the JPlag repository, just delete the corresponding folder");
        System.out.println("  - To mark entries as plagiats run \"mark-plagiarized <slug_1 slug_2 ...>\"");
        System.out.println("  - To continue with the next stage run the same command again");

        return submissions;
    }
}
