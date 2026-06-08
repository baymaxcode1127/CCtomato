package pomodoro.view;

import pomodoro.controller.AppController;
import pomodoro.model.TimerModel;
import pomodoro.util.DataManager;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main application window with tabbed layout and system tray support.
 */
public class MainFrame extends JFrame {

    private final AppController controller;

    private JTabbedPane tabbedPane;
    private TimerPanel timerPanel;
    private TaskPanel taskPanel;
    private StatsPanel statsPanel;
    private JLabel statusLabel;

    // System tray
    private TrayIcon trayIcon;
    private boolean traySupported;

    public MainFrame(AppController controller, TimerModel model, DataManager dataManager) {
        this.controller = controller;

        setTitle("🍅 番茄钟");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(520, 620);
        setLocationRelativeTo(null);
        setResizable(false);

        initUI(model, dataManager);
        setupSystemTray();
        setupWindowClose();
    }

    private void initUI(TimerModel model, DataManager dataManager) {
        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        contentPanel.setBackground(new Color(245, 245, 245));

        // Tabbed pane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));

        timerPanel = new TimerPanel(model);
        taskPanel = new TaskPanel(dataManager);
        statsPanel = new StatsPanel(dataManager);

        tabbedPane.addTab("⏱ 计时", timerPanel);
        tabbedPane.addTab("📋 任务", taskPanel);
        tabbedPane.addTab("📊 统计", statsPanel);

        // Refresh stats when switching to stats tab
        tabbedPane.addChangeListener(e -> {
            if (tabbedPane.getSelectedIndex() == 2) {
                statsPanel.refresh();
            }
        });

        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        // Status bar at bottom
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusLabel = new JLabel("🍅 今天: " + dataManager.getTodayCount()
                + "  |  本周: " + dataManager.getWeekCount()
                + "  |  总计: " + dataManager.getTotalCount());
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(120, 120, 120));
        statusBar.add(statusLabel, BorderLayout.WEST);

        // Settings button
        JButton settingsBtn = new JButton("⚙ 设置");
        settingsBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        settingsBtn.setFocusPainted(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        settingsBtn.setForeground(new Color(100, 100, 100));
        settingsBtn.addActionListener(e -> controller.showSettings());
        statusBar.add(settingsBtn, BorderLayout.EAST);

        contentPanel.add(statusBar, BorderLayout.SOUTH);

        setContentPane(contentPanel);
    }

    private void setupSystemTray() {
        traySupported = SystemTray.isSupported();
        if (!traySupported) {
            // Fallback: actually close on exit
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            return;
        }

        try {
            // Create a simple tray icon programmatically
            Image iconImage = createTrayIconImage();
            PopupMenu popup = new PopupMenu();

            MenuItem showItem = new MenuItem("显示窗口");
            showItem.addActionListener(e -> {
                setVisible(true);
                setState(Frame.NORMAL);
                toFront();
            });
            popup.add(showItem);

            MenuItem exitItem = new MenuItem("退出");
            exitItem.addActionListener(e -> {
                trayIcon = null;
                SystemTray.getSystemTray().remove(trayIcon);
                System.exit(0);
            });
            popup.add(exitItem);

            trayIcon = new TrayIcon(iconImage, "番茄钟", popup);
            trayIcon.setImageAutoSize(true);

            // Double-click tray icon to show window
            trayIcon.addActionListener(e -> {
                setVisible(true);
                setState(Frame.NORMAL);
                toFront();
            });

            SystemTray.getSystemTray().add(trayIcon);

            // Register tray icon for notifications
            pomodoro.util.NotificationUtil.setTrayIcon(trayIcon);

        } catch (Exception e) {
            System.err.println("Failed to setup system tray: " + e.getMessage());
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

    private void setupWindowClose() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (traySupported && trayIcon != null) {
                    // Minimize to tray
                    setVisible(false);
                } else {
                    System.exit(0);
                }
            }
        });
    }

    private Image createTrayIconImage() {
        // Create a 16x16 red circle as tray icon
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(new Color(220, 80, 80));
        g2d.fillOval(1, 1, size - 2, size - 2);
        g2d.setColor(new Color(180, 40, 40));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(1, 1, size - 2, size - 2);
        g2d.dispose();
        return img;
    }

    // ==================== Public methods ====================

    public TimerPanel getTimerPanel() {
        return timerPanel;
    }

    public TaskPanel getTaskPanel() {
        return taskPanel;
    }

    public StatsPanel getStatsPanel() {
        return statsPanel;
    }

    public void updateStatusBar(int today, int week, int total) {
        statusLabel.setText("🍅 今天: " + today + "  |  本周: " + week + "  |  总计: " + total);
    }

    public void refreshTaskList() {
        timerPanel.setTasks(controller.getDataManager().getTasks());
        taskPanel.refresh();
    }
}
