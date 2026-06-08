package pomodoro.view;

import pomodoro.model.Task;
import pomodoro.model.TimerModel;
import pomodoro.model.TimerState;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Timer display panel — large countdown, progress bar, control buttons.
 */
public class TimerPanel extends JPanel implements PropertyChangeListener {

    private final TimerModel model;

    // UI components
    private JLabel timeLabel;
    private JLabel phaseLabel;
    private JProgressBar progressBar;
    private JButton startPauseButton;
    private JButton resetButton;
    private JButton skipButton;
    private JLabel completedLabel;
    private JComboBox<Task> taskComboBox;

    // Fonts
    private Font timeFont;
    private Font phaseFont;

    private Runnable onStartPause;
    private Runnable onReset;
    private Runnable onSkip;
    private java.util.function.Consumer<Task> onTaskSelected;

    public TimerPanel(TimerModel model) {
        this.model = model;
        this.model.addPropertyChangeListener(this);

        timeFont = new Font("Monospaced", Font.BOLD, 80);
        phaseFont = new Font("SansSerif", Font.PLAIN, 18);

        initUI();
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        setBackground(new Color(250, 250, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 20, 8, 20);
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;

        // --- Phase label ---
        phaseLabel = new JLabel("就绪", SwingConstants.CENTER);
        phaseLabel.setFont(phaseFont);
        phaseLabel.setForeground(new Color(100, 100, 100));
        add(phaseLabel, gbc);

        // --- Time display ---
        timeLabel = new JLabel(model.getFormattedTime(), SwingConstants.CENTER);
        timeLabel.setFont(timeFont);
        timeLabel.setForeground(new Color(50, 50, 50));
        add(timeLabel, gbc);

        // --- Progress bar ---
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(350, 10));
        progressBar.setForeground(new Color(220, 80, 80));
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setBorderPainted(false);
        add(progressBar, gbc);

        // --- Completed count ---
        completedLabel = new JLabel("本轮已完成: 0 个番茄", SwingConstants.CENTER);
        completedLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        completedLabel.setForeground(new Color(130, 130, 130));
        add(completedLabel, gbc);

        // --- Task selector ---
        JPanel taskPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        taskPanel.setOpaque(false);
        JLabel taskLabel = new JLabel("关联任务:");
        taskLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        taskPanel.add(taskLabel);

        taskComboBox = new JComboBox<>();
        taskComboBox.setPreferredSize(new Dimension(200, 28));
        taskComboBox.setFont(new Font("SansSerif", Font.PLAIN, 14));
        taskComboBox.addActionListener(e -> {
            if (onTaskSelected != null) {
                Task selected = (Task) taskComboBox.getSelectedItem();
                onTaskSelected.accept(selected);
            }
        });
        taskPanel.add(taskComboBox);
        add(taskPanel, gbc);

        // --- Button panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);

        startPauseButton = createButton("▶ 开始", new Color(76, 175, 80));
        startPauseButton.addActionListener(e -> {
            if (onStartPause != null) onStartPause.run();
        });
        buttonPanel.add(startPauseButton);

        resetButton = createButton("↺ 重置", new Color(158, 158, 158));
        resetButton.addActionListener(e -> {
            if (onReset != null) onReset.run();
        });
        buttonPanel.add(resetButton);

        skipButton = createButton("⏭ 跳过", new Color(255, 152, 0));
        skipButton.addActionListener(e -> {
            if (onSkip != null) onSkip.run();
        });
        buttonPanel.add(skipButton);

        add(buttonPanel, gbc);
    }

    private JButton createButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        btn.setBackground(bgColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ==================== Callbacks ====================

    public void setOnStartPause(Runnable callback) { this.onStartPause = callback; }
    public void setOnReset(Runnable callback) { this.onReset = callback; }
    public void setOnSkip(Runnable callback) { this.onSkip = callback; }
    public void setOnTaskSelected(java.util.function.Consumer<Task> callback) { this.onTaskSelected = callback; }

    // ==================== Property Change Listener ====================

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        switch (evt.getPropertyName()) {
            case TimerModel.PROP_TIME:
                timeLabel.setText(model.getFormattedTime());
                break;
            case TimerModel.PROP_PROGRESS:
                progressBar.setValue((int) (model.getProgress() * 100));
                break;
            case TimerModel.PROP_STATE:
                updateForState(model.getState());
                break;
            case TimerModel.PROP_COMPLETED_COUNT:
                completedLabel.setText("本轮已完成: " + model.getCompletedPomodoros() + " 个番茄");
                break;
        }

        // Update progress bar color based on state
        TimerState state = model.getState();
        if (state == TimerState.WORKING) {
            progressBar.setForeground(new Color(220, 80, 80));
        } else if (state == TimerState.SHORT_BREAK) {
            progressBar.setForeground(new Color(76, 175, 80));
        } else if (state == TimerState.LONG_BREAK) {
            progressBar.setForeground(new Color(33, 150, 243));
        }
    }

    private void updateForState(TimerState state) {
        switch (state) {
            case IDLE:
                phaseLabel.setText("准备开始");
                phaseLabel.setForeground(new Color(100, 100, 100));
                startPauseButton.setText("▶ 开始");
                startPauseButton.setBackground(new Color(76, 175, 80));
                skipButton.setEnabled(false);
                break;
            case WORKING:
                phaseLabel.setText("🍅 工作中 — 保持专注！");
                phaseLabel.setForeground(new Color(198, 40, 40));
                startPauseButton.setText("⏸ 暂停");
                startPauseButton.setBackground(new Color(244, 67, 54));
                skipButton.setEnabled(true);
                break;
            case SHORT_BREAK:
                phaseLabel.setText("☕ 短休息 — 放松一下");
                phaseLabel.setForeground(new Color(56, 142, 60));
                startPauseButton.setText("⏸ 暂停");
                startPauseButton.setBackground(new Color(244, 67, 54));
                skipButton.setEnabled(true);
                break;
            case LONG_BREAK:
                phaseLabel.setText("🌴 长休息 — 好好休息");
                phaseLabel.setForeground(new Color(21, 101, 192));
                startPauseButton.setText("⏸ 暂停");
                startPauseButton.setBackground(new Color(244, 67, 54));
                skipButton.setEnabled(true);
                break;
            case PAUSED:
                phaseLabel.setText("⏸ 已暂停");
                phaseLabel.setForeground(new Color(150, 150, 150));
                startPauseButton.setText("▶ 继续");
                startPauseButton.setBackground(new Color(76, 175, 80));
                skipButton.setEnabled(true);
                break;
        }
    }

    // ==================== Public methods for task list ====================

    public void setTasks(java.util.List<Task> tasks) {
        Task currentSelection = (Task) taskComboBox.getSelectedItem();
        taskComboBox.removeAllItems();
        // Add a "no task" option
        Task noTask = new Task("(无任务)");
        taskComboBox.addItem(noTask);
        for (Task t : tasks) {
            taskComboBox.addItem(t);
        }
        // Restore selection
        if (currentSelection != null) {
            for (int i = 0; i < taskComboBox.getItemCount(); i++) {
                if (taskComboBox.getItemAt(i).getId().equals(currentSelection.getId())) {
                    taskComboBox.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    public Task getSelectedTask() {
        Task selected = (Task) taskComboBox.getSelectedItem();
        if (selected != null && selected.getTitle().equals("(无任务)")) {
            return null;
        }
        return selected;
    }
}
