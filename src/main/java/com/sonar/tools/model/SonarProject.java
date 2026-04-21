package com.sonar.tools.model;

import java.util.ArrayList;
import java.util.List;

public class SonarProject {
    private String key;
    private String name;
    private String visibility;
    private boolean selected;
    private List<String> branches;
    private List<String> selectedBranches;

    public SonarProject() {
        this.selected = true;
        this.branches = new ArrayList<>();
        this.selectedBranches = new ArrayList<>();
    }

    public SonarProject(String key, String name, String mainBranch, String visibility) {
        this.key = key;
        this.name = name;
        this.visibility = visibility;
        this.selected = true;
        this.branches = new ArrayList<>();
        this.selectedBranches = new ArrayList<>();
        if (mainBranch != null) {
            this.branches.add(mainBranch);
            this.selectedBranches.add(mainBranch);
        }
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public List<String> getBranches() { return branches; }
    public void setBranches(List<String> branches) {
        this.branches = branches != null ? branches : new ArrayList<>();
    }

    public List<String> getSelectedBranches() { return selectedBranches; }
    public void setSelectedBranches(List<String> selectedBranches) {
        this.selectedBranches = selectedBranches != null ? selectedBranches : new ArrayList<>();
    }

    /** Get main branch (first one) for display */
    public String getBranch() {
        return selectedBranches.isEmpty() ? "" : selectedBranches.get(0);
    }

    public void setBranch(String branch) {
        // kept for compatibility - adds to selected branches
        if (branch != null && !branch.isEmpty()) {
            if (!branches.contains(branch)) branches.add(branch);
            if (!selectedBranches.contains(branch)) selectedBranches.add(branch);
        }
    }

    /** Get branch summary for display in table, e.g. "main, dev" */
    public String getBranchDisplay() {
        if (selectedBranches.isEmpty()) return "(none)";
        if (selectedBranches.size() <= 2) return String.join(", ", selectedBranches);
        return selectedBranches.get(0) + " +" + (selectedBranches.size() - 1);
    }

    @Override
    public String toString() {
        return name + " (" + key + ")";
    }
}
