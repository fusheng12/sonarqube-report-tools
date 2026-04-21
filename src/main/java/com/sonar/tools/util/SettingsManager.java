package com.sonar.tools.util;

import com.sonar.tools.model.ExportConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class SettingsManager {
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = CONFIG_DIR + File.separator + "settings.properties";

    private Properties props;

    public SettingsManager() {
        props = new Properties();
    }

    public void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            return;
        }
        try (InputStream is = new FileInputStream(file)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
        }
    }

    public void save() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try (OutputStream os = new FileOutputStream(CONFIG_FILE)) {
            props.store(os, "SonarQube Report Tool Settings");
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    public ExportConfig loadConfig() {
        load();
        ExportConfig config = new ExportConfig();
        config.setServerUrl(props.getProperty("serverUrl", config.getServerUrl()));
        config.setToken(props.getProperty("token", ""));
        config.setAuthor(props.getProperty("author", ""));
        config.setLanguage(props.getProperty("language", config.getLanguage()));
        config.setOutputDir(props.getProperty("outputDir", config.getOutputDir()));
        config.setEnableReport(Boolean.parseBoolean(props.getProperty("enableReport", "true")));
        config.setEnableSpreadsheet(Boolean.parseBoolean(props.getProperty("enableSpreadsheet", "true")));
        config.setEnableCsv(Boolean.parseBoolean(props.getProperty("enableCsv", "false")));
        config.setEnableMarkdown(Boolean.parseBoolean(props.getProperty("enableMarkdown", "true")));
        return config;
    }

    public void saveConfig(ExportConfig config) {
        props.setProperty("serverUrl", config.getServerUrl());
        if (config.getToken() != null) {
            props.setProperty("token", config.getToken());
        }
        props.setProperty("author", config.getAuthor() != null ? config.getAuthor() : "");
        props.setProperty("language", config.getLanguage());
        props.setProperty("outputDir", config.getOutputDir());
        props.setProperty("enableReport", String.valueOf(config.isEnableReport()));
        props.setProperty("enableSpreadsheet", String.valueOf(config.isEnableSpreadsheet()));
        props.setProperty("enableCsv", String.valueOf(config.isEnableCsv()));
        props.setProperty("enableMarkdown", String.valueOf(config.isEnableMarkdown()));
        save();
    }
}
