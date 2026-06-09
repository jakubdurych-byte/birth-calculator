import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;

public class BirthCalculator extends JFrame {
    private static final String PREF_THEME_INDEX = "theme.index";
    private static final String PREF_HISTORY = "birthdate.history";
    private static final String PREF_BIRTHDAYS = "birthday.entries";
    private static final int MAX_HISTORY_ITEMS = 12;
    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter STORAGE_DATE_FORMATTER =
        DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String UI_FONT = chooseFont("Inter", "Ubuntu", "Roboto", "Noto Sans", 
        "DejaVu Sans", "Segoe UI", "Arial", Font.SANS_SERIF);
    private static final String MONO_FONT = chooseFont("JetBrains Mono", "Cascadia Code", 
        "Fira Code", "Noto Sans Mono", Font.MONOSPACED);
    private static final Font FONT_BODY = new Font(UI_FONT, Font.PLAIN, 13);
    private static final Font FONT_BODY_BOLD = new Font(UI_FONT, Font.BOLD, 14);
    private static final Font FONT_BUTTON = new Font(UI_FONT, Font.BOLD, 14);
    private static final Font FONT_INPUT = new Font(UI_FONT, Font.BOLD, 20);
    private static final Font FONT_TITLE = new Font(UI_FONT, Font.BOLD, 30);
    private static final Font FONT_MONO = new Font(MONO_FONT, Font.PLAIN, 13);

    private JTextField dateField;
    private JPanel resultsContentPanel;
    private CardLayout resultsLayout;
    private HeaderCard headerCard;
    private MetricCard cardBirthDate;
    private MetricCard cardZodiac;
    private MetricCard cardTodayDate;
    private MetricCard cardMonthsTotal;
    private MetricCard cardNextBirthday;
    private MetricCard cardCountdown;
    private MetricCard cardDaysTotal;
    private MetricCard cardHoursTotal;
    private MetricCard cardMinutesTotal;
    private MetricCard cardSecondsTotal;
    private JLabel liveTimeLabel;
    private JPanel themePanel;
    private JButton historyButton;
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel instructionLabel;
    private JLabel resultsLabel;
    private JPanel mainPanel;
    private JPanel centerPanel;
    private JPanel resultsPanel;
    private JPanel inputPanel;
    private GradientButton calculateButton;
    private GradientButton clearButton;
    private GradientButton calendarButton;
    private GradientButton birthdaysButton;
    private GradientButton birthdaysBackButton;
    private GradientButton removeHistoryButton;
    private Timer realtimeTimer;
    private Theme currentTheme;
    private int globalMouseX = -1;
    private int globalMouseY = -1;
    private LocalDate lastBirthDate;
    private boolean updatingHistory;
    private final Preferences preferences;
    private final java.util.List<BirthdayEntry> birthdayEntries = new ArrayList<>();
    private DefaultListModel<BirthdayEntry> birthdayListModel;
    private JList<BirthdayEntry> birthdayList;
    private JTextField birthdayNameField;
    private JTextField birthdayDateField;
    private JLabel birthdaySummaryLabel;
    private GradientButton birthdayAddButton;
    private GradientButton birthdayRemoveButton;
    private GradientButton birthdayPickDateButton;
    private JLabel birthdaysCountLabel;
    private JPanel cardsPanel;
    private JPanel birthdayCard;
    private CardLayout cardsLayout;
    private BirthdayEntry selectedBirthdayForDetail;

    private static class Star {
        float x, y;
        float size;
        float speed;
        float phase;
        float fallSpeed;

        Star(int width, int height) {
            x = (float) (Math.random() * width);
            y = (float) (Math.random() * height);
            size = (float) (Math.random() * 3.5 + 1.5); // Larger stars
            speed = (float) (Math.random() * 0.003 + 0.001);
            phase = (float) (Math.random() * Math.PI * 2);
            fallSpeed = (float) (Math.random() * 0.3 + 0.05); // Very slow drift downwards
        }
        
        void update(int width, int height) {
            y += fallSpeed;
            if (y > height) {
                y = -size;
                x = (float) (Math.random() * width);
            }
        }
    }
    private Star[] ambientStars;
    private int lastStarWidth = -1;
    private int lastStarHeight = -1;
    
    private JPanel detailPanel;
    private HeaderCard friendHeaderCard;
    private MetricCard friendNextBirthdayCard;
    private MetricCard friendCountdownCard;

    private static class Theme {
        final String name;
        Color primary;
        Color secondary;
        Color accent;
        Color glow;
        Color backgroundTop;
        Color backgroundBottom;
        Color cardBackground;
        Color textMain;
        Color textMuted;
        Color inputBackground;

        Theme(String name, Color primary, Color secondary, Color accent,
              Color glow, Color backgroundTop, Color backgroundBottom, Color cardBackground,
              Color textMain, Color textMuted, Color inputBackground) {
            this.name = name;
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.glow = glow;
            this.backgroundTop = backgroundTop;
            this.backgroundBottom = backgroundBottom;
            this.cardBackground = cardBackground;
            this.textMain = textMain;
            this.textMuted = textMuted;
            this.inputBackground = inputBackground;
        }
    }

    private class GradientButton extends JButton {
        private Color start;
        private Color end;
        private float hoverProgress = 0.0f; // 0.0 = normal, 1.0 = hovered
        private Timer hoverTimer;
        private static final int ANIMATION_DURATION = 150; // ms
        private static final int ANIMATION_FPS = 60; // frames per second
        private long animationStartTime;
        private boolean hovering = false; // true if mouse is currently over the button

        GradientButton(String text, Color start, Color end) {
            super(text);
            this.start = start;
            this.end = end;
            setFont(FONT_BUTTON);
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(146, 42));
            setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));

            // Initialize timer for hover animation
            hoverTimer = new Timer(1000 / ANIMATION_FPS, e -> animateHover());
            hoverTimer.setCoalesce(true); // Only one event per interval

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    hoverTimer.start(); // Start the timer, animateHover will set animationStartTime
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    hoverTimer.start(); // Start the timer, animateHover will set animationStartTime
                }
            });
        }

        void setColors(Color start, Color end) {
            this.start = start;
            this.end = end;
            this.hoverProgress = 0.0f; // Reset hover state
            if (hoverTimer != null && hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            repaint();
        }

        private void animateHover() {
            if (animationStartTime == 0) { // First frame of animation
                animationStartTime = System.currentTimeMillis();
            }
            long currentTime = System.currentTimeMillis();
            float fraction = (float) (currentTime - animationStartTime) / ANIMATION_DURATION;

            if (hovering) { // Mouse entered, animate towards 1.0
                hoverProgress = Math.min(1.0f, fraction);
            } else { // Mouse exited, animate towards 0.0
                hoverProgress = Math.max(0.0f, 1.0f - fraction);
            }

            if ((hovering && hoverProgress == 1.0f) || (!hovering && hoverProgress == 0.0f)) {
                hoverTimer.stop();
                animationStartTime = 0; // Reset for next animation cycle
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int panelW = getWidth();
            int panelH = getHeight();
            int w = panelW - 12;
            int h = panelH - 12;

            float scale = 1.0f + (hoverProgress * 0.04f);
            g2.translate(panelW / 2.0, panelH / 2.0);
            g2.scale(scale, scale);
            g2.translate(-w / 2.0, -h / 2.0);

            if (currentTheme != null && currentTheme.name.startsWith("Test")) {
                // Draw Liquid Glass Button
                boolean isDark = currentTheme.backgroundTop.getRed() < 128;
                
                // Shadow
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(0, 4, w, h - 4, h, h);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(0, 8, w, h - 4, h, h);

                // Base frosted color + hover brightness
                int baseAlpha = isDark ? 30 : 140; // Increased base alpha slightly to show tint
                int hoverAlpha = (int)(hoverProgress * (isDark ? 40 : 60));
                if (getModel().isPressed()) hoverAlpha -= 10;
                g2.setColor(new Color(start.getRed(), start.getGreen(), start.getBlue(), Math.max(0, baseAlpha + hoverAlpha)));
                g2.fillRoundRect(0, 0, w, h - 4, h, h);

                // Top sheen
                int sheenAlpha = isDark ? 60 : 200;
                sheenAlpha += (int)(hoverProgress * 40);
                GradientPaint sheen = new GradientPaint(
                    0, 0, new Color(255, 255, 255, Math.min(255, sheenAlpha)),
                    0, (h - 4) / 2, new Color(255, 255, 255, 0)
                );
                g2.setPaint(sheen);
                g2.fillRoundRect(0, 0, w, h - 4, h, h);
                
                // Crisp border + hover brightness
                int borderAlpha = isDark ? 40 : 180;
                borderAlpha += (int)(hoverProgress * 60);
                g2.setColor(new Color(255, 255, 255, Math.min(255, borderAlpha)));
                g2.drawRoundRect(0, 0, w - 1, h - 5, h, h);

            } else {
                Color drawStart = start;
                Color drawEnd = end;

                // Interpolate base colors for hover effect
                Color animatedStart = interpolateColor(start, end, hoverProgress);
                Color animatedEnd = interpolateColor(end, start, hoverProgress);

                // Apply pressed state if active
                if (getModel().isPressed()) {
                    animatedStart = animatedStart.darker();
                    animatedEnd = animatedEnd.darker();
                }

                GradientPaint gradient = new GradientPaint(0, 0, animatedStart, 0, h, animatedEnd);
                g2.setPaint(gradient);

                // Draw shadow
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, w, h - 2, h, h);

                // Draw main button body
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, w, h - 4, h, h);

                // Animate the white border's alpha based on hoverProgress
                if (hoverProgress > 0.0f) {
                    g2.setColor(new Color(255, 255, 255, (int)(80 * hoverProgress)));
                    g2.drawRoundRect(1, 1, w - 3, h - 7, h, h);
                }
            }

            // DRAW TEXT AND ICON: Ensure visibility and precise centering
            String text = getText();
            Icon icon = getIcon();
            g2.setFont(getFont());
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            
            int textWidth = (text != null && !text.isEmpty()) ? fm.stringWidth(text) : 0;
            int iconWidth = (icon != null) ? icon.getIconWidth() : 0;
            int gap = (textWidth > 0 && iconWidth > 0) ? 8 : 0;
            int totalWidth = iconWidth + gap + textWidth;
            
            int startX = (w - totalWidth) / 2;
            
            if (icon != null) {
                int iconY = (h - 4 - icon.getIconHeight()) / 2;
                icon.paintIcon(this, g2, startX, iconY);
            }
            
            if (text != null && !text.isEmpty()) {
                int textX = startX + iconWidth + gap;
                int textY = (h - 4 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(text, textX, textY);
            }

            g2.dispose();
        }

        // Helper for color interpolation
        private Color interpolateColor(Color c1, Color c2, float fraction) {
            int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * fraction);
            int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * fraction);
            int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * fraction);
            int a = (int) (c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * fraction);
            return new Color(r, g, b, a);
        }
    }

    private static class CalendarDayButton extends JButton {
        private final Theme theme;
        private boolean isToday;
        private boolean isWeekend;
        private boolean isSelected;

        CalendarDayButton(String text, Theme theme) {
            super(text);
            this.theme = theme;
            setFont(FONT_BODY_BOLD);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(42, 36));
        }

        void setStates(boolean today, boolean weekend, boolean selected) {
            this.isToday = today;
            this.isWeekend = weekend;
            this.isSelected = selected;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 4;
            int xOffset = (getWidth() - size) / 2;
            int yOffset = (getHeight() - size) / 2;
            
            Color textColor = theme.textMain;

            if (isSelected) {
                // Subtle glowing ring instead of a heavy solid block
                g2.setColor(new Color(theme.primary.getRed(), theme.primary.getGreen(), theme.primary.getBlue(), 40));
                g2.fillOval(xOffset, yOffset, size, size);
                
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(theme.primary.brighter());
                g2.drawOval(xOffset, yOffset, size, size);
                
                textColor = Color.WHITE;
            } else if (isToday) {
                // Just a very faint border for 'today'
                g2.setColor(new Color(theme.secondary.getRed(), theme.secondary.getGreen(), theme.secondary.getBlue(), 20));
                g2.fillOval(xOffset, yOffset, size, size);
                
                g2.setStroke(new BasicStroke(1.0f));
                g2.setColor(new Color(theme.secondary.getRed(), theme.secondary.getGreen(), theme.secondary.getBlue(), 120));
                g2.drawOval(xOffset, yOffset, size, size);
                
                textColor = theme.secondary.brighter();
            } else if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 15));
                g2.fillOval(xOffset, yOffset, size, size);
                textColor = Color.WHITE;
            } else {
                textColor = isWeekend ? theme.accent : new Color(theme.textMuted.getRed(), theme.textMuted.getGreen(), theme.textMuted.getBlue(), 160);
            }

            if (!isEnabled()) {
                textColor = new Color(theme.textMuted.getRed(), theme.textMuted.getGreen(), theme.textMuted.getBlue(), 50);
            }

            FontMetrics fm = g2.getFontMetrics();
            int cx = (getWidth() - fm.stringWidth(getText())) / 2;
            int cy = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            
            g2.setColor(textColor);
            g2.drawString(getText(), cx, cy);
            
            g2.dispose();
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        private final Theme theme;
        private boolean isHovered = false;
        private boolean isPressed = false;

        ModernScrollBarUI(Theme theme) {
            this.theme = theme;
        }

        @Override
        protected void installListeners() {
            super.installListeners();
            scrollbar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    scrollbar.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    scrollbar.repaint();
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    isPressed = true;
                    scrollbar.repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    isPressed = false;
                    scrollbar.repaint();
                }
            });
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createInvisibleButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createInvisibleButton();
        }

        private JButton createInvisibleButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Completely transparent track to let the card/background show through
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            boolean isDark = theme.backgroundTop.getRed() < 128;
            Color baseColor = isDark ? Color.WHITE : Color.BLACK;
            
            int alphaMain = isPressed ? 140 : (isHovered ? 100 : 30);
            int alphaGlow = isPressed ? 200 : (isHovered ? 160 : 50);
            
            int thumbWidth = isHovered ? 8 : 6;
            int x = thumbBounds.x + (thumbBounds.width - thumbWidth) / 2;
            
            int padY = 4;
            int h = Math.max(16, thumbBounds.height - padY * 2);
            int y = thumbBounds.y + padY;
            
            GradientPaint glassGradient = new GradientPaint(
                x, y, new Color(theme.primary.getRed(), theme.primary.getGreen(), theme.primary.getBlue(), alphaMain),
                x + thumbWidth, y, new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), alphaGlow)
            );
            
            g2.setPaint(glassGradient);
            g2.fillRoundRect(x, y, thumbWidth, h, thumbWidth, thumbWidth);
            
            // Add a glossy bubble reflection (sheen)
            g2.setPaint(new GradientPaint(
                x, y, new Color(255, 255, 255, isHovered ? 140 : 60),
                x, y + h / 2, new Color(255, 255, 255, 0)
            ));
            g2.fillRoundRect(x + 1, y + 1, thumbWidth - 2, h - 2, thumbWidth - 2, thumbWidth - 2);
            
            g2.setColor(new Color(255, 255, 255, isHovered ? 60 : 20));
            g2.drawRoundRect(x, y, thumbWidth, h, thumbWidth, thumbWidth);
            
            g2.dispose();
        }
    }

    private static class VectorIcon implements Icon {
        enum Type { CALENDAR, PLUS, CLOSE, ARROW_LEFT }
        private final Type type;
        private final int width;
        private final int height;
        private final Color color;

        VectorIcon(Type type, int width, int height, Color color) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public int getIconWidth() { return width; }

        @Override
        public int getIconHeight() { return height; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            
            switch (type) {
                case CALENDAR:
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(x + 2, y + 4, width - 4, height - 6, 3, 3);
                    g2.drawLine(x + 2, y + 8, x + width - 2, y + 8);
                    g2.fillRect(x + 5, y + 1, 2, 4);
                    g2.fillRect(x + width - 7, y + 1, 2, 4);
                    g2.fillRect(x + 5, y + 11, 2, 2);
                    g2.fillRect(x + 9, y + 11, 2, 2);
                    g2.fillRect(x + 13, y + 11, 2, 2);
                    g2.fillRect(x + 5, y + 15, 2, 2);
                    g2.fillRect(x + 9, y + 15, 2, 2);
                    g2.fillRect(x + 13, y + 15, 2, 2);
                    break;
                case PLUS:
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + width/2, y + 3, x + width/2, y + height - 3);
                    g2.drawLine(x + 3, y + height/2, x + width - 3, y + height/2);
                    break;
                case CLOSE:
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 4, y + 4, x + width - 4, y + height - 4);
                    g2.drawLine(x + width - 4, y + 4, x + 4, y + height - 4);
                    break;
                case ARROW_LEFT:
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(x + 4, y + height/2, x + width - 4, y + height/2);
                    g2.drawLine(x + 4, y + height/2, x + 10, y + 4);
                    g2.drawLine(x + 4, y + height/2, x + 10, y + height - 4);
                    break;
            }
            g2.dispose();
        }
    }

    private class MetricCard extends JPanel {
        private final JLabel label;
        private final JLabel value;
        private long animationStartTime = 0;
        private int animationDelay = 0;

        public void triggerEntryAnimation(int delayMs) {
            this.animationStartTime = System.currentTimeMillis();
            this.animationDelay = delayMs;
        }

        private float getEntryProgress() {
            if (animationStartTime == 0) return 1.0f;
            long elapsed = System.currentTimeMillis() - (animationStartTime + animationDelay);
            if (elapsed < 0) return 0f;
            if (elapsed >= 400) {
                animationStartTime = 0;
                return 1.0f;
            }
            float p = elapsed / 400f;
            return 1.0f - (float)Math.pow(1.0f - p, 3);
        }

        @Override
        public void paint(Graphics g) {
            float entryProgress = getEntryProgress();
            if (entryProgress == 0f) return;
            if (entryProgress >= 1.0f) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entryProgress));
            g2.translate(0, (int)((1.0f - entryProgress) * 15));
            super.paint(g2);
            g2.dispose();
        }

        MetricCard(String title, String initialValue) {
            setLayout(new BorderLayout(2, 2));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));

            label = new JLabel(title);
            label.setFont(new Font(UI_FONT, Font.PLAIN, 11));
            label.setForeground(currentTheme.textMuted);

            value = new JLabel(initialValue);
            value.setFont(new Font(UI_FONT, Font.BOLD, 17));
            value.setForeground(currentTheme.textMain);

            add(label, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setValue(String val) {
            value.setText(val);
        }

        void updateColors() {
            label.setForeground(currentTheme.textMuted);
            value.setForeground(currentTheme.textMain);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            paintLiquidGlass(g2, getWidth(), getHeight(), 24, false);
            g2.dispose();
        }
    }

    private class HeaderCard extends JPanel {
        private final JLabel titleLabel;
        private final JLabel ageYearsLabel;
        private final JLabel ageDetailLabel;
        private long animationStartTime = 0;
        private int animationDelay = 0;

        public void triggerEntryAnimation(int delayMs) {
            this.animationStartTime = System.currentTimeMillis();
            this.animationDelay = delayMs;
        }

        private float getEntryProgress() {
            if (animationStartTime == 0) return 1.0f;
            long elapsed = System.currentTimeMillis() - (animationStartTime + animationDelay);
            if (elapsed < 0) return 0f;
            if (elapsed >= 400) {
                animationStartTime = 0;
                return 1.0f;
            }
            float p = elapsed / 400f;
            return 1.0f - (float)Math.pow(1.0f - p, 3);
        }

        @Override
        public void paint(Graphics g) {
            float entryProgress = getEntryProgress();
            if (entryProgress == 0f) return;
            if (entryProgress >= 1.0f) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, entryProgress));
            g2.translate(0, (int)((1.0f - entryProgress) * 15));
            super.paint(g2);
            g2.dispose();
        }

        HeaderCard() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

            titleLabel = new JLabel("VĚK DNES");
            titleLabel.setFont(new Font(UI_FONT, Font.BOLD, 11));
            titleLabel.setForeground(new Color(255, 255, 255, 200));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            ageYearsLabel = new JLabel("0 let");
            ageYearsLabel.setFont(new Font(UI_FONT, Font.BOLD, 32));
            ageYearsLabel.setForeground(Color.WHITE);
            ageYearsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            ageDetailLabel = new JLabel("0 měsíců a 0 dní");
            ageDetailLabel.setFont(new Font(UI_FONT, Font.PLAIN, 14));
            ageDetailLabel.setForeground(new Color(255, 255, 255, 220));
            ageDetailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(titleLabel);
            add(Box.createRigidArea(new Dimension(0, 4)));
            add(ageYearsLabel);
            add(Box.createRigidArea(new Dimension(0, 2)));
            add(ageDetailLabel);
        }

        void setAge(int years, int months, int days) {
            ageYearsLabel.setText(years + " let");
            ageDetailLabel.setText(months + " měsíců a " + days + " dní");
        }

        void updateAll(String title, String mainValue, String subValue) {
            titleLabel.setText(title);
            ageYearsLabel.setText(mainValue);
            ageDetailLabel.setText(subValue);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            paintLiquidGlass(g2, getWidth(), getHeight(), 28, true);
            g2.dispose();
        }
    }

    private Map<String, BufferedImage> glassCache = new HashMap<>();

    private void paintLiquidGlass(Graphics2D g2, int width, int height, int radius, boolean isHeader) {
        if (width <= 0 || height <= 0) return;
        String key = width + "x" + height + "_" + radius + "_" + isHeader + "_" + currentTheme.name;
        BufferedImage img = glassCache.get(key);
        if (img == null) {
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = img.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean isDark = currentTheme.backgroundTop.getRed() < 128;
            
            int shadowOffset = 10;
            int glassHeight = height - shadowOffset;

            cg.setColor(new Color(0, 0, 0, 40));
            cg.fillRoundRect(0, 3, width, glassHeight, radius, radius);
            cg.setColor(new Color(0, 0, 0, 20));
            cg.fillRoundRect(0, 6, width, glassHeight, radius, radius);
            cg.setColor(new Color(0, 0, 0, 10));
            cg.fillRoundRect(0, 10, width, glassHeight, radius, radius);

            Color baseColor = isDark ? new Color(255, 255, 255, 12) : new Color(255, 255, 255, 160);
            if (isHeader) {
                GradientPaint headerGrad = new GradientPaint(
                    0, 0, new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 160),
                    width, 0, new Color(currentTheme.secondary.getRed(), currentTheme.secondary.getGreen(), currentTheme.secondary.getBlue(), 160)
                );
                cg.setPaint(headerGrad);
                cg.fillRoundRect(0, 0, width, glassHeight, radius, radius);
            } else {
                cg.setColor(baseColor);
                cg.fillRoundRect(0, 0, width, glassHeight, radius, radius);
            }
            
            GradientPaint sheen = new GradientPaint(
                0, 0, new Color(255, 255, 255, isDark ? 20 : 100),
                width, glassHeight, new Color(255, 255, 255, 0)
            );
            cg.setPaint(sheen);
            cg.fillRoundRect(0, 0, width, glassHeight, radius, radius);
            
            cg.setColor(new Color(255, 255, 255, isDark ? 40 : 200));
            cg.drawRoundRect(0, 0, width - 1, glassHeight - 1, radius, radius);
            cg.dispose();
            glassCache.put(key, img);
        }
        g2.drawImage(img, 0, 0, null);
    }

    private BufferedImage bgCache = null;
    private int bgCacheWidth = -1;
    private int bgCacheHeight = -1;
    private Theme bgCacheTheme = null;

    private void paintVibrantBackground(Graphics2D g2, int width, int height) {
        if (width <= 0 || height <= 0) return;
        
        if (currentTheme != null && currentTheme.name.startsWith("Test")) {
            // Premium, subtle interactive background
            
            // 1. Base dark ambient background
            GradientPaint base = new GradientPaint(0, 0, currentTheme.backgroundTop, 0, height, currentTheme.backgroundBottom);
            g2.setPaint(base);
            g2.fillRect(0, 0, width, height);

            // 2. Subtle static ambient blobs for depth
            RadialGradientPaint ambient1 = new RadialGradientPaint(
                width * 0.8f, height * 0.2f, width * 0.7f,
                new float[]{0f, 1f},
                new Color[]{new Color(currentTheme.secondary.getRed(), currentTheme.secondary.getGreen(), currentTheme.secondary.getBlue(), 35), new Color(0,0,0,0)}
            );
            g2.setPaint(ambient1);
            g2.fillRect(0, 0, width, height);

            RadialGradientPaint ambient2 = new RadialGradientPaint(
                width * 0.2f, height * 0.85f, width * 0.7f,
                new float[]{0f, 1f},
                new Color[]{new Color(currentTheme.accent.getRed(), currentTheme.accent.getGreen(), currentTheme.accent.getBlue(), 25), new Color(0,0,0,0)}
            );
            g2.setPaint(ambient2);
            g2.fillRect(0, 0, width, height);
            
            // 2.5 Subtle flashing falling stars
            if (ambientStars == null || lastStarWidth != width || lastStarHeight != height) {
                ambientStars = new Star[150]; // 150 stars for a rich starry night
                for (int i = 0; i < ambientStars.length; i++) {
                    ambientStars[i] = new Star(width, height);
                }
                lastStarWidth = width;
                lastStarHeight = height;
            }
            
            long time = System.currentTimeMillis();
            for (Star star : ambientStars) {
                star.update(width, height);
                float sine = (float) ((Math.sin(time * star.speed + star.phase) + 1.0) / 2.0); // 0.0 to 1.0
                // Use a high power to create a sharp "flash" peak that is dark most of the time
                float spike = (float) Math.pow(sine, 20);
                float opacity = 0.08f + (0.8f * spike); // Faint 8% base opacity, flashes up to ~88% opacity
                g2.setColor(new Color(255, 255, 255, (int)(Math.min(1.0f, opacity) * 255)));
                g2.fillOval((int)star.x, (int)star.y, (int)star.size, (int)star.size);
            }
            
            // 3. Mouse following spotlight (brighter)
            if (globalMouseX >= 0 && globalMouseY >= 0) {
                int glowRadius = Math.max(width, height) / 2 + 100;
                RadialGradientPaint glow = new RadialGradientPaint(
                    globalMouseX, globalMouseY, glowRadius,
                    new float[]{0f, 0.3f, 1f},
                    new Color[]{
                        new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 110),
                        new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 35),
                        new Color(0, 0, 0, 0)
                    }
                );
                g2.setPaint(glow);
                g2.fillOval(globalMouseX - glowRadius, globalMouseY - glowRadius, glowRadius * 2, glowRadius * 2);
            }
            return;
        }

        if (bgCache == null || bgCacheWidth != width || bgCacheHeight != height || bgCacheTheme != currentTheme) {
            bgCache = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D cg = bgCache.createGraphics();
            cg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            GradientPaint gradient = new GradientPaint(0, 0, currentTheme.backgroundTop,
                0, height, currentTheme.backgroundBottom);
            cg.setPaint(gradient);
            cg.fillRect(0, 0, width, height);

            int blobSize = Math.max(width, height) / 2;
            if (blobSize > 0) {
                RadialGradientPaint blob1 = new RadialGradientPaint(
                    Math.max(1, blobSize / 2f), Math.max(1, blobSize / 2f), Math.max(1, blobSize),
                    new float[]{0f, 1f},
                    new Color[]{new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 120), new Color(0,0,0,0)}
                );
                cg.setPaint(blob1);
                cg.fillOval(-blobSize/4, -blobSize/4, blobSize*2, blobSize*2);
                
                RadialGradientPaint blob2 = new RadialGradientPaint(
                    Math.max(1, width - blobSize / 2f), Math.max(1, height - blobSize / 2f), Math.max(1, blobSize),
                    new float[]{0f, 1f},
                    new Color[]{new Color(currentTheme.secondary.getRed(), currentTheme.secondary.getGreen(), currentTheme.secondary.getBlue(), 120), new Color(0,0,0,0)}
                );
                cg.setPaint(blob2);
                cg.fillOval(width - blobSize, height - blobSize, blobSize*2, blobSize*2);
                
                RadialGradientPaint blob3 = new RadialGradientPaint(
                    Math.max(1, width / 2f), Math.max(1, height / 2f), Math.max(1, blobSize / 1.5f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(currentTheme.accent.getRed(), currentTheme.accent.getGreen(), currentTheme.accent.getBlue(), 80), new Color(0,0,0,0)}
                );
                cg.setPaint(blob3);
                cg.fillOval((int)(width / 2f - blobSize/2f), (int)(height / 2f - blobSize/2f), blobSize, blobSize);
            }
            cg.dispose();
            bgCacheWidth = width;
            bgCacheHeight = height;
            bgCacheTheme = currentTheme;
        }
        g2.drawImage(bgCache, 0, 0, null);
    }

    private static final Theme[] THEMES = new Theme[] {
        new Theme(
            "Test 1",
            new Color(140, 180, 255), // Primary: soft slate blue
            new Color(160, 130, 230), // Secondary: soft muted lavender
            new Color(100, 190, 170), // Accent: muted deep mint
            new Color(140, 180, 255, 60), 
            new Color(15, 17, 26),    // Top bg: deep twilight blue/grey
            new Color(10, 11, 16),    // Bottom bg: darker twilight
            new Color(22, 25, 38, 180), // Frosted cards
            new Color(230, 235, 245), // Soft white text
            new Color(140, 150, 170), // Muted text
            new Color(18, 20, 30)     // Inputs
        ),
        new Theme(
            "Test 2",
            new Color(120, 230, 160), // Primary: soft emerald glow
            new Color(80, 190, 210),  // Secondary: teal ambient
            new Color(200, 220, 120), // Accent: lime ambient
            new Color(120, 230, 160, 60), 
            new Color(15, 24, 18),    // Top bg: deep forest
            new Color(10, 16, 12),    // Bottom bg: dark forest
            new Color(22, 36, 26, 180), // Frosted cards
            new Color(235, 245, 235), // Text
            new Color(140, 170, 150), // Muted text
            new Color(18, 28, 20)     // Inputs
        ),
        new Theme(
            "Test 3",
            new Color(255, 120, 120), // Primary: soft crimson glow
            new Color(240, 160, 100), // Secondary: amber ambient
            new Color(200, 100, 150), // Accent: rose ambient
            new Color(255, 120, 120, 60), 
            new Color(26, 15, 15),    // Top bg: deep ember
            new Color(16, 10, 10),    // Bottom bg: dark ember
            new Color(38, 22, 22, 180), // Frosted cards
            new Color(245, 230, 230), // Text
            new Color(170, 140, 140), // Muted text
            new Color(30, 18, 18)     // Inputs
        ),
        new Theme(
            "Test 4",
            new Color(255, 210, 120), // Primary: soft gold glow
            new Color(240, 180, 100), // Secondary: warm ambient
            new Color(250, 160, 120), // Accent: peach ambient
            new Color(255, 210, 120, 60), 
            new Color(26, 22, 15),    // Top bg: deep gold
            new Color(16, 13, 10),    // Bottom bg: dark gold
            new Color(38, 30, 22, 180), // Frosted cards
            new Color(245, 240, 230), // Text
            new Color(170, 160, 140), // Muted text
            new Color(30, 24, 18)     // Inputs
        ),
        new Theme(
            "Test 5",
            new Color(210, 120, 255), // Primary: soft purple glow
            new Color(240, 140, 200), // Secondary: magenta ambient
            new Color(150, 120, 240), // Accent: violet ambient
            new Color(210, 120, 255, 60), 
            new Color(22, 15, 26),    // Top bg: deep amethyst
            new Color(13, 10, 16),    // Bottom bg: dark amethyst
            new Color(32, 22, 38, 180), // Frosted cards
            new Color(240, 230, 245), // Text
            new Color(160, 140, 170), // Muted text
            new Color(26, 18, 30)     // Inputs
        ),
        new Theme(
            "Noctalia",
            new Color(125, 169, 255),
            new Color(185, 129, 255),
            new Color(255, 105, 151),
            new Color(125, 169, 255, 58),
            new Color(12, 14, 24),
            new Color(22, 18, 36),
            new Color(28, 31, 48),
            new Color(232, 237, 255),
            new Color(170, 180, 210),
            new Color(18, 21, 34)
        ),
        new Theme(
            "Midnight",
            new Color(52, 211, 153),
            new Color(56, 189, 248),
            new Color(251, 113, 133),
            new Color(56, 189, 248, 50),
            new Color(6, 16, 27),
            new Color(9, 24, 42),
            new Color(15, 31, 49),
            new Color(229, 247, 255),
            new Color(145, 170, 190),
            new Color(9, 22, 36)
        ),
        new Theme(
            "Graphite",
            new Color(163, 230, 53),
            new Color(45, 212, 191),
            new Color(250, 204, 21),
            new Color(163, 230, 53, 42),
            new Color(14, 15, 18),
            new Color(30, 32, 36),
            new Color(39, 42, 48),
            new Color(244, 247, 242),
            new Color(183, 190, 178),
            new Color(24, 26, 31)
        ),
        new Theme(
            "Cyber Grape",
            new Color(168, 85, 247),
            new Color(34, 211, 238),
            new Color(251, 113, 133),
            new Color(168, 85, 247, 54),
            new Color(18, 11, 32),
            new Color(31, 18, 54),
            new Color(39, 29, 65),
            new Color(248, 244, 255),
            new Color(193, 178, 218),
            new Color(27, 20, 48)
        ),
        new Theme(
            "Ocean Depth",
            new Color(45, 212, 191),
            new Color(96, 165, 250),
            new Color(244, 114, 182),
            new Color(45, 212, 191, 48),
            new Color(4, 20, 28),
            new Color(9, 44, 57),
            new Color(14, 55, 70),
            new Color(225, 252, 255),
            new Color(146, 194, 204),
            new Color(8, 35, 46)
        ),
        new Theme(
            "Ember Night",
            new Color(251, 146, 60),
            new Color(239, 68, 68),
            new Color(250, 204, 21),
            new Color(251, 146, 60, 46),
            new Color(24, 14, 12),
            new Color(49, 22, 18),
            new Color(58, 33, 28),
            new Color(255, 241, 232),
            new Color(218, 176, 158),
            new Color(38, 22, 19)
        ),
        new Theme(
            "Aurora",
            new Color(129, 140, 248),
            new Color(52, 211, 153),
            new Color(244, 114, 182),
            new Color(52, 211, 153, 50),
            new Color(10, 19, 26),
            new Color(21, 38, 42),
            new Color(27, 45, 52),
            new Color(237, 255, 249),
            new Color(163, 194, 185),
            new Color(14, 29, 35)
        ),
        new Theme(
            "Royal Dark",
            new Color(96, 165, 250),
            new Color(236, 72, 153),
            new Color(250, 204, 21),
            new Color(236, 72, 153, 48),
            new Color(12, 15, 35),
            new Color(29, 21, 55),
            new Color(35, 32, 62),
            new Color(241, 245, 255),
            new Color(180, 188, 218),
            new Color(20, 22, 44)
        ),
        new Theme(
            "Porcelain",
            new Color(37, 99, 235),
            new Color(14, 165, 233),
            new Color(225, 29, 72),
            new Color(37, 99, 235, 36),
            new Color(248, 250, 252),
            new Color(226, 232, 240),
            new Color(255, 255, 255),
            new Color(15, 23, 42),
            new Color(71, 85, 105),
            new Color(248, 250, 252)
        ),
        new Theme(
            "Sakura",
            new Color(244, 114, 182),
            new Color(251, 146, 60),
            new Color(239, 68, 68),
            new Color(244, 114, 182, 42),
            new Color(32, 13, 28),
            new Color(56, 19, 43),
            new Color(64, 25, 50),
            new Color(255, 240, 248),
            new Color(221, 170, 194),
            new Color(44, 19, 38)
        ),
        new Theme(
            "Solar Flare",
            new Color(250, 204, 21),
            new Color(251, 146, 60),
            new Color(239, 68, 68),
            new Color(250, 204, 21, 44),
            new Color(31, 18, 8),
            new Color(66, 31, 9),
            new Color(78, 40, 14),
            new Color(255, 248, 226),
            new Color(231, 196, 139),
            new Color(47, 24, 9)
        ),
        new Theme(
            "Nordic",
            new Color(96, 165, 250),
            new Color(45, 212, 191),
            new Color(148, 163, 184),
            new Color(96, 165, 250, 40),
            new Color(8, 18, 28),
            new Color(15, 34, 45),
            new Color(18, 44, 58),
            new Color(234, 245, 255),
            new Color(150, 181, 199),
            new Color(12, 28, 39)
        )
    };

    private static String chooseFont(String... preferredNames) {
        String[] availableFonts = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .getAvailableFontFamilyNames();

        for (String preferredName : preferredNames) {
            for (String availableFont : availableFonts) {
                if (availableFont.equalsIgnoreCase(preferredName)) {
                    return availableFont;
                }
            }
        }

        return preferredNames[preferredNames.length - 1];
    }
    
    public BirthCalculator() {
        preferences = Preferences.userNodeForPackage(BirthCalculator.class);
        currentTheme = THEMES[0];

        setTitle("Kalkulátor Věku");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setType(Window.Type.UTILITY);
        setPreferredSize(new Dimension(1100, 850));
        setMinimumSize(new Dimension(1100, 850));
        setResizable(false);
        initializeUI();
        startRealtimeUpdates();

        int savedTheme = preferences.getInt(PREF_THEME_INDEX, 0);
        if (savedTheme < 0 || savedTheme >= THEMES.length) {
            savedTheme = 0;
        }
        applyTheme(savedTheme);
        loadHistory();
        loadBirthdayEntries();
        restoreMostRecentDate();

        setVisible(true);
    }
    
    private void initializeUI() {
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_MOVED || me.getID() == MouseEvent.MOUSE_DRAGGED) {
                    if (mainPanel != null && mainPanel.isShowing()) {
                        Point p = me.getLocationOnScreen();
                        SwingUtilities.convertPointFromScreen(p, mainPanel);
                        globalMouseX = p.x;
                        globalMouseY = p.y;
                        if (currentTheme != null && currentTheme.name.startsWith("Test")) {
                            mainPanel.repaint();
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_MOTION_EVENT_MASK);

        // Timer for ambient background animations (e.g. stars flashing)
        Timer backgroundAnimationTimer = new Timer(33, e -> {
            if (currentTheme != null && currentTheme.name.startsWith("Test") && mainPanel != null && mainPanel.isShowing()) {
                mainPanel.repaint();
            }
        });
        backgroundAnimationTimer.start();

        getContentPane().setBackground(currentTheme.backgroundTop);

        // Hlavní panel s BorderLayout
        mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintVibrantBackground((Graphics2D) g, getWidth(), getHeight());
            }
        };
        mainPanel.setOpaque(true);
        mainPanel.setBackground(currentTheme.backgroundTop);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JPanel topPanel = new JPanel(new BorderLayout(0, 6));
        topPanel.setOpaque(false);
        
        // ===== TOP THEME SELECTOR - ONE ICON WITH DROPDOWN =====
        JPanel themeSelectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        themeSelectorPanel.setOpaque(false);
        
        JButton themeDropdownBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getWidth(), getHeight());
                GradientPaint grad = new GradientPaint(0, 0, currentTheme.primary, getWidth(), getHeight(), currentTheme.secondary);
                g2.setPaint(grad);
                g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, getWidth() - 5, getHeight() - 5);
                
                int size = 18;
                int cx = (getWidth() - size) / 2;
                int cy = (getHeight() - 4 - size) / 2;
                
                g2.setStroke(new BasicStroke(2.0f));
                g2.setColor(Color.WHITE);
                g2.drawOval(cx, cy, size, size);
                g2.fillArc(cx, cy, size, size, -90, 180);
                
                g2.dispose();
            }
        };
        themeDropdownBtn.setPreferredSize(new Dimension(40, 40));
        themeDropdownBtn.setFocusPainted(false);
        themeDropdownBtn.setBorderPainted(false);
        themeDropdownBtn.setContentAreaFilled(false);
        themeDropdownBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        themeDropdownBtn.setToolTipText("Vybrat téma");
        themeDropdownBtn.addActionListener(e -> showThemeMenu(themeDropdownBtn));
        themeSelectorPanel.add(themeDropdownBtn);
        
        topPanel.add(themeSelectorPanel, BorderLayout.NORTH);
        
        // ===== HORNÍ PANEL (Nadpis) =====
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, currentTheme.primary,
                    getWidth(), getHeight(), currentTheme.secondary);
                g2.setPaint(gradient);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() - 3, 14, 14);
                
                // Shadow effect
                g2.setColor(new Color(255, 255, 255, 38));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 6, 14, 14);
                g2.setColor(new Color(0, 0, 0, 35));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 4, 14, 14);
            }
        };
        titlePanel.setLayout(new BorderLayout(0, 6));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        titleLabel = new JLabel("Kalkulátor věku");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        subtitleLabel = new JLabel("Rychlý přehled věku, kalendáře a narozenin přátel");
        subtitleLabel.setFont(FONT_BODY);
        subtitleLabel.setForeground(new Color(255, 255, 255, 220));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        topPanel.add(titlePanel, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // ===== STŘEDNÍ PANEL (Vstup + Tlačítka) =====
        inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setOpaque(false);
        
        // Instrukce
        instructionLabel = new JLabel("Zadejte datum narození ve formátu dd.mm.yyyy");
        instructionLabel.setFont(FONT_BODY_BOLD);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        inputPanel.add(instructionLabel, BorderLayout.NORTH);
        
        // Textové pole s lepším designem
        dateField = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        dateField.setOpaque(false);
        dateField.setFont(FONT_INPUT);
        dateField.setHorizontalAlignment(JTextField.CENTER);
        dateField.setToolTipText("Například 15.03.2000");
        dateField.setPreferredSize(new Dimension(260, 48));
        dateField.addActionListener(e -> calculateAge());
        dateField.setForeground(Color.WHITE);
        updateDateFieldBorder(false);
        dateField.setCaretColor(Color.WHITE);
        dateField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                updateDateFieldBorder(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                updateDateFieldBorder(false);
            }
        });
        
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setOpaque(false);
        GridBagConstraints dateConstraints = new GridBagConstraints();
        dateConstraints.insets = new Insets(4, 5, 4, 5);
        dateConstraints.gridy = 0;
        dateConstraints.gridx = 0;
        datePanel.add(dateField, dateConstraints);

        calendarButton = createStyledButton("Kalendář", currentTheme.primary, currentTheme.secondary);
        calendarButton.setIcon(new VectorIcon(VectorIcon.Type.CALENDAR, 16, 16, Color.WHITE));
        calendarButton.setPreferredSize(new Dimension(136, 54));
        calendarButton.addActionListener(e -> showCalendarDialog());
        dateConstraints.gridx = 1;
        datePanel.add(calendarButton, dateConstraints);

        historyButton = new JButton() {
            private float hoverProgress = 0f;
            private javax.swing.Timer hoverTimer;
            private boolean isHovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { isHovered = true; animateHover(); }
                    @Override
                    public void mouseExited(MouseEvent e) { isHovered = false; animateHover(); }
                });
            }

            private void animateHover() {
                if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
                hoverTimer = new javax.swing.Timer(16, e -> {
                    if (isHovered && hoverProgress < 1f) hoverProgress += 0.15f;
                    else if (!isHovered && hoverProgress > 0f) hoverProgress -= 0.15f;
                    else ((javax.swing.Timer)e.getSource()).stop();
                    hoverProgress = Math.max(0f, Math.min(1f, hoverProgress));
                    repaint();
                });
                hoverTimer.start();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int panelW = getWidth();
                int panelH = getHeight();
                int w = 260; // Base visual width
                int h = 48;  // Base visual height

                float scale = 1.0f + (hoverProgress * 0.04f);
                g2.translate(panelW/2.0, panelH/2.0);
                g2.scale(scale, scale);
                g2.translate(-w/2.0, -h/2.0);

                if (hoverProgress > 0) {
                    g2.setColor(new Color(0, 0, 0, (int)(hoverProgress * 30)));
                    g2.fillRoundRect(0, 6, w, h - 2, h, h);
                }

                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, w, h - 2, h, h);
                g2.setColor(new Color(0, 0, 0, 40 + (int)(hoverProgress * 20)));
                g2.fillRoundRect(0, 0, w, h, h, h);
                
                // Draw border manually so it scales
                g2.setColor(new Color(255, 255, 255, 100));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

                g2.setColor(Color.WHITE);
                g2.setFont(FONT_INPUT);
                String text = getText();
                if (text == null || text.isEmpty()) text = "Vyberte z historie";
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(text)) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(text, x, y);

                g2.setColor(new Color(255, 255, 255, 120));
                int arrowX = w - 24;
                int arrowY = h / 2 - 2;
                int[] xPoints = {arrowX, arrowX + 8, arrowX + 4};
                int[] yPoints = {arrowY, arrowY, arrowY + 5};
                g2.fillPolygon(xPoints, yPoints, 3);
                g2.dispose();
            }
        };
        historyButton.setOpaque(false);
        historyButton.setContentAreaFilled(false);
        historyButton.setFocusPainted(false);
        historyButton.setBorderPainted(false);
        historyButton.setBorder(BorderFactory.createEmptyBorder());
        historyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        historyButton.setPreferredSize(new Dimension(274, 62)); // Extra space for scaling effect
        historyButton.addActionListener(e -> showHistoryMenu());
        dateConstraints.gridy = 1;
        dateConstraints.gridx = 0;
        datePanel.add(historyButton, dateConstraints);

        removeHistoryButton = createStyledButton("Odebrat", currentTheme.accent, currentTheme.accent.brighter());
        removeHistoryButton.setPreferredSize(new Dimension(136, 46));
        removeHistoryButton.addActionListener(e -> removeSelectedHistoryDate());
        dateConstraints.gridx = 1;
        datePanel.add(removeHistoryButton, dateConstraints);
        inputPanel.add(datePanel, BorderLayout.CENTER);
        
        // Tlačítka jako ikony
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        buttonPanel.setOpaque(false);
        
        calculateButton = createStyledButton("Calculator", currentTheme.primary, currentTheme.secondary);
        calculateButton.setPreferredSize(new Dimension(182, 60));
        calculateButton.setToolTipText("Vypočítat věk");
        calculateButton.addActionListener(e -> calculateAge());
        buttonPanel.add(calculateButton);

        birthdaysButton = createStyledButton("Friends BD", currentTheme.secondary, currentTheme.primary);
        birthdaysButton.setPreferredSize(new Dimension(182, 60));
        birthdaysButton.setToolTipText("Narozeniny přátel");
        birthdaysButton.addActionListener(e -> showBirthdaysDialog());
        buttonPanel.add(birthdaysButton);
        
        clearButton = createStyledButton("Clear", currentTheme.accent, currentTheme.accent.brighter());
        clearButton.setPreferredSize(new Dimension(182, 60));
        clearButton.setToolTipText("Vymazat");
        clearButton.addActionListener(e -> clear());
        buttonPanel.add(clearButton);
        
        inputPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // ===== DOLNÍ PANEL (Výsledky) =====
        resultsPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Shadow effect
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(8, 8, getWidth() - 10, getHeight() - 10, 16, 16);
                
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 16, 16);

                GradientPaint lineGradient = new GradientPaint(
                    16, 0, currentTheme.primary,
                    Math.max(120, getWidth() / 2), 0, currentTheme.secondary);
                g2.setPaint(lineGradient);
                g2.fillRoundRect(16, 12, Math.max(90, getWidth() / 4), 4, 4, 4);
            }
        };
        resultsPanel.setOpaque(false);
        updateResultsPanelBorder();
        
        resultsLabel = new JLabel("Výsledky");
        resultsLabel.setFont(FONT_BODY_BOLD);
        resultsLabel.setForeground(currentTheme.primary);
        resultsPanel.add(resultsLabel, BorderLayout.NORTH);
        
        resultsLayout = new CardLayout();
        resultsContentPanel = new JPanel(resultsLayout);
        resultsContentPanel.setOpaque(false);

        JPanel emptyStatePanel = createEmptyStatePanel();
        JPanel dashboardPanel = createDashboardPanel();

        resultsContentPanel.add(emptyStatePanel, "EMPTY");
        resultsContentPanel.add(dashboardPanel, "DASHBOARD");

        resultsPanel.add(resultsContentPanel, BorderLayout.CENTER);
        setEmptyResults();
        
        // ===== KOMBINACE VŠECH PANELŮ =====
        centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(inputPanel, BorderLayout.NORTH);
        centerPanel.add(resultsPanel, BorderLayout.CENTER);

        birthdayCard = createBirthdayCard();

        cardsLayout = new CardLayout();
        cardsPanel = new JPanel(cardsLayout);
        cardsPanel.setOpaque(false);
        cardsPanel.add(centerPanel, "CALCULATOR");
        cardsPanel.add(birthdayCard, "BIRTHDAYS");

        mainPanel.add(cardsPanel, BorderLayout.CENTER);
        
        add(mainPanel);
        pack();
        setSize(new Dimension(1100, 850));
    }
    
    private class ThemeFadeGlassPane extends JComponent {
        private final BufferedImage image;
        private float alpha = 1.0f;

        public ThemeFadeGlassPane(BufferedImage image) {
            this.image = image;
            setOpaque(false);
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (alpha > 0 && image != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawImage(image, 0, 0, null);
                g2.dispose();
            }
        }
    }

    private class ModalOverlayPanel extends JPanel {
        private float alpha = 0f;
        private int yOffset = 20;

        public ModalOverlayPanel() {
            super(null);
            setOpaque(false);
            addMouseListener(new MouseAdapter() {});
            addMouseWheelListener(e -> {});
            addKeyListener(new java.awt.event.KeyAdapter() {});
            setFocusable(true);
            setRequestFocusEnabled(true);
        }

        public void setAlpha(float alpha) {
            this.alpha = alpha;
            repaint();
        }

        public void setYOffset(int yOffset) {
            this.yOffset = yOffset;
        }

        @Override
        public void paint(Graphics g) {
            if (alpha == 0f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paint(g2);
            g2.dispose();
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void crossfadeAnimation(Runnable uiUpdate) {
        if (getRootPane().getWidth() <= 0 || getRootPane().getHeight() <= 0) {
            uiUpdate.run();
            return;
        }
        final BufferedImage fadeImage = new BufferedImage(getRootPane().getWidth(), getRootPane().getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = fadeImage.createGraphics();
        getRootPane().paint(g2d);
        g2d.dispose();

        final ThemeFadeGlassPane glassPane = new ThemeFadeGlassPane(fadeImage);
        final Component oldGlassPane = getGlassPane();
        setGlassPane(glassPane);
        glassPane.setVisible(true);

        uiUpdate.run();

        SwingUtilities.updateComponentTreeUI(this);
        mainPanel.repaint();

        javax.swing.Timer fadeTimer = new javax.swing.Timer(16, null);
        fadeTimer.addActionListener(new ActionListener() {
            long startTime = System.currentTimeMillis();
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, elapsed / 400f);
                float ease = 1.0f - (float)Math.pow(1.0f - progress, 3);
                glassPane.setAlpha(1.0f - ease);
                if (progress >= 1.0f) {
                    ((javax.swing.Timer)e.getSource()).stop();
                    glassPane.setVisible(false);
                    setGlassPane(oldGlassPane);
                }
            }
        });
        fadeTimer.start();
    }

    private void applyTheme(int themeIndex) {
        if (themeIndex < 0 || themeIndex >= THEMES.length) {
            return;
        }

        crossfadeAnimation(() -> {
            currentTheme = THEMES[themeIndex];
            preferences.putInt(PREF_THEME_INDEX, themeIndex);
        getContentPane().setBackground(currentTheme.backgroundTop);
        mainPanel.setBackground(currentTheme.backgroundTop);
        
        if (cardBirthDate != null) cardBirthDate.updateColors();
        if (cardZodiac != null) cardZodiac.updateColors();
        if (cardTodayDate != null) cardTodayDate.updateColors();
        if (cardMonthsTotal != null) cardMonthsTotal.updateColors();
        if (cardNextBirthday != null) cardNextBirthday.updateColors();
        if (cardCountdown != null) cardCountdown.updateColors();
        if (cardDaysTotal != null) cardDaysTotal.updateColors();
        if (cardHoursTotal != null) cardHoursTotal.updateColors();
        if (cardMinutesTotal != null) cardMinutesTotal.updateColors();
        if (cardSecondsTotal != null) cardSecondsTotal.updateColors();
        if (liveTimeLabel != null) liveTimeLabel.setForeground(currentTheme.textMuted);

        if (lastBirthDate == null) {
            setEmptyResults();
        } else {
            renderResults(lastBirthDate, true, false);
        }

        instructionLabel.setForeground(currentTheme.textMuted);
        resultsLabel.setForeground(currentTheme.primary);
        titleLabel.setForeground(Color.WHITE);
        subtitleLabel.setForeground(new Color(255, 255, 255, 220));

        historyButton.setBackground(currentTheme.cardBackground);
        historyButton.setForeground(currentTheme.textMain);

        calculateButton.setColors(currentTheme.primary, currentTheme.secondary);
        clearButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
        calendarButton.setColors(currentTheme.primary, currentTheme.secondary);
        if (birthdaysButton != null) {
            birthdaysButton.setColors(currentTheme.secondary, currentTheme.primary);
        }
        if (birthdaysBackButton != null) {
            birthdaysBackButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
        }
        removeHistoryButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
        if (birthdayAddButton != null) {
            birthdayAddButton.setColors(currentTheme.primary, currentTheme.secondary);
        }
        if (birthdayRemoveButton != null) {
            birthdayRemoveButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
        }
        if (birthdayPickDateButton != null) {
            birthdayPickDateButton.setColors(currentTheme.primary, currentTheme.secondary);
        }
        if (birthdayNameField != null) {
            birthdayNameField.setBackground(currentTheme.inputBackground);
            birthdayNameField.setForeground(currentTheme.textMain);
            birthdayNameField.setCaretColor(currentTheme.primary);
        }
        if (birthdayDateField != null) {
            birthdayDateField.setBackground(currentTheme.inputBackground);
            birthdayDateField.setForeground(currentTheme.textMain);
            birthdayDateField.setCaretColor(currentTheme.primary);
        }
        if (birthdayList != null) {
            birthdayList.setBackground(currentTheme.inputBackground);
            birthdayList.setForeground(currentTheme.textMain);
            birthdayList.repaint();
        }
        if (birthdaysCountLabel != null) {
            birthdaysCountLabel.setForeground(currentTheme.primary);
        }
        if (detailPanel != null) {
            detailPanel.repaint();
            if (selectedBirthdayForDetail != null) {
                updateBirthdayDetail();
            }
        }
        
        // Update Global UI Defaults for Popups/Menus to match the theme
        UIManager.put("PopupMenu.background", currentTheme.cardBackground);
        UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.primary, 1),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        UIManager.put("Menu.background", currentTheme.cardBackground);
        UIManager.put("Menu.foreground", currentTheme.textMain);
        UIManager.put("MenuItem.background", currentTheme.cardBackground);
        UIManager.put("MenuItem.foreground", currentTheme.textMain);
        UIManager.put("MenuItem.selectionBackground", new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 180));
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.font", FONT_BODY);
        UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(10, 15, 10, 15));
        
            getContentPane().setBackground(currentTheme.backgroundTop);
            mainPanel.setBackground(currentTheme.backgroundTop);
            
            if (cardBirthDate != null) cardBirthDate.updateColors();
            if (cardZodiac != null) cardZodiac.updateColors();
            if (cardTodayDate != null) cardTodayDate.updateColors();
            if (cardMonthsTotal != null) cardMonthsTotal.updateColors();
            if (cardNextBirthday != null) cardNextBirthday.updateColors();
            if (cardCountdown != null) cardCountdown.updateColors();
            if (cardDaysTotal != null) cardDaysTotal.updateColors();
            if (cardHoursTotal != null) cardHoursTotal.updateColors();
            if (cardMinutesTotal != null) cardMinutesTotal.updateColors();
            if (cardSecondsTotal != null) cardSecondsTotal.updateColors();
            if (liveTimeLabel != null) liveTimeLabel.setForeground(currentTheme.textMuted);

            if (lastBirthDate == null) {
                setEmptyResults();
            } else {
                renderResults(lastBirthDate, true, false);
            }

            instructionLabel.setForeground(currentTheme.textMuted);
            resultsLabel.setForeground(currentTheme.primary);
            titleLabel.setForeground(Color.WHITE);
            subtitleLabel.setForeground(new Color(255, 255, 255, 220));

            historyButton.setBackground(currentTheme.cardBackground);
            historyButton.setForeground(currentTheme.textMain);

            calculateButton.setColors(currentTheme.primary, currentTheme.secondary);
            clearButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
            calendarButton.setColors(currentTheme.primary, currentTheme.secondary);
            if (birthdaysButton != null) {
                birthdaysButton.setColors(currentTheme.secondary, currentTheme.primary);
            }
            if (birthdaysBackButton != null) {
                birthdaysBackButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
            }
            removeHistoryButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
            if (birthdayAddButton != null) {
                birthdayAddButton.setColors(currentTheme.primary, currentTheme.secondary);
            }
            if (birthdayRemoveButton != null) {
                birthdayRemoveButton.setColors(currentTheme.accent, currentTheme.accent.brighter());
            }
            if (birthdayPickDateButton != null) {
                birthdayPickDateButton.setColors(currentTheme.primary, currentTheme.secondary);
            }
            if (birthdayNameField != null) {
                birthdayNameField.setBackground(currentTheme.inputBackground);
                birthdayNameField.setForeground(currentTheme.textMain);
                birthdayNameField.setCaretColor(currentTheme.primary);
            }
            if (birthdayDateField != null) {
                birthdayDateField.setBackground(currentTheme.inputBackground);
                birthdayDateField.setForeground(currentTheme.textMain);
                birthdayDateField.setCaretColor(currentTheme.primary);
            }
            if (birthdayList != null) {
                birthdayList.setBackground(currentTheme.inputBackground);
                birthdayList.setForeground(currentTheme.textMain);
                birthdayList.repaint();
            }
            if (birthdaysCountLabel != null) {
                birthdaysCountLabel.setForeground(currentTheme.primary);
            }
            if (detailPanel != null) {
                detailPanel.repaint();
                if (selectedBirthdayForDetail != null) {
                    updateBirthdayDetail();
                }
            }
            
            // Update Global UI Defaults for Popups/Menus to match the theme
            UIManager.put("PopupMenu.background", currentTheme.cardBackground);
            UIManager.put("PopupMenu.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(currentTheme.primary, 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            UIManager.put("Menu.background", currentTheme.cardBackground);
            UIManager.put("Menu.foreground", currentTheme.textMain);
            UIManager.put("MenuItem.background", currentTheme.cardBackground);
            UIManager.put("MenuItem.foreground", currentTheme.textMain);
            UIManager.put("MenuItem.selectionBackground", new Color(currentTheme.primary.getRed(), currentTheme.primary.getGreen(), currentTheme.primary.getBlue(), 180));
            UIManager.put("MenuItem.selectionForeground", Color.WHITE);
            UIManager.put("MenuItem.font", FONT_BODY);
            UIManager.put("MenuItem.border", BorderFactory.createEmptyBorder(10, 15, 10, 15));
            
            UIManager.put("ToolTip.background", currentTheme.cardBackground);
            UIManager.put("ToolTip.foreground", currentTheme.textMain);
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(currentTheme.primary, 1));
            
            UIManager.put("OptionPane.background", currentTheme.cardBackground);
            UIManager.put("OptionPane.messageForeground", currentTheme.textMain);
            
            // Update existing components if necessary
            SwingUtilities.updateComponentTreeUI(this);
            
            mainPanel.repaint();
            centerPanel.repaint();
            if (birthdayCard != null) {
                birthdayCard.repaint();
            }
            resultsPanel.repaint();
        });
    }

    private JPanel createEmptyStatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(10, 20, 10, 20);

        JLabel title = new JLabel("Čekám na datum narození");
        title.setFont(new Font(UI_FONT, Font.BOLD, 22));
        title.setForeground(currentTheme.textMain);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, gbc);

        gbc.gridy++;
        JLabel desc = new JLabel("<html><center>Zadejte datum ve formátu <b>dd.mm.yyyy</b> a výsledek se zobrazí jako přehledný moderní panel.</center></html>");
        desc.setFont(FONT_BODY);
        desc.setForeground(currentTheme.textMuted);
        desc.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(desc, gbc);

        gbc.gridy++;
        JPanel tipBox = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.inputBackground);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(currentTheme.primary.darker());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        tipBox.setOpaque(false);
        tipBox.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel tipLabel = new JLabel("Tip: můžete použít například 15.03.2000.");
        tipLabel.setFont(FONT_BODY_BOLD);
        tipLabel.setForeground(currentTheme.textMuted);
        tipLabel.setHorizontalAlignment(SwingConstants.CENTER);
        tipBox.add(tipLabel, BorderLayout.CENTER);
        
        JPanel tipWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        tipWrapper.setOpaque(false);
        tipWrapper.add(tipBox);
        
        panel.add(tipWrapper, gbc);

        return panel;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        headerCard = new HeaderCard();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(headerCard, gbc);

        // Row 1
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.BOTH;
        
        cardBirthDate = new MetricCard("Datum narození", "");
        gbc.gridx = 0;
        panel.add(cardBirthDate, gbc);

        cardZodiac = new MetricCard("Znamení zvěrokruhu", "");
        gbc.gridx = 1;
        panel.add(cardZodiac, gbc);

        cardTodayDate = new MetricCard("Dnešní datum", "");
        gbc.gridx = 2;
        panel.add(cardTodayDate, gbc);

        cardMonthsTotal = new MetricCard("Měsíců celkem", "");
        gbc.gridx = 3;
        panel.add(cardMonthsTotal, gbc);

        // Row 2
        gbc.gridy = 2;
        
        cardNextBirthday = new MetricCard("Příští narozeniny", "");
        gbc.gridx = 0;
        panel.add(cardNextBirthday, gbc);

        cardCountdown = new MetricCard("Odpočet", "");
        gbc.gridx = 1;
        panel.add(cardCountdown, gbc);

        cardDaysTotal = new MetricCard("Dní celkem", "");
        gbc.gridx = 2;
        panel.add(cardDaysTotal, gbc);

        cardHoursTotal = new MetricCard("Hodin celkem", "");
        gbc.gridx = 3;
        panel.add(cardHoursTotal, gbc);

        // Row 3
        gbc.gridy = 3;
        
        cardMinutesTotal = new MetricCard("Minut celkem", "");
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        panel.add(cardMinutesTotal, gbc);

        cardSecondsTotal = new MetricCard("Sekund celkem", "");
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        panel.add(cardSecondsTotal, gbc);

        // System time label
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 4, 4, 4); // Add spacing above the label
        gbc.fill = GridBagConstraints.HORIZONTAL;
        liveTimeLabel = new JLabel("Živě podle systémového času: --:--:--");
        liveTimeLabel.setFont(new Font(UI_FONT, Font.BOLD, 13));
        liveTimeLabel.setForeground(new Color(255, 255, 255, 200));
        liveTimeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(liveTimeLabel, gbc);

        return panel;
    }

    private void startRealtimeUpdates() {
        realtimeTimer = new Timer(1000, e -> {
            if (lastBirthDate != null) {
                renderResults(lastBirthDate, false, true);
            }
        });
        realtimeTimer.setInitialDelay(1000);
        realtimeTimer.start();
    }

    private void loadHistory() {
        updatingHistory = true;
        java.util.List<String> dates = getHistoryDates();
        if (dates.isEmpty()) {
            historyButton.setText("Vyberte z historie");
        } else {
            historyButton.setText(dates.get(0));
        }
        updatingHistory = false;
        removeHistoryButton.setEnabled(!dates.isEmpty());
    }

    private java.util.List<String> getHistoryDates() {
        String savedHistory = preferences.get(PREF_HISTORY, "");
        java.util.List<String> dates = new ArrayList<>();

        if (savedHistory.isBlank()) {
            return dates;
        }

        for (String date : savedHistory.split("\\|")) {
            if (!date.isBlank()) {
                dates.add(date);
            }
        }

        return dates;
    }

    private void saveHistoryDates(java.util.List<String> dates) {
        preferences.put(PREF_HISTORY, String.join("|", dates));
    }

    private void addDateToHistory(LocalDate birthDate) {
        String formattedDate = birthDate.format(DATE_FORMATTER);
        LinkedHashSet<String> uniqueDates = new LinkedHashSet<>();
        uniqueDates.add(formattedDate);
        uniqueDates.addAll(getHistoryDates());

        java.util.List<String> dates = new ArrayList<>(uniqueDates);
        if (dates.size() > MAX_HISTORY_ITEMS) {
            dates = new ArrayList<>(dates.subList(0, MAX_HISTORY_ITEMS));
        }

        saveHistoryDates(dates);
        loadHistory();
        updatingHistory = true;
        historyButton.setText(formattedDate);
        updatingHistory = false;
    }

    private void restoreMostRecentDate() {
        java.util.List<String> dates = getHistoryDates();
        if (dates.isEmpty()) {
            return;
        }

        dateField.setText(dates.get(0));
        LocalDate birthDate = validateDate(dates.get(0));
        if (birthDate != null) {
            lastBirthDate = birthDate;
            renderResults(birthDate, true, false);
        }
    }

    private void selectHistoryDate() {
        if (updatingHistory) return;
        String selected = historyButton.getText();
        if (selected == null || selected.isEmpty() || selected.equals("Vyberte z historie")) {
            return;
        }

        dateField.setText(selected);
        calculateAge();
    }

    private void removeSelectedHistoryDate() {
        String selectedDate = historyButton.getText();
        if (selectedDate == null || selectedDate.isEmpty() || selectedDate.equals("Vyberte z historie")) {
            return;
        }

        java.util.List<String> dates = new ArrayList<>();
        for (String date : getHistoryDates()) {
            if (!date.equals(selectedDate)) {
                dates.add(date);
            }
        }

        saveHistoryDates(dates);
        loadHistory();

        if (selectedDate.equals(dateField.getText().trim())) {
            clear();
        }
    }

    private void showHistoryMenu() {
        JPopupMenu menu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        menu.setOpaque(false);
        menu.setBackground(new Color(0, 0, 0, 0));
        menu.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        
        java.util.List<String> history = getHistoryDates();
        if (history.isEmpty()) {
            JMenuItem empty = new JMenuItem("Žádná historie");
            empty.setEnabled(false);
            empty.setFont(FONT_BODY);
            empty.setForeground(currentTheme.textMuted);
            empty.setBackground(currentTheme.cardBackground);
            menu.add(empty);
        } else {
            for (String date : history) {
                JMenuItem item = new JMenuItem(date);
                item.setOpaque(true);
                item.setFont(FONT_BODY_BOLD);
                item.setBackground(currentTheme.cardBackground);
                item.setForeground(currentTheme.textMain);
                item.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
                
                item.addChangeListener(e -> {
                    if (item.isArmed()) {
                        item.setBackground(currentTheme.primary);
                        item.setForeground(Color.WHITE);
                    } else {
                        item.setBackground(currentTheme.cardBackground);
                        item.setForeground(currentTheme.textMain);
                    }
                });
                
                item.addActionListener(e -> {
                    historyButton.setText(date);
                    selectHistoryDate();
                });
                menu.add(item);
            }
        }
        menu.show(historyButton, 0, historyButton.getHeight() + 4);
    }

    private void loadBirthdayEntries() {
        birthdayEntries.clear();
        String savedEntries = preferences.get(PREF_BIRTHDAYS, "");

        if (!savedEntries.isBlank()) {
            for (String rawEntry : savedEntries.split("\n")) {
                BirthdayEntry entry = decodeBirthdayEntry(rawEntry);
                if (entry != null) {
                    birthdayEntries.add(entry);
                }
            }
        }

        if (birthdayListModel != null) {
            refreshBirthdayListModel();
        }
        updateBirthdaySummary();
    }

    private void saveBirthdayEntries() {
        java.util.List<String> encodedEntries = new ArrayList<>();
        for (BirthdayEntry entry : birthdayEntries) {
            encodedEntries.add(encodeBirthdayEntry(entry));
        }

        preferences.put(PREF_BIRTHDAYS, String.join("\n", encodedEntries));
    }

    private String encodeBirthdayEntry(BirthdayEntry entry) {
        String encodedName = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(entry.name.getBytes(StandardCharsets.UTF_8));
        return encodedName + "|" + entry.birthDate.format(STORAGE_DATE_FORMATTER);
    }

    private BirthdayEntry decodeBirthdayEntry(String rawEntry) {
        String[] parts = rawEntry.split("\\|", 2);
        if (parts.length != 2) {
            return null;
        }

        try {
            String name = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            LocalDate birthDate = LocalDate.parse(parts[1], STORAGE_DATE_FORMATTER);
            if (name.isBlank()) {
                return null;
            }
            return new BirthdayEntry(name, birthDate);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return null;
        }
    }

    private void showBirthdaysDialog() {
        if (cardsLayout == null || cardsPanel == null) {
            return;
        }

        crossfadeAnimation(() -> {
            cardsLayout.show(cardsPanel, "BIRTHDAYS");
            if (birthdayNameField != null) {
                birthdayNameField.requestFocus();
            }
            refreshBirthdayListModel();
            updateBirthdaySummary();
        });
    }

    private void showCalculatorView() {
        if (cardsLayout == null || cardsPanel == null) {
            return;
        }

        crossfadeAnimation(() -> {
            cardsLayout.show(cardsPanel, "CALCULATOR");
            if (dateField != null) {
                dateField.requestFocus();
            }
        });
    }

    private JPanel createBirthdayCard() {
        JPanel birthdayRoot = new JPanel(new BorderLayout(15, 15));
        birthdayRoot.setOpaque(false);
        birthdayRoot.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel outer = new JPanel(new BorderLayout(15, 15));
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);

        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 8, 22));

        JLabel headerLabel = new JLabel("Narozeniny přátel");
        headerLabel.setFont(FONT_TITLE);
        headerLabel.setForeground(currentTheme.textMain);

        JLabel headerHint = new JLabel("Klikni na jméno pro detaily.");
        headerHint.setFont(FONT_BODY);
        headerHint.setForeground(currentTheme.textMuted);

        JPanel headerText = new JPanel(new BorderLayout(0, 4));
        headerText.setOpaque(false);
        headerText.add(headerLabel, BorderLayout.NORTH);
        headerText.add(headerHint, BorderLayout.SOUTH);

        birthdaysCountLabel = new JLabel();
        birthdaysCountLabel.setFont(FONT_BODY_BOLD);
        birthdaysCountLabel.setForeground(currentTheme.primary);

        birthdaysBackButton = createIconButton(VectorIcon.Type.ARROW_LEFT, currentTheme.accent, currentTheme.accent.brighter());
        birthdaysBackButton.addActionListener(e -> showCalculatorView());

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        headerActions.setOpaque(false);
        headerActions.add(birthdaysCountLabel);
        headerActions.add(birthdaysBackButton);

        headerPanel.add(headerText, BorderLayout.CENTER);
        headerPanel.add(headerActions, BorderLayout.EAST);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 18, 18, 18));

        JPanel formCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                paintLiquidGlass(g2, getWidth() - 8, getHeight() - 8, 28, false);
                g2.dispose();
            }
        };
        formCard.setOpaque(false);
        formCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        GridBagConstraints form = new GridBagConstraints();
        form.insets = new Insets(6, 6, 6, 6);
        form.fill = GridBagConstraints.HORIZONTAL;
        form.weightx = 1.0;

        JLabel nameLabel = new JLabel("Jméno");
        nameLabel.setFont(FONT_BODY_BOLD);
        nameLabel.setForeground(currentTheme.textMuted);
        form.gridx = 0;
        form.gridy = 0;
        form.gridwidth = 2;
        formCard.add(nameLabel, form);

        birthdayNameField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        birthdayNameField.setOpaque(false);
        birthdayNameField.setFont(FONT_BODY_BOLD);
        birthdayNameField.setPreferredSize(new Dimension(260, 48));
        birthdayNameField.setForeground(Color.WHITE);
        birthdayNameField.setCaretColor(Color.WHITE);
        birthdayNameField.setHorizontalAlignment(JTextField.CENTER);
        birthdayNameField.setBorder(new PillBorder(new Color(255, 255, 255, 120), 1));
        form.gridy = 1;
        formCard.add(birthdayNameField, form);

        JLabel dateLabel = new JLabel("Datum narození");
        dateLabel.setFont(FONT_BODY_BOLD);
        dateLabel.setForeground(currentTheme.textMuted);
        form.gridy = 2;
        formCard.add(dateLabel, form);

        JPanel birthdayDateRow = new JPanel(new GridBagLayout());
        birthdayDateRow.setOpaque(false);
        GridBagConstraints birthdayDateConstraints = new GridBagConstraints();
        birthdayDateConstraints.insets = new Insets(0, 0, 0, 8);
        birthdayDateConstraints.gridy = 0;
        birthdayDateConstraints.gridx = 0;
        birthdayDateConstraints.weightx = 1.0;
        birthdayDateConstraints.fill = GridBagConstraints.HORIZONTAL;

        birthdayDateField = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        birthdayDateField.setOpaque(false);
        birthdayDateField.setFont(FONT_BODY_BOLD);
        birthdayDateField.setPreferredSize(new Dimension(260, 48));
        birthdayDateField.setToolTipText("Například 15.03.2000");
        birthdayDateField.setForeground(Color.WHITE);
        birthdayDateField.setCaretColor(Color.WHITE);
        birthdayDateField.setHorizontalAlignment(JTextField.CENTER);
        birthdayDateField.setBorder(new PillBorder(new Color(255, 255, 255, 120), 1));
        birthdayDateRow.add(birthdayDateField, birthdayDateConstraints);

        birthdayPickDateButton = createIconButton(VectorIcon.Type.CALENDAR, currentTheme.primary, currentTheme.secondary);
        birthdayPickDateButton.setToolTipText("Vyber z kalendáře");
        birthdayPickDateButton.addActionListener(e -> showCalendarDialog(
            birthdayDateField,
            "Vyber narozeniny",
            this::updateBirthdaySummary));
        birthdayDateConstraints.gridx = 1;
        birthdayDateConstraints.weightx = 0.0;
        birthdayDateRow.add(birthdayPickDateButton, birthdayDateConstraints);

        form.gridy = 3;
        formCard.add(birthdayDateRow, form);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionRow.setOpaque(false);
        birthdayAddButton = createIconButton(VectorIcon.Type.PLUS, currentTheme.primary, currentTheme.secondary);
        birthdayAddButton.setToolTipText("Přidat narozeniny");
        birthdayAddButton.addActionListener(e -> addBirthdayEntry());
        actionRow.add(birthdayAddButton);

        birthdayRemoveButton = createIconButton(VectorIcon.Type.CLOSE, currentTheme.accent, currentTheme.accent.brighter());
        birthdayRemoveButton.setToolTipText("Odstranit");
        birthdayRemoveButton.addActionListener(e -> removeSelectedBirthdayEntry());
        actionRow.add(birthdayRemoveButton);

        form.gridy = 4;
        formCard.add(actionRow, form);

        birthdaySummaryLabel = new JLabel("Zatím nemáš uložené žádné narozeniny.");
        birthdaySummaryLabel.setFont(FONT_BODY);
        birthdaySummaryLabel.setForeground(currentTheme.textMuted);
        form.gridy = 5;
        formCard.add(birthdaySummaryLabel, form);

        JPanel listCard = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                paintLiquidGlass(g2, getWidth() - 8, getHeight() - 8, 28, false);
                g2.dispose();
            }
        };
        listCard.setOpaque(false);
        listCard.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel listTitle = new JLabel("Uložené záznamy");
        listTitle.setFont(FONT_BODY_BOLD);
        listTitle.setForeground(currentTheme.textMuted);
        listCard.add(listTitle, BorderLayout.NORTH);

        birthdayListModel = new DefaultListModel<>();
        birthdayList = new JList<>(birthdayListModel);
        birthdayList.setFont(FONT_BODY);
        birthdayList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        birthdayList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JPanel panel = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    int w = getWidth() - 8;
                    int h = getHeight() - 8;
                    
                    if (isSelected) {
                        g2.setColor(currentTheme.primary);
                        g2.fillRoundRect(4, 4, w, h, h, h);
                        
                        if (currentTheme.name.startsWith("Test")) {
                            GradientPaint sheen = new GradientPaint(
                                0, 4, new Color(255, 255, 255, 60),
                                0, 4 + h / 2, new Color(255, 255, 255, 0)
                            );
                            g2.setPaint(sheen);
                            g2.fillRoundRect(4, 4, w, h, h, h);
                        }
                    } else {
                        g2.setColor(new Color(128, 128, 128, 20));
                        g2.fillRoundRect(4, 4, w, h, h, h);
                        g2.setColor(new Color(255, 255, 255, 20));
                        g2.drawRoundRect(4, 4, w, h, h, h);
                    }
                    g2.dispose();
                }
            };
            panel.setOpaque(false);
            
            JLabel label = new JLabel(value.toString());
            label.setOpaque(false);
            label.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
            label.setFont(FONT_BODY_BOLD);
            
            if (isSelected) {
                label.setForeground(Color.WHITE);
            } else {
                label.setForeground(currentTheme.textMain);
            }
            
            panel.add(label, BorderLayout.CENTER);
            return panel;
        });
        
        birthdayList.setOpaque(false);
        birthdayList.setBackground(new Color(0, 0, 0, 0));
        birthdayList.setSelectionBackground(new Color(0, 0, 0, 0));
        birthdayList.addListSelectionListener(e -> {
            if (birthdayList.getSelectedValue() != null) {
                selectedBirthdayForDetail = birthdayList.getSelectedValue();
                updateBirthdayDetail();
            }
        });

        JScrollPane birthdayScrollPane = new JScrollPane(birthdayList);
        birthdayScrollPane.setBorder(BorderFactory.createEmptyBorder());
        birthdayScrollPane.setOpaque(false);
        birthdayScrollPane.getViewport().setOpaque(false);
        birthdayScrollPane.setBackground(new Color(0, 0, 0, 0));
        birthdayScrollPane.getViewport().setBackground(new Color(0, 0, 0, 0));
        birthdayScrollPane.setPreferredSize(new Dimension(300, 200));
        birthdayScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        birthdayScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        birthdayScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        birthdayScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(currentTheme));
        enableSmoothScrolling(birthdayScrollPane);
        listCard.add(birthdayScrollPane, BorderLayout.CENTER);

        JPanel leftColumn = new JPanel(new BorderLayout(0, 15));
        leftColumn.setOpaque(false);
        leftColumn.add(formCard, BorderLayout.NORTH);
        leftColumn.add(listCard, BorderLayout.CENTER);

        detailPanel = createBirthdayDetailPanel();

        contentPanel.add(leftColumn, BorderLayout.WEST);
        contentPanel.add(detailPanel, BorderLayout.CENTER);

        outer.add(headerPanel, BorderLayout.NORTH);
        outer.add(contentPanel, BorderLayout.CENTER);
        birthdayRoot.add(outer, BorderLayout.CENTER);
        return birthdayRoot;
    }

    private void refreshBirthdayListModel() {
        if (birthdayListModel == null) {
            return;
        }

        birthdayListModel.clear();
        for (BirthdayEntry entry : birthdayEntries) {
            birthdayListModel.addElement(entry);
        }

        if (!birthdayEntries.isEmpty() && birthdayList != null && birthdayList.getSelectedIndex() < 0) {
            birthdayList.setSelectedIndex(0);
        }
    }

    private void updateBirthdaySummary() {
        if (birthdaysCountLabel != null) {
            birthdaysCountLabel.setText(String.format("%d uložených", birthdayEntries.size()));
        }

        if (birthdayEntries.isEmpty()) {
            if (birthdayRemoveButton != null) {
                birthdayRemoveButton.setEnabled(false);
            }
            selectedBirthdayForDetail = null;
            return;
        }

        if (selectedBirthdayForDetail == null && !birthdayEntries.isEmpty()) {
            selectedBirthdayForDetail = birthdayEntries.get(0);
        }

        if (selectedBirthdayForDetail != null) {
            updateBirthdayDetail();
        }
        if (birthdayRemoveButton != null) {
            birthdayRemoveButton.setEnabled(true);
        }
    }

    private void addBirthdayEntry() {
        if (birthdayNameField == null || birthdayDateField == null) {
            return;
        }

        String name = birthdayNameField.getText().trim();
        String dateText = birthdayDateField.getText().trim();

        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this,
                "Zadej jméno kamaráda.",
                "Chybí jméno",
                JOptionPane.WARNING_MESSAGE);
            birthdayNameField.requestFocus();
            return;
        }

        LocalDate birthDate = validateDate(dateText);
        if (birthDate == null) {
            return;
        }

        BirthdayEntry newEntry = new BirthdayEntry(name, birthDate);
        birthdayEntries.removeIf(entry -> entry.name.equalsIgnoreCase(name));
        birthdayEntries.add(0, newEntry);
        saveBirthdayEntries();
        refreshBirthdayListModel();
        updateBirthdaySummary();
        birthdayNameField.setText("");
        birthdayDateField.setText("");
        birthdayNameField.requestFocus();
    }

    private void removeSelectedBirthdayEntry() {
        if (birthdayList == null) {
            return;
        }

        BirthdayEntry selectedEntry = birthdayList.getSelectedValue();
        if (selectedEntry == null) {
            return;
        }

        birthdayEntries.remove(selectedEntry);
        saveBirthdayEntries();
        refreshBirthdayListModel();
        updateBirthdaySummary();
    }

    private static String[] themeNames() {
        String[] names = new String[THEMES.length];
        for (int i = 0; i < THEMES.length; i++) {
            names[i] = THEMES[i].name;
        }
        return names;
    }

    private static class BirthdayEntry {
        final String name;
        final LocalDate birthDate;

        BirthdayEntry(String name, LocalDate birthDate) {
            this.name = name;
            this.birthDate = birthDate;
        }

        @Override
        public String toString() {
            return name + " • " + birthDate.format(DATE_FORMATTER);
        }
    }
    
    private GradientButton createIconButton(VectorIcon.Type iconType, Color color1, Color color2) {
        GradientButton btn = new GradientButton("", color1, color2);
        btn.setPreferredSize(new Dimension(52, 52));
        btn.setIcon(new VectorIcon(iconType, 20, 20, Color.WHITE));
        return btn;
    }

    private GradientButton createStyledButton(String text, Color color1, Color color2) {
        return new GradientButton(text, color1, color2);
    }

    private JPanel createBirthdayDetailPanel() {
        JPanel detailRoot = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                paintLiquidGlass(g2, getWidth() - 8, getHeight() - 8, 28, false);
                g2.dispose();
            }
        };
        detailRoot.setOpaque(false);
        detailRoot.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JPanel emptyState = new JPanel(new GridBagLayout());
        emptyState.setOpaque(false);
        JLabel emptyLabel = new JLabel("Vyber jméno ze seznamu");
        emptyLabel.setFont(FONT_BODY_BOLD);
        emptyLabel.setForeground(currentTheme.textMuted);
        emptyState.add(emptyLabel);

        JPanel detailContent = new JPanel(new GridBagLayout());
        detailContent.setOpaque(false);
        detailContent.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);

        friendHeaderCard = new HeaderCard();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        detailContent.add(friendHeaderCard, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0.5;
        gbc.fill = GridBagConstraints.BOTH;

        friendNextBirthdayCard = new MetricCard("Příští narozeniny", "");
        gbc.gridx = 0;
        detailContent.add(friendNextBirthdayCard, gbc);

        friendCountdownCard = new MetricCard("Odpočet", "");
        gbc.gridx = 1;
        detailContent.add(friendCountdownCard, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        detailContent.add(Box.createGlue(), gbc);

        CardLayout detailLayout = new CardLayout();
        JPanel detailCards = new JPanel(detailLayout);
        detailCards.setOpaque(false);
        detailCards.add(emptyState, "EMPTY");
        detailCards.add(detailContent, "DETAIL");
        
        detailRoot.add(detailCards, BorderLayout.CENTER);
        detailRoot.putClientProperty("detailLayout", detailLayout);
        detailRoot.putClientProperty("detailCards", detailCards);
        
        return detailRoot;
    }



    private void updateBirthdayDetail() {
        if (selectedBirthdayForDetail == null || detailPanel == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate nextBirthday = getNextBirthday(selectedBirthdayForDetail.birthDate, today);
        long daysUntilBirthday = ChronoUnit.DAYS.between(today, nextBirthday);
        Period age = Period.between(selectedBirthdayForDetail.birthDate, today);

        friendHeaderCard.updateAll(
            "NAROZENINY PŘÍTELE", 
            selectedBirthdayForDetail.name, 
            String.format("Věk: %d let (Narozen: %s)", age.getYears(), selectedBirthdayForDetail.birthDate.format(DATE_FORMATTER))
        );
        
        String countdownText = daysUntilBirthday == 0 ? "Dnes!" : String.format("za %,d dní", daysUntilBirthday);
        friendNextBirthdayCard.setValue(nextBirthday.format(DATE_FORMATTER));
        friendCountdownCard.setValue(countdownText);

        CardLayout detailLayout = (CardLayout) detailPanel.getClientProperty("detailLayout");
        if (detailLayout != null) {
            detailLayout.show((JPanel) detailPanel.getClientProperty("detailCards"), "DETAIL");
        }
    }

    private class InteractiveThemeItem extends JPanel {
        private final Theme theme;
        private final int index;
        private final JPopupMenu parentMenu;
        private float hoverProgress = 0f;
        private javax.swing.Timer hoverTimer;
        private boolean isHovered = false;

        public InteractiveThemeItem(Theme theme, int index, JPopupMenu parentMenu) {
            this.theme = theme;
            this.index = index;
            this.parentMenu = parentMenu;
            setPreferredSize(new Dimension(190, 46));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    animateHover();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    animateHover();
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isHovered) {
                        parentMenu.setVisible(false);
                        applyTheme(index);
                    }
                }
            });
        }

        private void animateHover() {
            if (hoverTimer != null && hoverTimer.isRunning()) {
                hoverTimer.stop();
            }
            hoverTimer = new javax.swing.Timer(16, e -> {
                if (isHovered && hoverProgress < 1f) {
                    hoverProgress += 0.15f;
                } else if (!isHovered && hoverProgress > 0f) {
                    hoverProgress -= 0.15f;
                } else {
                    ((javax.swing.Timer)e.getSource()).stop();
                }
                hoverProgress = Math.max(0f, Math.min(1f, hoverProgress));
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            float scale = 1.0f + (hoverProgress * 0.06f); // 6% zoom
            
            g2.translate(w/2, h/2);
            g2.scale(scale, scale);
            g2.translate(-w/2, -h/2);

            if (hoverProgress > 0) {
                g2.setColor(new Color(0, 0, 0, (int)(hoverProgress * 40)));
                g2.fillRoundRect(8, 8, w - 16, h - 10, 14, 14); // shadow
            }

            int bgAlpha = (int)(hoverProgress * 50);
            g2.setColor(new Color(theme.primary.getRed(), theme.primary.getGreen(), theme.primary.getBlue(), bgAlpha));
            g2.fillRoundRect(8, 4, w - 16, h - 8, 14, 14);

            int swatchX = 22;
            int swatchY = h/2 - 8;
            GradientPaint gp = new GradientPaint(swatchX, swatchY, theme.primary, swatchX + 16, swatchY + 16, theme.secondary);
            g2.setPaint(gp);
            g2.fillOval(swatchX, swatchY, 16, 16);
            g2.setColor(new Color(255, 255, 255, 100));
            g2.drawOval(swatchX, swatchY, 16, 16);

            g2.setFont(FONT_BODY_BOLD);
            if (hoverProgress > 0) {
                // slightly brighter text on hover
                int r = Math.min(255, currentTheme.textMain.getRed() + (int)(hoverProgress * 30));
                int gr = Math.min(255, currentTheme.textMain.getGreen() + (int)(hoverProgress * 30));
                int b = Math.min(255, currentTheme.textMain.getBlue() + (int)(hoverProgress * 30));
                g2.setColor(new Color(r, gr, b));
            } else {
                g2.setColor(currentTheme.textMain);
            }
            
            FontMetrics fm = g2.getFontMetrics();
            int textY = h/2 + fm.getAscent()/2 - 1;
            g2.drawString(theme.name, swatchX + 30, textY);

            g2.dispose();
        }
    }

    private void showThemeMenu(JButton button) {
        JPopupMenu themeMenu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        themeMenu.setOpaque(false);
        themeMenu.setBackground(new Color(0, 0, 0, 0));
        themeMenu.setBorder(BorderFactory.createEmptyBorder(12, 6, 12, 6));
        for (int i = 0; i < THEMES.length; i++) {
            themeMenu.add(new InteractiveThemeItem(THEMES[i], i, themeMenu));
        }
        themeMenu.show(button, 0, button.getHeight() + 4);
    }

    private class PillBorder implements javax.swing.border.Border {
        private final Color color;
        private final int thickness;

        PillBorder(Color color, int thickness) {
            this.color = color;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            int r = height - 1;
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2, width - thickness, height - thickness, r, r);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness + 6, 20, thickness + 6, 20);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private void updateDateFieldBorder(boolean focused) {
        int thickness = focused ? 2 : 1;
        Color color = focused ? currentTheme.secondary : new Color(255, 255, 255, 120);
        dateField.setBorder(new PillBorder(color, thickness));
    }

    private void updateResultsPanelBorder() {
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
    }

    private void showCalendarDialog() {
        showCalendarDialog(dateField, "Vyberte datum narození", this::calculateAge);
    }

    private void showCalendarDialog(JTextField targetField, String dialogTitle, Runnable afterPick) {
        LocalDate selectedDate = getDateFieldOrDefault(targetField);
        
        ModalOverlayPanel overlayPanel = new ModalOverlayPanel();
        overlayPanel.setBounds(0, 0, getRootPane().getWidth(), getRootPane().getHeight());
        
        JPanel contentPanel = new JPanel(new BorderLayout(12, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                paintLiquidGlass(g2, getWidth(), getHeight(), 28, false);
                g2.dispose();
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        headerPanel.setOpaque(false);
        JLabel dialogTitleLabel = new JLabel(dialogTitle);
        dialogTitleLabel.setFont(FONT_TITLE);
        dialogTitleLabel.setForeground(currentTheme.textMain);
        JLabel dialogHintLabel = new JLabel("Vyber datum kliknutím na den v kalendáři.");
        dialogHintLabel.setFont(FONT_BODY);
        dialogHintLabel.setForeground(currentTheme.textMuted);
        headerPanel.add(dialogTitleLabel, BorderLayout.NORTH);
        headerPanel.add(dialogHintLabel, BorderLayout.SOUTH);

        JPanel controlsPanel = new JPanel(new GridBagLayout());
        controlsPanel.setOpaque(false);
        controlsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        GridBagConstraints controls = new GridBagConstraints();
        controls.insets = new Insets(0, 4, 0, 4);

        GradientButton previousMonthButton = new GradientButton("◄", currentTheme.primary, currentTheme.secondary);
        previousMonthButton.setPreferredSize(new Dimension(56, 36));
        GradientButton nextMonthButton = new GradientButton("►", currentTheme.primary, currentTheme.secondary);
        nextMonthButton.setPreferredSize(new Dimension(56, 36));
        
        JComboBox<String> monthCombo = new JComboBox<>(new String[] {
            "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
            "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
        }) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paint(g);
            }
        };
        monthCombo.setOpaque(false);
        monthCombo.setForeground(Color.WHITE);
        monthCombo.setBorder(new PillBorder(new Color(255, 255, 255, 120), 1));
        monthCombo.setFont(FONT_BODY_BOLD);
        monthCombo.setPreferredSize(new Dimension(140, 36)); // Prevent truncation of long month names
        monthCombo.setSelectedIndex(selectedDate.getMonthValue() - 1);
        monthCombo.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton btn = new JButton("▼");
                btn.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 10)); // Add padding to center it
                btn.setContentAreaFilled(false);
                btn.setForeground(currentTheme.primary.brighter()); // Make it brighter to pop
                btn.setFocusPainted(false);
                btn.setFont(FONT_BODY_BOLD);
                return btn;
            }
        });
        monthCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                
                if (index == -1) {
                    // Selected item inside the combo box (should be transparent)
                    label.setOpaque(false);
                    label.setBackground(new Color(0, 0, 0, 0));
                    label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); // Match spinner padding
                } else {
                    // Items in the popup dropdown menu
                    label.setOpaque(true);
                    label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                    list.setBackground(currentTheme.backgroundTop);
                    if (isSelected) {
                        label.setBackground(currentTheme.secondary);
                        label.setForeground(Color.WHITE);
                    } else {
                        label.setBackground(currentTheme.backgroundTop);
                        label.setForeground(Color.WHITE);
                    }
                }
                return label;
            }
        });

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
            selectedDate.getYear(), 1900, LocalDate.now().getYear(), 1)) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 45));
                g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 40));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paint(g);
            }
        };
        yearSpinner.setOpaque(false);
        yearSpinner.setBorder(new PillBorder(new Color(255, 255, 255, 120), 1));
        yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#")); // Remove grouping separators like commas
        JComponent editor = yearSpinner.getEditor();
        editor.setOpaque(false);
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField tf = ((JSpinner.DefaultEditor)editor).getTextField();
            tf.setOpaque(false);
            tf.setBackground(new Color(0, 0, 0, 0)); // Ensure completely transparent background
            tf.setForeground(Color.WHITE);
            tf.setCaretColor(Color.WHITE);
            tf.setHorizontalAlignment(JTextField.CENTER);
            tf.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8)); // padding to replace the removed arrows
        }
        yearSpinner.setFont(FONT_BODY_BOLD);
        yearSpinner.setPreferredSize(new Dimension(100, 36)); // Increased dimension to show full year text (e.g. 2,009)

        // Hide ugly default arrows
        yearSpinner.setUI(new javax.swing.plaf.basic.BasicSpinnerUI() {
            @Override
            protected Component createNextButton() { return null; }
            @Override
            protected Component createPreviousButton() { return null; }
        });

        // Add slick mouse wheel scrolling for the year
        yearSpinner.addMouseWheelListener(e -> {
            int year = (Integer) yearSpinner.getValue();
            if (e.getWheelRotation() < 0) {
                year = Math.min(LocalDate.now().getYear(), year + 1);
            } else {
                year = Math.max(1900, year - 1);
            }
            yearSpinner.setValue(year);
        });

        controls.gridx = 0;
        controlsPanel.add(previousMonthButton, controls);
        controls.gridx = 1;
        controlsPanel.add(monthCombo, controls);
        controls.gridx = 2;
        controlsPanel.add(yearSpinner, controls);
        controls.gridx = 3;
        controlsPanel.add(nextMonthButton, controls);

        JPanel daysPanel = new JPanel(new GridLayout(0, 7, 6, 6));
        daysPanel.setOpaque(false);

        Runnable closeAction = () -> {
            javax.swing.Timer fadeOut = new javax.swing.Timer(16, null);
            fadeOut.addActionListener(new ActionListener() {
                long startTime = System.currentTimeMillis();
                @Override
                public void actionPerformed(ActionEvent e) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    float progress = Math.min(1.0f, elapsed / 250f);
                    float ease = (float)Math.pow(progress, 3); // ease-in cubic
                    overlayPanel.setAlpha(1.0f - ease);
                    overlayPanel.setYOffset((int)(20 * ease));
                    
                    int cx = (overlayPanel.getWidth() - contentPanel.getWidth()) / 2;
                    int cy = (overlayPanel.getHeight() - contentPanel.getHeight()) / 2;
                    contentPanel.setLocation(cx, cy + overlayPanel.yOffset);
                    
                    if (progress >= 1.0f) {
                        ((javax.swing.Timer)e.getSource()).stop();
                        getLayeredPane().remove(overlayPanel);
                        getLayeredPane().repaint();
                    }
                }
            });
            fadeOut.start();
        };

        Runnable refreshCalendar = () -> updateCalendarDays(daysPanel, monthCombo, yearSpinner, closeAction, targetField, selectedDate, afterPick);
        monthCombo.addActionListener(e -> refreshCalendar.run());
        yearSpinner.addChangeListener(e -> refreshCalendar.run());
        previousMonthButton.addActionListener(e -> moveCalendarMonth(monthCombo, yearSpinner, -1));
        nextMonthButton.addActionListener(e -> moveCalendarMonth(monthCombo, yearSpinner, 1));

        JPanel topBlock = new JPanel(new BorderLayout(0, 10));
        topBlock.setOpaque(false);
        topBlock.add(headerPanel, BorderLayout.NORTH);
        topBlock.add(controlsPanel, BorderLayout.SOUTH);

        GradientButton closeButton = new GradientButton("Zavřít", currentTheme.accent, currentTheme.accent.brighter());
        closeButton.setPreferredSize(new Dimension(100, 36));
        closeButton.addActionListener(e -> closeAction.run());
        
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);
        bottomPanel.add(closeButton);

        contentPanel.add(topBlock, BorderLayout.NORTH);
        contentPanel.add(daysPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        refreshCalendar.run();
        contentPanel.setSize(new Dimension(520, 500));
        
        int cx = (overlayPanel.getWidth() - contentPanel.getWidth()) / 2;
        int cy = (overlayPanel.getHeight() - contentPanel.getHeight()) / 2;
        contentPanel.setLocation(cx, cy + 20); // Start lower
        overlayPanel.add(contentPanel);

        getLayeredPane().add(overlayPanel, JLayeredPane.MODAL_LAYER);
        getLayeredPane().repaint();
        overlayPanel.requestFocusInWindow();

        javax.swing.Timer fadeIn = new javax.swing.Timer(16, null);
        fadeIn.addActionListener(new ActionListener() {
            long startTime = System.currentTimeMillis();
            @Override
            public void actionPerformed(ActionEvent e) {
                long elapsed = System.currentTimeMillis() - startTime;
                float progress = Math.min(1.0f, elapsed / 300f);
                float ease = 1.0f - (float)Math.pow(1.0f - progress, 3); // ease-out cubic
                
                overlayPanel.setAlpha(ease);
                overlayPanel.setYOffset(20 - (int)(20 * ease));
                
                int ncx = (overlayPanel.getWidth() - contentPanel.getWidth()) / 2;
                int ncy = (overlayPanel.getHeight() - contentPanel.getHeight()) / 2;
                contentPanel.setLocation(ncx, ncy + overlayPanel.yOffset);
                
                if (progress >= 1.0f) {
                    ((javax.swing.Timer)e.getSource()).stop();
                }
            }
        });
        fadeIn.start();
    }

    private LocalDate getDateFieldOrDefault(JTextField targetField) {
        LocalDate parsedDate = parseDateSilently(targetField.getText().trim());
        if (parsedDate != null && !parsedDate.isAfter(LocalDate.now())) {
            return parsedDate;
        }

        if (targetField == dateField && lastBirthDate != null) {
            return lastBirthDate;
        }

        return LocalDate.now();
    }

    private void moveCalendarMonth(JComboBox<String> monthCombo, JSpinner yearSpinner, int direction) {
        int month = monthCombo.getSelectedIndex() + 1;
        int year = (Integer) yearSpinner.getValue();
        YearMonth movedMonth = YearMonth.of(year, month).plusMonths(direction);
        LocalDate today = LocalDate.now();

        if (movedMonth.isAfter(YearMonth.from(today))) {
            return;
        }

        yearSpinner.setValue(movedMonth.getYear());
        monthCombo.setSelectedIndex(movedMonth.getMonthValue() - 1);
    }

    private void updateCalendarDays(
            JPanel daysPanel,
            JComboBox<String> monthCombo,
            JSpinner yearSpinner,
            Runnable closeAction,
            JTextField targetField,
            LocalDate selectedInField,
            Runnable afterPick) {
        daysPanel.removeAll();
        String[] dayNames = {"Po", "Út", "St", "Čt", "Pá", "So", "Ne"};
        for (String dayName : dayNames) {
            JLabel label = new JLabel(dayName, JLabel.CENTER);
            label.setFont(FONT_BODY_BOLD);
            label.setForeground(currentTheme.textMuted);
            label.setOpaque(true);
            label.setBackground(new Color(0,0,0,0));
            label.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
            daysPanel.add(label);
        }

        int month = monthCombo.getSelectedIndex() + 1;
        int year = (Integer) yearSpinner.getValue();
        YearMonth shownMonth = YearMonth.of(year, month);
        int firstDayOffset = shownMonth.atDay(1).getDayOfWeek().getValue() - 1;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < firstDayOffset; i++) {
            daysPanel.add(Box.createRigidArea(new Dimension(42, 36)));
        }

        for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
            LocalDate date = shownMonth.atDay(day);
            CalendarDayButton dayButton = createSmallCalendarButton(String.valueOf(day));
            dayButton.setEnabled(!date.isAfter(today));
            
            boolean isToday = date.equals(today);
            boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
            boolean isSelected = date.equals(selectedInField);
            
            dayButton.setStates(isToday, isWeekend, isSelected);
            dayButton.setToolTipText("Znamení: " + getZodiacSign(date));
            
            dayButton.addActionListener(e -> {
                targetField.setText(date.format(DATE_FORMATTER));
                closeAction.run();
                if (afterPick != null) {
                    afterPick.run();
                }
            });
            daysPanel.add(dayButton);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    private CalendarDayButton createSmallCalendarButton(String text) {
        return new CalendarDayButton(text, currentTheme);
    }

    private String getZodiacSign(LocalDate date) {
        int m = date.getMonthValue();
        int d = date.getDayOfMonth();
        if (m == 1)  return d < 20 ? "Kozoroh (Capricorn)" : "Vodnář (Aquarius)";
        if (m == 2)  return d < 19 ? "Vodnář (Aquarius)" : "Ryby (Pisces)";
        if (m == 3)  return d < 21 ? "Ryby (Pisces)" : "Beran (Aries)";
        if (m == 4)  return d < 20 ? "Beran (Aries)" : "Býk (Taurus)";
        if (m == 5)  return d < 21 ? "Býk (Taurus)" : "Blíženci (Gemini)";
        if (m == 6)  return d < 21 ? "Blíženci (Gemini)" : "Rak (Cancer)";
        if (m == 7)  return d < 23 ? "Rak (Cancer)" : "Lev (Leo)";
        if (m == 8)  return d < 23 ? "Lev (Leo)" : "Panna (Virgo)";
        if (m == 9)  return d < 23 ? "Panna (Virgo)" : "Váhy (Libra)";
        if (m == 10) return d < 23 ? "Váhy (Libra)" : "Štír (Scorpio)";
        if (m == 11) return d < 22 ? "Štír (Scorpio)" : "Střelec (Sagittarius)";
        if (m == 12) return d < 22 ? "Střelec (Sagittarius)" : "Kozoroh (Capricorn)";
        return "";
    }

    private LocalDate parseDateSilently(String dateStr) {
        if (dateStr.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
    
    private LocalDate validateDate(String dateStr) {
        try {
            LocalDate birthDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            LocalDate today = LocalDate.now();
            
            if (birthDate.isAfter(today)) {
                JOptionPane.showMessageDialog(this, 
                    "Datum narozeni nemuze byt v budoucnosti!", 
                    "Chyba", 
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            return birthDate;
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, 
                "Nespravny format data!\nZadejte: dd.mm.yyyy\nPriklad: 15.03.2000", 
                "Chyba", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private LocalDate getNextBirthday(LocalDate birthDate, LocalDate today) {
        LocalDate nextBirthday = birthDate.withYear(today.getYear());

        if (nextBirthday.isBefore(today)) {
            nextBirthday = nextBirthday.plusYears(1);
        }

        return nextBirthday;
    }

    private void setEmptyResults() {
        if (resultsLayout != null && resultsContentPanel != null) {
            resultsLayout.show(resultsContentPanel, "EMPTY");
        }
    }

    private void renderResults(LocalDate birthDate, boolean resetScroll, boolean isTimerUpdate) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();

        Period age = Period.between(birthDate, today);
        LocalDate nextBirthday = getNextBirthday(birthDate, today);
        long daysUntilBirthday = ChronoUnit.DAYS.between(today, nextBirthday);
        long days = ChronoUnit.DAYS.between(birthDate, today);
        long seconds = ChronoUnit.SECONDS.between(birthDate.atStartOfDay(), now);
        long hours = seconds / 3600;
        long minutes = seconds / 60;
        long months = ChronoUnit.MONTHS.between(birthDate, today);
        String birthdayText = daysUntilBirthday == 0
            ? "Dnes"
            : String.format("Za %,d dní", daysUntilBirthday);

        if (headerCard != null) {
            headerCard.setAge(age.getYears(), age.getMonths(), age.getDays());
        }
        if (cardBirthDate != null) {
            cardBirthDate.setValue(birthDate.format(DATE_FORMATTER));
        }
        if (cardZodiac != null) {
            cardZodiac.setValue(getZodiacSign(birthDate));
        }
        if (cardTodayDate != null) {
            cardTodayDate.setValue(today.format(DATE_FORMATTER));
        }
        if (cardMonthsTotal != null) {
            cardMonthsTotal.setValue(String.format("%,d", months));
        }
        if (cardNextBirthday != null) {
            cardNextBirthday.setValue(nextBirthday.format(DATE_FORMATTER));
        }
        if (cardCountdown != null) {
            cardCountdown.setValue(birthdayText);
        }
        if (cardDaysTotal != null) {
            cardDaysTotal.setValue(String.format("%,d", days));
        }
        if (cardHoursTotal != null) {
            cardHoursTotal.setValue(String.format("%,d", hours));
        }
        if (cardMinutesTotal != null) {
            cardMinutesTotal.setValue(String.format("%,d", minutes));
        }
        if (cardSecondsTotal != null) {
            cardSecondsTotal.setValue(String.format("%,d", seconds));
        }
        if (liveTimeLabel != null) {
            liveTimeLabel.setText("Živě podle systémového času: " + now.format(TIME_FORMATTER));
        }

        if (resultsLayout != null && resultsContentPanel != null) {
            resultsLayout.show(resultsContentPanel, "DASHBOARD");
        }

        if (!isTimerUpdate) {
            int delay = 0;
            if (headerCard != null) { headerCard.triggerEntryAnimation(delay); delay += 35; }
            if (cardBirthDate != null) { cardBirthDate.triggerEntryAnimation(delay); delay += 35; }
            if (cardZodiac != null) { cardZodiac.triggerEntryAnimation(delay); delay += 35; }
            if (cardTodayDate != null) { cardTodayDate.triggerEntryAnimation(delay); delay += 35; }
            if (cardMonthsTotal != null) { cardMonthsTotal.triggerEntryAnimation(delay); delay += 35; }
            if (cardNextBirthday != null) { cardNextBirthday.triggerEntryAnimation(delay); delay += 35; }
            if (cardCountdown != null) { cardCountdown.triggerEntryAnimation(delay); delay += 35; }
            if (cardDaysTotal != null) { cardDaysTotal.triggerEntryAnimation(delay); delay += 35; }
            if (cardHoursTotal != null) { cardHoursTotal.triggerEntryAnimation(delay); delay += 35; }
            if (cardMinutesTotal != null) { cardMinutesTotal.triggerEntryAnimation(delay); delay += 35; }
            if (cardSecondsTotal != null) { cardSecondsTotal.triggerEntryAnimation(delay); }
            
            long totalDuration = delay + 400;
            long animStart = System.currentTimeMillis();
            javax.swing.Timer masterTimer = new javax.swing.Timer(16, e -> {
                if (resultsContentPanel != null) {
                    resultsContentPanel.repaint();
                }
                if (System.currentTimeMillis() - animStart > totalDuration + 50) {
                    ((javax.swing.Timer)e.getSource()).stop();
                }
            });
            masterTimer.start();
        } else {
            if (resultsContentPanel != null) {
                resultsContentPanel.repaint();
            }
        }
    }

    private String metricRow(String firstLabel, String firstValue, String firstId,
                             String secondLabel, String secondValue, String secondId) {
        return String.format("<tr>%s%s</tr>",
            metricCell(firstLabel, firstValue, firstId),
            metricCell(secondLabel, secondValue, secondId));
    }

    private String metricCell(String label, String value, String valueId) {
        String idAttr = (valueId != null) ? String.format(" id='%s'", valueId) : "";
        return String.format(
            "<td width='50%%' bgcolor='%s' style='padding: 13px 15px; border: 1px solid %s;'>" +
            "<div style='font-size: 11px; color: %s;'>%s</div>" +
            "<div%s style='margin-top: 5px; font-size: 19px; font-weight: 700; color: %s;'>%s</div>" +
            "</td>",
            hex(currentTheme.inputBackground),
            hex(currentTheme.primary.darker()),
            hex(currentTheme.textMuted),
            label,
            idAttr,
            hex(currentTheme.textMain),
            value
        );
    }

    private String bodyStyle() {
        return String.format(
            "margin: 0; font-family: %s, sans-serif; background: %s; color: %s;",
            UI_FONT,
            hex(currentTheme.cardBackground),
            hex(currentTheme.textMain)
        );
    }

    private String gradientFallback() {
        return hex(mix(currentTheme.primary, currentTheme.secondary));
    }

    private static Color mix(Color first, Color second) {
        return new Color(
            (first.getRed() + second.getRed()) / 2,
            (first.getGreen() + second.getGreen()) / 2,
            (first.getBlue() + second.getBlue()) / 2
        );
    }

    private static String hex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void enableSmoothScrolling(JScrollPane scrollPane) {
        JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        verticalBar.setUnitIncrement(24);
        verticalBar.setBlockIncrement(120);
    }
    
    private void calculateAge() {
        String dateStr = dateField.getText().trim();
        
        if (dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Prosim, zadejte datum narozeni!", 
                "Upozorneni", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        LocalDate birthDate = validateDate(dateStr);
        if (birthDate == null) return;

        lastBirthDate = birthDate;
        renderResults(birthDate, true, false);
        addDateToHistory(birthDate);
    }
    
    private void clear() {
        dateField.setText("");
        lastBirthDate = null;
        setEmptyResults();
        dateField.requestFocus();
    }
    
    public static void main(String[] args) {
        System.setProperty("sun.awt.X11.XWMClass", "BirthCalculator");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("sun.java2d.d3d", "true");
        System.setProperty("sun.java2d.noddraw", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            // Keep Swing's default look and feel when the platform one is unavailable.
        }

        configureDarkUIDefaults();

        SwingUtilities.invokeLater(() -> {
            BirthCalculator frame = new BirthCalculator();
            frame.setVisible(true);
        });
    }

    private static void configureDarkUIDefaults() {
        Theme theme = THEMES[0];
        UIManager.put("Button.font", FONT_BUTTON);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("OptionPane.buttonFont", FONT_BUTTON);
        UIManager.put("OptionPane.messageFont", FONT_BODY);
        UIManager.put("TextArea.font", FONT_MONO);
        UIManager.put("TextField.font", FONT_INPUT);
        UIManager.put("ToolTip.font", FONT_BODY);
        UIManager.put("ToolTip.background", theme.cardBackground);
        UIManager.put("ToolTip.foreground", theme.textMain);
        UIManager.put("ToolTip.border", BorderFactory.createLineBorder(theme.primary, 1));
        UIManager.put("Panel.background", theme.backgroundTop);
        UIManager.put("OptionPane.background", theme.cardBackground);
        UIManager.put("OptionPane.messageForeground", theme.textMain);
        UIManager.put("TextField.background", theme.inputBackground);
        UIManager.put("TextField.foreground", theme.textMain);
        UIManager.put("TextField.caretForeground", theme.primary);
        UIManager.put("TextArea.background", theme.inputBackground);
        UIManager.put("TextArea.foreground", theme.textMain);
        UIManager.put("ScrollPane.background", theme.cardBackground);
        UIManager.put("Viewport.background", theme.inputBackground);
        UIManager.put("ComboBox.background", theme.cardBackground);
        UIManager.put("ComboBox.foreground", theme.textMain);
        UIManager.put("ComboBox.selectionBackground", theme.primary);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("Label.foreground", theme.textMain);
    }
}
