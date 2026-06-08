package pomodoro.model;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Core timer engine for the Pomodoro timer.
 * Driven by javax.swing.Timer (fires on EDT).
 * Fires property change events for UI binding.
 */
public class TimerModel {

    // Property names for UI binding
    public static final String PROP_STATE = "state";
    public static final String PROP_TIME = "time";
    public static final String PROP_PROGRESS = "progress";
    public static final String PROP_PHASE_LABEL = "phaseLabel";
    public static final String PROP_COMPLETED_COUNT = "completedCount";
    public static final String PROP_POMODORO_COMPLETED = "pomodoroCompleted";

    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final javax.swing.Timer swingTimer;

    private TimerState state = TimerState.IDLE;
    private int remainingSeconds = 0;
    private int totalSeconds = 0;
    private int completedPomodoros = 0;
    private boolean isPomodoroJustCompleted = false;

    private AppSettings settings;

    public TimerModel(AppSettings settings) {
        this.settings = settings;
        this.remainingSeconds = settings.getWorkDuration() * 60;
        this.totalSeconds = remainingSeconds;

        swingTimer = new javax.swing.Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tick();
            }
        });
    }

    // ==================== Public API ====================

    public void start() {
        if (state == TimerState.IDLE) {
            // Starting fresh
            setState(TimerState.WORKING);
            remainingSeconds = settings.getWorkDuration() * 60;
            totalSeconds = remainingSeconds;
            isPomodoroJustCompleted = false;
        } else if (state == TimerState.PAUSED) {
            // Resuming
            if (remainingSeconds == settings.getWorkDuration() * 60
                    || remainingSeconds == settings.getShortBreakDuration() * 60
                    || remainingSeconds == settings.getLongBreakDuration() * 60) {
                // Was paused at the start of a phase — need to determine correct state
                // remainingSeconds matches work → WORKING, short break → SHORT_BREAK, etc.
            }
            setState(getStateForRemaining());
        }
        swingTimer.start();
    }

    public void pause() {
        if (state == TimerState.WORKING || state == TimerState.SHORT_BREAK || state == TimerState.LONG_BREAK) {
            swingTimer.stop();
            setState(TimerState.PAUSED);
        }
    }

    public void reset() {
        swingTimer.stop();
        setState(TimerState.IDLE);
        remainingSeconds = settings.getWorkDuration() * 60;
        totalSeconds = remainingSeconds;
        completedPomodoros = 0;
        isPomodoroJustCompleted = false;
        pcs.firePropertyChange(PROP_TIME, null, getFormattedTime());
        pcs.firePropertyChange(PROP_PROGRESS, null, getProgress());
        pcs.firePropertyChange(PROP_COMPLETED_COUNT, null, completedPomodoros);
    }

    public void skip() {
        swingTimer.stop();
        advanceToNextPhase();
    }

    public void updateSettings(AppSettings newSettings) {
        boolean wasWorking = (state == TimerState.WORKING);
        this.settings = newSettings;
        reset();
        if (wasWorking) {
            // Optionally: auto-start with new settings
        }
    }

    // ==================== Getters ====================

    public TimerState getState() {
        return state;
    }

    public int getRemainingSeconds() {
        return remainingSeconds;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public int getCompletedPomodoros() {
        return completedPomodoros;
    }

    public String getFormattedTime() {
        int minutes = remainingSeconds / 60;
        int seconds = remainingSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public double getProgress() {
        if (totalSeconds == 0) return 0.0;
        return 1.0 - (double) remainingSeconds / totalSeconds;
    }

    public String getPhaseLabel() {
        return state.getLabel();
    }

    public boolean isRunning() {
        return state == TimerState.WORKING || state == TimerState.SHORT_BREAK || state == TimerState.LONG_BREAK;
    }

    public boolean isPomodoroJustCompleted() {
        return isPomodoroJustCompleted;
    }

    public void clearPomodoroCompletedFlag() {
        isPomodoroJustCompleted = false;
    }

    public AppSettings getSettings() {
        return settings;
    }

    // ==================== Listeners ====================

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    // ==================== Internal ====================

    private void tick() {
        remainingSeconds--;
        pcs.firePropertyChange(PROP_TIME, null, getFormattedTime());
        pcs.firePropertyChange(PROP_PROGRESS, null, getProgress());

        if (remainingSeconds <= 0) {
            swingTimer.stop();
            if (state == TimerState.WORKING) {
                // Work session completed → increment pomodoro count
                completedPomodoros++;
                pcs.firePropertyChange(PROP_COMPLETED_COUNT, null, completedPomodoros);
                isPomodoroJustCompleted = true;
                pcs.firePropertyChange(PROP_POMODORO_COMPLETED, false, true);
            }
            advanceToNextPhase();
        }
    }

    private void advanceToNextPhase() {
        if (state == TimerState.WORKING || state == TimerState.IDLE) {
            // After work → break
            if (completedPomodoros > 0 && completedPomodoros % settings.getLongBreakInterval() == 0) {
                setState(TimerState.LONG_BREAK);
                remainingSeconds = settings.getLongBreakDuration() * 60;
            } else {
                setState(TimerState.SHORT_BREAK);
                remainingSeconds = settings.getShortBreakDuration() * 60;
            }
        } else if (state == TimerState.SHORT_BREAK || state == TimerState.LONG_BREAK) {
            // After break → work
            setState(TimerState.WORKING);
            remainingSeconds = settings.getWorkDuration() * 60;
        } else if (state == TimerState.PAUSED) {
            // Skip from paused state
            if (remainingSeconds == settings.getWorkDuration() * 60 || remainingSeconds == 0) {
                // was in work phase
                if (completedPomodoros > 0 && completedPomodoros % settings.getLongBreakInterval() == 0) {
                    setState(TimerState.LONG_BREAK);
                    remainingSeconds = settings.getLongBreakDuration() * 60;
                } else {
                    setState(TimerState.SHORT_BREAK);
                    remainingSeconds = settings.getShortBreakDuration() * 60;
                }
            } else {
                setState(TimerState.WORKING);
                remainingSeconds = settings.getWorkDuration() * 60;
            }
        }

        totalSeconds = remainingSeconds;
        pcs.firePropertyChange(PROP_TIME, null, getFormattedTime());
        pcs.firePropertyChange(PROP_PROGRESS, null, getProgress());
    }

    private void setState(TimerState newState) {
        TimerState oldState = this.state;
        this.state = newState;
        pcs.firePropertyChange(PROP_STATE, oldState, newState);
        pcs.firePropertyChange(PROP_PHASE_LABEL, null, getPhaseLabel());
    }

    private TimerState getStateForRemaining() {
        if (remainingSeconds == settings.getWorkDuration() * 60) return TimerState.WORKING;
        if (remainingSeconds == settings.getShortBreakDuration() * 60) return TimerState.SHORT_BREAK;
        if (remainingSeconds == settings.getLongBreakDuration() * 60) return TimerState.LONG_BREAK;
        // Was in the middle of something
        return TimerState.WORKING; // safe default
    }
}
