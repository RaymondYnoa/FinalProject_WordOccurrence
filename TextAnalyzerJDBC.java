/* *
 * CEN 3024C
 * @version Final Project | Word Occurrences
 * @author Raymond Ynoa
 * 
 * The combined project.
 * */

// Imported Java packages.
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * This class represents a text analyzer application with a graphical user interface (GUI).
 * It reads a text file from a URL and displays word frequencies sorted by the most frequently used word.
 */

public class TextAnalyzerJDBC extends JFrame {
    JTextArea outputTextArea;
    private JButton analyzeButton;
    private JButton skipToTopButton;

    // Database connection properties
    private static final String DB_URL = "jdbc:mysql://localhost:3306/word_occurrences";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "mypass";

    /**
     * Constructs a new TextAnalyzerJunit instance.
     * Sets up the GUI elements and event listeners for the Analyze Text and Skip to Top buttons.
     */
    
    public TextAnalyzerJDBC() {
        setTitle("Text Analyzer"); // Frame title.
        setSize(400, 600); // Frame size.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close frame on exit.

        outputTextArea = new JTextArea(); // The output text area.

        // Scrolling for output text area.
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        add(scrollPane);

        analyzeButton = new JButton("Analyze Text"); // Analyze Text button.
        analyzeButton.addActionListener(new ActionListener() { // Listen for Analyze Text button.
            @Override
            public void actionPerformed(ActionEvent e) {
            	/**
                 * Analyzes the text from the specified URL and displays word frequencies in the outputTextArea.
                 *
                 * @param url The URL of the text file to analyze.
                 */
                String url = "https://www.gutenberg.org/files/1065/1065-h/1065-h.htm"; // The URL of the text file.
                analyzeAndDisplayText(url); // Analyze and display text from URL.
            }
        });

        skipToTopButton = new JButton("Skip to Top"); // Skip to top button.
        skipToTopButton.addActionListener(new ActionListener() { // Listen for Analyze Text button.
            @Override
            public void actionPerformed(ActionEvent e) {
                outputTextArea.setCaretPosition(0); // Set output area to the top.
            }
        });

        // Organize button components.
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(analyzeButton);
        buttonPanel.add(skipToTopButton);
        add(buttonPanel, "South");

        // Create the schema and table if they don't exist
        createSchemaAndTable();
    }

    private void createSchemaAndTable() {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            // Create the "word_occurrences" schema if it doesn't exist
            String createSchemaQuery = "CREATE SCHEMA IF NOT EXISTS word_occurrences";
            try (PreparedStatement preparedStatement = connection.prepareStatement(createSchemaQuery)) {
                preparedStatement.executeUpdate();
            }

            // Switch to the created schema
            String useSchemaQuery = "USE word_occurrences";
            try (PreparedStatement preparedStatement = connection.prepareStatement(useSchemaQuery)) {
                preparedStatement.executeUpdate();
            }

            // Create the "word" table if it doesn't exist
            String createTableQuery = "CREATE TABLE IF NOT EXISTS word (" +
                    "word_text VARCHAR(255) NOT NULL," +
                    "frequency INT NOT NULL," +
                    "PRIMARY KEY (word_text)" +
                    ")";
            try (PreparedStatement preparedStatement = connection.prepareStatement(createTableQuery)) {
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            showError("Error creating schema and table: " + e.getMessage());
        }
    }
    void analyzeAndDisplayText(String url) {
        List<String> words = new ArrayList<>();

        URL textUrl;
        try {
            textUrl = new URL(url);
        } catch (IOException e) { // Throw an exception.
            showError("Error opening URL: " + url);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(textUrl.openStream()))) { // Extract text from URL.
            boolean isInPoem = false; // Test if poem text is counted.
            String line;
            while ((line = reader.readLine()) != null) {
                if (!isInPoem && line.matches(".*<h1>.*")) { // HTML tag at the title.
                    isInPoem = true;
                    continue;
                }

                if (isInPoem && line.matches(".*<!--end chapter-->.*")) { // End using HTML tag at end of chapter.
                    break;
                }

                // Within poem's text.
                if (isInPoem) {
                    line = line.replaceAll("<.*?>", ""); // Remove HTML tags
                    line = line.replaceAll("[^A-Za-z ]", ""); // Remove non-word characters
                    String[] wordsArray = line.split("\\s+"); // Split line into words using whitespace as delimiter
                    for (String word : wordsArray) { // For each word.
                        if (!word.isEmpty()) { // If word is not empty.
                            words.add(word);
                        }
                    }
                }
            }
        } catch (IOException e) { // Throw an exception.
            showError("Error reading text from URL: " + url);
            return;
        }

        updateWordOccurrencesInDatabase(words);
        displayWordFrequencies();
    }

    private void updateWordOccurrencesInDatabase(List<String> words) {
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String insertQuery = "INSERT INTO word (word_text, frequency) VALUES (?, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1";
            try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                for (String word : words) {
                    insertStatement.setString(1, word);
                    insertStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            showError("Error updating word occurrences in the database: " + e.getMessage());
        }
    }
    
    private void displayWordFrequencies() {
        List<String> wordList = new ArrayList<>();
        List<Integer> frequencyList = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD)) {
            String selectQuery = "SELECT word_text, frequency FROM word ORDER BY frequency DESC";
            try (PreparedStatement preparedStatement = connection.prepareStatement(selectQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String word = resultSet.getString("word_text");
                        int frequency = resultSet.getInt("frequency");
                        wordList.add(word);
                        frequencyList.add(frequency);
                    }
                }
            }
        } catch (SQLException e) {
            showError("Error reading word occurrences from the database: " + e.getMessage());
            return;
        }
        
     // Build the result string with numbered list.
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < wordList.size(); i++) {
            result.append(i + 1).append(". ").append(wordList.get(i)).append(": ").append(frequencyList.get(i)).append("\n");
        }
        // Display the word frequencies in the JTextArea.
        outputTextArea.setText(result.toString());
    }

    /**
     * Displays an error message in a JOptionPane.
     *
     * @param message The error message to display.
     */
    void showError(String message) { // JFrame error message.
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TextAnalyzerJDBC frame = new TextAnalyzerJDBC();
            frame.setVisible(true);
        });
    }
}
