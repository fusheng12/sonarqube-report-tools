package com.sonar.tools.ui;

import com.sonar.tools.model.SonarProject;
import com.sonar.tools.service.SonarApiClient;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class ProjectPanel extends JPanel {
    private final ProjectTableModel tableModel;
    private final JTable table;
    private final JButton fetchBtn;
    private final JButton selectAllBtn;
    private final JButton deselectAllBtn;
    private final JLabel countLabel;

    private SonarApiClient apiClient;

    private static final int COL_CHECK = 0;
    private static final int COL_KEY = 1;
    private static final int COL_NAME = 2;
    private static final int COL_BRANCH = 3;
    private static final int COL_VISIBILITY = 4;

    public ProjectPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fetchBtn = new JButton("\u83B7\u53D6\u9879\u76EE\u5217\u8868");
        selectAllBtn = new JButton("\u5168\u9009");
        deselectAllBtn = new JButton("\u5168\u4E0D\u9009");
        countLabel = new JLabel("\u5171 0 \u4E2A\u9879\u76EE");

        toolbar.add(fetchBtn);
        toolbar.add(selectAllBtn);
        toolbar.add(deselectAllBtn);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(countLabel);

        add(toolbar, BorderLayout.NORTH);

        // Table
        tableModel = new ProjectTableModel();
        table = new JTable(tableModel);
        table.setRowHeight(25);
        table.getTableHeader().setReorderingAllowed(false);

        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(COL_CHECK).setPreferredWidth(40);
        colModel.getColumn(COL_KEY).setPreferredWidth(150);
        colModel.getColumn(COL_NAME).setPreferredWidth(200);
        colModel.getColumn(COL_BRANCH).setPreferredWidth(180);
        colModel.getColumn(COL_VISIBILITY).setPreferredWidth(80);

        // Checkbox editor for select column
        table.getColumnModel().getColumn(COL_CHECK).setCellEditor(new DefaultCellEditor(new JCheckBox()));

        // Branch column: clickable to show branch selector popup
        table.getColumnModel().getColumn(COL_BRANCH).setCellEditor(new BranchCellEditor());
        // Mouse double-click on branch column triggers editing
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() < 2) return;
                int col = table.columnAtPoint(e.getPoint());
                if (col == COL_BRANCH) {
                    int row = table.rowAtPoint(e.getPoint());
                    table.editCellAt(row, col);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Help text
        JLabel helpLabel = new JLabel("<html><body>"
                + "\u70B9\u51FB\u201C\u83B7\u53D6\u9879\u76EE\u5217\u8868\u201D\u81EA\u52A8\u52A0\u8F7D\u9879\u76EE\u548C\u5206\u652F\uFF0C"
                + "\u70B9\u51FB\u201C\u5206\u652F\u201D\u5217\u53EF\u52FE\u9009\u8981\u5BFC\u51FA\u7684\u5206\u652F"
                + "</body></html>");
        helpLabel.setForeground(Color.GRAY);
        helpLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(helpLabel, BorderLayout.SOUTH);

        // Actions
        fetchBtn.addActionListener(e -> fetchProjects());
        selectAllBtn.addActionListener(e -> {
            for (SonarProject p : tableModel.getProjects()) p.setSelected(true);
            tableModel.fireTableDataChanged();
            updateCount();
        });
        deselectAllBtn.addActionListener(e -> {
            for (SonarProject p : tableModel.getProjects()) p.setSelected(false);
            tableModel.fireTableDataChanged();
            updateCount();
        });
    }

    private void fetchProjects() {
        if (apiClient == null) {
            JOptionPane.showMessageDialog(this,
                    "\u8BF7\u5148\u5728\u201C\u670D\u52A1\u5668\u914D\u7F6E\u201D\u4E2D\u8FDE\u63A5\u670D\u52A1\u5668",
                    "\u63D0\u793A", JOptionPane.WARNING_MESSAGE);
            return;
        }

        fetchBtn.setEnabled(false);
        fetchBtn.setText("\u6B63\u5728\u83B7\u53D6...");

        SwingWorker<List<SonarProject>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<SonarProject> doInBackground() throws Exception {
                List<SonarProject> projects = apiClient.fetchProjects();
                // Auto-fetch branches for each project
                for (SonarProject p : projects) {
                    try {
                        List<String> branches = apiClient.fetchBranches(p.getKey());
                        p.setBranches(branches);
                        // Default: select the main branch only
                        String mainBranch = p.getBranch();
                        p.getSelectedBranches().clear();
                        if (mainBranch != null && !mainBranch.isEmpty()) {
                            p.getSelectedBranches().add(mainBranch);
                        } else if (!branches.isEmpty()) {
                            p.getSelectedBranches().add(branches.get(0));
                        }
                    } catch (Exception ex) {
                        // Skip branch fetch failures, keep default
                    }
                }
                return projects;
            }

            @Override
            protected void done() {
                fetchBtn.setEnabled(true);
                fetchBtn.setText("\u83B7\u53D6\u9879\u76EE\u5217\u8868");
                try {
                    List<SonarProject> projects = get();
                    tableModel.setProjects(projects);
                    updateCount();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ProjectPanel.this,
                            "\u83B7\u53D6\u9879\u76EE\u5931\u8D25: " + ex.getMessage(),
                            "\u9519\u8BEF", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void updateCount() {
        int total = tableModel.getProjectCount();
        int selected = tableModel.getSelectedCount();
        int branches = 0;
        for (SonarProject p : tableModel.getProjects()) {
            if (p.isSelected()) {
                branches += Math.max(1, p.getSelectedBranches().size());
            }
        }
        countLabel.setText(String.format("\u5171 %d \u9879\u76EE, \u5DF2\u9009 %d \u9879\u76EE (%d \u4E2A\u4EFB\u52A1)", total, selected, branches));
    }

    public void setApiClient(SonarApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public List<SonarProject> getSelectedProjects() {
        List<SonarProject> result = new ArrayList<>();
        for (SonarProject p : tableModel.getProjects()) {
            if (!p.isSelected()) continue;
            List<String> branches = p.getSelectedBranches();
            if (branches.isEmpty()) {
                result.add(p);
            } else {
                for (String branch : branches) {
                    SonarProject task = new SonarProject(p.getKey(), p.getName(), branch, p.getVisibility());
                    task.setBranches(p.getBranches());
                    List<String> singleBranch = new ArrayList<>();
                    singleBranch.add(branch);
                    task.setSelectedBranches(singleBranch);
                    result.add(task);
                }
            }
        }
        return result;
    }

    // === Branch cell editor: click to show branch selection popup ===
    private class BranchCellEditor extends AbstractCellEditor implements javax.swing.table.TableCellEditor {
        private SonarProject currentProject;
        private int currentRow;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            currentProject = tableModel.getProject(row);
            // Show branch selection dialog immediately
            SwingUtilities.invokeLater(() -> showBranchPopup());
            // Return a label showing current value while dialog is opening
            JLabel label = new JLabel(value != null ? value.toString() : "");
            label.setOpaque(true);
            label.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return label;
        }

        @Override
        public Object getCellEditorValue() {
            return currentProject != null ? currentProject.getBranchDisplay() : "";
        }

        private void showBranchPopup() {
            if (currentProject == null || currentProject.getBranches().isEmpty()) {
                fireEditingCanceled();
                return;
            }

            List<String> allBranches = currentProject.getBranches();
            List<String> currentSelected = new ArrayList<>(currentProject.getSelectedBranches());

            JPanel panel = new JPanel(new BorderLayout(5, 5));
            panel.add(new JLabel("\u52FE\u9009\u8981\u5BFC\u51FA\u7684\u5206\u652F:"), BorderLayout.NORTH);

            JPanel checkPanel = new JPanel(new GridLayout(0, 1));
            JCheckBox[] checkboxes = new JCheckBox[allBranches.size()];
            for (int i = 0; i < allBranches.size(); i++) {
                checkboxes[i] = new JCheckBox(allBranches.get(i), currentSelected.contains(allBranches.get(i)));
                checkPanel.add(checkboxes[i]);
            }

            JScrollPane scroll = new JScrollPane(checkPanel);
            scroll.setPreferredSize(new Dimension(250, Math.min(200, allBranches.size() * 28 + 10)));
            panel.add(scroll, BorderLayout.CENTER);

            int result = JOptionPane.showConfirmDialog(table, panel,
                    currentProject.getName() + " - \u5206\u652F\u9009\u62E9",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                List<String> selected = new ArrayList<>();
                for (int i = 0; i < checkboxes.length; i++) {
                    if (checkboxes[i].isSelected()) {
                        selected.add(allBranches.get(i));
                    }
                }
                currentProject.setSelectedBranches(selected);
                tableModel.fireTableRowsUpdated(currentRow, currentRow);
                updateCount();
            }
            fireEditingStopped();
        }
    }

    // === Table Model ===
    private static class ProjectTableModel extends AbstractTableModel {
        private final String[] COLUMNS = {"\u9009\u62E9", "\u9879\u76EE Key", "\u9879\u76EE\u540D\u79F0", "\u5206\u652F", "\u53EF\u89C1\u6027"};
        private List<SonarProject> projects = new ArrayList<>();

        void setProjects(List<SonarProject> projects) {
            this.projects = projects != null ? projects : new ArrayList<>();
            fireTableDataChanged();
        }

        List<SonarProject> getProjects() { return projects; }
        SonarProject getProject(int row) { return projects.get(row); }
        int getProjectCount() { return projects.size(); }

        int getSelectedCount() {
            int c = 0;
            for (SonarProject p : projects) if (p.isSelected()) c++;
            return c;
        }

        @Override
        public int getRowCount() { return projects.size(); }
        @Override
        public int getColumnCount() { return COLUMNS.length; }
        @Override
        public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Class<?> getColumnClass(int col) {
            if (col == COL_CHECK) return Boolean.class;
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return col == COL_CHECK || col == COL_BRANCH;
        }

        @Override
        public Object getValueAt(int row, int col) {
            SonarProject p = projects.get(row);
            return switch (col) {
                case COL_CHECK -> p.isSelected();
                case COL_KEY -> p.getKey();
                case COL_NAME -> p.getName();
                case COL_BRANCH -> p.getBranchDisplay();
                case COL_VISIBILITY -> p.getVisibility();
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            if (col == COL_CHECK && value instanceof Boolean) {
                projects.get(row).setSelected((Boolean) value);
                fireTableCellUpdated(row, col);
            }
        }
    }
}
