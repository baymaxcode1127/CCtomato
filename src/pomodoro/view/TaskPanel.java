package pomodoro.view;

import pomodoro.model.Task;
import pomodoro.util.DataManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Task management panel — list tasks, add/edit/delete.
 */
public class TaskPanel extends JPanel {

    private final DataManager dataManager;
    private DefaultListModel<Task> listModel;
    private JList<Task> taskList;

    public TaskPanel(DataManager dataManager) {
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(250, 250, 250));

        // --- Title label ---
        JLabel titleLabel = new JLabel("📋 任务列表", SwingConstants.LEFT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(titleLabel, BorderLayout.NORTH);

        // --- Task list ---
        listModel = new DefaultListModel<>();
        taskList = new JList<>(listModel);
        taskList.setFont(new Font("SansSerif", Font.PLAIN, 15));
        taskList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        taskList.setFixedCellHeight(36);
        taskList.setCellRenderer(new TaskCellRenderer());

        JScrollPane scrollPane = new JScrollPane(taskList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        add(scrollPane, BorderLayout.CENTER);

        // --- Button panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false);

        JButton addBtn = createSmallButton("+ 添加", new Color(76, 175, 80));
        addBtn.addActionListener(e -> addTask());
        buttonPanel.add(addBtn);

        JButton editBtn = createSmallButton("✎ 编辑", new Color(33, 150, 243));
        editBtn.addActionListener(e -> editTask());
        buttonPanel.add(editBtn);

        JButton deleteBtn = createSmallButton("✕ 删除", new Color(244, 67, 54));
        deleteBtn.addActionListener(e -> deleteTask());
        buttonPanel.add(deleteBtn);

        JButton completeBtn = createSmallButton("✓ 完成", new Color(156, 39, 176));
        completeBtn.addActionListener(e -> toggleComplete());
        buttonPanel.add(completeBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        refresh();
    }

    private JButton createSmallButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 32));
        return btn;
    }

    // ==================== CRUD Operations ====================

    private void addTask() {
        String title = JOptionPane.showInputDialog(this,
                "请输入任务名称:", "添加任务", JOptionPane.PLAIN_MESSAGE);
        if (title != null && !title.trim().isEmpty()) {
            Task task = new Task(title.trim());
            dataManager.addTask(task);
            refresh();
        }
    }

    private void editTask() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个任务。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String title = JOptionPane.showInputDialog(this,
                "修改任务名称:", selected.getTitle());
        if (title != null && !title.trim().isEmpty()) {
            selected.setTitle(title.trim());
            dataManager.updateTask(selected);
            refresh();
        }
    }

    private void deleteTask() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个任务。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this,
                "确定要删除任务 \"" + selected.getTitle() + "\" 吗？",
                "确认删除", JOptionPane.OK_CANCEL_OPTION);
        if (choice == JOptionPane.OK_OPTION) {
            dataManager.deleteTask(selected.getId());
            refresh();
        }
    }

    private void toggleComplete() {
        Task selected = taskList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个任务。", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        selected.setCompleted(!selected.isCompleted());
        dataManager.updateTask(selected);
        refresh();
    }

    // ==================== Public ====================

    public void refresh() {
        Task selected = taskList.getSelectedValue();
        listModel.clear();
        List<Task> tasks = dataManager.getTasks();
        for (Task t : tasks) {
            listModel.addElement(t);
        }
        // Restore selection
        if (selected != null) {
            for (int i = 0; i < listModel.size(); i++) {
                if (listModel.get(i).getId().equals(selected.getId())) {
                    taskList.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    // ==================== Cell Renderer ====================

    private static class TaskCellRenderer extends JLabel implements ListCellRenderer<Task> {
        public TaskCellRenderer() {
            setOpaque(true);
            setFont(new Font("SansSerif", Font.PLAIN, 15));
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Task> list, Task task,
                                                       int index, boolean isSelected, boolean cellHasFocus) {
            setText(task.toString());
            if (isSelected) {
                setBackground(new Color(200, 220, 255));
                setForeground(Color.BLACK);
            } else {
                setBackground(Color.WHITE);
                setForeground(task.isCompleted() ? new Color(150, 150, 150) : Color.BLACK);
            }
            setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            return this;
        }
    }
}
