package com.sonar.tools.ui;

import com.sonar.tools.model.ExportConfig;
import com.sonar.tools.service.SonarApiClient;
import com.sonar.tools.util.SettingsManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ServerPanel extends JPanel {
    private final JTextField serverUrlField;
    private final JPasswordField tokenField;
    private final JLabel statusLabel;
    private final JButton testBtn;
    private final JButton saveBtn;

    private SonarApiClient apiClient;
    private Runnable onConnectSuccess;
    private final ExportConfig config;
    private final SettingsManager settingsManager;

    public ServerPanel(ExportConfig config, SettingsManager settingsManager) {
        this.config = config;
        this.settingsManager = settingsManager;

        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Title
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        JLabel titleLabel = new JLabel("SonarQube \u670D\u52A1\u5668\u8FDE\u63A5\u914D\u7F6E");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
        add(titleLabel, gbc);

        // Server URL
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        add(new JLabel("SonarQube \u5730\u5740:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1.0;
        serverUrlField = new JTextField(config.getServerUrl(), 30);
        add(serverUrlField, gbc);

        // Token
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        add(new JLabel("\u7528\u6237 Token:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0;
        tokenField = new JPasswordField(config.getToken(), 30);
        add(tokenField, gbc);

        // Buttons row: test + save + status
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        testBtn = new JButton("\u6D4B\u8BD5\u8FDE\u63A5");
        add(testBtn, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0;
        saveBtn = new JButton("\u4FDD\u5B58\u914D\u7F6E");
        add(saveBtn, gbc);

        gbc.gridx = 2; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        statusLabel = new JLabel("\u8FDE\u63A5\u72B6\u6001: \u672A\u8FDE\u63A5");
        add(statusLabel, gbc);

        // Help text
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        gbc.insets = new Insets(20, 5, 5, 5);
        JLabel helpLabel = new JLabel("<html><body style='width:400px'>"
                + "<b>\u8BF4\u660E:</b><br>"
                + "1. \u8F93\u5165 SonarQube \u670D\u52A1\u5668\u5730\u5740\uFF08\u4F8B\u5982 http://localhost:9000\uFF09<br>"
                + "2. \u8F93\u5165\u7528\u6237 Token\uFF08\u5728 SonarQube \u7684 My Account > Security \u4E2D\u751F\u6210\uFF09<br>"
                + "3. \u70B9\u51FB\u201C\u6D4B\u8BD5\u8FDE\u63A5\u201D\u786E\u8BA4\u670D\u52A1\u5668\u53EF\u8FBE<br>"
                + "4. \u8FDE\u63A5\u6210\u529F\u540E\u914D\u7F6E\u81EA\u52A8\u4FDD\u5B58\uFF0C\u4E5F\u53EF\u70B9\u51FB\u201C\u4FDD\u5B58\u914D\u7F6E\u201D\u624B\u52A8\u4FDD\u5B58"
                + "</body></html>");
        helpLabel.setForeground(Color.GRAY);
        add(helpLabel, gbc);

        // Actions
        testBtn.addActionListener(e -> testConnection());
        saveBtn.addActionListener(this::saveConfig);
    }

    private void testConnection() {
        String url = serverUrlField.getText().trim();
        String token = new String(tokenField.getPassword()).trim();

        if (url.isEmpty()) {
            JOptionPane.showMessageDialog(this, "\u8BF7\u8F93\u5165 SonarQube \u670D\u52A1\u5668\u5730\u5740", "\u63D0\u793A", JOptionPane.WARNING_MESSAGE);
            return;
        }

        testBtn.setEnabled(false);
        statusLabel.setText("\u6B63\u5728\u8FDE\u63A5...");
        statusLabel.setForeground(Color.ORANGE);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                apiClient = new SonarApiClient(url, token);
                return apiClient.testConnection();
            }

            @Override
            protected void done() {
                testBtn.setEnabled(true);
                try {
                    boolean connected = get();
                    if (connected) {
                        statusLabel.setText("\u8FDE\u63A5\u72B6\u6001: \u5DF2\u8FDE\u63A5 \u2713");
                        statusLabel.setForeground(new Color(0, 128, 0));
                        // Auto-save silently on successful connection
                        applyToConfig(config);
                        settingsManager.saveConfig(config);
                        if (onConnectSuccess != null) {
                            onConnectSuccess.run();
                        }
                    } else {
                        statusLabel.setText("\u8FDE\u63A5\u72B6\u6001: \u670D\u52A1\u5668\u672A\u5C31\u7EEA");
                        statusLabel.setForeground(Color.RED);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("\u8FDE\u63A5\u5931\u8D25: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                    apiClient = null;
                }
            }
        };
        worker.execute();
    }

    private void saveConfig(ActionEvent e) {
        applyToConfig(config);
        settingsManager.saveConfig(config);
        statusLabel.setText("\u914D\u7F6E\u5DF2\u4FDD\u5B58 \u2713");
        statusLabel.setForeground(new Color(0, 128, 0));

        // Reset status text after 3 seconds
        Timer timer = new Timer(3000, ev -> {
            statusLabel.setText("\u8FDE\u63A5\u72B6\u6001: \u5DF2\u8FDE\u63A5 \u2713");
        });
        timer.setRepeats(false);
        timer.start();
    }

    public SonarApiClient getApiClient() {
        return apiClient;
    }

    public void setApiClient(SonarApiClient client) {
        this.apiClient = client;
        if (client != null) {
            statusLabel.setText("\u8FDE\u63A5\u72B6\u6001: \u5DF2\u914D\u7F6E (\u672A\u9A8C\u8BC1)");
            statusLabel.setForeground(new Color(0, 100, 180));
        }
    }

    public void setOnConnectSuccess(Runnable callback) {
        this.onConnectSuccess = callback;
    }

    public void applyToConfig(ExportConfig config) {
        config.setServerUrl(serverUrlField.getText().trim());
        config.setToken(new String(tokenField.getPassword()).trim());
    }
}
