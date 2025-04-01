import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        // Έλεγχος ότι τα απαραίτητα arguments έχουν περαστεί
        if (args.length < 3) {
            System.out.println("Usage: java Client <server_ip> <port> <function_id> [arguments]");
            return;
        }

        String serverIP = args[0];
        int port = Integer.parseInt(args[1]);
        int functionID = Integer.parseInt(args[2]);

        try (Socket socket = new Socket(serverIP, port)) {
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            switch (functionID) {
                case 1: // Δημιουργία λογαριασμού
                    if (args.length < 4) {
                        System.out.println("Missing username for Create Account");
                        return;
                    }
                    String username = args[3];
                    writer.println(functionID + " " + username);
                    System.out.println(reader.readLine());
                    break;

                case 2: // Εμφάνιση λογαριασμών
                    if (args.length < 4) {
                        System.out.println("Missing authToken for Show Accounts");
                        return;
                    }
                    int authToken = Integer.parseInt(args[3]);
                    writer.println(functionID + " " + authToken);
                    String account;
                    while ((account = reader.readLine()) != null) {
                        if (!account.equals("END-OF-LIST") && !account.equals("Invalid authToken")) {
                            System.out.println(account);
                        } else {
                            if (account.equals("Invalid authToken")) {
                                System.out.println("Invalid authToken");
                            }
                            break;
                        }
                    }
                    break;

                case 3: // Αποστολή μηνύματος
                    if (args.length < 6) {
                        System.out.println("Usage: java Client <server_ip> <port> 3 <authToken> <recipient> <message>");
                        return;
                    }
                    int senderToken = Integer.parseInt(args[3]);
                    String recipient = args[4];
                    String messageBody = args[5];
                    writer.println(functionID + " " + senderToken + " " + recipient + " " + messageBody);
                    System.out.println(reader.readLine());
                    break;

                case 4: // Προβολή εισερχόμενων μηνυμάτων
                    if (args.length < 4) {
                        System.out.println("Missing authToken for Show Inbox");
                        return;
                    }
                    int inboxAuthToken = Integer.parseInt(args[3]);
                    writer.println(functionID + " " + inboxAuthToken);
                    String inboxMessage;
                    while ((inboxMessage = reader.readLine()) != null) {
                        if (!inboxMessage.equals("END-OF-LIST") && !inboxMessage.equals("Invalid authToken")) {
                            System.out.println(inboxMessage);
                        } else {
                            if (inboxMessage.equals("Invalid authToken")){
                                System.out.println("Invalid authToken");
                            }
                            break;
                        }

                    }
                    break;

                case 5: // Ανάγνωση μηνύματος
                    if (args.length < 5) {
                        System.out.println("Usage: java Client <server_ip> <port> 5 <authToken> <message_id>");
                        return;
                    }
                    int readAuthToken = Integer.parseInt(args[3]);
                    int messageID = Integer.parseInt(args[4]);
                    writer.println(functionID + " " + readAuthToken + " " + messageID);
                    System.out.println(reader.readLine());
                    break;

                case 6: // Διαγραφή μηνύματος
                    if (args.length < 5) {
                        System.out.println("Usage: java Client <server_ip> <port> 6 <authToken> <message_id>");
                        return;
                    }
                    int deleteAuthToken = Integer.parseInt(args[3]);
                    int deleteMessageID = Integer.parseInt(args[4]);
                    writer.println(functionID + " " + deleteAuthToken + " " + deleteMessageID);
                    System.out.println(reader.readLine());
                    break;

                default:
                    System.out.println("Invalid function ID");
                    break;
            }
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}
