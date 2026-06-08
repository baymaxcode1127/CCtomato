package pomodoro.model;

public class AppSettings {
    private int workDuration;        // minutes
    private int shortBreakDuration;  // minutes
    private int longBreakDuration;   // minutes
    private int longBreakInterval;   // number of pomodoros before long break

    public AppSettings() {
        this.workDuration = 25;
        this.shortBreakDuration = 5;
        this.longBreakDuration = 15;
        this.longBreakInterval = 4;
    }

    public AppSettings(int workDuration, int shortBreakDuration, int longBreakDuration, int longBreakInterval) {
        this.workDuration = workDuration;
        this.shortBreakDuration = shortBreakDuration;
        this.longBreakDuration = longBreakDuration;
        this.longBreakInterval = longBreakInterval;
    }

    public int getWorkDuration() { return workDuration; }
    public void setWorkDuration(int workDuration) { this.workDuration = workDuration; }

    public int getShortBreakDuration() { return shortBreakDuration; }
    public void setShortBreakDuration(int shortBreakDuration) { this.shortBreakDuration = shortBreakDuration; }

    public int getLongBreakDuration() { return longBreakDuration; }
    public void setLongBreakDuration(int longBreakDuration) { this.longBreakDuration = longBreakDuration; }

    public int getLongBreakInterval() { return longBreakInterval; }
    public void setLongBreakInterval(int longBreakInterval) { this.longBreakInterval = longBreakInterval; }
}
