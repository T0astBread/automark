package automark.subcommands;

import automark.*;
import automark.io.*;
import automark.models.*;

import java.io.*;

public class Rollback {
    public static void run(File workingDir, Stage targetStage) throws UserFriendlyException {
        File dataDir = Metadata.getDataDir(workingDir);

        try {
            boolean isSkipping = true;
            for (Stage stage : Stage.values()) {
                if (isSkipping && stage != targetStage)
                    continue;

                isSkipping = false;

                FileIO.rm(new File(dataDir, stage.name().toLowerCase()));
                FileIO.rm(Metadata.getMetadataFile(workingDir, stage));
            }
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to revert stages", e);
        }
    }
}
