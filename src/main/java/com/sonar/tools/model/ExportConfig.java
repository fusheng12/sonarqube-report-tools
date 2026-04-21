package com.sonar.tools.model;

public class ExportConfig {
    private String serverUrl;
    private String token;
    private String author;
    private String language;
    private String outputDir;
    private boolean enableReport;    // docx
    private boolean enableSpreadsheet; // xlsx
    private boolean enableCsv;
    private boolean enableMarkdown;

    public ExportConfig() {
        this.serverUrl = "http://localhost:9000";
        this.language = "en_US";
        this.outputDir = "./output";
        this.enableReport = true;
        this.enableSpreadsheet = true;
        this.enableCsv = false;
        this.enableMarkdown = true;
    }

    public String getServerUrl() { return serverUrl; }
    public void setServerUrl(String serverUrl) { this.serverUrl = serverUrl; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public boolean isEnableReport() { return enableReport; }
    public void setEnableReport(boolean enableReport) { this.enableReport = enableReport; }

    public boolean isEnableSpreadsheet() { return enableSpreadsheet; }
    public void setEnableSpreadsheet(boolean enableSpreadsheet) { this.enableSpreadsheet = enableSpreadsheet; }

    public boolean isEnableCsv() { return enableCsv; }
    public void setEnableCsv(boolean enableCsv) { this.enableCsv = enableCsv; }

    public boolean isEnableMarkdown() { return enableMarkdown; }
    public void setEnableMarkdown(boolean enableMarkdown) { this.enableMarkdown = enableMarkdown; }
}
