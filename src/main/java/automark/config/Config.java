package automark.config;

import automark.*;

import java.io.*;
import java.util.*;

public class Config {
    private final CommandLineArgs commandLineArgs;
    private final File localConfigFile, globalConfigFile;
    private Properties localConfig, globalConfig;

    public Config(CommandLineArgs commandLineArgs) {
        this.commandLineArgs = commandLineArgs;
        File workingDir = commandLineArgs.workingDir;
        this.localConfigFile = new File(workingDir, "config.properties");
        this.globalConfigFile = new File(System.getProperty("user.home"), ".config/automark.properties");
    }

    public String get(String key) {
        String val = getStringFromCommandLine(key);
        if (val == null)
            val = getStringFromLocalConfig(key);
        if (val == null)
            val = getStringFromGlobalConfig(key);
        if (val == null)
            val = getStringFromUI(key);
        return val;
    }

    private String getStringFromCommandLine(String key) {
        return this.commandLineArgs.config.get(key);
    }

    private String getStringFromLocalConfig(String key) {
        if (this.localConfig == null)
            this.localConfig = tryReadConfigFile(this.localConfigFile, "local");
        return this.localConfig != null
                ? this.localConfig.getProperty(key)
                : null;
    }

    private String getStringFromGlobalConfig(String key) {
        if (this.globalConfig == null)
            this.globalConfig = tryReadConfigFile(this.globalConfigFile, "global");
        return this.globalConfig != null
                ? this.globalConfig.getProperty(key)
                : null;
    }

    private String getStringFromUI(String key) {
        return UI.get().prompt(key);
    }

    private static Properties tryReadConfigFile(File configFile, String configFileName) {
        try (FileReader reader = new FileReader(configFile)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            System.out.println("Info: Couldn't read " + configFileName + " config file (" + configFile.getAbsolutePath() + ")");
            System.out.println("This doesn't have to concern you unless you were using that config file");
            System.out.println();
            return null;
        }
    }
}
