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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppUI extends JFrame {
    private JTextArea chatHistoryTextArea;
    private JTextField userMessageTextField;
    private JButton sendButton;

    private List<String> chatHistory = new ArrayList<>();
    private boolean isLoggedIn = false;

    private final VertexAI vertexAi;
    private final GenerativeModel model;
    private final MongoClient mongoClient;
    private final MongoCollection<Document> userCollection;

    public AppUI() throws IOException {
        super("Chat Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(640, 480));
        setResizable(false);

        // Initialize VertexAI, MongoDB, and other components
        vertexAi = new VertexAI("searchapi-378717", "us-central1");
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("JAVA");
        userCollection = database.getCollection("user");

        GenerationConfig generationConfig = GenerationConfig.newBuilder()
                .setMaxOutputTokens(8192)
                .setTemperature(0.1F)
                .setTopP(1)
                .build();
        model = new GenerativeModel("gemini-pro", generationConfig, vertexAi);

        // GUI components
        chatHistoryTextArea = new JTextArea();
        chatHistoryTextArea.setEditable(false);
        add(new JScrollPane(chatHistoryTextArea), BorderLayout.CENTER);

        userMessageTextField = new JTextField();
        add(userMessageTextField, BorderLayout.SOUTH);

        sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    handleUserMessage();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        });
        add(sendButton, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void handleUserMessage() throws IOException {
        String userMessage = userMessageTextField.getText();
        chatHistoryTextArea.append("You: " + userMessage + "\n");
        userMessageTextField.setText("");

        if (!isLoggedIn) {
            handleLoginSignup(userMessage);
        } else {
            handleChatInteraction(userMessage);
        }
    }

    private void handleLoginSignup(String userMessage) {
        if (userMessage.equals("1")) {
            System.out.print("Enter username: ");
            String username = JOptionPane.showInputDialog("Enter username:");
            String password = JOptionPane.showInputDialog("Enter password:");

            if (isLoginSuccessful(userCollection, username, password)) {
                // Login successful
                isLoggedIn = true;
            }
        } else if (userMessage.equals("2")) {
            System.out.print("Enter username: ");
            String username = JOptionPane.showInputDialog("Enter username:");
            String password = JOptionPane.showInputDialog("Enter password:");

            if (signup(userCollection, username, password)) {
                JOptionPane.showMessageDialog(null, "Signup successful. Please login to continue.");
            } else {
                JOptionPane.showMessageDialog(null, "Username already exists. Please try another one.");
            }
        } else {
            JOptionPane.showMessageDialog(null, "Invalid choice. Please enter 1 or 2.");
        }
    }

    private void handleChatInteraction(String userMessage) throws IOException {
        if (userMessage.equalsIgnoreCase("exit")) {
            // Implement exit logic
            System.exit(0);
        } else {
            // Add user's message to chat history
            chatHistory.add(userMessage);

            // Generate response using entire chat history as context
            List<Content> contents = new ArrayList<>();
            for (int i = 0; i < chatHistory.size(); i++) {
                String message = chatHistory.get(i);
                String role = (i % 2 == 0) ? "user" : "model";
                contents.add(
                        Content.newBuilder().setRole(role).addParts(Part.newBuilder().setText(message))
                                .build());
            }

            ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(contents);

            StringBuilder response = new StringBuilder();
            responseStream.forEach(generateContentResponse -> generateContentResponse.getCandidatesList()
                    .forEach(candidate -> candidate.getContent().getPartsList()
                            .forEach(part -> response.append(part.getText()))));

            // Display the bot's response
            chatHistoryTextArea.append("Bot: " + response + "\n");

            // Add bot's response to chat history
            chatHistory.add(response.toString());
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    new AppUI();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
