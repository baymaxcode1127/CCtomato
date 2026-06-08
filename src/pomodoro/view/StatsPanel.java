package pomodoro.view;

import pomodoro.util.DataManager;

import javax.swing.*;
import java.awt.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

/**
 * Statistics panel — shows today/week/total counts and a weekly bar chart.
 */
public class StatsPanel extends JPanel {

    private final DataManager dataManager;

    // Labels
    private JLabel todayLabel;
    private JLabel weekLabel;
    private JLabel totalLabel;
    private BarChartPanel chartPanel;

    public StatsPanel(DataManager dataManager) {
        this.dataManager = dataManager;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(15, 15));
        setBackground(new Color(250, 250, 250));

        // --- Top summary cards ---
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setOpaque(false);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        todayLabel = createSummaryCard("今日", "0");
        weekLabel = createSummaryCard("本周", "0");
        totalLabel = createSummaryCard("总计", "0");

        summaryPanel.add(todayLabel);
        summaryPanel.add(weekLabel);
        summaryPanel.add(totalLabel);
        add(summaryPanel, BorderLayout.NORTH);

        // --- Bar chart ---
        chartPanel = new BarChartPanel();
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                "本周每日番茄数"));
        add(chartPanel, BorderLayout.CENTER);

        // --- Tip ---
        JLabel tipLabel = new JLabel("💡 每完成一个番茄钟，统计会自动更新", SwingConstants.CENTER);
        tipLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        tipLabel.setForeground(new Color(150, 150, 150));
        tipLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(tipLabel, BorderLayout.SOUTH);
    }

    private JLabel createSummaryCard(String title, String value) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(130, 130, 130));
        card.add(titleLbl, BorderLayout.NORTH);

        JLabel valueLbl = new JLabel(value, SwingConstants.CENTER);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 32));
        valueLbl.setForeground(new Color(220, 80, 80));
        valueLbl.setName("value");
        card.add(valueLbl, BorderLayout.CENTER);

        return valueLbl;
    }

    // ==================== Public ====================

    public void refresh() {
        int today = dataManager.getTodayCount();
        int week = dataManager.getWeekCount();
        int total = dataManager.getTotalCount();

        todayLabel.setText(String.valueOf(today));
        weekLabel.setText(String.valueOf(week));
        totalLabel.setText(String.valueOf(total));

        chartPanel.setData(dataManager.getWeekDailyCounts());
        chartPanel.repaint();
    }

    // ==================== Bar Chart Panel ====================

    private static class BarChartPanel extends JPanel {

        private int[] data = new int[7];
        private static final String[] DAY_LABELS = {"一", "二", "三", "四", "五", "六", "日"};
        private static final Color BAR_COLOR = new Color(220, 80, 80);
        private static final Color BAR_COLOR_TODAY = new Color(180, 30, 30);

        public void setData(int[] data) {
            this.data = data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int padding = 50;
            int chartW = width - padding * 2;
            int chartH = height - padding * 2;
            int barCount = 7;
            int barGap = 12;
            int barWidth = (chartW - barGap * (barCount - 1)) / barCount;

            // Find max value for scaling
            int maxVal = 1;
            for (int v : data) {
                if (v > maxVal) maxVal = v;
            }
            // Round up to nearest nice number
            maxVal = Math.max(maxVal, 4);
            maxVal = ((maxVal + 3) / 4) * 4;

            // Draw axes
            g2d.setColor(new Color(200, 200, 200));
            g2d.setStroke(new BasicStroke(1f));
            int baseline = padding + chartH;
            g2d.drawLine(padding, baseline, padding + chartW, baseline);

            // Draw horizontal grid lines and labels
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int gridLines = 4;
            for (int i = 0; i <= gridLines; i++) {
                int y = baseline - (chartH * i / gridLines);
                int val = maxVal * i / gridLines;
                g2d.setColor(new Color(230, 230, 230));
                g2d.drawLine(padding, y, padding + chartW, y);
                g2d.setColor(new Color(150, 150, 150));
                g2d.drawString(String.valueOf(val), padding - 30, y + 4);
            }

            // Draw bars
            int todayIndex = LocalDate.now().getDayOfWeek().getValue() - 1; // Monday=0

            for (int i = 0; i < barCount; i++) {
                int barH = (int) ((double) data[i] / maxVal * chartH);
                int x = padding + i * (barWidth + barGap);
                int y = baseline - barH;

                // Bar color
                if (i == todayIndex) {
                    g2d.setColor(BAR_COLOR_TODAY);
                } else {
                    g2d.setColor(BAR_COLOR);
                }
                g2d.fillRoundRect(x, y, barWidth, barH, 6, 6);

                // Bar value on top
                if (data[i] > 0) {
                    g2d.setColor(new Color(80, 80, 80));
                    g2d.setFont(new Font("SansSerif", Font.BOLD, 11));
                    String valStr = String.valueOf(data[i]);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textW = fm.stringWidth(valStr);
                    g2d.drawString(valStr, x + barWidth / 2 - textW / 2, y - 6);
                }

                // Day label below
                g2d.setColor(new Color(120, 120, 120));
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                String dayLabel = DAY_LABELS[i];
                FontMetrics fm = g2d.getFontMetrics();
                int labelW = fm.stringWidth(dayLabel);
                g2d.drawString(dayLabel, x + barWidth / 2 - labelW / 2, baseline + 18);
            }
        }
    }
}
