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

    public void set(Map<String, String> properties, boolean global) throws IOException {
        Properties props = global ? getGlobalConfig() : getLocalConfig();
        File propsFile = global ? this.globalConfigFile : this.localConfigFile;

        if(props == null) {
            if(global) this.globalConfig = new Properties();
            else this.localConfig = new Properties();

            props = global ? getGlobalConfig() : getLocalConfig();
        }
        props.putAll(properties);

        try(FileWriter writer = new FileWriter(propsFile)) {
            props.store(writer, "");
        }
    }

    private String getStringFromCommandLine(String key) {
        return this.commandLineArgs.config.get(key);
    }

    private String getStringFromLocalConfig(String key) {
        return getLocalConfig() != null
                ? this.localConfig.getProperty(key)
                : null;
    }

    private String getStringFromGlobalConfig(String key) {
        return getGlobalConfig() != null
                ? this.globalConfig.getProperty(key)
                : null;
    }

    public Properties getLocalConfig() {
        if (this.localConfig == null)
            this.localConfig = tryReadConfigFile(this.localConfigFile, "local");
        return this.localConfig;
    }

    public Properties getGlobalConfig() {
        if (this.globalConfig == null)
            this.globalConfig = tryReadConfigFile(this.globalConfigFile, "global");
        return this.globalConfig;
    }

    private Properties tryReadConfigFile(File configFile, String configFileName) {
        try (FileReader reader = new FileReader(configFile)) {
            Properties properties = new Properties();
            properties.load(reader);
            return properties;
        } catch (IOException e) {
            System.err.println("Info: Couldn't read " + configFileName + " config file (" + configFile.getAbsolutePath() + ")");
            System.err.println("This doesn't have to concern you unless you were using that config file");
            System.err.println();
            return null;
        }
    }

    private String getStringFromUI(String key) {
        return UI.get().prompt(key);
    }
}
