package lti;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpsExchange;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

/**
 * Class for HTTP request parsing as defined by RFC 2612:
 * 
 * Request = Request-Line ; Section 5.1 (( general-header ; Section 4.5 |
 * request-header ; Section 5.3 | entity-header ) CRLF) ; Section 7.1 CRLF [
 * message-body ] ; Section 4.3
 * 
 * @author izelaya
 *
 */
public class HttpRequestParser {

    private String _requestLine;
    private Map<String, String> _requestHeaders;
    private StringBuffer _messagetBody;
    private HttpExchange _he;
    private Map<String, String> _params = new HashMap<>();
    private String _method;
    private URL _url;
    private InputStream _messageBodyStream;
        
    public HttpRequestParser(HttpExchange he) {
        _requestHeaders = new HashMap<>();
        _messagetBody = new StringBuffer();
        _he = he;
        _messageBodyStream = he.getRequestBody();
        
        _method = he.getRequestMethod();

        String protocol;
        if (he instanceof HttpsExchange) {
            protocol = "https";
        }
        else {
            protocol = "http";
        }
        
        try {
            setRequestLine(he.getRequestURI().toString());
            parseRequest();
                        
            _url = new URL(protocol + "://" +  _requestHeaders.get("Host"));
            _url = new URL(_url, he.getRequestURI().toString());
        } catch (IOException | HttpFormatException ex) {
        
        }
    }

    /**
     * Parse and HTTP request.
     * 
     * @param request
     *            String holding http request.
     * @throws IOException
     *             If an I/O error occurs reading the input stream.
     * @throws HttpFormatException
     *             If HTTP Request is malformed
     */
    private void parseRequest() throws IOException {//, HttpFormatException {
        
        Headers reqHead = _he.getRequestHeaders();

        for (Entry<String, List<String>> e : reqHead.entrySet()) {
            for (String v : e.getValue()) {
                _requestHeaders.put(e.getKey(), v);
            }
        }
        
        InputStream is = _he.getRequestBody();
        
        Scanner scan = new Scanner(is);
        
        while (scan.hasNextLine()) {
            parseMessageBody(scan.nextLine());
        }
    }

    /**
     * 
     * 5.1 Request-Line The Request-Line begins with a method token, followed by
     * the Request-URI and the protocol version, and ending with CRLF. The
     * elements are separated by SP characters. No CR or LF is allowed except in
     * the final CRLF sequence.
     * 
     * @return String with Request-Line
     */
    public String getRequestLine() {
        return _requestLine;
    }

    private void setRequestLine(String requestLine)
            throws HttpFormatException, UnsupportedEncodingException {
        if (requestLine == null || requestLine.length() == 0) {
            throw new HttpFormatException("Invalid Request-Line: " + requestLine);
        }
        _requestLine = requestLine;
        
        if (requestLine.contains("?")) {
            parseMessageBody(requestLine.substring(requestLine.indexOf("?")));
        }
    }

    private void parseMessageBody(String qry)
            throws UnsupportedEncodingException {
        String encoding = "ISO-8859-1";
        String defs[] = qry.split("[&]");
        
        for (String def: defs) {
            int ix = def.indexOf('=');
            String name;
            String value;
            if (ix < 0) {
                name = URLDecoder.decode(def, encoding);
                value = "";
            } else {
                name = URLDecoder.decode(def.substring(0, ix), encoding);
                value = URLDecoder.decode(def.substring(ix+1), encoding);
            }
            _params.put(name, value);
        }
    }

    /**
     * For list of available headers refer to sections: 4.5, 5.3, 7.1 of RFC 2616
     * @param headerName Name of header
     * @return String with the value of the header or null if not found.
     */
    public String getHeaderParam(String headerName){
        return _requestHeaders.get(headerName);
    }
    
    public Map<String, String> getHeaderParams(){
        return _requestHeaders;
    }
    
    public Map<String, String>getParams() {
        return _params;
    }
    
    public String getParam(String key) {
        return _params.get(key);
    }
    
    public String getMethod() {
        return _method;
    }
    
    public String getRequestURL() {
        return _url.toString();
    }
    
    public InputStream getBodyStream() {
        return _messageBodyStream;
    }
}