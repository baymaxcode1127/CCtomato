package pomodoro.util;

import pomodoro.model.AppSettings;
import pomodoro.model.PomodoroRecord;
import pomodoro.model.Task;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages data persistence for tasks, pomodoro records, and settings.
 * Saves to ~/.pomodoro/data.json
 */
public class DataManager {

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".pomodoro");
    private static final Path DATA_FILE = DATA_DIR.resolve("data.json");

    private List<Task> tasks = new ArrayList<>();
    private List<PomodoroRecord> records = new ArrayList<>();
    private AppSettings settings = new AppSettings();

    // ==================== Load / Save ====================

    public void load() {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }
            if (!Files.exists(DATA_FILE)) {
                save(); // create initial empty file
                return;
            }

            String content = Files.readString(DATA_FILE);
            Map<String, Object> root = JsonUtil.parseObject(content);

            // Load settings
            Map<String, Object> settingsMap = JsonUtil.getObject(root, "settings");
            if (!settingsMap.isEmpty()) {
                settings = new AppSettings(
                        JsonUtil.getInt(settingsMap, "workDuration", 25),
                        JsonUtil.getInt(settingsMap, "shortBreakDuration", 5),
                        JsonUtil.getInt(settingsMap, "longBreakDuration", 15),
                        JsonUtil.getInt(settingsMap, "longBreakInterval", 4)
                );
            }

            // Load tasks
            List<Map<String, Object>> taskList = JsonUtil.getArray(root, "tasks");
            tasks.clear();
            for (Map<String, Object> t : taskList) {
                Task task = new Task(
                        JsonUtil.getString(t, "id", UUID.randomUUID().toString().substring(0, 8)),
                        JsonUtil.getString(t, "title", ""),
                        JsonUtil.getInt(t, "pomodoroCount", 0),
                        JsonUtil.getBoolean(t, "completed"),
                        JsonUtil.getString(t, "createdAt", LocalDate.now().toString())
                );
                tasks.add(task);
            }

            // Load records
            List<Map<String, Object>> recordList = JsonUtil.getArray(root, "records");
            records.clear();
            for (Map<String, Object> r : recordList) {
                PomodoroRecord record = new PomodoroRecord(
                        JsonUtil.getString(r, "date", ""),
                        JsonUtil.getString(r, "taskId", ""),
                        JsonUtil.getString(r, "taskTitle", "")
                );
                records.add(record);
            }

        } catch (IOException e) {
            System.err.println("Failed to load data: " + e.getMessage());
        }
    }

    public void save() {
        try {
            if (!Files.exists(DATA_DIR)) {
                Files.createDirectories(DATA_DIR);
            }

            Map<String, Object> root = new LinkedHashMap<>();

            // Save settings
            root.put("settings", JsonUtil.toMap(
                    "workDuration", settings.getWorkDuration(),
                    "shortBreakDuration", settings.getShortBreakDuration(),
                    "longBreakDuration", settings.getLongBreakDuration(),
                    "longBreakInterval", settings.getLongBreakInterval()
            ));

            // Save tasks
            List<Map<String, Object>> taskList = new ArrayList<>();
            for (Task t : tasks) {
                taskList.add(JsonUtil.toMap(
                        "id", t.getId(),
                        "title", t.getTitle(),
                        "pomodoroCount", t.getPomodoroCount(),
                        "completed", t.isCompleted(),
                        "createdAt", t.getCreatedAt()
                ));
            }
            root.put("tasks", taskList);

            // Save records
            List<Map<String, Object>> recordList = new ArrayList<>();
            for (PomodoroRecord r : records) {
                recordList.add(JsonUtil.toMap(
                        "date", r.getDate(),
                        "taskId", r.getTaskId() != null ? r.getTaskId() : "",
                        "taskTitle", r.getTaskTitle() != null ? r.getTaskTitle() : ""
                ));
            }
            root.put("records", recordList);

            String json = JsonUtil.toJson(root);
            Files.writeString(DATA_FILE, json);

        } catch (IOException e) {
            System.err.println("Failed to save data: " + e.getMessage());
        }
    }

    // ==================== Task CRUD ====================

    public List<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
        save();
    }

    public void updateTask(Task task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(task.getId())) {
                tasks.set(i, task);
                break;
            }
        }
        save();
    }

    public void deleteTask(String taskId) {
        tasks.removeIf(t -> t.getId().equals(taskId));
        save();
    }

    public Task getTaskById(String taskId) {
        for (Task t : tasks) {
            if (t.getId().equals(taskId)) return t;
        }
        return null;
    }

    // ==================== Records ====================

    public List<PomodoroRecord> getRecords() {
        return records;
    }

    public void addRecord(PomodoroRecord record) {
        records.add(record);
        save();
    }

    /**
     * Get today's completed pomodoro count
     */
    public int getTodayCount() {
        String today = LocalDate.now().toString();
        return (int) records.stream().filter(r -> today.equals(r.getDate())).count();
    }

    /**
     * Get this week's completed pomodoro count (Monday to Sunday)
     */
    public int getWeekCount() {
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return (int) records.stream()
                .filter(r -> {
                    try {
                        LocalDate d = LocalDate.parse(r.getDate());
                        return !d.isBefore(monday) && !d.isAfter(sunday);
                    } catch (Exception e) { return false; }
                })
                .count();
    }

    /**
     * Get total completed pomodoro count
     */
    public int getTotalCount() {
        return records.size();
    }

    /**
     * Get daily counts for current week (Monday=0 ... Sunday=6)
     */
    public int[] getWeekDailyCounts() {
        int[] counts = new int[7];
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (PomodoroRecord r : records) {
            try {
                LocalDate d = LocalDate.parse(r.getDate());
                long daysBetween = d.toEpochDay() - monday.toEpochDay();
                if (daysBetween >= 0 && daysBetween < 7) {
                    counts[(int) daysBetween]++;
                }
            } catch (Exception ignored) { }
        }
        return counts;
    }

    // ==================== Settings ====================

    public AppSettings getSettings() {
        return settings;
    }

    public void updateSettings(AppSettings newSettings) {
        this.settings = newSettings;
        save();
    }
}
