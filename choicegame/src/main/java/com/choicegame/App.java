package com.choicegame;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseStream;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
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
        String username = ""; // Declare the username variable
        String password = ""; // Declare the password variable

        try (VertexAI vertexAi = new VertexAI("searchapi-378717", "us-central1");
                MongoClient mongoClient = MongoClients.create(MONGO_URI)) {

            // Access the MongoDB database
            MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);

            // Access the MongoDB collection
            MongoCollection<Document> collection = database.getCollection(COLLECTION_NAME);
            MongoCollection<Document> userCollection = database.getCollection(USER_COLLECTION_NAME);

            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setMaxOutputTokens(8192)
                    .setTemperature(0.3F)
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
                        username = scanner.nextLine();
                        System.out.print("Enter password: ");
                        password = scanner.nextLine();

                        if (isLoginSuccessful(userCollection, username, password)) {
                            // Login successful
                            isLoggedIn = true;
                        }
                        // // if login is successful give option to user to create new caht session or load
                        // // old chat sessions
                        // System.out.println("1. Create new chat session");
                        // System.out.println("2. Load chat session");
                        // System.out.print("Enter your choice: ");
                        // choice = Integer.parseInt(scanner.nextLine());
                        // if (choice == 1) {
                        //     // Create new chat session
                        //     chatHistory.clear();
                        // } else if (choice == 2) {
                        //     // Load chat session
                        //     loadChatSessionById(collection, username, password);
                        // } else {
                        //     // Invalid choice
                        //     System.out.println("Invalid choice. Please enter 1 or 2.");
                        // }
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
                    // Give the first prompt predefined
                    if (chatHistory.size() == 0) {
                        chatHistory.add(
                                "let's play a text-based interactive storytelling game! You can choose from the following genres: Fantasy, Mystery, Thriller, Romance, Historical Fiction, Literary Fiction, Non-Fiction, Horror, Contemporary Fiction. Once you pick a genre, I'll provide a brief outline, and then you can make choices that will shape the story. I'll present three options at key points, and you can decide the direction of the narrative. Ready to begin? If so, please choose a genre from the list.");
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
                    // saveChatSession(collection, chatHistory, username, password);

                    // Save the chat session to MongoDB with a unique ID
                    // String chatId = saveChatSession(collection, chatHistory, username, password);

                    // Display the unique ID to the user
                    // System.out.println("Chat saved with ID: " + chatId);
                }
            }
        }

    }

    // private static void displayChatSessions(MongoCollection<Document> collection, String username, String password) {
    //     // Create a document to represent the search criteria
    //     Document searchCriteria = new Document();
    //     searchCriteria.append("username", username);
    //     searchCriteria.append("password", password);

    //     // Find all chat sessions for the given username and password
    //     FindIterable<Document> chatSessions = collection.find(searchCriteria);

    //     // Check if chat sessions were found
    //     if (chatSessions == null || !chatSessions.iterator().hasNext()) {
    //         System.out.println("No chat sessions found for the given username and password.");
    //         return;
    //     }

    //     // Display available chat sessions with their unique IDs
    //     System.out.println("Available Chat Sessions:");
    //     int index = 1;
    //     for (Document chatSession : chatSessions) {
    //         String chatId = chatSession.getObjectId("_id").toString();
    //         System.out.println(index + ". ID: " + chatId);
    //         index++;
    //     }
    // }

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

    // private static String saveChatSession(MongoCollection<Document> collection, List<String> chatHistory,
    //         String username, String password) {
    //     // Create a document to represent the chat session
    //     Document chatSession = new Document();
    //     chatSession.append("username", username); // Use the actual username
    //     chatSession.append("password", password); // Use the actual password
    //     chatSession.append("chats", chatHistory);

    //     // Insert the document into the MongoDB collection
    //     collection.insertOne(chatSession);

    //     // Return the unique ID generated by MongoDB
    //     return chatSession.getObjectId("_id").toString();
    // }

    // private static void loadChatSessions(MongoCollection<Document> collection,
    // String username, String password) {
    // // Create a document to represent the search criteria
    // Document searchCriteria = new Document();
    // searchCriteria.append("username", username);
    // searchCriteria.append("password", password);

    // // Find all chat sessions for the given username and password
    // FindIterable<Document> chatSessions = collection.find(searchCriteria);

    // // Check if chat sessions were found
    // if (chatSessions == null || !chatSessions.iterator().hasNext()) {
    // System.out.println("No chat sessions found for the given username and
    // password.");
    // return;
    // }

    // // Display available chat sessions with their unique IDs
    // System.out.println("Available Chat Sessions:");
    // for (Document chatSession : chatSessions) {
    // String chatId = chatSession.getObjectId("_id").toString();
    // System.out.println("ID: " + chatId);
    // }

    // // Prompt the user to select a chat session by its ID
    // System.out.print("Enter the ID of the chat session you want to load: ");
    // String selectedChatId = scanner.nextLine();

    // // Load the selected chat session by its ID
    // loadChatSessionById(collection, selectedChatId);
    // }

    // private static void loadChatSessionById(MongoCollection<Document> collection, String username, String password) {
    //     // Display available chat sessions with IDs
    //     displayChatSessions(collection, username, password);

    //     // Prompt the user to select a chat session by its ID
    //     String selectedChatId;
    //     while (true) {
    //         System.out.print("Enter the ID of the chat session you want to load: ");
    //         selectedChatId = scanner.nextLine(); // Read user input as a string

    //         try {
    //             // Attempt to validate if chatId is a valid ObjectId
    //             new org.bson.types.ObjectId(selectedChatId);
    //             break; // Break out of the loop if validation succeeds
    //         } catch (IllegalArgumentException e) {
    //             System.out.println("Invalid ObjectId format. Please enter a valid ObjectId.");
    //         }
    //     }

    //     // Create a document to represent the search criteria using the unique ID
    //     Document searchCriteria = new Document("_id", new org.bson.types.ObjectId(selectedChatId));

    //     // Find the chat session for the given unique ID
    //     Document chatSession = collection.find(searchCriteria).first();

    //     // Check if a chat session was found
    //     if (chatSession == null) {
    //         System.out.println("No chat sessions found for the given ID.");
    //         return;
    //     }

    //     // Get the chat history from the chat session
    //     List<String> loadedChatHistory = chatSession.getList("chats", String.class);

    //     // Display the loaded chat history
    //     System.out.println("Loaded Chat History:");
    //     for (String message : loadedChatHistory) {
    //         System.out.println(message);
    //     }
    // }

    // Clear all chat
    // private static void clearChatSession(MongoCollection<Document> collection, String username, String password) {
    //     // Create a document to represent the search criteria
    //     Document searchCriteria = new Document();
    //     searchCriteria.append("username", username);
    //     searchCriteria.append("password", password);

    //     // Find the chat session for the given username and password
    //     Document chatSession = collection.find(searchCriteria).first();

    //     // Check if a chat session was found
    //     if (chatSession == null) {
    //         System.out.println("No chat sessions found for the given username and password.");
    //         return;
    //     }

    //     // Get the chat history from the chat session
    //     List<String> chatHistory = (List<String>) chatSession.get("chats");

    //     // Display the chat history
    //     for (String message : chatHistory) {
    //         System.out.println(message);
    //     }
    // }
}