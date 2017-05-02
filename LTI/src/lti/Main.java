package lti;

import com.sun.net.httpserver.Headers;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;
import net.oauth.signature.OAuthSignatureMethod;

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
            
            System.out.println("--------------------------");
            
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
                    
                    String oauth_consumer_key = reqParams.getParam("oauth_consumer_key");
                    String resource_link_id = reqParams.getParam("resource_link_id");
                    if ( ! "basic-lti-launch-request".equals(reqParams.getParam("lti_message_type")) ||
                                    ! "LTI-1p0".equals(reqParams.getParam("lti_version")) ||
                                    oauth_consumer_key == null || resource_link_id == null ) {
                            
                            response = "Missing required parameter.";
                            t.sendResponseHeaders(400, response.length());
                            OutputStream os = t.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                            return;
                    }
                    
                    long oauthTime = Long.parseLong(reqParams.getParam("oauth_timestamp"));
                    java.util.Date timestamp = new java.util.Date((long)oauthTime * 1000);
                    System.out.println("Request timestamp: " + timestamp.toString());
                    
                    String oauth_secret = "81fdf8aa4c575cb3962474d243e86089";
                                                            
                    OAuthMessage oam = new OAuthMessage(reqParams);
                    OAuthValidator oav = new SimpleOAuthValidator();
                    OAuthConsumer cons = new OAuthConsumer("about:blank#OAuth+CallBack+NotUsed",oauth_consumer_key, oauth_secret, null);
                    OAuthAccessor acc = new OAuthAccessor(cons);
                    String base_string = null;
                    
                    try {
                            base_string = OAuthSignatureMethod.getBaseString(oam);
                    } catch (Exception e) {
                            base_string = null;
                    }

                    try {
                            oav.validateMessage(oam,acc);
                    } catch(Exception e) {
                            System.out.println("Provider failed to validate message");
                            System.out.println(e.getMessage());
                            if ( base_string != null ) System.out.println(base_string);
                            
                            response = "Launch data does not validate.";
                            t.sendResponseHeaders(400, response.length());
                            OutputStream os = t.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                            return;
                    }
                    
                    
                    URI resource = Main.class.getClassLoader().getResource("index.html").toURI();
                    Path index = Paths.get(resource);
                    byte[] bytes = Files.readAllBytes(index);
                    response = new String(bytes);
                    /*
                    if (reqParams.getParam("launch_presentation_return_url") != null) {
                        response = "";
                        
                        URI location = appendUri(reqParams.getParam("launch_presentation_return_url"),
                                                 "embed_type", 
                                                 "oembed");
                        location = appendUri(location.toString(), "url", "http://www.flickr.com/photos/bees/2341623661/");
                        location = appendUri(location.toString(), "endpoint", "http://www.flickr.com/services/oembed/");
                        
                        System.out.println(location.toString());
                        
                        respHead.add("Location", location.toString());
                        t.sendResponseHeaders(302, response.length());
                        OutputStream os = t.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                    */
                    if (reqParams.getParam("lis_outcome_service_url") != null) {
                        URI location = new URI(reqParams.getParam("lis_outcome_service_url"));
                        System.out.println(location.toString());
                    }
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
            
        }
    }
    
    public static URI appendUri(String uri, String key, String value) throws URISyntaxException {
        URI oldUri = new URI(uri);

        try {
            key = URLEncoder.encode(key, "UTF-8");
            //value = URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        String appendQuery = key + "=" + value;
                
        String newQuery = oldUri.getQuery();
        if (newQuery == null) {
            newQuery = appendQuery;
        } else {
            newQuery += "&" + appendQuery;  
        }

        URI newUri = new URI(oldUri.getScheme(), oldUri.getAuthority(),
                oldUri.getPath(), newQuery, oldUri.getFragment());

        return newUri;
    }

}
