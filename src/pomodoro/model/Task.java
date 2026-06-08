package pomodoro.model;

import java.time.LocalDate;
import java.util.UUID;

public class Task {
    private String id;
    private String title;
    private int pomodoroCount;
    private boolean completed;
    private String createdAt;

    public Task() {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = LocalDate.now().toString();
    }

    public Task(String title) {
        this();
        this.title = title;
    }

    public Task(String id, String title, int pomodoroCount, boolean completed, String createdAt) {
        this.id = id;
        this.title = title;
        this.pomodoroCount = pomodoroCount;
        this.completed = completed;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPomodoroCount() { return pomodoroCount; }
    public void setPomodoroCount(int pomodoroCount) { this.pomodoroCount = pomodoroCount; }

    public void incrementPomodoro() { this.pomodoroCount++; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        String status = completed ? "[✓]" : "[ ]";
        return status + " " + title + "  [🍅×" + pomodoroCount + "]";
    }

    public String getDisplayTitle() {
        return title;
    }
}
