import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
import java.util.Set;

/*Basic Features

    A web server that supports IP address filtering and direct traffic to fake websites.

Detailed Features

    Be able to parse HTTP Get request messages. Direct the traffic to different pages based on the
    host domain.  For example: if the http get request is “Get /account.html HTTP/1.1\r\nHost:
    www.paypal.com\r\n”, the server will send the file  .\paypal\bank.html back to the client.

    Be able to block HTTP request from certain IP address. Always send 404 files not found response
    back to this IP address.

    Support HTTP response message with status code 200, 400, 404

    Support text/html, image/jpg, application/pdf

    Main html index file

    (Optional) Support Mutlithreading.  */
public class WebController {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(8080);
        try {
            while (true) {

                // wait for a connection
                Socket client = server.accept();

                // loop through for multithreading
                new Thread(() -> {
                    try {
                        clientHandler(client);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

    }

    public static void clientHandler(Socket client) throws Exception {
            Scanner infrom = new Scanner(client.getInputStream());

            // get clients data output stream
            DataOutputStream outTo = new DataOutputStream(client.getOutputStream());

            String requestLine = infrom.nextLine();

            if(requestLine == null || requestLine.isEmpty()){
                error400(outTo);
                client.close();
                return;
            }

            // get the HTTP request message
            String host = "";
            while (infrom.hasNextLine()) {
                String line = infrom.nextLine();

                if (line.isEmpty()) {
                    break;
                }

                if (line.startsWith("Host: ")) {
                    host = line.split(" ")[1];
                }
            }

            // get the client's IP
            String clientIP = client.getInetAddress().getHostAddress();
            System.out.println(clientIP);

            // set a list of IPs that are blocked
            //---------------PLACE BLOCKED IPS HERE: v
            Set<String> blockedIPs = Set.of("");

            //auto return 404 if client is blocked
            if (blockedIPs.contains(clientIP)) {
                error404(outTo);
                client.close();
                return;
            }

            String[] tokens = requestLine.split(" ");

            //check for bad request
            if(tokens.length < 3){
                error400(outTo);
                client.close();
                return;
            }

            // routing logic to direct traffic
            String path = tokens[1]; // get the html end of a url
            System.out.println(path);
            System.out.println(host);
            String filePath = "";

            // if website is paypal, return paypal html filepath
            if (host.contains("paypal.com")) {
                filePath = "./paypal/bank.html";
            } else if (path.equals("/")) {
                filePath = "./index.html";
            } else {
                filePath = "." + path;
            }

            File file = new File(filePath);
            if (!file.exists()) {
                error404(outTo);
                client.close();
                return;
            }

            FileInputStream f = new FileInputStream(file);
            byte[] fData = f.readAllBytes();
            f.close();

            // support all content types
            String contType = "text/html";
            if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
                contType = "image/jpg";
            } else if (filePath.endsWith(".pdf")) {
                contType = "application/pdf";
            }

            // Send OK message (code 200)
            outTo.writeBytes("HTTP/1.0 200 OK\r\n");
            outTo.writeBytes("Content-Type: " + contType + "\r\n");
            outTo.writeBytes("Content-Length: " + fData.length + "\r\n");
            outTo.writeBytes("\r\n");
            outTo.write(fData);
    }

    // send error file not found (404)
    private static void error404(DataOutputStream out) throws Exception {
        String body = "<html><h1>404 Not Found</h1></html>";
        out.writeBytes("HTTP/1.0 404 Not Found\r\n");
        out.writeBytes("Content-Type: text/html\r\n");
        out.writeBytes("Content-Length: " + body.length() + "\r\n");
        out.writeBytes("\r\n");
        out.writeBytes(body);
    }

    // send error bad request (400)
    private static void error400(DataOutputStream out) throws Exception {
        String body = "<html><h1>400 Bad Request</h1></html>";
        out.writeBytes("HTTP/1.0 400 Bad Request\r\n");
        out.writeBytes("Content-Type: text/html\r\n");
        out.writeBytes("Content-Length: " + body.length() + "\r\n");
        out.writeBytes("\r\n");
        out.writeBytes(body);
    }
}