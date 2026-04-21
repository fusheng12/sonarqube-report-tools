package com.sonar.tools.service;

import com.sonar.tools.model.SonarProject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SonarApiClient {
    private static final int CONNECT_TIMEOUT = 10000;
    private static final int READ_TIMEOUT = 30000;

    private String serverUrl;
    private String token;

    public SonarApiClient(String serverUrl, String token) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        this.token = token;
    }

    public boolean testConnection() throws IOException {
        String url = serverUrl + "/api/system/status";
        String response = httpGet(url);
        return response.contains("\"status\":\"UP\"");
    }

    public List<SonarProject> fetchProjects() throws IOException {
        List<SonarProject> allProjects = new ArrayList<>();
        int page = 1;
        int pageSize = 500;

        while (true) {
            String url = serverUrl + "/api/projects/search?ps=" + pageSize + "&p=" + page;
            String response = httpGet(url);

            List<SonarProject> projects = parseProjects(response);
            allProjects.addAll(projects);

            // Check if there are more pages
            int total = extractInt(response, "\"total\":", ",");
            if (total <= allProjects.size()) {
                break;
            }
            page++;
        }
        return allProjects;
    }

    public List<String> fetchBranches(String projectKey) throws IOException {
        String url = serverUrl + "/api/project_branches/list?project=" + encode(projectKey);
        String response = httpGet(url);
        return parseBranches(response);
    }

    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);

        // SonarQube accepts basic auth with token as username and empty password
        if (token != null && !token.isEmpty()) {
            String auth = token + ":";
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encoded);
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errBody = new StringBuilder();
            String line;
            while ((line = errReader.readLine()) != null) {
                errBody.append(line);
            }
            errReader.close();
            throw new IOException("HTTP " + code + ": " + errBody);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder body = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }
        reader.close();
        conn.disconnect();
        return body.toString();
    }

    private List<SonarProject> parseProjects(String json) {
        List<SonarProject> projects = new ArrayList<>();

        // Simple JSON parsing without external libraries
        // Extract the "components" array
        int componentsStart = json.indexOf("\"components\":[");
        if (componentsStart < 0) {
            return projects;
        }
        String arrayContent = extractArrayContent(json, componentsStart + "\"components\":".length());

        // Split by object boundaries - find each { }
        List<String> objects = splitJsonObjects(arrayContent);
        for (String obj : objects) {
            SonarProject p = new SonarProject();
            p.setKey(extractString(obj, "\"key\":\"", "\""));
            p.setName(extractString(obj, "\"name\":\"", "\""));
            p.setVisibility(extractString(obj, "\"visibility\":\"", "\""));

            // Default branch
            String mainBranch = extractString(obj, "\"mainBranch\":\"", "\"");
            p.setBranch(mainBranch != null ? mainBranch : "main");
            projects.add(p);
        }
        return projects;
    }

    private List<String> parseBranches(String json) {
        List<String> branches = new ArrayList<>();
        int branchesStart = json.indexOf("\"branches\":[");
        if (branchesStart < 0) {
            return branches;
        }
        String arrayContent = extractArrayContent(json, branchesStart + "\"branches\":".length());
        List<String> objects = splitJsonObjects(arrayContent);
        for (String obj : objects) {
            String name = extractString(obj, "\"name\":\"", "\"");
            if (name != null) {
                branches.add(name);
            }
        }
        return branches;
    }

    private String extractArrayContent(String json, int startIndex) {
        int depth = 0;
        int i = startIndex;
        for (; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) break;
            }
        }
        if (i >= json.length()) return json.substring(startIndex);
        return json.substring(startIndex + 1, i);
    }

    private List<String> splitJsonObjects(String arrayContent) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    objects.add(arrayContent.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    private String extractString(String json, String prefix, String suffix) {
        int start = json.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int end = json.indexOf(suffix, start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    private int extractInt(String json, String prefix, String suffix) {
        String val = extractString(json, prefix, suffix);
        if (val == null) return 0;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }
}
