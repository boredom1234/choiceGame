package com.choicegame;

import com.choicegame.App;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class Login extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;
    private JTextField tfUsername;
    private JPasswordField pfPassword;
    private JButton btnLogin;
    private JButton btnSignUp;

    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "JAVA"; // Replace with your database name
    private static final String USER_COLLECTION_NAME = "user";

    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> userCollection;
    private static App app;

    public Login(App app) {
        this.app = app;
        setTitle("Login");
        setSize(300, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel();
        panel.setLayout(null);
        panel.setBackground(Color.DARK_GRAY);
        setContentPane(panel);

        JLabel lblUsername = new JLabel("User Name");
        lblUsername.setBounds(20, 20, 80, 25);
        lblUsername.setFont(new Font("Arial", Font.BOLD, 14));
        lblUsername.setForeground(Color.WHITE);
        panel.add(lblUsername);

        tfUsername = new JTextField();
        tfUsername.setBounds(100, 20, 165, 25);
        tfUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(tfUsername);

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setBounds(20, 60, 80, 25);
        lblPassword.setFont(new Font("Arial", Font.BOLD, 14));
        lblPassword.setForeground(Color.WHITE);
        panel.add(lblPassword);

        pfPassword = new JPasswordField();
        pfPassword.setBounds(100, 60, 165, 25);
        pfPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        panel.add(pfPassword);

        btnLogin = new JButton("Login");
        btnLogin.setBounds(100, 100, 90, 25);
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogin.setBackground(Color.CYAN);
        btnLogin.setForeground(Color.BLACK);
        btnLogin.addActionListener(this);
        panel.add(btnLogin);

        btnSignUp = new JButton("Sign Up");
        btnSignUp.setBounds(100, 140, 90, 25);
        btnSignUp.setFont(new Font("Arial", Font.BOLD, 14));
        btnSignUp.setBackground(Color.CYAN);
        btnSignUp.setForeground(Color.BLACK);
        btnSignUp.addActionListener(this);
        panel.add(btnSignUp);

        mongoClient = MongoClients.create(MONGO_URI);
        database = mongoClient.getDatabase(DATABASE_NAME);
        userCollection = database.getCollection(USER_COLLECTION_NAME);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnLogin) {
            String username = tfUsername.getText();
            String password = new String(pfPassword.getPassword());

            Document user = userCollection.find(new Document("username", username).append("password", password)).first();

            if (user != null) {
                JOptionPane.showMessageDialog(null, "Login successful!");
                app.launchAppGUI();
                dispose();
            } else {
                JOptionPane.showMessageDialog(null, "Invalid username or password!");
            }
        } else if (e.getSource() == btnSignUp) {
            String newUsername = tfUsername.getText(); // Using the same text field
            String newPassword = new String(pfPassword.getPassword()); // Using the same password field

            long existingUserCount = userCollection.countDocuments(new Document("username", newUsername));

            if (existingUserCount > 0) {
                JOptionPane.showMessageDialog(null, "Username already exists. Please choose a different one.");
            } else {
                Document newUser = new Document("username", newUsername).append("password", newPassword);
                userCollection.insertOne(newUser);

                JOptionPane.showMessageDialog(null, "Sign up successful! You can now log in.");

                tfUsername.setText(""); // Clearing the text field
                pfPassword.setText(""); // Clearing the password field
            }
        }
    }

    public static void main(String[] args) {
        new Login(app).setVisible(true);
    }
}
