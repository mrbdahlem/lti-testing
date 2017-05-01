package lti;

import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

/**
 *
 */
public class Main {
    public static void main(String[] args) throws Exception {     
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new MyHandler());
        System.out.println(server.getAddress());
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            HttpRequestParser reqParams = new HttpRequestParser(t);
            
            Headers reqHead = t.getRequestHeaders();
            Headers respHead = t.getResponseHeaders();
            String response = "Yo";
            String method = t.getRequestMethod();
            
            System.out.println(t.getProtocol() + " " +  method + " Request: " + t.getRequestURI().toString());
            
            respHead.add("X-Frame-Options", "ALLOW-FROM https://canvas.instructure.com");
                                    
            switch(t.getRequestURI().toString()) { 
            case "/":
                try {
                    for (Entry<String, String> param : reqParams.getHeaderParams().entrySet()) {
                        System.out.println(param.getKey() + " = " + param.getValue());
                    }
                    for (Entry<String, String> param : reqParams.getParams().entrySet()) {
                        System.out.println(param.getKey() + " = " + param.getValue());
                    }
                    
                    URI resource = Main.class.getClassLoader().getResource("index.html").toURI();
                    Path index = Paths.get(resource);
                    byte[] bytes = Files.readAllBytes(index);
                    response = new String(bytes);
                    
                } catch (URISyntaxException ex) {
                    System.out.println("No");
                }
                break;
                
            default:
                response = "GoodBye";
            }
            
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
            
            System.out.println("--------------------------");
        }
    }

}
