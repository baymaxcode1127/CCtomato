package pomodoro.view;

import pomodoro.model.AppSettings;

import javax.swing.*;
import java.awt.*;

/**
 * Settings dialog — customize work/break durations and long break interval.
 */
public class SettingsDialog extends JDialog {

    private final AppSettings settings;
    private boolean confirmed = false;

    private JSpinner workSpinner;
    private JSpinner shortBreakSpinner;
    private JSpinner longBreakSpinner;
    private JSpinner intervalSpinner;

    public SettingsDialog(JFrame parent, AppSettings settings) {
        super(parent, "⚙ 设置", true);
        this.settings = settings;
        initUI();
        pack();
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));
        panel.setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        // Work duration
        addLabel(panel, gbc, row, "工作时长（分钟）:");
        workSpinner = createSpinner(settings.getWorkDuration(), 1, 120);
        panel.add(workSpinner, createRightGbc(gbc, row));
        row++;

        // Short break duration
        addLabel(panel, gbc, row, "短休息时长（分钟）:");
        shortBreakSpinner = createSpinner(settings.getShortBreakDuration(), 1, 60);
        panel.add(shortBreakSpinner, createRightGbc(gbc, row));
        row++;

        // Long break duration
        addLabel(panel, gbc, row, "长休息时长（分钟）:");
        longBreakSpinner = createSpinner(settings.getLongBreakDuration(), 1, 120);
        panel.add(longBreakSpinner, createRightGbc(gbc, row));
        row++;

        // Long break interval
        addLabel(panel, gbc, row, "长休息间隔（番茄数）:");
        intervalSpinner = createSpinner(settings.getLongBreakInterval(), 1, 10);
        panel.add(intervalSpinner, createRightGbc(gbc, row));
        row++;

        // Separator
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 8, 10, 8);
        JSeparator sep = new JSeparator();
        panel.add(sep, gbc);
        row++;

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        JButton okBtn = new JButton("✓ 确定");
        okBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        okBtn.setBackground(new Color(76, 175, 80));
        okBtn.setForeground(Color.WHITE);
        okBtn.setFocusPainted(false);
        okBtn.setBorderPainted(false);
        okBtn.setPreferredSize(new Dimension(100, 36));
        okBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> {
            confirmed = true;
            dispose();
        });
        buttonPanel.add(okBtn);

        JButton cancelBtn = new JButton("取消");
        cancelBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cancelBtn.setBackground(new Color(180, 180, 180));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setPreferredSize(new Dimension(100, 36));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> {
            confirmed = false;
            dispose();
        });
        buttonPanel.add(cancelBtn);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 8, 0, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(buttonPanel, gbc);

        setContentPane(panel);
    }

    private void addLabel(JPanel panel, GridBagConstraints gbc, int row, String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        panel.add(label, gbc);
    }

    private GridBagConstraints createRightGbc(GridBagConstraints template, int row) {
        GridBagConstraints gbc = (GridBagConstraints) template.clone();
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        return gbc;
    }

    private JSpinner createSpinner(int value, int min, int max) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font("SansSerif", Font.PLAIN, 14));
        spinner.setPreferredSize(new Dimension(80, 30));
        return spinner;
    }

    // ==================== Getters ====================

    public boolean isConfirmed() {
        return confirmed;
    }

    public AppSettings getSettings() {
        return new AppSettings(
                (int) workSpinner.getValue(),
                (int) shortBreakSpinner.getValue(),
                (int) longBreakSpinner.getValue(),
                (int) intervalSpinner.getValue()
        );
    }
}
