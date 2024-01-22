package com.choicegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.*;

public class UI extends JFrame implements ActionListener {

    private JPanel chatPanel, messagePanel;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JScrollPane scrollPane;
    private ArrayList<String> messages;

    public UI() {
        super("WhatsApp");
        messages = new ArrayList<>();

        // Create the chat panel
        chatPanel = new JPanel();
        chatPanel.setLayout(new BorderLayout());
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Create the chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        scrollPane = new JScrollPane(chatArea);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        // Create the message panel
        messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());
        
        // Create the message field
        messageField = new JTextField();
        messageField.addActionListener(this);
        messagePanel.add(messageField, BorderLayout.CENTER);
        
        // Create the send button
        sendButton = new JButton("Send");
        sendButton.addActionListener(this);
        messagePanel.add(sendButton, BorderLayout.EAST);

        // Add the chat and message panels to the frame
        add(chatPanel, BorderLayout.CENTER);
        add(messagePanel, BorderLayout.SOUTH);

        // Set the frame properties
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == sendButton || e.getSource() == messageField) {
            String message = messageField.getText();
            if (!message.isEmpty()) {
                messages.add(message);
                chatArea.setText("");
                for (String m : messages) {
                    chatArea.append(m + "\n");
                }
                messageField.setText("");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UI();
            }
        });
    }
}