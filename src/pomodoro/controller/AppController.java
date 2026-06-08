package pomodoro.controller;

import pomodoro.model.*;
import pomodoro.util.DataManager;
import pomodoro.util.NotificationUtil;
import pomodoro.view.MainFrame;
import pomodoro.view.SettingsDialog;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Main application controller — connects model, view, and data.
 */
public class AppController implements PropertyChangeListener {

    private final TimerModel model;
    private final DataManager dataManager;
    private MainFrame mainFrame;

    public AppController() {
        this.dataManager = new DataManager();
        this.dataManager.load();

        this.model = new TimerModel(dataManager.getSettings());
        this.model.addPropertyChangeListener(this);
    }

    public void start() {
        // Build UI on EDT
        SwingUtilities.invokeLater(() -> {
            mainFrame = new MainFrame(this, model, dataManager);
            setupCallbacks();
            mainFrame.setVisible(true);

            // Initial task list
            mainFrame.getTimerPanel().setTasks(dataManager.getTasks());
        });
    }

    private void setupCallbacks() {
        // Timer panel callbacks
        mainFrame.getTimerPanel().setOnStartPause(() -> {
            if (model.getState() == TimerState.IDLE || model.getState() == TimerState.PAUSED) {
                model.start();
            } else {
                model.pause();
            }
        });

        mainFrame.getTimerPanel().setOnReset(() -> {
            int choice = JOptionPane.showConfirmDialog(mainFrame,
                    "确定要重置计时器吗？当前进度将丢失。",
                    "确认重置", JOptionPane.OK_CANCEL_OPTION);
            if (choice == JOptionPane.OK_OPTION) {
                model.reset();
            }
        });

        mainFrame.getTimerPanel().setOnSkip(() -> {
            model.skip();
        });

        mainFrame.getTimerPanel().setOnTaskSelected(task -> {
            // Task selection handled — stored in TimerPanel combo
        });
    }

    // ==================== Timer Events ====================

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (TimerModel.PROP_POMODORO_COMPLETED.equals(evt.getPropertyName())) {
            onPomodoroCompleted();
        }
    }

    private void onPomodoroCompleted() {
        // Get current task from timer panel
        Task activeTask = mainFrame.getTimerPanel().getSelectedTask();

        // Record the pomodoro
        String taskTitle = activeTask != null ? activeTask.getTitle() : "无任务";
        String taskId = activeTask != null ? activeTask.getId() : null;
        PomodoroRecord record = new PomodoroRecord(taskId, taskTitle);
        dataManager.addRecord(record);

        // Update task pomodoro count
        if (activeTask != null) {
            activeTask.incrementPomodoro();
            dataManager.updateTask(activeTask);
            mainFrame.getTimerPanel().setTasks(dataManager.getTasks());
        }

        // Update status bar
        mainFrame.updateStatusBar(dataManager.getTodayCount(),
                dataManager.getWeekCount(),
                dataManager.getTotalCount());

        // Refresh task panel
        mainFrame.getTaskPanel().refresh();

        // Show notification
        NotificationUtil.notifyPomodoroComplete(model.getCompletedPomodoros());

        // Notify break complete when break ends
        // We'll schedule a notification for when the break ends
        // Actually, let's use a simple approach: check next phase
        scheduleBreakEndNotification();
    }

    private void scheduleBreakEndNotification() {
        // Use a background thread to notify when break ends
        // Simple approach: set a flag that the controller checks
        Thread notifier = new Thread(() -> {
            try {
                int breakSeconds;
                if (model.getState() == TimerState.SHORT_BREAK) {
                    breakSeconds = dataManager.getSettings().getShortBreakDuration() * 60;
                } else {
                    breakSeconds = dataManager.getSettings().getLongBreakDuration() * 60;
                }
                Thread.sleep(breakSeconds * 1000L);
                NotificationUtil.notifyBreakComplete();
            } catch (InterruptedException ignored) { }
        });
        notifier.setDaemon(true);
        notifier.start();
    }

    // ==================== Settings ====================

    public void showSettings() {
        SettingsDialog dialog = new SettingsDialog(mainFrame, dataManager.getSettings());
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            AppSettings newSettings = dialog.getSettings();
            dataManager.updateSettings(newSettings);
            model.updateSettings(newSettings);

            JOptionPane.showMessageDialog(mainFrame,
                    "设置已保存！计时器已重置。",
                    "设置已更新", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ==================== Getters ====================

    public DataManager getDataManager() {
        return dataManager;
    }

    public TimerModel getModel() {
        return model;
    }
}
