package com.sonar.tools;

import com.sonar.tools.ui.MainFrame;

import javax.swing.*;

public class App {
    public static void main(String[] args) {
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to default
        }

        // Set UI defaults for Chinese text rendering
        UIManager.put("OptionPane.messageFont", new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", new java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
