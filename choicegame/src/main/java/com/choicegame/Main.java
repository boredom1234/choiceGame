package com.choicegame;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.preview.GenerativeModel;
import com.google.cloud.vertexai.generativeai.preview.ResponseStream;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
  public static void main(String[] args) throws IOException {
    try (VertexAI vertexAi = new VertexAI("searchapi-378717", "asia-southeast1");) {
      GenerationConfig generationConfig = GenerationConfig.newBuilder()
          .setMaxOutputTokens(8192)
          .setTemperature(0.7F)
          .setTopP(1)
          .build();
      GenerativeModel model = new GenerativeModel("gemini-pro", generationConfig, vertexAi);

      List<Content> chatHistory = new ArrayList<>();

      Scanner scanner = new Scanner(System.in);

      // Keep track of the full chat history
      List<String> fullChatHistory = new ArrayList<>();

      while (true) {
        // Get user input
        System.out.print("You: ");
        String userMessage = scanner.nextLine();

        // Add user message to chat history
        chatHistory.add(Content.newBuilder().setRole("user").addParts(Part.newBuilder().setText(userMessage)).build());

        // Generate model response
        ResponseStream<GenerateContentResponse> responseStream = model.generateContentStream(chatHistory);
        List<GenerateContentResponse> responses = new ArrayList<>();
        responseStream.iterator().forEachRemaining(responses::add);

        // Display model response
        String modelResponse = responses.get(responses.size() - 0).getCandidates(0).getContent().getParts(0).getText();
        System.out.println("Model: " + modelResponse);

        // Add model response to full chat history
        fullChatHistory.add("Model: " + modelResponse);

        // Add model response to chat history if not empty
        if (!responses.isEmpty()) {
          chatHistory
              .add(Content.newBuilder().setRole("model").addParts(Part.newBuilder().setText(modelResponse)).build());
        }

        // Display the full chat history
        // System.out.println("Full Chat History:");
        // for (String message : fullChatHistory) {
        //   System.out.println(message);
        // }
      }
    }
  }
}