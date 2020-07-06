package automark.execution.jplag;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.*;
import automark.execution.extract.*;
import automark.models.*;

import java.awt.*;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

public class JPlagStage implements Stage {
    @Override
    public String getName() {
        return "JPLAG";
    }

    @Override
    public List<Submission> run(Config config, List<Submission> submissions) throws AutomarkException {
        // Set up some directories
        File extractDir = new File(config.getWorkingDir(), ExtractStage.NAME.toLowerCase());
        if (!(extractDir.exists() && extractDir.isDirectory()))
            throw new AutomarkException("Folder from previous stage doesn't exist - maybe try re-running the " + ExtractStage.NAME + " stage? (i.e. set nextStage to 1)");

        File stageDir = Utils.cleanAndMakeStageDir(new File(config.getWorkingDir(), getName().toLowerCase()));
        File submissionsDir = new File(stageDir, "submissions");
        File resultsDir = new File(stageDir, "results");
        resultsDir.mkdirs();

        // Get & save back config
        String language = config.get(ConfigConstants.JPLAG_LANGUAGE);
        String assignmentName = config.get(ConfigConstants.ASSIGNMENT_NAME);
        String repository = config.get(ConfigConstants.JPLAG_REPOSITORY);
        config.trySaveBack(ConfigConstants.ASSIGNMENT_NAME, assignmentName);
        config.trySaveBack(ConfigConstants.JPLAG_LANGUAGE, language);
        config.trySaveBack(ConfigConstants.JPLAG_REPOSITORY, repository);

        int currentYear = LocalDate.now().getYear();

        // Copy extract dir into submissions (with year prefix)
        try {
            File[] filesInExtractDir = extractDir.listFiles();
            if(filesInExtractDir == null)
                throw new AutomarkException("extractDir.listFiles() returned null");

            for (File f : filesInExtractDir) {
                String newFileName = getJPlagSlug(currentYear, f.getName());
                File newFolder = new File(submissionsDir, newFileName);
                Utils.copyFolder(f.toPath(), newFolder.toPath());
            }
        } catch (IOException e) {
            throw new AutomarkException("Failed to copy submissions for JPLag", e);
        }

        // Copy repository into submissions
        File repositoryDir = new File(new File(repository), assignmentName);
        if(repositoryDir.exists()) {
            try {
                Utils.copyFolder(repositoryDir.toPath(), submissionsDir.toPath());
            } catch (IOException e) {
                throw new AutomarkException("Failed to copy repo folder to workspace for JPLag", e);
            }
        }

        // Write out jplag.jar (yes, this is happening)
        File jplagJar = new File(stageDir, "jplag.jar");
        try (InputStream resStream = JPlagStage.class.getResourceAsStream("jplag.jar");
             FileOutputStream outputStream = new FileOutputStream(jplagJar)) {
            Utils.pipe(resStream, outputStream);
        } catch (IOException e) {
            throw new AutomarkException("Failed to write jplag.jar", e);
        }

        // Run JPlag
        try {
            File parseLog = new File(stageDir, "jplag-log.txt");
            int exitCode = new ProcessBuilder()
                    .command("java", "-jar", "jplag.jar",
                            "-vl", "-s", "-l", language,
                            "-r", resultsDir.getAbsolutePath(),
                            "-o", parseLog.getAbsolutePath(),
                            submissionsDir.getAbsolutePath())
                    .directory(stageDir)
                    .inheritIO()
                    .start()
                    .waitFor();

            if(exitCode != 0) {
                throw new AutomarkException("JPlag exited with non-zero value " + exitCode);
            }
        } catch (InterruptedException | IOException e) {
            throw new AutomarkException("Failed to run JPlag", e);
        }

        // Handle/open results
        File resultsFile = new File(resultsDir, "index.html");
        if (!resultsFile.exists())
            throw new AutomarkException("JPlag didn't produce results");

        UI.get().println("\nJPlag analysis finished\n");
        if(UI.get().askForConfirmation("Open the results in a browser?", true)) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop != null) {
                try {
                    desktop.open(resultsFile);
                } catch (IOException e) {
                    UI.get().println("Unable to automatically open JPlag results in a browser");
                    UI.get().println(e.getMessage());
                }
            }
        }

        List<String> jplagSlugs = submissions.stream()
                .filter(submission -> !submission.isDisqualified())
                .map(submission -> getJPlagSlug(currentYear, submission.getSlug()))
                .sorted()
                .collect(Collectors.toList());

        List<String> plagiarizedSlugs = UI.get().editWhitelist(
                jplagSlugs, Collections.emptyList(),
                "Select submissions that should be marked as plagiats");
        submissions.stream()
                .filter(submission -> plagiarizedSlugs.contains(getJPlagSlug(currentYear, submission.getSlug())))
                .forEach(submission -> {
                    submission.addProblem(Problem.createdPlagiarized(getName()));
                });

        List<String> repoSlugs = UI.get().editWhitelist(
                jplagSlugs, jplagSlugs,
                "Select submissions that should be included in the JPlag repository");
        for (String repoSlug : repoSlugs) {
            File submissionFolder = new File(submissionsDir, repoSlug);
            File destinationFolder = new File(repositoryDir, repoSlug);
            try {
                Utils.copyFolder(submissionFolder.toPath(), destinationFolder.toPath());
            } catch (IOException e) {
                UI.get().println("Failed to copy submission to JPlag repository: " + repoSlug);
                e.printStackTrace();
            }
        }

        return submissions;
    }

    private String getJPlagSlug(int currentYear, String slug) {
        return currentYear + "_" + slug;
    }
}
