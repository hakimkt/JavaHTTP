import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.util.logging.Logger;

public class SimpleHttpFileServer {
    private static final Logger logger = Logger.getLogger(SimpleHttpFileServer.class.getName());

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: java SimpleHttpFileServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        // Create HTTP server and bind to all interfaces on specified port
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        System.out.println("Server started on port " + port);

        // Define root handler to serve files from current directory
        server.createContext("/", new FileHandler());
        server.setExecutor(null);  // default executor
        server.start();
    }

    static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestMethod = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(requestMethod)) {
                String filePath = "." + exchange.getRequestURI().getPath();
                Path path = Paths.get(filePath);

                if (Files.isDirectory(path)) {
                    // Directory listing
                    StringBuilder response = new StringBuilder();
                    File directory = path.toFile();
                    File[] files = directory.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            response.append("<a href=\"")
                                    .append(file.getName())
                                    .append("\">")
                                    .append(file.getName())
                                    .append("</a><br>");
                        }
                    }
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, response.length());
                    exchange.getResponseBody().write(response.toString().getBytes());
                    exchange.getResponseBody().close();
                } else if (Files.exists(path) && !Files.isDirectory(path)) {
                    // Log download request
                    logger.info("File downloaded: " + path.toAbsolutePath());

                    // Set response headers
                    exchange.getResponseHeaders().set("Content-Type", Files.probeContentType(path));
                    exchange.sendResponseHeaders(200, Files.size(path));

                    // Send file data
                    OutputStream os = exchange.getResponseBody();
                    InputStream is = Files.newInputStream(path);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    is.close();
                    os.close();
                } else {
                    // Send 404 if file not found
                    String response = "404 (Not Found)";
                    exchange.sendResponseHeaders(404, response.length());
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.getResponseBody().close();
                }
            }
        }
    }
}
