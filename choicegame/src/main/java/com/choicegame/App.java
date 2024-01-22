package com.choicegame;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseStream;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.FileReader;

//import swing
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultCaret;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;

public class App {
    private static Scanner scanner = new Scanner(System.in);
    private static List<String> chatHistory = new ArrayList<>();
    private static boolean isLoggedIn = false;

    // MongoDB connection details
    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "JAVA";
    private static final String COLLECTION_NAME = "java";
    // user collection
    private static final String USER_COLLECTION_NAME = "user";

    // Swing components
    private static JTextArea textArea;
    private static JTextField textField;
    private static GenerativeModel model;

    public static void main(String[] args) throws IOException {
        String username = ""; // Declare the username variable
        String password = ""; // Declare the password variable
        // SwingUtilities.invokeLater(() -> createUI(new StringBuilder()));
        App app = new App();
        Login login = new Login(app);
        login.setVisible(true);

        try (VertexAI vertexAi = new VertexAI("searchapi-378717", "us-central1");
                MongoClient mongoClient = MongoClients.create(MONGO_URI)) {

            // Access the MongoDB database
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);

            // Access the MongoDB collection
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
            MongoCollection<Document> userCollection = database.getCollection(USER_COLLECTION_NAME);

            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setMaxOutputTokens(8192)
                    .setTemperature(0.9F)
                    .setTopP(1)
                    .build();
            model = new GenerativeModel("gemini-pro", generationConfig, vertexAi);

            // Main interaction loop
            while (true) {
                if (!isLoggedIn) {
                    // Display login/signup options
                    System.out.println("1. Login");
                    System.out.println("2. Signup");
                    System.out.print("Enter your choice: ");
                    int choice = Integer.parseInt(scanner.nextLine());

                    if (choice == 1) {
                        // Handle login
                        System.out.print("Enter username: ");
                        username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        password = scanner.nextLine();

                        if (isLoginSuccessful(userCollection, username, password)) {
                            // Login successful
                            isLoggedIn = true;
                        }
                    } else if (choice == 2) {
                        // Handle signup
                        System.out.print("Enter username: ");
                        username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        password = scanner.nextLine();

                        if (signup(userCollection, username, password)) {
                            System.out.println("Signup successful. Please login to continue.");
                        } else {
                            System.out.println("Username already exists. Please try another one.");
                        }
                    } else {
                        // Invalid choice
                        System.out.println("Invalid choice. Please enter 1 or 2.");
                    }
                } else if (isLoggedIn) {
                    // Logged in user interaction
                    System.out.println("You: ");
                    String userMessage = scanner.nextLine();

                    if (chatHistory.size() == 0) {
                        // Add to chat history from the file Prompt.txt
                        BufferedReader reader = new BufferedReader(new FileReader(
                                "C:\\Users\\banik\\OneDrive\\Documents\\CHRIST UNIVERSITY\\Second_Tri_Semester\\JAVA_Programming\\choiceGame\\choicegame\\src\\main\\java\\com\\choicegame\\Prompt.txt"));
                        String line = reader.readLine();
                        while (line != null) {
                            chatHistory.add(line);
                            line = reader.readLine();
                        }
                        reader.close();
                    }

                    if (userMessage.equalsIgnoreCase("exit")) {
                        break;
                    }

                    // Add the user's message to the chat history
                    chatHistory.add(userMessage);

                    // Generate a response using the entire chat history as context
                    List<Content> contents = new ArrayList<>();

                    // Alternating turns between user and model
                    for (int i = 0; i < chatHistory.size(); i++) {
                        String message = chatHistory.get(i);
                        String role = (i % 2 == 0) ? "user" : "model"; // Alternate roles
                        contents.add(
                                Content.newBuilder().setRole(role).addParts(Part.newBuilder().setText(message))
                                        .build());
                    }

                    ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(contents);

                    StringBuilder response = new StringBuilder();
                    // Concatenate all parts into a single StringBuilder
                    responseStream.forEach(generateContentResponse -> generateContentResponse.getCandidatesList()
                            .forEach(candidate -> candidate.getContent().getPartsList()
                                    .forEach(part -> response.append(part.getText()))));

                    // Display the bot's response
                    System.out.println("Bot: " + response);

                    // Add the bot's response to the chat history
                    chatHistory.add(response.toString());
                }
            }
        }

    }

    private static boolean isLoginSuccessful(MongoCollection<Document> userCollection, String username,
            String password) {
        Document user = userCollection.find(new Document("username", username)).first();
        if (user != null) {
            String storedPassword = user.getString("password");
            return storedPassword.equals(password);
        }
        return false;
    }

    private static boolean signup(MongoCollection<Document> userCollection, String username, String password) {
        if (userCollection.find(new Document("username", username)).first() == null) {
            Document newUser = new Document("username", username).append("password", password);
            userCollection.insertOne(newUser);
            return true;
        }
        return false;
    }

    // Create UI

    private static void createUI(StringBuilder response) {
        // Create frame
        JFrame frame = new JFrame("Choice Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 500);

        // Create panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Create label
        JLabel label = new JLabel("Choice Game");
        label.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        panel.add(label);

        // Create text area
        textArea = new JTextArea(10, 40);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // Auto-scrolling for the text area
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setAlignmentX(JScrollPane.CENTER_ALIGNMENT);
        panel.add(scrollPane);

        // Create text field
        textField = new JTextField(40);
        textField.setAlignmentX(JTextField.CENTER_ALIGNMENT);
        panel.add(textField);

        // Create button
        JButton button = new JButton("Send");
        button.setAlignmentX(JButton.CENTER_ALIGNMENT);
        panel.add(button);

        // Create action listener
        button.addActionListener(e -> {
            try {
                handleSendMessage(response);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });

        // Add panel to frame
        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private static void updateTextArea(String message) {
        textArea.append(message + "\n");
    }

    private static void handleSendMessage(StringBuilder response) throws IOException {
        String userMessage = textField.getText();
        if (userMessage.equalsIgnoreCase("exit")) {
            System.exit(0);
        }
        chatHistory.add(userMessage);
        updateTextArea("You: " + userMessage);

        // Generate a response using the entire chat history as context
        List<Content> contents = new ArrayList<>();

        // Alternating turns between user and model
        for (int i = 0; i < chatHistory.size(); i++) {
            String message = chatHistory.get(i);
            String role = (i % 2 == 0) ? "user" : "model"; // Alternate roles
            contents.add(
                    Content.newBuilder().setRole(role).addParts(Part.newBuilder().setText(message))
                            .build());
        }

        ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(contents);

        StringBuilder botResponse = new StringBuilder();
        // Concatenate all parts into a single StringBuilder
        responseStream.forEach(generateContentResponse -> generateContentResponse.getCandidatesList()
                .forEach(candidate -> candidate.getContent().getPartsList()
                        .forEach(part -> botResponse.append(part.getText()))));

        // Display the bot's response in the UI
        updateTextArea("Vertex: " + botResponse + "\n");

        // Add the bot's response to the chat history
        chatHistory.add(botResponse.toString());

        // Clear text field
        textField.setText("");

        updateUI(response);
    }

    private static void updateUI(StringBuilder response) {
        SwingUtilities.invokeLater(() -> {
            // updateTextArea("Bot: " + response);
        });
    }

    public void launchAppGUI() {
        // Create and show the App GUI
        createUI(new StringBuilder());
    }
}