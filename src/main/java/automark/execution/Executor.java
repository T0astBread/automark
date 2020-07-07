package automark.execution;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.compile.*;
import automark.execution.download.*;
import automark.execution.extract.*;
import automark.execution.jplag.*;
import automark.execution.test.*;
import automark.models.*;
import com.google.gson.*;
import com.google.gson.internal.*;
import com.google.gson.reflect.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

public class Executor {
    private static final Stage[] STAGES = new Stage[]{
            new DownloadStage(),
            new ExtractStage(),
            new JPlagStage(),
            new CompileStage(),
            new TestStage(),
    };
    private static final String STAGE_KEY = "nextStage";
    private static final Gson GSON = new Gson();

    private final Config config;

    public Executor(Config config) {
        this.config = config;
    }

    public void run() throws AutomarkException {
        int stageIndex = getStageIndex();

        if (stageIndex >= STAGES.length) {
            UI.get().println("All done!\n\n" +
                    "If this is an error, you can reset to an earlier stage by running\n" +
                    "`automark set -d <workingdir> -o " + STAGE_KEY + "=<earlierstage>`\n\n" +
                    "These are the numbers for each stage:"
            );
            for (int i = 0; i < STAGES.length; i++) {
                Stage stage = STAGES[i];
                UI.get().println(i + " " + stage.getName());
            }
        } else {
            List<Submission> submissions = null;
            File submissionsFolder = getSubmissionsFolder();
            if(submissionsFolder.exists()) {
                if(!submissionsFolder.isDirectory())
                    throw new AutomarkException("submissions folder exists but is not a directory");
            } else {
                submissionsFolder.mkdirs();
            }

            if(stageIndex > 0) {
                Stage prevStage = STAGES[stageIndex - 1];
                submissions = readSubmissions(prevStage);
            }

            Stage stage = STAGES[stageIndex];
            List<Submission> newSubmissions = stage.run(this.config, submissions);

            List<Submission> submissionsWithNewProblems = newSubmissions.stream()
                    .filter(submission -> submission.getProblems().stream()
                            .anyMatch(problem -> problem.stage.equals(stage.getName())))
                    .collect(Collectors.toList());

            UI.get().println("\n\nSubmissions with problems created in this stage:\n");
            UI.get().printStatus(submissionsWithNewProblems);
            UI.get().println("\n");

            try {
                this.config.set(Map.of(STAGE_KEY, Integer.toString(stageIndex + 1)), false);
            } catch (IOException e) {
                throw new AutomarkException("Failed to record stage execution success in config", e);
            }

            File newSubmissionsFile = new File(submissionsFolder, getSubmissionsFileName(stage));
            try (FileWriter writer = new FileWriter(newSubmissionsFile)) {
                GSON.toJson(newSubmissions, writer);
            } catch (IOException e) {
                throw new AutomarkException("Failed to write submissions.json", e);
            }

            UI.get().println("Done with stage " + stage.getName());
        }
    }

    public void rollback(String target) throws AutomarkException {
        int targetIdx = indexOfRollbackTarget(target);

        if(targetIdx == -1) {
            boolean targetExists = Arrays.stream(STAGES)
                    .anyMatch(stage -> target.equalsIgnoreCase(stage.getName()));
            if(targetExists)
                throw new AutomarkException("Stage " + target + " can't be rolled back to as it hasn't been completed yet");
            else
                throw new AutomarkException("Stage " + target + " doesn't exist");
        }

        try {
            config.set(Map.of(STAGE_KEY, Integer.toString(targetIdx)), false);
        } catch (IOException e) {
            throw new AutomarkException("Failed to set stage in config", e);
        }

        for (int i = STAGES.length - 1; i >= targetIdx; i--) {
            Stage stage = STAGES[i];
            File submissionFolder = getSubmissionsFolder();

            File submissionsFile = new File(submissionFolder, getSubmissionsFileName(stage));
            if(submissionsFile.exists())
                submissionsFile.delete();

            File stageDir = new File(this.config.getWorkingDir(), stage.getName().toLowerCase());
            if(stageDir.exists()) {
                try {
                    Utils.deleteFolder(stageDir.toPath());
                } catch (IOException e) {
                    throw new AutomarkException("Failed to delete working directory for stage " + stage.getName() + ": " + stageDir.getAbsolutePath(), e);
                }
            }
        }
    }

    public void printStatus() throws AutomarkException {
        int stageIndex = getStageIndex() - 1;  // Get the last completed stage (as opposed to the next stage)

        if(stageIndex == -1) {
            UI.get().println("No stages have been run yet - there's nothing to show!");
        } else if(stageIndex >= STAGES.length) {
            stageIndex = STAGES.length - 1;
        }

        Stage lastStage = STAGES[stageIndex];
        List<Submission> submissions = readSubmissions(lastStage);

        UI.get().printStatus(submissions);
    }

    public static int indexOfStage(String stageName) {
        for (int i = 0; i < STAGES.length; i++) {
            Stage stage = STAGES[i];
            if (stage.getName().equals(stageName))
                return i;
        }
        return -1;
    }

    private List<Submission> readSubmissions(Stage stage) throws AutomarkException {
        File submissionsFile = new File(getSubmissionsFolder(), getSubmissionsFileName(stage));
        if (submissionsFile.exists()) {
            try (FileReader reader = new FileReader(submissionsFile)) {
                Type listType = new TypeToken<ArrayList<Submission>>() {
                }.getType();
                return GSON.fromJson(reader, listType);
            } catch (IOException e) {
                throw new AutomarkException("Failed to read " + submissionsFile.getName(), e);
            }
        } else {
            throw new AutomarkException("Submissions data file does not exist (looked for " + submissionsFile.getAbsolutePath() + ")");
        }
    }

    private File getSubmissionsFolder() {
        return new File(this.config.getWorkingDir(), "submissions");
    }

    private int indexOfRollbackTarget(String target) throws InvalidConfigException {
        int stageIndex = getStageIndex();
        for (int i = 0; i < stageIndex; i++) {
            Stage stage = STAGES[i];
            if(target.equalsIgnoreCase(stage.getName()))
                return i;
        }
        return -1;
    }

    private int getStageIndex() throws InvalidConfigException {
        String stageProp = this.config.get(STAGE_KEY, Config.INPUT_LOCAL_CONFIG);
        int stageIndex;
        if (stageProp != null) {
            try {
                stageIndex = Integer.parseInt(stageProp);
            } catch (NumberFormatException e) {
                throw new InvalidConfigException(STAGE_KEY, "Must be a number > 0 and <= " + STAGES.length);
            }
        } else {
            stageIndex = 0;
        }
        return stageIndex;
    }

    private String getSubmissionsFileName(Stage stage) {
        return stage.getName().toLowerCase() + ".json";
    }
}
