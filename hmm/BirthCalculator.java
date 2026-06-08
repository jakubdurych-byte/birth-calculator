import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class BirthCalculator extends JFrame {
    private JTextField dateField;
    private JTextArea resultsArea;
    
    public BirthCalculator() {
        // Nastavení okna
        setTitle("Kalkulátor Věku");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 700);
        setLocationRelativeTo(null);
        setResizable(true);
        
        // Hlavní panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Horní panel (nadpis)
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(44, 62, 80));
        JLabel titleLabel = new JLabel("KALKULÁTOR VĚKU");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Střední panel (vstup a tlačítka)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(new Color(240, 240, 240));
        
        // Panel pro vstup
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
        
        JLabel instructionLabel = new JLabel("Zadejte datum narození (formát: dd.mm.yyyy)");
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        instructionLabel.setForeground(new Color(127, 140, 141));
        inputPanel.add(instructionLabel, BorderLayout.NORTH);
        
        dateField = new JTextField();
        dateField.setFont(new Font("Arial", Font.PLAIN, 16));
        dateField.setHorizontalAlignment(JTextField.CENTER);
        dateField.addActionListener(e -> calculateAge());
        inputPanel.add(dateField, BorderLayout.CENTER);
        
        centerPanel.add(inputPanel, BorderLayout.NORTH);
        
        // Panel pro tlačítka
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(new Color(240, 240, 240));
        
        JButton calculateButton = new JButton("VYPOČÍTEJ");
        calculateButton.setFont(new Font("Arial", Font.BOLD, 12));
        calculateButton.setBackground(new Color(39, 174, 96));
        calculateButton.setForeground(Color.WHITE);
        calculateButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        calculateButton.addActionListener(e -> calculateAge());
        buttonPanel.add(calculateButton);
        
        JButton clearButton = new JButton("VYMAZAT");
        clearButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearButton.setBackground(new Color(231, 76, 60));
        clearButton.setForeground(Color.WHITE);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clear());
        buttonPanel.add(clearButton);
        
        centerPanel.add(buttonPanel, BorderLayout.CENTER);
        
        // Panel pro výsledky
        JPanel resultsPanel = new JPanel(new BorderLayout(10, 10));
        resultsPanel.setBackground(Color.WHITE);
        resultsPanel.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199)));
        
        JLabel resultsLabel = new JLabel("VÝSLEDKY:");
        resultsLabel.setFont(new Font("Arial", Font.BOLD, 12));
        resultsPanel.add(resultsLabel, BorderLayout.NORTH);
        
        resultsArea = new JTextArea();
        resultsArea.setFont(new Font("Courier", Font.PLAIN, 11));
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);
        resultsArea.setMargin(new Insets(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);
        
        centerPanel.add(resultsPanel, BorderLayout.SOUTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        add(mainPanel);
    }
    
    private LocalDate validateDate(String dateStr) {
        try {
            if (dateStr.length() != 10 || dateStr.charAt(2) != '.' || dateStr.charAt(5) != '.') {
                throw new IllegalArgumentException("Nesprávný formát!");
            }
            
            String[] parts = dateStr.split("\\.");
            int day = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int year = Integer.parseInt(parts[2]);
            
            LocalDate birthDate = LocalDate.of(year, month, day);
            LocalDate today = LocalDate.now();
            
            if (birthDate.isAfter(today)) {
                JOptionPane.showMessageDialog(this, 
                    "Datum narození nemůže být v budoucnosti!", 
                    "Chyba", 
                    JOptionPane.ERROR_MESSAGE);
                return null;
            }
            
            return birthDate;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Nesprávný formát data!\nVerezujte: dd.mm.yyyy\nPříklad: 15.03.2000", 
                "Chyba", 
                JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
    
    private void calculateAge() {
        String dateStr = dateField.getText().trim();
        
        if (dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Prosím, zadejte datum narození!", 
                "Upozornění", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        LocalDate birthDate = validateDate(dateStr);
        if (birthDate == null) return;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = LocalDate.now();
        
        long days = ChronoUnit.DAYS.between(birthDate, today);
        long seconds = ChronoUnit.SECONDS.between(birthDate.atStartOfDay(), now);
        
        int years = (int) (days / 365);
        long remainingDays = days % 365;
        long hours = days * 24;
        long minutes = days * 24 * 60;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        
        String results = String.format(
            "Datum narození: %s\n" +
            "Dnešní datum:   %s\n" +
            "\n" +
            "=========================================\n" +
            "VĚK: %d let a %d dní\n" +
            "=========================================\n" +
            "\n" +
            "DETAILNĚ:\n" +
            "  • Dní:       %,d\n" +
            "  • Hodin:     %,d\n" +
            "  • Minut:     %,d\n" +
            "  • Sekund:    %,d\n" +
            "\n" +
            "CELKEM:\n" +
            "  • %,d dní\n" +
            "  • %,d hodin\n" +
            "  • %,d minut\n" +
            "  • %,d sekund",
            birthDate.format(formatter),
            today.format(formatter),
            years, remainingDays,
            days, hours, minutes, seconds,
            days, hours, minutes, seconds
        );
        
        resultsArea.setText(results);
    }
    
    private void clear() {
        dateField.setText("");
        resultsArea.setText("");
        dateField.requestFocus();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BirthCalculator frame = new BirthCalculator();
            frame.setVisible(true);
        });
    }
}
