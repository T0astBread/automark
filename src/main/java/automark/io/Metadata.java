package automark.io;

import automark.*;
import automark.models.*;
import com.google.gson.*;
import com.google.gson.reflect.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

public class Metadata {
    private static final Gson GSON = new Gson();

    public static void saveSubmissions(List<Submission> submissions, File file) throws UserFriendlyException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(submissions, writer);
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to save metadata file " + file.getAbsolutePath(), e);
        }
    }

    public static List<Submission> loadSubmissions(File file) throws UserFriendlyException {
        try (FileReader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<Submission>>() {
            }.getType();
            return GSON.fromJson(reader, listType);
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to read metadata file " + file.getAbsolutePath(), e);
        }
    }

    public static MetadataLoadingResult loadLatestMetadata(File workingDir) throws UserFriendlyException {
        File metadataDir = getMetadataDir(workingDir);
        List<Submission> submissions = null;

        File lastGoodMetadataFile = null;
        int stageIndex = 0;
        for (Stage stage : Stage.values()) {
            String stageName = stage.name().toLowerCase();
            File metadataFile = new File(metadataDir, stageName + ".json");

            if (metadataFile.exists()) {
                lastGoodMetadataFile = metadataFile;
            } else {
                if (lastGoodMetadataFile != null) {
                    submissions = Metadata.loadSubmissions(lastGoodMetadataFile);
                }
                return new MetadataLoadingResult(submissions, metadataFile, stageIndex, false);
            }

            stageIndex++;
        }

        // We've gone through all the stages
        submissions = Metadata.loadSubmissions(lastGoodMetadataFile);
        return new MetadataLoadingResult(submissions, lastGoodMetadataFile, stageIndex, true);
    }

    public static File getMetadataDir(File workingDir) {
        return new File(workingDir, "metadata");
    }

    public static File getDataDir(File workingDir) {
        return new File(workingDir, "data");
    }

    public static File getMetadataFile(File workingDir, Stage stage) {
        return new File(getMetadataDir(workingDir), stage.name().toLowerCase());
    }

    public static File mkStageDir(Stage stage, File workingDir) {
        File stageDir = new File(getDataDir(workingDir), stage.name().toLowerCase());
        stageDir.mkdirs();
        return stageDir;
    }

    public static class MetadataLoadingResult {
        public final List<Submission> submissions;
        public final File metadataFile;
        public final Stage stage;
        public final int stageIndex;
        public final boolean hasGoneThroughAllStages;

        public MetadataLoadingResult(List<Submission> submissions, File metadataFile, int stageIndex, boolean hasGoneThroughAllStages) {
            this.submissions = submissions;
            this.metadataFile = metadataFile;
            this.stageIndex = stageIndex;
            this.hasGoneThroughAllStages = hasGoneThroughAllStages;
            this.stage = Stage.values()[stageIndex];
        }
    }
}
