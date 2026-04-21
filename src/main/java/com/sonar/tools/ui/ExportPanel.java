package com.sonar.tools.ui;

import com.sonar.tools.model.ExportConfig;
import com.sonar.tools.model.SonarProject;
import com.sonar.tools.service.ReportExporter;
import com.sonar.tools.service.SonarApiClient;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.List;

public class ExportPanel extends JPanel {
    private final JTextField authorField;
    private final JComboBox<String> languageCombo;
    private final JTextField outputDirField;
    private final JCheckBox docxCheck;
    private final JCheckBox xlsxCheck;
    private final JCheckBox csvCheck;
    private final JCheckBox mdCheck;
    private final JButton exportBtn;
    private final JButton stopBtn;
    private final JProgressBar progressBar;
    private final JTextArea logArea;
    private final ExportConfig config;

    private SonarApiClient apiClient;
    private OnExportListener listener;
    private volatile boolean cancelled;
    private ReportExporter currentExporter;

    public ExportPanel(ExportConfig config) {
        this.config = config;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // ===== Config form (top) =====
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // Author
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        formPanel.add(new JLabel("\u62A5\u544A\u4F5C\u8005:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        authorField = new JTextField(config.getAuthor(), 20);
        formPanel.add(authorField, gbc);

        // Language
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("\u8BED\u8A00:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        languageCombo = new JComboBox<>(new String[]{"en_US", "fr_FR"});
        languageCombo.setSelectedItem(config.getLanguage());
        formPanel.add(languageCombo, gbc);

        // Output dir
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("\u8F93\u51FA\u76EE\u5F55:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        outputDirField = new JTextField(config.getOutputDir(), 20);
        formPanel.add(outputDirField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0;
        JButton browseBtn = new JButton("\u6D4F\u89C8...");
        formPanel.add(browseBtn, gbc);
        browseBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser(FileSystemView.getFileSystemView());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setCurrentDirectory(new File(outputDirField.getText()));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Format checkboxes
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("\u62A5\u544A\u683C\u5F0F:"), gbc);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0;
        JPanel formatPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        docxCheck = new JCheckBox("Word (docx)", config.isEnableReport());
        xlsxCheck = new JCheckBox("Excel (xlsx)", config.isEnableSpreadsheet());
        csvCheck = new JCheckBox("CSV", config.isEnableCsv());
        mdCheck = new JCheckBox("Markdown", config.isEnableMarkdown());
        formatPanel.add(docxCheck);
        formatPanel.add(xlsxCheck);
        formatPanel.add(csvCheck);
        formatPanel.add(mdCheck);
        formPanel.add(formatPanel, gbc);

        // Export button + progress
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1; gbc.weightx = 0;
        exportBtn = new JButton("\u5F00\u59CB\u5BFC\u51FA");
        exportBtn.setFont(exportBtn.getFont().deriveFont(Font.BOLD));
        formPanel.add(exportBtn, gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("\u5C31\u7EEA");
        formPanel.add(progressBar, gbc);

        // Stop button
        gbc.gridx = 3; gbc.gridy = 4; gbc.weightx = 0;
        stopBtn = new JButton("\u505C\u6B62");
        stopBtn.setEnabled(false);
        formPanel.add(stopBtn, gbc);

        add(formPanel, BorderLayout.NORTH);

        // ===== Log area (center) =====
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("\u5BFC\u51FA\u65E5\u5FD7"));
        add(logScroll, BorderLayout.CENTER);

        // Actions
        exportBtn.addActionListener(e -> startExport());
        stopBtn.addActionListener(e -> {
            cancelled = true;
            currentExporter.cancel();
            log("[User] Cancelling...");
        });
    }

    private void startExport() {
        if (listener == null) return;

        ExportConfig cfg = listener.getConfig();
        List<SonarProject> projects = listener.getSelectedProjects();

        if (projects.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "\u8BF7\u5148\u5728\u201C\u9879\u76EE\u7BA1\u7406\u201D\u4E2D\u52FE\u9009\u9700\u8981\u5BFC\u51FA\u7684\u9879\u76EE",
                    "\u63D0\u793A", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!cfg.isEnableReport() && !cfg.isEnableSpreadsheet() && !cfg.isEnableCsv() && !cfg.isEnableMarkdown()) {
            JOptionPane.showMessageDialog(this,
                    "\u8BF7\u81F3\u5C11\u9009\u62E9\u4E00\u79CD\u62A5\u544A\u683C\u5F0F",
                    "\u63D0\u793A", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File outDir = new File(cfg.getOutputDir());
        if (!outDir.exists() && !outDir.mkdirs()) {
            JOptionPane.showMessageDialog(this,
                    "\u65E0\u6CD5\u521B\u5EFA\u8F93\u51FA\u76EE\u5F55: " + cfg.getOutputDir(),
                    "\u9519\u8BEF", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cancelled = false;
        exportBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setMaximum(projects.size());

        ReportExporter exporter = new ReportExporter(cfg);
        currentExporter = exporter;
        if (!exporter.jarExists()) {
            JOptionPane.showMessageDialog(this,
                    "\u627E\u4E0D\u5230 lib/sonar-cnes-report-5.0.3.jar\n\u8BF7\u786E\u4FDD lib \u76EE\u5F55\u4E0B\u5B58\u5728\u8BE5\u6587\u4EF6\u3002",
                    "\u9519\u8BEF", JOptionPane.ERROR_MESSAGE);
            exportBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            return;
        }

        String javaPath = resolveJavaPath();

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                exporter.exportBatch(projects, javaPath, new ReportExporter.ExportCallback() {
                    @Override
                    public void onProjectStart(SonarProject project, String branch, int current, int total) {
                        SwingUtilities.invokeLater(() -> {
                            String label = branch.isEmpty()
                                    ? project.getName()
                                    : project.getName() + " [" + branch + "]";
                            log(String.format("[%d/%d] Exporting: %s ...", current, total, label));
                        });
                    }

                    @Override
                    public void onProjectDone(SonarProject project, String branch, boolean success, String message) {
                        SwingUtilities.invokeLater(() -> {
                            String status = success ? "OK" : "FAIL";
                            String label = branch.isEmpty()
                                    ? project.getName()
                                    : project.getName() + " [" + branch + "]";
                            log(String.format("  -> %s: %s %s", label, status, message));
                            int done = progressBar.getValue() + 1;
                            progressBar.setValue(done);
                            progressBar.setString(String.format("%d/%d", done, projects.size()));
                        });
                    }

                    @Override
                    public void onLog(String line) {
                        SwingUtilities.invokeLater(() -> log("  " + line));
                    }

                    @Override
                    public void onAllDone(java.util.List<String> zipPaths) {
                        SwingUtilities.invokeLater(() -> {
                            exportBtn.setEnabled(true);
                            stopBtn.setEnabled(false);
                            if (!cancelled) {
                                log("\n========== All done ==========");
                                if (!zipPaths.isEmpty()) {
                                    log("Generated zip files:");
                                    for (String zp : zipPaths) {
                                        log("  " + new java.io.File(zp).getName());
                                    }
                                }
                                progressBar.setString("Done");
                            } else {
                                log("\n========== Cancelled ==========");
                                progressBar.setString("Cancelled");
                            }
                        });
                    }
                });
                return null;
            }
        };
        worker.execute();
    }

    private String resolveJavaPath() {
        File embeddedJava = new File("jdk-21/bin/java.exe");
        if (embeddedJava.exists()) return embeddedJava.getAbsolutePath();
        embeddedJava = new File("jdk-21/bin/java");
        if (embeddedJava.exists()) return embeddedJava.getAbsolutePath();

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome != null) {
            File javaHomeBin = new File(javaHome, "bin/java.exe");
            if (javaHomeBin.exists()) return javaHomeBin.getAbsolutePath();
            javaHomeBin = new File(javaHome, "bin/java");
            if (javaHomeBin.exists()) return javaHomeBin.getAbsolutePath();
        }

        return "java";
    }

    private void log(String message) {
        logArea.append(message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void setApiClient(SonarApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void setOnExportListener(OnExportListener listener) {
        this.listener = listener;
    }

    public void applyToConfig(ExportConfig config) {
        config.setAuthor(authorField.getText().trim());
        config.setLanguage((String) languageCombo.getSelectedItem());
        config.setOutputDir(outputDirField.getText().trim());
        config.setEnableReport(docxCheck.isSelected());
        config.setEnableSpreadsheet(xlsxCheck.isSelected());
        config.setEnableCsv(csvCheck.isSelected());
        config.setEnableMarkdown(mdCheck.isSelected());
    }

    public interface OnExportListener {
        ExportConfig getConfig();
        List<SonarProject> getSelectedProjects();
    }
}
