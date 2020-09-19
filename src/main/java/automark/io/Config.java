package automark.io;

import automark.*;

import java.io.*;
import java.util.*;

public class Config {
    public static final String DOWNLOAD_STAGE = "downloadStage";
    public static final String ASSIGNMENT_NAME = "assignmentName";
    public static final String JPLAG_LANGUAGE = "jplagLanguage";
    public static final String JPLAG_REPOSITORY = "jplagRepository";
    public static final String MOODLE_ASSIGNMENT_ID = "moodleAssignmentID";
    public static final String MOODLE_BASE_URL = "moodleBaseURL";
    public static final String MOODLE_PASSWORD = "moodlePassword";
    public static final String MOODLE_TEACHERS = "moodleTeachers";
    public static final String MOODLE_USERNAME = "moodleUsername";
    public static final String SOURCE_FILES = "sourceFiles";
    public static final String EMAIL_STAGE_ENABLED = "emailStageEnabled";
    public static final String SMTP_HOST = "smtpHost";
    public static final String SMTP_PORT = "smtpPort";
    public static final String SMTP_USERNAME = "smtpUsername";
    public static final String SMTP_PASSWORD = "smtpPassword";
    public static final String SMTP_PROTOCOL = "smtpProtocol";
    public static final String SMTP_FROM_NAME = "smtpFromName";
    public static final String SMTP_FROM_ADDRESS = "smtpFromAddress";


    public static Properties loadConfig(File workingDir) throws UserFriendlyException {
        File configFile = getConfigFile(workingDir);
        if (!(configFile.exists() && configFile.isFile()))
            throw new UserFriendlyException("Config file " + configFile.getAbsolutePath() + " doesn't exist");

        Properties config = new Properties();
        try (FileReader reader = new FileReader(configFile)) {
            config.load(reader);
        } catch (IOException e) {
            throw new UserFriendlyException("Failed to load config file " + configFile.getAbsolutePath(), e);
        }

        return config;
    }

    public static List<String> getMissingRequiredProperties(Properties config) {
        final String[] REQUIRED_CONFIG_PROPERTIES = {
                DOWNLOAD_STAGE,
                ASSIGNMENT_NAME,
                JPLAG_LANGUAGE,
                JPLAG_REPOSITORY,
                SOURCE_FILES
        };

        List<String> missingRequiredProperties = new ArrayList<>();
        for (String requiredProperty : REQUIRED_CONFIG_PROPERTIES) {
            if (!config.containsKey(requiredProperty))
                missingRequiredProperties.add(requiredProperty);
        }

        return missingRequiredProperties;
    }

    public static File getConfigFile(File workingDir) {
        return new File(workingDir, "config.properties");
    }

    public static List<String> asList(String configProp) {
        return List.of(configProp.split(",\\s+"));
    }

    public static File asFile(String configProp) {
        return new File(configProp);
    }

    public static int asInt(String configProp) {
        return Integer.parseInt(configProp);
    }

    public static boolean asBoolean(String configProp) {
        return Boolean.parseBoolean(configProp);
    }
}
