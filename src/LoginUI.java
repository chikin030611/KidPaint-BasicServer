import java.awt.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;

public class LoginUI extends JFrame {
    String serverIp;
    int serverPort;

    /**
     * The LoginUI class represents the graphical user interface for the login page.
     * It allows users to input their name and start the application.
     */
    public LoginUI() {
        this.setTitle("Login Page");
        this.setSize(200, 150);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        Container container = this.getContentPane();
        container.setLayout(new BorderLayout());

        JPanel usernamePanel = new JPanel();
        JPanel submitPanel = new JPanel();

        Label label = new Label("Please input your name:");
        JTextField usernameTextField = new JTextField(10);
        JButton submitButton = new JButton("Submit");

        submitPanel.add(label);
        submitPanel.add(usernameTextField);
        usernamePanel.add(submitButton);

        container.add(usernamePanel, BorderLayout.SOUTH);
        container.add(submitPanel);

        submitButton.addActionListener(e -> {
            String username = usernameTextField.getText();

            if (username.equals("")) username = "Anonymous";
            submitButton.setEnabled(false);
            try {
                req(username);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Sends a request with the given username to the server and establishes a connection.
     * 
     * @param username the username to be sent to the server
     * @throws UnknownHostException if the IP address of the server is unknown
     * @throws IOException if an I/O error occurs while sending or receiving data
     */
    void req(String username) throws UnknownHostException, IOException {
        DatagramSocket socket = new DatagramSocket();
        DatagramPacket packet = new DatagramPacket(username.getBytes(), username.length(),
                InetAddress.getByName("255.255.255.255"), 5555);
        socket.send(packet);
        DatagramPacket received = new DatagramPacket(new byte[1024], 1024);

        while (true) {
            System.out.println("Listening...");

            socket.receive(received);
            String content = new String(received.getData(), 0, received.getLength());

            if (content != null) {
                serverIp = received.getAddress().toString();
                serverIp = serverIp.substring(1);   // remove the first "/" of the address string
                serverPort = Integer.parseInt(content);
                System.out.println("Server IP: " + serverIp);
                System.out.println("Server Port: " + serverPort);

                socket.close();
                this.setVisible(false);

                UI ui = UI.getInstance(serverIp, serverPort, username);

                ui.setData(new int[50][50], 20);
                ui.setVisible(true);
                break;
            }
        }
    }
}
