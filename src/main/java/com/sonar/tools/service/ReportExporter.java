package com.sonar.tools.service;

import com.sonar.tools.model.ExportConfig;
import com.sonar.tools.model.SonarProject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ReportExporter {
    private static final String JAR_NAME = "sonar-cnes-report-5.0.3.jar";

    private final ExportConfig config;
    private final String jarPath;

    public ReportExporter(ExportConfig config) {
        this.config = config;
        File libDir = new File("lib");
        this.jarPath = new File(libDir, JAR_NAME).getAbsolutePath();
    }

    public interface ExportCallback {
        void onProjectStart(SonarProject project, String branch, int current, int total);
        void onProjectDone(SonarProject project, String branch, boolean success, String message);
        void onLog(String line);
        void onAllDone(List<String> zipPaths);
    }

    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    public void exportBatch(List<SonarProject> projects, String javaPath, ExportCallback callback) {
        cancelled = false;

        // Group tasks by project key to handle multi-branch per project
        // LinkedHashMap preserves insertion order
        Map<String, ProjectTasks> grouped = new LinkedHashMap<>();
        for (SonarProject p : projects) {
            if (!p.isSelected()) continue;
            grouped.computeIfAbsent(p.getKey(), k -> new ProjectTasks(p));
            List<String> branches = p.getSelectedBranches();
            if (branches.isEmpty()) {
                grouped.get(p.getKey()).branches.add("");
            } else {
                for (String b : branches) {
                    grouped.get(p.getKey()).branches.add(b);
                }
            }
        }

        // Flatten to ordered task list
        List<Task> tasks = new ArrayList<>();
        for (ProjectTasks pt : grouped.values()) {
            for (String branch : pt.branches) {
                tasks.add(new Task(pt.project, branch));
            }
        }

        int total = tasks.size();
        int current = 0;
        Set<String> zippedProjects = new HashSet<>();
        List<String> allZips = new ArrayList<>();

        for (Task task : tasks) {
            if (cancelled) break;
            current++;
            callback.onProjectStart(task.project, task.branch, current, total);
            boolean ok = doExport(task.project, task.branch, javaPath, callback);
            callback.onProjectDone(task.project, task.branch, ok, ok ? "OK" : "Failed");

            // Check if all branches of this project are done
            String key = task.project.getKey();
            if (!zippedProjects.contains(key) && isProjectDone(key, tasks, current)) {
                zippedProjects.add(key);
                callback.onLog("Packing zip for " + task.project.getName() + "...");
                String zipPath = zipProjectDir(task.project);
                if (zipPath != null) {
                    allZips.add(zipPath);
                    callback.onLog("  -> " + new File(zipPath).getName());
                }
            }
        }

        callback.onAllDone(allZips);
    }

    private boolean isProjectDone(String projectKey, List<Task> tasks, int completedCount) {
        int projectTotal = 0;
        int projectDone = 0;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).project.getKey().equals(projectKey)) {
                projectTotal++;
                if (i < completedCount) projectDone++;
            }
        }
        return projectDone == projectTotal;
    }

    private boolean doExport(SonarProject project, String branch, String javaPath, ExportCallback callback) {
        try {
            List<String> cmd = buildCommand(project, branch, javaPath);
            callback.onLog("Running: " + String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            pb.directory(new File("."));

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                callback.onLog("  " + line);
            }

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (IOException | InterruptedException e) {
            callback.onLog("  Error: " + e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    private List<String> buildCommand(SonarProject project, String branch, String javaPath) {
        List<String> cmd = new ArrayList<>();
        cmd.add(javaPath != null ? javaPath : "java");
        cmd.add("-jar");
        cmd.add(jarPath);

        cmd.add("-s");
        cmd.add(config.getServerUrl());

        if (config.getToken() != null && !config.getToken().isEmpty()) {
            cmd.add("-t");
            cmd.add(config.getToken());
        }

        cmd.add("-p");
        cmd.add(project.getKey());

        if (branch != null && !branch.isEmpty()) {
            cmd.add("-b");
            cmd.add(branch);
        }

        if (config.getAuthor() != null && !config.getAuthor().isEmpty()) {
            cmd.add("-a");
            cmd.add(config.getAuthor());
        }

        cmd.add("-l");
        cmd.add(config.getLanguage());

        String safeKey = sanitizePath(project.getKey());
        String projectOutput = config.getOutputDir() + File.separator + safeKey;
        if (branch != null && !branch.isEmpty()) {
            projectOutput += File.separator + sanitizePath(branch);
        }
        new File(projectOutput).mkdirs();
        cmd.add("-o");
        cmd.add(projectOutput);

        if (!config.isEnableReport()) {
            cmd.add("-w");
        }
        if (!config.isEnableSpreadsheet()) {
            cmd.add("-e");
        }
        if (!config.isEnableCsv()) {
            cmd.add("-f");
        }
        if (!config.isEnableMarkdown()) {
            cmd.add("-m");
        }

        return cmd;
    }

    /** Zip a single project's output directory, named as yyyyMMdd-HHmm-projectName.zip */
    private String zipProjectDir(SonarProject project) {
        String safeKey = sanitizePath(project.getKey());
        File projectDir = new File(config.getOutputDir(), safeKey);
        if (!projectDir.exists()) return null;

        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmm").format(new java.util.Date());
        String safeName = sanitizePath(project.getName());
        String zipFileName = timestamp + "-" + safeName + ".zip";
        File zipFile = new File(config.getOutputDir(), zipFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)))) {
            Path sourcePath = projectDir.toPath();
            Files.walk(sourcePath)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(p -> {
                        ZipEntry entry = new ZipEntry(sourcePath.relativize(p).toString().replace('\\', '/'));
                        try {
                            zos.putNextEntry(entry);
                            Files.copy(p, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            // skip failed files
                        }
                    });
            return zipFile.getAbsolutePath();
        } catch (IOException e) {
            return null;
        }
    }

    private static String sanitizePath(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public boolean jarExists() {
        return new File(jarPath).exists();
    }

    // === Helper classes ===
    private static class ProjectTasks {
        final SonarProject project;
        final List<String> branches = new ArrayList<>();
        ProjectTasks(SonarProject project) { this.project = project; }
    }

    private static class Task {
        final SonarProject project;
        final String branch;
        Task(SonarProject project, String branch) {
            this.project = project;
            this.branch = branch;
        }
    }
}
