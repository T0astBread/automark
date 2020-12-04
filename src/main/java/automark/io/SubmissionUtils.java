package automark.io;

import automark.models.*;

import java.io.*;
import java.util.*;

public class SubmissionUtils {

    public static List<File> getTestFiles(File workingDir, Properties config) {
        String testsDirProp = config.getProperty(Config.TESTS_DIR, null);
        if (testsDirProp == null)
            return new ArrayList<>();
        File testsDir = Config.asFile(testsDirProp, workingDir);
        File[] testFiles = testsDir.listFiles();
        return testFiles == null ? new ArrayList<>() : List.of(testFiles);
    }

    public static String getPackageNameForSubmission(Submission submission) {
        return "automark.testbed." + submission.getSlug();
    }
}
