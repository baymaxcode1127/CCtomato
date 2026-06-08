package pomodoro.util;

import java.awt.*;

/**
 * Utility for system tray notifications.
 */
public class NotificationUtil {

    private static TrayIcon trayIcon;

    public static void setTrayIcon(TrayIcon icon) {
        trayIcon = icon;
    }

    /**
     * Show a system notification bubble.
     */
    public static void showNotification(String title, String message) {
        if (trayIcon != null && SystemTray.isSupported()) {
            trayIcon.displayMessage(title, message, TrayIcon.MessageType.INFO);
        }
    }

    /**
     * Show a notification for pomodoro completion.
     */
    public static void notifyPomodoroComplete(int totalCompleted) {
        showNotification("🍅 番茄钟完成！",
                "太棒了！你已完成 " + totalCompleted + " 个番茄钟。休息一下吧！");
    }

    /**
     * Show a notification for break completion.
     */
    public static void notifyBreakComplete() {
        showNotification("⏰ 休息结束",
                "休息时间到，准备开始新的番茄钟吧！");
    }
}
