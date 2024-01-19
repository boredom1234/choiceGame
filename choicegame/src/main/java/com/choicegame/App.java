package com.choicegame;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseStream;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

    public static void main(String[] args) throws IOException {
        try (VertexAI vertexAi = new VertexAI("searchapi-378717", "us-central1");
             MongoClient mongoClient = MongoClients.create(MONGO_URI)) {

            // Access the MongoDB database
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);

            // Access the MongoDB collection
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
            MongoCollection<Document> userCollection = database.getCollection(USER_COLLECTION_NAME);

            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setMaxOutputTokens(8192)
                    .setTemperature(1F)
                    .setTopP(1)
                    .build();
            GenerativeModel model = new GenerativeModel("gemini-pro", generationConfig, vertexAi);

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
                        String username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();

                        if (isLoginSuccessful(userCollection, username, password)) {
                            // Login successful
                            isLoggedIn = true;
                        } else {
                            // Login failed
                            System.out.println("Invalid credentials. Please try again.");
                        }
                    } else if (choice == 2) {
                        // Handle signup
                        System.out.print("Enter username: ");
                        String username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        String password = scanner.nextLine();

                        if (signup(userCollection, username, password)) {
                            System.out.println("Signup successful. Please login to continue.");
                        } else {
                            System.out.println("Username already exists. Please try another one.");
                        }
                    }
                } else {
                    // Logged in user interaction
                    System.out.println("You: ");
                    String userMessage = scanner.nextLine();
                    //Give the first prompt predefined
                    if (chatHistory.size() == 0) {
                        chatHistory.add("Lets play a choice based game. You give me some genres to select from and then I will select one and then you will give a small sorry outline and then give 3 choices. I will select from one of those choices and then you will continue the story after that just like the game Dungeons and Dragons.");
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

                    // Save the chat session to MongoDB
                    saveChatSession(collection, chatHistory);
                }
            }
        }
    }

    private static boolean isLoginSuccessful(MongoCollection<Document> userCollection, String username, String password) {
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

    private static void saveChatSession(MongoCollection<Document> collection, List<String> chatHistory) {
        // Create a document to represent the chat session
        Document chatSession = new Document();
        chatSession.append("username", "your_username"); // Replace with the actual username
        chatSession.append("password", "your_password"); // Replace with the actual password
        chatSession.append("chats", chatHistory);

        // Insert the document into the MongoDB collection
        collection.insertOne(chatSession);
    }
}
