package automark.execution;

import automark.*;
import automark.config.*;
import automark.errors.*;
import automark.execution.download.*;
import automark.execution.extract.*;
import automark.models.*;
import com.google.gson.*;
import com.google.gson.reflect.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Executor {
    private static final Stage[] STAGES = new Stage[]{
            new DownloadStage(),
            new ExtractStage(),
    };

    private final Config config;

    public Executor(Config config) {
        this.config = config;
    }

    public void run() throws AutomarkException {
        final String STAGE_KEY = "nextStage";
        final Gson GSON = new Gson();

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
        }

        File submissionsFile = new File(this.config.getWorkingDir(), "submissions.json");
        List<Submission> submissions = null;
        if (submissionsFile.exists()) {
            try (FileReader reader = new FileReader(submissionsFile)) {
                Type listType = new TypeToken<ArrayList<Submission>>() {
                }.getType();
                submissions = GSON.fromJson(reader, listType);
            } catch (IOException e) {
                throw new AutomarkException("Failed to read submissions.json", e);
            }
        }

        Stage stage = STAGES[stageIndex];
        List<Submission> newSubmissions = stage.run(this.config, submissions);

        try {
            this.config.set(Map.of(STAGE_KEY, Integer.toString(stageIndex + 1)), false);
        } catch (IOException e) {
            throw new AutomarkException("Failed to record stage execution success in config", e);
        }

        try (FileWriter writer = new FileWriter(submissionsFile)) {
            GSON.toJson(newSubmissions, writer);
        } catch (IOException e) {
            throw new AutomarkException("Failed to write submissions.json", e);
        }
    }
}
