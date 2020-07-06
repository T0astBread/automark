package automark.config;

import automark.*;

import java.io.*;
import java.util.*;

public class Config {
    public static final int
            INPUT_CMD_LINE = 1,
            INPUT_LOCAL_CONFIG = 2,
            INPUT_GLOBAL_CONFIG = 4,
            INPUT_UI = 8;
    public static final int INPUT_ALL = INPUT_CMD_LINE |
            INPUT_LOCAL_CONFIG |
            INPUT_GLOBAL_CONFIG |
            INPUT_UI;

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
        return get(key, INPUT_ALL);
    }

    public String get(String key, int configSourceMask) {
        String val = null;
        if (Utils.isSet(configSourceMask, INPUT_CMD_LINE))
            val = getStringFromCommandLine(key);
        if (val == null && Utils.isSet(configSourceMask, INPUT_LOCAL_CONFIG))
            val = getStringFromLocalConfig(key);
        if (val == null && Utils.isSet(configSourceMask, INPUT_GLOBAL_CONFIG))
            val = getStringFromGlobalConfig(key);
        if (val == null && Utils.isSet(configSourceMask, INPUT_UI))
            val = UI.get().askForConfigValue(key);
        return val;
    }

    public String[] getList(String key) {
        return getList(key, INPUT_ALL);
    }

    public String[] getList(String key, int configSourceMask) {
        String rawVal = get(key, configSourceMask);
        return rawVal == null ? null : rawVal.split("\\s+");
    }

    public void set(Map<String, String> properties, boolean global) throws IOException {
        Properties props = global ? getGlobalConfig() : getLocalConfig();
        File propsFile = global ? this.globalConfigFile : this.localConfigFile;

        if (props == null) {
            if (global) this.globalConfig = new Properties();
            else this.localConfig = new Properties();

            props = global ? getGlobalConfig() : getLocalConfig();
        }
        props.putAll(properties);

        try (FileWriter writer = new FileWriter(propsFile)) {
            props.store(writer, "");
        }
    }

    public void trySaveBack(String key, String value) {
        if(get(key, INPUT_LOCAL_CONFIG) == null) {
            try {
                set(Map.of(key, value), false);
            } catch (IOException e) {
                UI.get().println("Warning: Unable to set " + value);
                e.printStackTrace();
            }
        }
    }

    public File getWorkingDir() {
        return this.commandLineArgs.workingDir;
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
}
