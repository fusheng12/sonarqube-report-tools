package com.sonar.tools.ui;

import com.sonar.tools.model.ExportConfig;
import com.sonar.tools.service.SonarApiClient;
import com.sonar.tools.util.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainFrame extends JFrame {
    private final ServerPanel serverPanel;
    private final ProjectPanel projectPanel;
    private final ExportPanel exportPanel;
    private final SettingsManager settingsManager;
    private ExportConfig config;

    public MainFrame() {
        super("SonarQube \u62A5\u544A\u751F\u6210\u5DE5\u5177");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(720, 580);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);

        settingsManager = new SettingsManager();
        config = settingsManager.loadConfig();

        JTabbedPane tabbedPane = new JTabbedPane();

        serverPanel = new ServerPanel(config, settingsManager);
        projectPanel = new ProjectPanel();
        exportPanel = new ExportPanel(config);

        tabbedPane.addTab("\u670D\u52A1\u5668\u914D\u7F6E", serverPanel);
        tabbedPane.addTab("\u9879\u76EE\u7BA1\u7406", projectPanel);
        tabbedPane.addTab("\u5BFC\u51FA\u62A5\u544A", exportPanel);

        setContentPane(tabbedPane);

        // Wire up interactions
        serverPanel.setOnConnectSuccess(() -> {
            projectPanel.setApiClient(serverPanel.getApiClient());
            exportPanel.setApiClient(serverPanel.getApiClient());
        });

        exportPanel.setOnExportListener(new ExportPanel.OnExportListener() {
            @Override
            public ExportConfig getConfig() {
                // Gather latest config from all panels
                serverPanel.applyToConfig(config);
                exportPanel.applyToConfig(config);
                return config;
            }

            @Override
            public java.util.List<com.sonar.tools.model.SonarProject> getSelectedProjects() {
                return projectPanel.getSelectedProjects();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                serverPanel.applyToConfig(config);
                exportPanel.applyToConfig(config);
                settingsManager.saveConfig(config);
                System.exit(0);
            }
        });

        // Auto-connect on startup if saved config exists
        if (config.getServerUrl() != null && !config.getServerUrl().isEmpty()
                && config.getToken() != null && !config.getToken().isEmpty()) {
            SonarApiClient client = new SonarApiClient(config.getServerUrl(), config.getToken());
            serverPanel.setApiClient(client);
            projectPanel.setApiClient(client);
            exportPanel.setApiClient(client);
        }
    }
}
