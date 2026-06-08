import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
import java.util.LinkedHashSet;
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
    private static final String UI_FONT = chooseFont(
        "Inter", "SF Pro Display", "Segoe UI", "Noto Sans", "Arial", Font.SANS_SERIF);
    private static final String MONO_FONT = chooseFont(
        "JetBrains Mono", "Cascadia Mono", "Fira Code", "Noto Sans Mono", Font.MONOSPACED);
    private static final Font FONT_BODY = new Font(UI_FONT, Font.PLAIN, 13);
    private static final Font FONT_BODY_BOLD = new Font(UI_FONT, Font.BOLD, 14);
    private static final Font FONT_BUTTON = new Font(UI_FONT, Font.BOLD, 14);
    private static final Font FONT_INPUT = new Font(UI_FONT, Font.BOLD, 20);
    private static final Font FONT_TITLE = new Font(UI_FONT, Font.BOLD, 30);
    private static final Font FONT_MONO = new Font(MONO_FONT, Font.PLAIN, 13);

    private JTextField dateField;
    private JEditorPane resultsArea;
    private JScrollPane resultsScrollPane;
    private JPanel themePanel;
    private JComboBox<String> historyCombo;
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
    private JPanel detailPanel;
    private JLabel detailNameLabel;
    private JLabel detailBirthdateLabel;
    private JLabel detailAgeLabel;
    private JLabel detailCountdownLabel;

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

    private static class GradientButton extends JButton {
        private Color start;
        private Color end;

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
        }

        void setColors(Color start, Color end) {
            this.start = start;
            this.end = end;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color drawStart = start;
            Color drawEnd = end;
            if (getModel().isPressed()) {
                drawStart = start.darker();
                drawEnd = end.darker();
            } else if (getModel().isRollover()) {
                drawStart = end;
                drawEnd = start;
            }

            GradientPaint gradient = new GradientPaint(0, 0, drawStart, 0, getHeight(), drawEnd);
            g2.setPaint(gradient);
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRoundRect(0, 4, getWidth(), getHeight() - 2, 12, 12);

            g2.setPaint(gradient);
            g2.fillRoundRect(0, 0, getWidth(), getHeight() - 4, 12, 12);

            if (getModel().isRollover()) {
                g2.setColor(new Color(255, 255, 255, 45));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 7, 12, 12);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class ModernScrollBarUI extends BasicScrollBarUI {
        private final Theme theme;

        ModernScrollBarUI(Theme theme) {
            this.theme = theme;
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
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(theme.inputBackground);
            g2.fillRoundRect(trackBounds.x + 3, trackBounds.y + 3,
                trackBounds.width - 6, trackBounds.height - 6, 10, 10);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gradient = new GradientPaint(
                thumbBounds.x, thumbBounds.y, theme.primary,
                thumbBounds.x + thumbBounds.width, thumbBounds.y + thumbBounds.height, theme.secondary);
            g2.setPaint(gradient);
            g2.fillRoundRect(thumbBounds.x + 3, thumbBounds.y + 3,
                thumbBounds.width - 6, thumbBounds.height - 6, 10, 10);
            g2.dispose();
        }
    }

    private static final Theme[] THEMES = new Theme[] {
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
        setPreferredSize(new Dimension(700, 680));
        setMinimumSize(new Dimension(700, 680));
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
        getContentPane().setBackground(currentTheme.backgroundTop);

        // Hlavní panel s BorderLayout
        mainPanel = new JPanel(new BorderLayout(14, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, currentTheme.backgroundTop,
                    getWidth(), getHeight(), currentTheme.backgroundBottom);
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(currentTheme.glow);
                int[] bandX = {0, getWidth(), getWidth(), 0};
                int[] bandY = {68, 0, 58, 128};
                g2.fillPolygon(bandX, bandY, 4);
            }
        };
        mainPanel.setOpaque(true);
        mainPanel.setBackground(currentTheme.backgroundTop);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        
        // ===== TOP THEME SELECTOR - ONE ICON WITH DROPDOWN =====
        JPanel themeSelectorPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 10));
        themeSelectorPanel.setOpaque(false);
        
        JButton themeDropdownBtn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                GradientPaint grad = new GradientPaint(0, 0, currentTheme.primary, getWidth(), getHeight(), currentTheme.secondary);
                g2.setPaint(grad);
                g2.fillRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
                g2.setFont(new Font(UI_FONT, Font.BOLD, 16));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth("🎨")) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString("🎨", x, y);
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
        titlePanel.setBorder(BorderFactory.createEmptyBorder(18, 22, 20, 22));

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
        inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setOpaque(false);
        
        // Instrukce
        instructionLabel = new JLabel("Zadejte datum narození ve formátu dd.mm.yyyy");
        instructionLabel.setFont(FONT_BODY_BOLD);
        instructionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        inputPanel.add(instructionLabel, BorderLayout.NORTH);
        
        // Textové pole s lepším designem
        dateField = new JTextField(20);
        dateField.setFont(FONT_INPUT);
        dateField.setHorizontalAlignment(JTextField.CENTER);
        dateField.setToolTipText("Například 15.03.2000");
        dateField.setPreferredSize(new Dimension(260, 48));
        dateField.addActionListener(e -> calculateAge());
        dateField.setBackground(currentTheme.inputBackground);
        dateField.setForeground(currentTheme.textMain);
        updateDateFieldBorder(false);
        dateField.setCaretColor(currentTheme.primary);
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
        calendarButton.setPreferredSize(new Dimension(124, 42));
        calendarButton.addActionListener(e -> showCalendarDialog());
        dateConstraints.gridx = 1;
        datePanel.add(calendarButton, dateConstraints);

        historyCombo = new JComboBox<>();
        historyCombo.setFont(FONT_BODY);
        historyCombo.setPreferredSize(new Dimension(260, 34));
        historyCombo.addActionListener(e -> selectHistoryDate());
        dateConstraints.gridy = 1;
        dateConstraints.gridx = 0;
        datePanel.add(historyCombo, dateConstraints);

        removeHistoryButton = createStyledButton("Odebrat", currentTheme.accent, currentTheme.accent.brighter());
        removeHistoryButton.setPreferredSize(new Dimension(124, 34));
        removeHistoryButton.addActionListener(e -> removeSelectedHistoryDate());
        dateConstraints.gridx = 1;
        datePanel.add(removeHistoryButton, dateConstraints);
        inputPanel.add(datePanel, BorderLayout.CENTER);
        
        // Tlačítka jako ikony
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 8));
        buttonPanel.setOpaque(false);
        
        calculateButton = createIconButton("✓", currentTheme.primary, currentTheme.secondary);
        calculateButton.setToolTipText("Vypočítat věk");
        calculateButton.addActionListener(e -> calculateAge());
        buttonPanel.add(calculateButton);

        birthdaysButton = createIconButton("🎂", currentTheme.secondary, currentTheme.primary);
        birthdaysButton.setToolTipText("Narozeniny přátel");
        birthdaysButton.addActionListener(e -> showBirthdaysDialog());
        buttonPanel.add(birthdaysButton);
        
        clearButton = createIconButton("✕", currentTheme.accent, currentTheme.accent.brighter());
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
        
        resultsArea = new JEditorPane();
        resultsArea.setContentType("text/html");
        resultsArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        resultsArea.setFont(FONT_BODY);
        resultsArea.setEditable(false);
        resultsArea.setOpaque(true);
        resultsArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        resultsArea.setMargin(new Insets(0, 0, 0, 0));
        resultsArea.setBackground(currentTheme.inputBackground);
        resultsArea.setForeground(currentTheme.textMain);
        setEmptyResults();
        
        resultsScrollPane = new JScrollPane(resultsArea);
        styleResultsScrollPane();
        resultsPanel.add(resultsScrollPane, BorderLayout.CENTER);
        
        // ===== KOMBINACE VŠECH PANELŮ =====
        centerPanel = new JPanel(new BorderLayout(15, 15));
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
        setSize(new Dimension(700, 680));
    }
    
    private void applyTheme(int themeIndex) {
        if (themeIndex < 0 || themeIndex >= THEMES.length) {
            return;
        }

        currentTheme = THEMES[themeIndex];
        preferences.putInt(PREF_THEME_INDEX, themeIndex);
        getContentPane().setBackground(currentTheme.backgroundTop);
        mainPanel.setBackground(currentTheme.backgroundTop);
        
        updateDateFieldBorder(dateField.hasFocus());
        updateResultsPanelBorder();
        dateField.setBackground(currentTheme.inputBackground);
        dateField.setForeground(currentTheme.textMain);
        dateField.setCaretColor(currentTheme.primary);

        resultsArea.setBackground(currentTheme.inputBackground);
        resultsArea.setForeground(currentTheme.textMain);
        styleResultsScrollPane();
        if (lastBirthDate == null) {
            setEmptyResults();
        } else {
            renderResults(lastBirthDate, true);
        }

        instructionLabel.setForeground(currentTheme.textMuted);
        resultsLabel.setForeground(currentTheme.primary);
        titleLabel.setForeground(Color.WHITE);
        subtitleLabel.setForeground(new Color(255, 255, 255, 220));

        historyCombo.setBackground(currentTheme.cardBackground);
        historyCombo.setForeground(currentTheme.textMain);
        historyCombo.setBorder(BorderFactory.createLineBorder(currentTheme.primary.darker(), 1));

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
            detailNameLabel.setForeground(currentTheme.textMain);
            detailBirthdateLabel.setForeground(currentTheme.textMuted);
            detailAgeLabel.setForeground(currentTheme.textMuted);
            detailCountdownLabel.setForeground(currentTheme.secondary);
            detailPanel.repaint();
            if (selectedBirthdayForDetail != null) {
                updateBirthdayDetail();
            }
        }
        
        mainPanel.repaint();
        centerPanel.repaint();
        if (birthdayCard != null) {
            birthdayCard.repaint();
        }
        resultsPanel.repaint();
    }

    private void styleResultsScrollPane() {
        if (resultsScrollPane == null) {
            return;
        }

        resultsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        resultsScrollPane.setBackground(currentTheme.inputBackground);
        resultsScrollPane.getViewport().setBackground(currentTheme.inputBackground);
        resultsScrollPane.getViewport().setOpaque(true);
        resultsScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        resultsScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        JScrollBar verticalBar = resultsScrollPane.getVerticalScrollBar();
        verticalBar.setPreferredSize(new Dimension(12, 0));
        verticalBar.setOpaque(false);
        verticalBar.setUnitIncrement(18);
        verticalBar.setUI(new ModernScrollBarUI(currentTheme));
    }

    private void startRealtimeUpdates() {
        realtimeTimer = new Timer(1000, e -> {
            if (lastBirthDate != null) {
                renderResults(lastBirthDate, false);
            }
        });
        realtimeTimer.setInitialDelay(1000);
        realtimeTimer.start();
    }

    private void loadHistory() {
        updatingHistory = true;
        historyCombo.removeAllItems();

        for (String date : getHistoryDates()) {
            historyCombo.addItem(date);
        }

        updatingHistory = false;
        removeHistoryButton.setEnabled(historyCombo.getItemCount() > 0);
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
        historyCombo.setSelectedItem(formattedDate);
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
            renderResults(birthDate, true);
        }
    }

    private void selectHistoryDate() {
        if (updatingHistory || historyCombo.getSelectedItem() == null) {
            return;
        }

        dateField.setText(historyCombo.getSelectedItem().toString());
        calculateAge();
    }

    private void removeSelectedHistoryDate() {
        Object selectedItem = historyCombo.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        String selectedDate = selectedItem.toString();
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

        cardsLayout.show(cardsPanel, "BIRTHDAYS");
        if (birthdayNameField != null) {
            birthdayNameField.requestFocus();
        }
        refreshBirthdayListModel();
        updateBirthdaySummary();
    }

    private void showCalculatorView() {
        if (cardsLayout == null || cardsPanel == null) {
            return;
        }

        cardsLayout.show(cardsPanel, "CALCULATOR");
        if (dateField != null) {
            dateField.requestFocus();
        }
    }

    private JPanel createBirthdayCard() {
        JPanel birthdayRoot = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                GradientPaint gradient = new GradientPaint(0, 0, currentTheme.backgroundTop,
                    getWidth(), getHeight(), currentTheme.backgroundBottom);
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(currentTheme.glow);
                int[] bandX = {getWidth(), 0, 0, getWidth()};
                int[] bandY = {58, 90, 0, 18};
                g2.fillPolygon(bandX, bandY, 4);
            }
        };
        birthdayRoot.setOpaque(true);
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

        birthdaysBackButton = createIconButton("←", currentTheme.accent, currentTheme.accent.brighter());
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(8, 8, getWidth() - 10, getHeight() - 10, 18, 18);
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 18, 18);
                g2.setPaint(new GradientPaint(16, 0, currentTheme.secondary, Math.max(120, getWidth() / 2), 0, currentTheme.primary));
                g2.fillRoundRect(16, 12, Math.max(88, getWidth() / 4), 4, 4, 4);
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

        birthdayNameField = new JTextField();
        birthdayNameField.setFont(FONT_BODY_BOLD);
        birthdayNameField.setPreferredSize(new Dimension(260, 40));
        birthdayNameField.setBackground(currentTheme.inputBackground);
        birthdayNameField.setForeground(currentTheme.textMain);
        birthdayNameField.setCaretColor(currentTheme.primary);
        birthdayNameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.primary.darker(), 2),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
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

        birthdayDateField = new JTextField();
        birthdayDateField.setFont(FONT_BODY_BOLD);
        birthdayDateField.setPreferredSize(new Dimension(260, 40));
        birthdayDateField.setToolTipText("Například 15.03.2000");
        birthdayDateField.setBackground(currentTheme.inputBackground);
        birthdayDateField.setForeground(currentTheme.textMain);
        birthdayDateField.setCaretColor(currentTheme.primary);
        birthdayDateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.primary.darker(), 2),
            BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        birthdayDateRow.add(birthdayDateField, birthdayDateConstraints);

        birthdayPickDateButton = createIconButton("📅", currentTheme.primary, currentTheme.secondary);
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
        birthdayAddButton = createIconButton("➕", currentTheme.primary, currentTheme.secondary);
        birthdayAddButton.setToolTipText("Přidat narozeniny");
        birthdayAddButton.addActionListener(e -> addBirthdayEntry());
        actionRow.add(birthdayAddButton);

        birthdayRemoveButton = createIconButton("✕", currentTheme.accent, currentTheme.accent.brighter());
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(8, 8, getWidth() - 10, getHeight() - 10, 18, 18);
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 18, 18);
                g2.setPaint(new GradientPaint(16, 0, currentTheme.primary, Math.max(120, getWidth() / 2), 0, currentTheme.secondary));
                g2.fillRoundRect(16, 12, Math.max(88, getWidth() / 4), 4, 4, 4);
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
            JLabel label = new JLabel();
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            label.setFont(FONT_BODY_BOLD);
            label.setText(value.name);

            if (isSelected) {
                label.setBackground(currentTheme.secondary);
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(index % 2 == 0 ? currentTheme.inputBackground : currentTheme.cardBackground);
                label.setForeground(currentTheme.textMain);
            }

            return label;
        });
        birthdayList.addListSelectionListener(e -> {
            if (birthdayList.getSelectedValue() != null) {
                selectedBirthdayForDetail = birthdayList.getSelectedValue();
                updateBirthdayDetail();
            }
        });

        JScrollPane birthdayScrollPane = new JScrollPane(birthdayList);
        birthdayScrollPane.setBorder(BorderFactory.createEmptyBorder());
        birthdayScrollPane.setBackground(currentTheme.inputBackground);
        birthdayScrollPane.getViewport().setBackground(currentTheme.inputBackground);
        birthdayScrollPane.setPreferredSize(new Dimension(320, 300));
        birthdayScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        birthdayScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        birthdayScrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI(currentTheme));
        listCard.add(birthdayScrollPane, BorderLayout.CENTER);

        JPanel listWithDetailPanel = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        listWithDetailPanel.setOpaque(false);
        listWithDetailPanel.add(listCard, BorderLayout.WEST);
        
        detailPanel = createBirthdayDetailPanel();
        listWithDetailPanel.add(detailPanel, BorderLayout.CENTER);

        contentPanel.add(formCard, BorderLayout.WEST);
        contentPanel.add(listWithDetailPanel, BorderLayout.CENTER);

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
    
    private GradientButton createIconButton(String icon, Color color1, Color color2) {
        GradientButton btn = new GradientButton(icon, color1, color2);
        btn.setPreferredSize(new Dimension(52, 52));
        btn.setFont(new Font(UI_FONT, Font.BOLD, 20));
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fillRoundRect(8, 8, getWidth() - 10, getHeight() - 10, 18, 18);
                g2.setColor(currentTheme.cardBackground);
                g2.fillRoundRect(0, 0, getWidth() - 8, getHeight() - 8, 18, 18);
                g2.setPaint(new GradientPaint(16, 0, currentTheme.primary, Math.max(120, getWidth() / 2), 0, currentTheme.secondary));
                g2.fillRoundRect(16, 12, Math.max(88, getWidth() / 4), 4, 4, 4);
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

        JPanel detailContent = new JPanel();
        detailContent.setLayout(new BoxLayout(detailContent, BoxLayout.Y_AXIS));
        detailContent.setOpaque(false);
        detailContent.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        detailNameLabel = new JLabel();
        detailNameLabel.setFont(new Font(UI_FONT, Font.BOLD, 20));
        detailNameLabel.setForeground(currentTheme.textMain);
        detailNameLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));
        detailContent.add(detailNameLabel);

        detailBirthdateLabel = new JLabel();
        detailBirthdateLabel.setFont(FONT_BODY);
        detailBirthdateLabel.setForeground(currentTheme.textMuted);
        detailBirthdateLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        detailContent.add(detailBirthdateLabel);

        detailAgeLabel = new JLabel();
        detailAgeLabel.setFont(FONT_BODY);
        detailAgeLabel.setForeground(currentTheme.textMuted);
        detailAgeLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        detailContent.add(detailAgeLabel);

        detailCountdownLabel = new JLabel();
        detailCountdownLabel.setFont(FONT_BODY);
        detailCountdownLabel.setForeground(currentTheme.secondary);
        detailCountdownLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        detailContent.add(detailCountdownLabel);
        
        detailContent.add(Box.createVerticalGlue());

        JScrollPane detailScroll = new JScrollPane(detailContent);
        detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setBackground(currentTheme.cardBackground);
        detailScroll.getViewport().setBackground(currentTheme.cardBackground);
        detailScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI(currentTheme));

        CardLayout detailLayout = new CardLayout();
        JPanel detailCards = new JPanel(detailLayout);
        detailCards.setOpaque(false);
        detailCards.add(emptyState, "EMPTY");
        detailCards.add(detailScroll, "DETAIL");
        
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

        detailNameLabel.setText(selectedBirthdayForDetail.name);
        detailBirthdateLabel.setText("📅 " + selectedBirthdayForDetail.birthDate.format(DATE_FORMATTER));
        detailAgeLabel.setText(String.format("🎂 %d let", age.getYears()));
        
        String countdownText = daysUntilBirthday == 0 ? "Dnes!" : String.format("Za %,d dní", daysUntilBirthday);
        detailCountdownLabel.setText(String.format("⏳ %s (%s)", 
            nextBirthday.format(DATE_FORMATTER), countdownText));

        CardLayout detailLayout = (CardLayout) detailPanel.getClientProperty("detailLayout");
        if (detailLayout != null) {
            detailLayout.show((JPanel) detailPanel.getClientProperty("detailCards"), "DETAIL");
        }
    }

    private void showThemeMenu(JButton button) {
        JPopupMenu themeMenu = new JPopupMenu();
        for (int i = 0; i < THEMES.length; i++) {
            final int themeIndex = i;
            Theme t = THEMES[i];
            JMenuItem item = new JMenuItem(t.name);
            item.addActionListener(e -> applyTheme(themeIndex));
            themeMenu.add(item);
        }
        themeMenu.show(button, 0, button.getHeight());
    }

    private void updateDateFieldBorder(boolean focused) {
        int thickness = focused ? 3 : 2;
        dateField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(focused ? currentTheme.secondary : currentTheme.primary, thickness),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    }

    private void updateResultsPanelBorder() {
        resultsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.primary.darker(), 1),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)));
    }

    private void showCalendarDialog() {
        showCalendarDialog(dateField, "Vyberte datum narození", this::calculateAge);
    }

    private void showCalendarDialog(JTextField targetField, String dialogTitle, Runnable afterPick) {
        LocalDate selectedDate = getDateFieldOrDefault(targetField);
        JDialog dialog = new JDialog(this, dialogTitle, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.getContentPane().setBackground(currentTheme.backgroundTop);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.setOpaque(true);
        contentPanel.setBackground(currentTheme.backgroundTop);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 4));
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
        GridBagConstraints controls = new GridBagConstraints();
        controls.insets = new Insets(0, 5, 0, 5);

        JButton previousMonthButton = createSmallCalendarButton("<");
        JButton nextMonthButton = createSmallCalendarButton(">");
        JComboBox<String> monthCombo = new JComboBox<>(new String[] {
            "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
            "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
        });
        monthCombo.setFont(FONT_BODY_BOLD);
        monthCombo.setBackground(currentTheme.cardBackground);
        monthCombo.setForeground(currentTheme.textMain);
        monthCombo.setSelectedIndex(selectedDate.getMonthValue() - 1);

        JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
            selectedDate.getYear(), 1900, LocalDate.now().getYear(), 1));
        yearSpinner.setFont(FONT_BODY_BOLD);
        yearSpinner.setPreferredSize(new Dimension(88, 32));

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

        Runnable refreshCalendar = () -> updateCalendarDays(daysPanel, monthCombo, yearSpinner, dialog, targetField, afterPick);
        monthCombo.addActionListener(e -> refreshCalendar.run());
        yearSpinner.addChangeListener(e -> refreshCalendar.run());
        previousMonthButton.addActionListener(e -> moveCalendarMonth(monthCombo, yearSpinner, -1));
        nextMonthButton.addActionListener(e -> moveCalendarMonth(monthCombo, yearSpinner, 1));

        JPanel topBlock = new JPanel(new BorderLayout(0, 10));
        topBlock.setOpaque(false);
        topBlock.add(headerPanel, BorderLayout.NORTH);
        topBlock.add(controlsPanel, BorderLayout.SOUTH);

        contentPanel.add(topBlock, BorderLayout.NORTH);
        contentPanel.add(daysPanel, BorderLayout.CENTER);
        dialog.add(contentPanel, BorderLayout.CENTER);

        refreshCalendar.run();
        dialog.pack();
        dialog.setMinimumSize(new Dimension(480, 430));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
            JDialog dialog,
            JTextField targetField,
            Runnable afterPick) {
        daysPanel.removeAll();
        String[] dayNames = {"Po", "Út", "St", "Čt", "Pá", "So", "Ne"};
        for (String dayName : dayNames) {
            JLabel label = new JLabel(dayName, SwingConstants.CENTER);
            label.setFont(FONT_BODY_BOLD);
            label.setForeground(currentTheme.textMuted);
            label.setOpaque(true);
            label.setBackground(currentTheme.cardBackground);
            label.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
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
            JButton dayButton = createSmallCalendarButton(String.valueOf(day));
            dayButton.setEnabled(!date.isAfter(today));
            boolean isToday = date.equals(today);
            boolean isWeekend = date.getDayOfWeek().getValue() >= 6;
            if (isToday) {
                dayButton.setBackground(currentTheme.secondary);
                dayButton.setForeground(Color.WHITE);
            } else if (isWeekend) {
                dayButton.setBackground(currentTheme.inputBackground);
                dayButton.setForeground(currentTheme.accent);
            } else {
                dayButton.setBackground(currentTheme.cardBackground);
                dayButton.setForeground(currentTheme.textMain);
            }
            dayButton.addActionListener(e -> {
                targetField.setText(date.format(DATE_FORMATTER));
                dialog.dispose();
                if (afterPick != null) {
                    afterPick.run();
                }
            });
            daysPanel.add(dayButton);
        }

        daysPanel.revalidate();
        daysPanel.repaint();
    }

    private JButton createSmallCalendarButton(String text) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY_BOLD);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setForeground(currentTheme.textMain);
        button.setBackground(currentTheme.cardBackground);
        button.setPreferredSize(new Dimension(42, 36));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(currentTheme.primary.darker(), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return button;
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
        resultsArea.setText(String.format(
            "<html><body style='%s'>" +
            "<div style='padding: 28px 30px;'>" +
            "<div style='font-size: 24px; font-weight: 700; color: %s;'>Čekám na datum narození</div>" +
            "<div style='margin-top: 10px; font-size: 13px; color: %s; line-height: 1.5;'>" +
            "Zadejte datum ve formátu <b>dd.mm.yyyy</b> a výsledek se zobrazí jako přehledný moderní panel." +
            "</div>" +
            "<div style='margin-top: 22px; padding: 16px; background: %s; color: %s; border: 1px solid %s;'>" +
            "Tip: můžete použít například <b>15.03.2000</b>." +
            "</div>" +
            "</div></body></html>",
            bodyStyle(),
            hex(currentTheme.textMain),
            hex(currentTheme.textMuted),
            hex(currentTheme.cardBackground),
            hex(currentTheme.textMuted),
            hex(currentTheme.primary.darker())
        ));
        resultsArea.setCaretPosition(0);
    }

    private void renderResults(LocalDate birthDate, boolean resetScroll) {
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

        String html = String.format(
            "<html><body style='%s'>" +
            "<div style='padding: 22px 24px 28px 24px;'>" +
            "<table width='100%%' cellpadding='0' cellspacing='0'>" +
            "<tr>" +
            "<td bgcolor='%s' style='padding: 18px; color: #ffffff;'>" +
            "<div style='font-size: 12px; letter-spacing: 1px;'>VĚK DNES</div>" +
            "<div style='font-size: 38px; font-weight: 800; margin-top: 6px;'>%d let</div>" +
            "<div style='font-size: 15px; margin-top: 4px;'>%d měsíců a %d dní</div>" +
            "</td>" +
            "</tr>" +
            "</table>" +
            "<table width='100%%' cellpadding='0' cellspacing='10'>" +
            "%s%s" +
            "</table>" +
            "<div style='margin-top: 8px; font-size: 16px; font-weight: 700; color: %s;'>Narozeniny</div>" +
            "<table width='100%%' cellpadding='0' cellspacing='10'>" +
            "%s" +
            "</table>" +
            "<div style='margin-top: 8px; font-size: 16px; font-weight: 700; color: %s;'>Celkový čas</div>" +
            "<table width='100%%' cellpadding='0' cellspacing='10'>" +
            "%s%s" +
            "</table>" +
            "<div style='margin: 4px 10px 0 10px; font-size: 11px; color: %s;'>Živě podle systémového času: %s</div>" +
            "</div></body></html>",
            bodyStyle(),
            gradientFallback(),
            age.getYears(), age.getMonths(), age.getDays(),
            metricRow("Datum narození", birthDate.format(DATE_FORMATTER),
                "Dnešní datum", today.format(DATE_FORMATTER)),
            metricRow("Měsíců celkem", String.format("%,d", months),
                "Přibližně týdnů", String.format("%,.1f", days / 7.0)),
            hex(currentTheme.textMain),
            metricRow("Příští narozeniny", nextBirthday.format(DATE_FORMATTER), "Odpočet", birthdayText),
            hex(currentTheme.textMain),
            metricRow("Dní", String.format("%,d", days), "Hodin", String.format("%,d", hours)),
            metricRow("Minut", String.format("%,d", minutes), "Sekund", String.format("%,d", seconds)),
            hex(currentTheme.textMuted),
            now.format(TIME_FORMATTER)
        );

        int previousScroll = resultsScrollPane == null ? 0 : resultsScrollPane.getVerticalScrollBar().getValue();
        resultsArea.setText(html);
        if (resetScroll) {
            resultsArea.setCaretPosition(0);
        } else if (resultsScrollPane != null) {
            SwingUtilities.invokeLater(() -> resultsScrollPane.getVerticalScrollBar().setValue(previousScroll));
        }
    }

    private String metricRow(String firstLabel, String firstValue, String secondLabel, String secondValue) {
        return String.format("<tr>%s%s</tr>",
            metricCell(firstLabel, firstValue),
            metricCell(secondLabel, secondValue));
    }

    private String metricCell(String label, String value) {
        return String.format(
            "<td width='50%%' bgcolor='%s' style='padding: 13px 15px; border: 1px solid %s;'>" +
            "<div style='font-size: 11px; color: %s;'>%s</div>" +
            "<div style='margin-top: 5px; font-size: 19px; font-weight: 700; color: %s;'>%s</div>" +
            "</td>",
            hex(currentTheme.inputBackground),
            hex(currentTheme.primary.darker()),
            hex(currentTheme.textMuted),
            label,
            hex(currentTheme.textMain),
            value
        );
    }

    private String bodyStyle() {
        return String.format(
            "margin: 0; font-family: %s, sans-serif; background: %s; color: %s;",
            UI_FONT,
            hex(currentTheme.inputBackground),
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
        renderResults(birthDate, true);
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
