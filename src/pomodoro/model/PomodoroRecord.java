package pomodoro.model;

public class PomodoroRecord {
    private String date;      // "2026-06-09"
    private String taskId;    // associated task, can be null
    private String taskTitle; // cached title for display

    public PomodoroRecord() {
        this.date = java.time.LocalDate.now().toString();
    }

    public PomodoroRecord(String taskId, String taskTitle) {
        this();
        this.taskId = taskId;
        this.taskTitle = taskTitle;
    }

    public PomodoroRecord(String date, String taskId, String taskTitle) {
        this.date = date;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
}
