package automark.execution;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.download.*;
import automark.execution.extract.*;
import automark.execution.jplag.*;
import automark.models.*;
import com.google.gson.*;
import com.google.gson.reflect.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Executor {
    private static final String STAGE_KEY = "nextStage";
    private static final Stage[] STAGES = new Stage[]{
            new DownloadStage(),
            new ExtractStage(),
            new JPlagStage(),
    };

    private final Config config;

    public Executor(Config config) {
        this.config = config;
    }

    public void run() throws AutomarkException {
        final Gson GSON = new Gson();

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
                File prevSubmissionsFile = new File(submissionsFolder, getSubmissionsFileName(prevStage));
                if (prevSubmissionsFile.exists()) {
                    try (FileReader reader = new FileReader(prevSubmissionsFile)) {
                        Type listType = new TypeToken<ArrayList<Submission>>() {
                        }.getType();
                        submissions = GSON.fromJson(reader, listType);
                    } catch (IOException e) {
                        throw new AutomarkException("Failed to read " + prevSubmissionsFile.getName(), e);
                    }
                }
            }

            Stage stage = STAGES[stageIndex];
            List<Submission> newSubmissions = stage.run(this.config, submissions);

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
