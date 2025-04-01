import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server {
    private static List<Account> accounts = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Server <port>");
            return;
        }

        try {
            int port = Integer.parseInt(args[0]);
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected");
                new ClientHandler(socket).start();
            }
        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        } catch (NumberFormatException ex) {
            System.out.println("Invalid port number. Please provide a valid integer.");
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (InputStream input = socket.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                 OutputStream output = socket.getOutputStream();
                 PrintWriter writer = new PrintWriter(output, true)) {

                String text;
                while ((text = reader.readLine()) != null) {
                    System.out.println("Received: " + text);

                    String[] parts = text.split(" ");
                    int functionId = Integer.parseInt(parts[0]);

                    switch (functionId) {
                        case 1: // Δημιουργία λογαριασμού
                            String username = parts[1];
                            try {
                                String authToken = createAccount(username);
                                writer.println("Account created successfully. Your authToken is: " + authToken);
                            } catch (IllegalArgumentException e){
                                writer.println(e.getMessage());
                            }
                            break;


                        case 2: // Εμφάνιση λογαριασμών
                            int token = Integer.parseInt(parts[1]);
                            Account account = getAccountByToken(token);
                            if (account != null){
                                for (Account ac : accounts) {
                                    writer.println(ac.getUsername());
                                }
                                writer.println("END-OF-LIST");
                            } else {
                                writer.println("Invalid authToken");
                            }

                            break;

                        case 3: // Αποστολή μηνύματος
                            int senderToken = Integer.parseInt(parts[1]);
                            String recipient = parts[2];
                            String messageBody = text.substring(text.indexOf(parts[3]));
                            int sendResult = sendMessage(senderToken, recipient, messageBody);

                            if (sendResult == 0) {
                                writer.println("OK");
                            } else if (sendResult == 1){
                                writer.println("Invalid authToken");
                            } else if (sendResult == 2){
                                writer.println("User does not exist");
                            }
                            break;

                        case 4: // Προβολή εισερχόμενων μηνυμάτων
                            int inboxToken = Integer.parseInt(parts[1]);
                            Account acc = getAccountByToken(inboxToken);
                            if (acc != null) {
                                for (Message message : acc.getMessageBox()) {
                                    writer.println(message.toString());
                                }
                                writer.println("END-OF-LIST");
                            } else {
                                writer.println("Invalid authToken");
                            }
                            break;

                        case 5: // Ανάγνωση μηνύματος
                            int readToken = Integer.parseInt(parts[1]);
                            int messageId = Integer.parseInt(parts[2]);
                            int readResult = readMessage(readToken, messageId,writer);

                            if (readResult == 0) {
                                writer.println("Message marked as read");
                            } else if (readResult == 1){
                                writer.println("Invalid authToken");
                            } else if (readResult == 2){
                                writer.println("Message ID does not exist");
                            }
                            break;

                        case 6: // Διαγραφή μηνύματος
                            int deleteToken = Integer.parseInt(parts[1]);
                            int deleteMessageId = Integer.parseInt(parts[2]);
                            int deleteResult = deleteMessage(deleteToken, deleteMessageId);

                            if (deleteResult == 0) {
                                writer.println("Message deleted successfully");
                            } else if (deleteResult == 1) {
                                writer.println("Invalid authToken");
                            } else if (deleteResult == 2) {
                                writer.println("Message does not exist");
                            }
                            break;

                    }
                }
            } catch (IOException ex) {
                System.out.println("Client handler exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private static String createAccount(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                throw new IllegalArgumentException("Username already exists");
            }
        }
        if(!username.matches("^[a-zA-Z0-9_]+$")){
            throw new IllegalArgumentException("Username is invalid");
        }
        Account newAccount = new Account(username);
        accounts.add(newAccount);
        return String.valueOf(newAccount.getAuthToken());
    }


    private static int sendMessage(int senderToken, String recipient, String messageBody) {
        Account sender = getAccountByToken(senderToken);
        Account receiver = getAccountByUsername(recipient);
        if (sender == null) {
            return 1;
        }
        if (receiver == null){
            return 2;
        }
        Message message = new Message(sender.getUsername(), recipient, messageBody);
        receiver.addMessage(message);
        return 0;
    }

    private static Account getAccountByToken(int authToken) {
        for (Account account : accounts) {
            if (account.getAuthToken() == authToken) {
                return account;
            }
        }
        return null;
    }

    private static Account getAccountByUsername(String username) {
        for (Account account : accounts) {
            if (account.getUsername().equals(username)) {
                return account;
            }
        }
        return null;
    }

    private static int readMessage(int authToken, int messageId, PrintWriter writer) {
        Account account = getAccountByToken(authToken);
        if (account == null) {
            return 1;
        }
        for (Message message : account.getMessageBox()) {
            if (message.getId() == messageId) {
                writer.println(message.getBody());
                message.markAsRead();
                return 0;
            }
        }
        return 2;
    }

    private static int deleteMessage(int authToken, int messageId) {
        Account account = getAccountByToken(authToken);
        if (account == null) {
            return 1;
        }
        Message message = getMessageById(account, messageId);
        if (message == null) {
            return 2;
        }
        account.deleteMessage(message);
        return 0;
    }

    private static Message getMessageById(Account account, int messageId) {
        for (Message message : account.getMessageBox()) {
            if (message.getId() == messageId) {
                return message;
            }
        }
        return null;
    }
}

// Κλάση Account
class Account {
    private String username;
    private int authToken;
    private List<Message> messageBox = new ArrayList<>();

    public Account(String username) {
        this.username = username;
        Random r = new Random();
        this.authToken = r.nextInt(8999) + 1000;
    }

    public String getUsername() {
        return username;
    }

    public int getAuthToken() {
        return authToken;
    }

    public List<Message> getMessageBox() {
        return messageBox;
    }

    public void addMessage(Message message) {
        messageBox.add(message);
    }

    public void deleteMessage(Message message) {
        messageBox.remove(message);
    }
}

// Κλάση Message
class Message {
    private int id;
    private String sender;
    private String recipient;
    private String body;
    private boolean isRead;

    public Message(String sender, String recipient, String body) {
        Random r = new Random();
        this.id = r.nextInt(89)+10;
        this.recipient = recipient;
        this.sender = sender;
        this.body = body;
        this.isRead = false;
    }
    public int getId() {
        return id;
    }
    public String getBody() {
        return body;
    }
    public void markAsRead() {
        isRead = true;
    }

    @Override
    public String toString() {
        return id + ". from: " + sender + (isRead ? "" : "*");
    }
}
