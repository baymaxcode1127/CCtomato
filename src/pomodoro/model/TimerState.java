package pomodoro.model;

public enum TimerState {
    IDLE("就绪"),
    WORKING("工作中"),
    SHORT_BREAK("短休息"),
    LONG_BREAK("长休息"),
    PAUSED("已暂停");

    private final String label;

    TimerState(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
